package com.dwlhm.finan.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.dao.SummaryDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.service.export.ImportService;
import com.dwlhm.finan.ui.category.CategoryEditorDialog;
import com.dwlhm.finan.ui.category.CategoryOverviewBottomSheet;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.export.ExportDateRangeBottomSheet;
import com.dwlhm.finan.ui.common.ScreenNavigator;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.wallet.WalletInputDialog;
import com.dwlhm.finan.ui.wallet.WalletOverviewBottomSheet;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SettingsFragment extends ScreenFragment {

  private static final String MASKED_MODE_PREFS_KEY = "settings_wallet_masked_mode";

  private ActivityResultLauncher<Intent> exportLauncher;
  private ActivityResultLauncher<Intent> importLauncher;
  private Button exportButton;
  private Button importButton;
  private ProgressBar exportProgress;
  private TextView exportStatus;
  private boolean exportInProgress;
  private boolean importInProgress;
  private boolean maskedMode;
  @Nullable private Long exportStartDate;
  @Nullable private Long exportEndDate;
  private TextView modeNominal;
  private TextView modeMasked;
  private long cachedTotalBalanceMinor;
  private String cachedCurrencyCode;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    exportLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() != Activity.RESULT_OK
                  || result.getData() == null) {
                return;
              }
              Uri uri = result.getData().getData();
              if (uri == null) {
                Toast.makeText(requireContext(), R.string.settings_export_failed, Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              writeCsvExportAsync(uri);
            });
    importLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() != Activity.RESULT_OK
                  || result.getData() == null) {
                return;
              }
              Uri uri = result.getData().getData();
              if (uri == null) {
                Toast.makeText(requireContext(), R.string.settings_import_failed, Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              readCsvImportAsync(uri);
            });
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_settings;
  }

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    exportButton = view.findViewById(R.id.settings_export);
    importButton = view.findViewById(R.id.settings_import);
    exportProgress = view.findViewById(R.id.settings_export_progress);
    exportStatus = view.findViewById(R.id.settings_export_status);
    Button walletsButton = view.findViewById(R.id.settings_wallets);
    Button categoriesButton = view.findViewById(R.id.settings_categories);
    TextView createCategoryText = view.findViewById(R.id.settings_create_category_text);

    AppServices services = ServicesProvider.get(requireContext());

    exportButton.setOnClickListener(v -> launchExportPicker());
    importButton.setOnClickListener(v -> launchImportPicker());
    walletsButton.setOnClickListener(v -> openWallets());
    categoriesButton.setOnClickListener(v -> openCategories());
    createCategoryText.setOnClickListener(
        v ->
            new CategoryEditorDialog(
                requireContext(),
                services,
                null,
                0,
                saved -> loadTopCategories(),
                null));
    TextView createWalletText = view.findViewById(R.id.settings_create_wallet_text);
    createWalletText.setOnClickListener(v -> openWalletInputDialog());

    modeNominal = view.findViewById(R.id.settings_mode_nominal);
    modeMasked = view.findViewById(R.id.settings_mode_masked);
    maskedMode = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
        .getBoolean(MASKED_MODE_PREFS_KEY, false);
    updateModeToggle();
    modeNominal.setOnClickListener(v -> setMaskedMode(false));
    modeMasked.setOnClickListener(v -> setMaskedMode(true));
  }

  @Override
  public void onResume() {
    super.onResume();
    loadTopCategories();
    loadTopWallets();
  }

  private void loadTopCategories() {
    AppServices services = ServicesProvider.get(requireContext());
    services.dbWorker.compute(
        () -> {
          List<Category> cats = services.categoryDao.findAllForManage();
          List<SummaryDao.CategorySumRow> rows = services.summaryDao.expenseByCategory(0L, Long.MAX_VALUE);
          Map<Long, Long> totals = new HashMap<>();
          for (SummaryDao.CategorySumRow row : rows) {
            totals.put(row.categoryId, row.totalMinor);
          }
          return new CategoriesWithTotals(cats, totals);
        },
        result -> {
          if (!isAdded() || result == null) {
            return;
          }
          renderTopCategories(result.categories, result.totals);
        });
  }

  private void renderTopCategories(List<Category> allCategories, Map<Long, Long> totals) {
    View view = getView();
    if (view == null) return;

    GridLayout categoriesGrid = view.findViewById(R.id.settings_categories_grid);
    TextView categoriesEmpty = view.findViewById(R.id.settings_categories_empty);
    categoriesGrid.removeAllViews();

    if (allCategories.isEmpty()) {
      categoriesEmpty.setVisibility(View.VISIBLE);
      categoriesGrid.setVisibility(View.GONE);
      return;
    }

    categoriesEmpty.setVisibility(View.GONE);
    categoriesGrid.setVisibility(View.VISIBLE);

    LayoutInflater inflater = LayoutInflater.from(requireContext());
    float density = getResources().getDisplayMetrics().density;
    int margin = (int) (4 * density + 0.5f);
    String currencyCode = cachedCurrencyCode != null ? cachedCurrencyCode : "IDR";
    allCategories.sort((a, b) -> Long.compare(
        totals.getOrDefault(b.getId(), 0L),
        totals.getOrDefault(a.getId(), 0L)));
    int limit = Math.min(allCategories.size(), 6);

    for (int i = 0; i < limit; i++) {
      Category category = allCategories.get(i);
      View item = inflater.inflate(R.layout.item_settings_category_grid, categoriesGrid, false);

      if (category.isDefault()) {
        item.setBackgroundResource(R.drawable.bg_card_wallet_default);
      }

      TextView emojiView = item.findViewById(R.id.grid_category_emoji);
      TextView nameView = item.findViewById(R.id.grid_category_name);
      TextView usageView = item.findViewById(R.id.grid_category_usage);

      String icon = category.getIcon();
      emojiView.setText(icon == null || icon.trim().isEmpty() ? "📂" : icon);
      nameView.setText(category.getName());

      long total = totals.getOrDefault(category.getId(), 0L);
      usageView.setText(maskedMode ? "***" : MoneyFormatter.format(total));

      item.setOnClickListener(v -> openCategoryOverview(category));

      GridLayout.LayoutParams params = new GridLayout.LayoutParams(
          GridLayout.spec(GridLayout.UNDEFINED, 1f),
          GridLayout.spec(GridLayout.UNDEFINED, 1f)
      );
      params.width = 0;
      params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
      params.setMargins(margin, margin, margin, margin);
      item.setLayoutParams(params);

      categoriesGrid.addView(item);
    }
  }

  private static final class CategoriesWithTotals {
    private final List<Category> categories;
    private final Map<Long, Long> totals;

    private CategoriesWithTotals(List<Category> categories, Map<Long, Long> totals) {
      this.categories = categories;
      this.totals = totals;
    }
  }

  private void openCategoryOverview(Category category) {
    if (!(requireActivity() instanceof ScreenNavigator)) return;
    AppServices services = ServicesProvider.get(requireContext());
    new CategoryOverviewBottomSheet(
        requireContext(),
        services,
        category,
        this::loadTopCategories,
        (ScreenNavigator) requireActivity());
  }

  private void loadTopWallets() {
    AppServices services = ServicesProvider.get(requireContext());
    services.dbWorker.compute(
        () -> services.walletService.findAll(),
        wallets -> {
          if (!isAdded() || wallets == null) {
            return;
          }
          renderTopWallets(wallets);
        });
  }

  private void renderTopWallets(List<Wallet> allWallets) {
    View view = getView();
    if (view == null) return;

    GridLayout walletsGrid = view.findViewById(R.id.settings_wallets_grid);
    TextView walletsEmpty = view.findViewById(R.id.settings_wallets_empty);
    LinearLayout balanceRow = view.findViewById(R.id.settings_wallets_balance_row);
    TextView totalBalanceView = view.findViewById(R.id.settings_wallets_total_balance);
    walletsGrid.removeAllViews();

    long totalBalanceMinor = 0L;
    for (Wallet w : allWallets) {
      totalBalanceMinor = Math.addExact(totalBalanceMinor, w.getCachedBalanceMinor());
    }

    if (allWallets.isEmpty()) {
      walletsEmpty.setVisibility(View.VISIBLE);
      walletsGrid.setVisibility(View.GONE);
      balanceRow.setVisibility(View.GONE);
      return;
    }

    walletsEmpty.setVisibility(View.GONE);
    walletsGrid.setVisibility(View.VISIBLE);

    cachedCurrencyCode = allWallets.get(0).getCurrencyCode();
    cachedTotalBalanceMinor = totalBalanceMinor;
    updateTotalBalanceView(totalBalanceView);
    balanceRow.setVisibility(View.VISIBLE);

    LayoutInflater inflater = LayoutInflater.from(requireContext());
    float density = getResources().getDisplayMetrics().density;
    int margin = (int) (4 * density + 0.5f);
    int limit = Math.min(allWallets.size(), 6);

    for (int i = 0; i < limit; i++) {
      Wallet wallet = allWallets.get(i);
      View item = inflater.inflate(R.layout.item_settings_wallet_grid, walletsGrid, false);

      if (wallet.isDefault()) {
        item.setBackgroundResource(R.drawable.bg_card_wallet_default);
      }

      TextView iconView = item.findViewById(R.id.grid_wallet_icon);
      TextView nameView = item.findViewById(R.id.grid_wallet_name);
      TextView usageView = item.findViewById(R.id.grid_wallet_usage);

      String walletIcon = wallet.getIcon();
      if (walletIcon != null && !walletIcon.trim().isEmpty()) {
        iconView.setText(walletIcon);
      } else {
        iconView.setText(wallet.isDefault() ? "⭐" : "💳");
      }
      nameView.setText(wallet.getName());
      usageView.setText(maskedMode ? "***" : MoneyFormatter.format(wallet.getCachedBalanceMinor()));

      item.setOnClickListener(v ->
          openWalletOverview(wallet, allWallets));

      GridLayout.LayoutParams params = new GridLayout.LayoutParams(
          GridLayout.spec(GridLayout.UNDEFINED, 1f),
          GridLayout.spec(GridLayout.UNDEFINED, 1f));
      params.width = 0;
      params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
      params.setMargins(margin, margin, margin, margin);
      item.setLayoutParams(params);

      walletsGrid.addView(item);
    }
  }

  private void updateTotalBalanceView(TextView totalBalanceView) {
    if (maskedMode) {
      totalBalanceView.setText("***");
    } else {
      totalBalanceView.setText(
          MoneyFormatter.formatWithCurrencyCode(cachedCurrencyCode, cachedTotalBalanceMinor));
    }
  }

  private void setMaskedMode(boolean masked) {
    maskedMode = masked;
    requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean(MASKED_MODE_PREFS_KEY, masked)
        .apply();
    updateModeToggle();
    View view = getView();
    if (view != null) {
      updateTotalBalanceView(view.findViewById(R.id.settings_wallets_total_balance));
    }
    loadTopCategories();
    loadTopWallets();
  }

  private void updateModeToggle() {
    if (modeNominal == null || modeMasked == null) return;
    if (maskedMode) {
      modeNominal.setBackground(null);
      modeNominal.setTextColor(0xFF4A9E7F);
      modeMasked.setBackgroundResource(R.drawable.bg_toggle_active);
      modeMasked.setTextColor(0xFFFFFFFF);
    } else {
      modeNominal.setBackgroundResource(R.drawable.bg_toggle_active);
      modeNominal.setTextColor(0xFFFFFFFF);
      modeMasked.setBackground(null);
      modeMasked.setTextColor(0xFF4A9E7F);
    }
  }

  private void openWalletOverview(Wallet wallet, List<Wallet> allWallets) {
    AppServices services = ServicesProvider.get(requireContext());
    new WalletOverviewBottomSheet(
        requireContext(), services, wallet, allWallets, this::loadTopWallets);
  }

  private void openWalletInputDialog() {
    AppServices services = ServicesProvider.get(requireContext());
    new WalletInputDialog(requireContext(), services, this::loadTopWallets);
  }

  private void launchExportPicker() {
    if (exportInProgress) return;
    new ExportDateRangeBottomSheet(
        requireContext(), exportStartDate, exportEndDate,
        (start, end) -> {
          exportStartDate = start;
          exportEndDate = end;
          launchExportFilePicker();
        }).show();
  }

  private void launchExportFilePicker() {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("text/csv");
    intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.settings_export_filename));
    exportLauncher.launch(intent);
  }

  private void launchImportPicker() {
    if (importInProgress) {
      return;
    }
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("text/csv");
    intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.settings_export_filename));
    importLauncher.launch(intent);
  }

  private void openCategories() {
    if (requireActivity() instanceof ScreenNavigator) {
      ((ScreenNavigator) requireActivity()).openCategories();
    }
  }

  private void openWallets() {
    if (requireActivity() instanceof ScreenNavigator) {
      ((ScreenNavigator) requireActivity()).openWallets();
    }
  }

  private void setExportInProgress(boolean inProgress) {
    exportInProgress = inProgress;
    if (exportButton == null) {
      return;
    }
    exportButton.setEnabled(!inProgress);
    importButton.setEnabled(!inProgress);
    exportProgress.setVisibility(inProgress ? View.VISIBLE : View.GONE);
    exportStatus.setVisibility(inProgress ? View.VISIBLE : View.GONE);
  }

  private void setImportInProgress(boolean inProgress) {
    importInProgress = inProgress;
    if (importButton == null) {
      return;
    }
    importButton.setEnabled(!inProgress);
    exportButton.setEnabled(!inProgress);
    exportProgress.setVisibility(inProgress ? View.VISIBLE : View.GONE);
    exportStatus.setVisibility(inProgress ? View.VISIBLE : View.GONE);
  }

  private void writeCsvExportAsync(Uri destination) {
    AppServices services = ServicesProvider.get(requireContext());
    android.content.ContentResolver resolver = requireActivity().getContentResolver();
    setExportInProgress(true);
    services.dbWorker.compute(
        () -> {
          try (OutputStream out = resolver.openOutputStream(destination)) {
            if (out == null) {
              return Boolean.FALSE;
            }
            services.exportService.exportTo(
                out,
                services.walletService.findAll(),
                services.categoryDao.findAllOrdered(),
                services.transferDao.findAll(),
                exportStartDate,
                exportEndDate,
                services.transactionGateway);
            return Boolean.TRUE;
          } catch (IOException e) {
            return Boolean.FALSE;
          }
        },
        success -> {
          if (!isAdded()) {
            return;
          }
          setExportInProgress(false);
          if (Boolean.TRUE.equals(success)) {
            Toast.makeText(requireContext(), R.string.settings_export_success, Toast.LENGTH_SHORT)
                .show();
          } else {
            Toast.makeText(requireContext(), R.string.settings_export_failed, Toast.LENGTH_SHORT)
                .show();
          }
        });
  }

  private void readCsvImportAsync(Uri source) {
    AppServices services = ServicesProvider.get(requireContext());
    android.content.ContentResolver resolver = requireActivity().getContentResolver();
    setImportInProgress(true);
    services.dbWorker.compute(
        () -> {
          try (InputStream in = resolver.openInputStream(source)) {
            if (in == null) {
              return ImportService.ImportResult.failure("Cannot open file");
            }
            return services.importService.importFrom(in);
          } catch (IOException e) {
            return ImportService.ImportResult.failure(e.getMessage());
          }
        },
        result -> {
          if (!isAdded()) {
            return;
          }
          setImportInProgress(false);
          if (result != null && result.isSuccess()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.settings_import_success, result.getTotalImported()),
                Toast.LENGTH_SHORT).show();
            loadTopCategories();
            loadTopWallets();
          } else {
            String error = result != null ? result.getError() : null;
            Toast.makeText(
                requireContext(),
                error != null ? error : getString(R.string.settings_import_failed),
                Toast.LENGTH_SHORT).show();
          }
        });
  }
}
