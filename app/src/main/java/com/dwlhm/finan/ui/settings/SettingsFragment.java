package com.dwlhm.finan.ui.settings;

import android.app.Activity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import androidx.appcompat.widget.SwitchCompat;
import com.dwlhm.finan.data.prefs.DefaultsStore;
import com.dwlhm.finan.ui.capture.AmountShortcutDialog;
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
  private View exportButton;
  private View importButton;
  private ProgressBar exportProgress;
  private TextView exportStatus;
  private boolean exportInProgress;
  private boolean importInProgress;
  private boolean maskedMode;
  @Nullable private Long exportStartDate;
  @Nullable private Long exportEndDate;
  private TextView payrollCycleValue;
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
    View walletsButton = view.findViewById(R.id.settings_wallets);
    View categoriesButton = view.findViewById(R.id.settings_categories);
    if (walletsButton != null) {
      walletsButton.setOnClickListener(v -> openWallets());
    }
    if (categoriesButton != null) {
      categoriesButton.setOnClickListener(v -> openCategories());
    }
    AppServices services = ServicesProvider.get(requireContext());

    if (exportButton != null) {
      exportButton.setOnClickListener(v -> launchExportPicker());
    }
    if (importButton != null) {
      importButton.setOnClickListener(v -> launchImportPicker());
    }
    payrollCycleValue = view.findViewById(R.id.settings_payroll_cycle_value);
    View payrollCycleButton = view.findViewById(R.id.settings_payroll_cycle);
    if (payrollCycleButton != null) {
      payrollCycleButton.setOnClickListener(v -> showPayrollCyclePicker());
    }
    updatePayrollCycleValue();

    View shortcutsButton = view.findViewById(R.id.settings_amount_shortcuts);
    if (shortcutsButton != null) {
      shortcutsButton.setOnClickListener(v -> openAmountShortcutsDialog());
    }

    SharedPreferences prefs = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE);

    SwitchCompat sensorSwitch = view.findViewById(R.id.settings_sensor_mode_switch);
    if (sensorSwitch != null) {
      sensorSwitch.setChecked(maskedMode);
      sensorSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
        if (isChecked != maskedMode) {
          setMaskedMode(isChecked);
        }
      });
    }

    SwitchCompat summarySwitch = view.findViewById(R.id.settings_summary_mode_switch);
    if (summarySwitch != null) {
      boolean summaryMode = prefs.getBoolean("summary_percentage_mode", false);
      summarySwitch.setChecked(summaryMode);
      summarySwitch.setOnCheckedChangeListener((btn, isChecked) ->
          prefs.edit().putBoolean("summary_percentage_mode", isChecked).apply());
    }

    SwitchCompat dashboardSwitch = view.findViewById(R.id.settings_dashboard_mode_switch);
    if (dashboardSwitch != null) {
      boolean dashboardMode = prefs.getBoolean("pref_display_mode", false);
      dashboardSwitch.setChecked(dashboardMode);
      dashboardSwitch.setOnCheckedChangeListener((btn, isChecked) ->
          prefs.edit()
              .putBoolean("pref_display_mode", isChecked)
              .putInt("dashboard_display_mode", isChecked ? 1 : 0)
              .apply());
    }

    View resetButton = view.findViewById(R.id.settings_reset_data);
    if (resetButton != null) {
      resetButton.setOnClickListener(v -> showResetDataConfirmationDialog());
    }
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  private void openCategoryOverview(Category category) {
    if (!(requireActivity() instanceof ScreenNavigator)) return;
    AppServices services = ServicesProvider.get(requireContext());
    new CategoryOverviewBottomSheet(
        requireContext(),
        services,
        category,
        null,
        (ScreenNavigator) requireActivity());
  }

  private void setMaskedMode(boolean masked) {
    maskedMode = masked;
    requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean(MASKED_MODE_PREFS_KEY, masked)
        .apply();
    View view = getView();
    if (view != null) {
      SwitchCompat sensorSwitch = view.findViewById(R.id.settings_sensor_mode_switch);
      if (sensorSwitch != null && sensorSwitch.isChecked() != masked) {
        sensorSwitch.setChecked(masked);
      }
    }
  }

  private void openWalletOverview(Wallet wallet, List<Wallet> allWallets) {
    AppServices services = ServicesProvider.get(requireContext());
    new WalletOverviewBottomSheet(
        requireContext(), services, wallet, allWallets, () -> {});
  }

  private void openWalletInputDialog() {
    AppServices services = ServicesProvider.get(requireContext());
    new WalletInputDialog(requireContext(), services, () -> {});
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

  private void updatePayrollCycleValue() {
    if (payrollCycleValue == null) {
      return;
    }
    int cutoffDay = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
        .getInt("cutoff_day", 1);
    payrollCycleValue.setText(getPayrollCycleLabel(cutoffDay));
  }

  private void showPayrollCyclePicker() {
    final String[] options = {
        "Tanggal 1 (Awal Bulan)", "Tanggal 5", "Tanggal 10", "Tanggal 15",
        "Tanggal 20", "Tanggal 25", "Tanggal 28",
        "Hari Kerja Terakhir (Akhir Bulan)"
    };
    final int[] values = {1, 5, 10, 15, 20, 25, 28, -1};

    int currentCutoff = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
        .getInt("cutoff_day", 1);
    int selectedIndex = 0;
    for (int i = 0; i < values.length; i++) {
      if (values[i] == currentCutoff) {
        selectedIndex = i;
        break;
      }
    }

    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.settings_change_payroll_cycle)
        .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
          int newCutoff = values[which];
          requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
              .edit()
              .putInt("cutoff_day", newCutoff)
              .apply();
          updatePayrollCycleValue();
          dialog.dismiss();
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private static String getPayrollCycleLabel(int cutoffDay) {
    if (cutoffDay == -1) {
      return "Hari Kerja Terakhir";
    }
    if (cutoffDay == 1) {
      return "Tanggal 1";
    }
    return "Tanggal " + cutoffDay;
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
          } else {
            String error = result != null ? result.getError() : null;
            Toast.makeText(
                requireContext(),
                error != null ? error : getString(R.string.settings_import_failed),
                Toast.LENGTH_SHORT).show();
          }
        });
  }

  private void openAmountShortcutsDialog() {
    AppServices services = ServicesProvider.get(requireContext());
    DefaultsStore defaultsStore = services.defaultsStore != null
        ? services.defaultsStore
        : new DefaultsStore(requireContext());
    List<Long> shortcuts = defaultsStore.getAmountShortcuts();
    new AmountShortcutDialog(
        requireContext(),
        shortcuts,
        newShortcuts -> defaultsStore.setAmountShortcuts(newShortcuts))
        .show();
  }

  private void showResetDataConfirmationDialog() {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.settings_reset_data_confirm_title)
        .setMessage(R.string.settings_reset_data_confirm_msg)
        .setPositiveButton(R.string.hc_dialog_confirm_delete_hapus, (dialog, which) -> resetAllData())
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void resetAllData() {
    AppServices services = ServicesProvider.get(requireContext());
    services.dbWorker.compute(
        () -> {
          try {
            SQLiteDatabase db = services.databaseHelper.getWritableDatabase();
            db.execSQL("DELETE FROM transfers");
            db.execSQL("DELETE FROM transactions");
            db.execSQL("DELETE FROM wallets WHERE is_default = 0 OR is_default IS NULL");
            db.execSQL("UPDATE wallets SET cached_balance_minor = 0, opening_balance_minor = 0");
            android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM wallets", null);
            boolean hasWallet = false;
            if (cursor != null) {
              if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                hasWallet = true;
              }
              cursor.close();
            }
            if (!hasWallet) {
              long now = System.currentTimeMillis();
              db.execSQL("INSERT INTO wallets (name, currency_code, is_default, cached_balance_minor, opening_balance_minor, created_at) "
                  + "VALUES ('Dompet Utama', 'IDR', 1, 0, 0, " + now + ")");
            }
            db.execSQL("DELETE FROM categories");
            db.execSQL("INSERT INTO categories (name, type_filter, sort_order, usage_count, last_used_at, cash_flow_activity, is_default) VALUES "
                + "('Makanan', 'EXPENSE', 0, 0, 0, 'OPERATING', 1), "
                + "('Transport', 'EXPENSE', 1, 0, 0, 'OPERATING', 0), "
                + "('Kopi', 'EXPENSE', 2, 0, 0, 'OPERATING', 0), "
                + "('Tagihan', 'EXPENSE', 3, 0, 0, 'OPERATING', 0), "
                + "('Belanja', 'EXPENSE', 4, 0, 0, 'OPERATING', 0), "
                + "('Gaji', 'INCOME', 5, 0, 0, 'OPERATING', 0), "
                + "('Lainnya', 'BOTH', 6, 0, 0, 'UNCLASSIFIED', 0)");
            return Boolean.TRUE;
          } catch (Exception e) {
            return Boolean.FALSE;
          }
        },
        success -> {
          if (!isAdded()) {
            return;
          }
          if (Boolean.TRUE.equals(success)) {
            Toast.makeText(requireContext(), R.string.settings_reset_data_success, Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(requireContext(), R.string.settings_export_failed, Toast.LENGTH_SHORT).show();
          }
        });
  }
}
