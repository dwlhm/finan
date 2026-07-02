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
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.ui.category.CategoryEditorDialog;
import com.dwlhm.finan.ui.category.CategoryOverviewBottomSheet;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ScreenNavigator;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.wallet.WalletInputDialog;
import com.dwlhm.finan.ui.wallet.WalletOverviewBottomSheet;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class SettingsFragment extends ScreenFragment {

  private static final String MASKED_MODE_PREFS_KEY = "settings_wallet_masked_mode";

  private ActivityResultLauncher<Intent> exportLauncher;
  private Button exportButton;
  private ProgressBar exportProgress;
  private TextView exportStatus;
  private boolean exportInProgress;
  private boolean maskedMode;
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
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_settings;
  }

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    exportButton = view.findViewById(R.id.settings_export);
    exportProgress = view.findViewById(R.id.settings_export_progress);
    exportStatus = view.findViewById(R.id.settings_export_status);
    Button walletsButton = view.findViewById(R.id.settings_wallets);
    Button categoriesButton = view.findViewById(R.id.settings_categories);
    TextView createCategoryText = view.findViewById(R.id.settings_create_category_text);

    AppServices services = ServicesProvider.get(requireContext());

    exportButton.setOnClickListener(v -> launchExportPicker());
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
        () -> services.categoryDao.findAllForManage(),
        categories -> {
          if (!isAdded() || categories == null) {
            return;
          }
          renderTopCategories(categories);
        });
  }

  private void renderTopCategories(List<Category> allCategories) {
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
    int limit = Math.min(allCategories.size(), 6);

    for (int i = 0; i < limit; i++) {
      Category category = allCategories.get(i);
      View item = inflater.inflate(R.layout.item_settings_category_grid, categoriesGrid, false);
      
      TextView emojiView = item.findViewById(R.id.grid_category_emoji);
      TextView nameView = item.findViewById(R.id.grid_category_name);
      TextView usageView = item.findViewById(R.id.grid_category_usage);

      String icon = category.getIcon();
      emojiView.setText(icon == null || icon.trim().isEmpty() ? "📂" : icon);
      nameView.setText(category.getName());
      
      int usage = category.getUsageCount();
      usageView.setText(usage + "x");

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
        () -> services.walletDao.findAll(),
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
      usageView.setText(wallet.getUsageCount() + "x");

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
    if (exportInProgress) {
      return;
    }
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("text/csv");
    intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.settings_export_filename));
    exportLauncher.launch(intent);
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
    exportProgress.setVisibility(inProgress ? View.VISIBLE : View.GONE);
    exportStatus.setVisibility(inProgress ? View.VISIBLE : View.GONE);
  }

  private void writeCsvExportAsync(Uri destination) {
    AppServices services = ServicesProvider.get(requireContext());
    android.content.ContentResolver resolver = requireContext().getContentResolver();
    setExportInProgress(true);
    services.dbWorker.compute(
        () -> {
          try (OutputStream out = resolver.openOutputStream(destination)) {
            if (out == null) {
              return Boolean.FALSE;
            }
            services.exportService.exportTo(
                out, services.walletDao.findAll(), services.transactionGateway);
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
}
