package com.dwlhm.finan.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ScreenNavigator;
import com.dwlhm.finan.ui.common.ServicesProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class SettingsFragment extends ScreenFragment {

  private ActivityResultLauncher<Intent> exportLauncher;

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
              if (writeCsvExport(uri)) {
                Toast.makeText(requireContext(), R.string.settings_export_success, Toast.LENGTH_SHORT)
                    .show();
              } else {
                Toast.makeText(requireContext(), R.string.settings_export_failed, Toast.LENGTH_SHORT)
                    .show();
              }
            });
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_settings;
  }

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    Button exportButton = view.findViewById(R.id.settings_export);
    Button categoriesButton = view.findViewById(R.id.settings_categories);

    exportButton.setOnClickListener(v -> launchExportPicker());
    categoriesButton.setOnClickListener(v -> openCategories());
  }

  private void launchExportPicker() {
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

  private boolean writeCsvExport(Uri destination) {
    List<Transaction> transactions =
        ServicesProvider.get(requireContext()).transactionGateway.findAll();
    String csv = ServicesProvider.get(requireContext()).exportService.toCsv(transactions);
    try (OutputStream out = requireContext().getContentResolver().openOutputStream(destination)) {
      if (out == null) {
        return false;
      }
      out.write(csv.getBytes(StandardCharsets.UTF_8));
      out.flush();
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
