package com.dwlhm.finan.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ScreenNavigator;
import com.dwlhm.finan.ui.common.ServicesProvider;

import java.io.IOException;
import java.io.OutputStream;

public final class SettingsFragment extends ScreenFragment {

  private ActivityResultLauncher<Intent> exportLauncher;
  private Button exportButton;
  private ProgressBar exportProgress;
  private TextView exportStatus;
  private boolean exportInProgress;

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

    exportButton.setOnClickListener(v -> launchExportPicker());
    walletsButton.setOnClickListener(v -> openWallets());
    categoriesButton.setOnClickListener(v -> openCategories());
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
