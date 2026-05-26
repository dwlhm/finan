package com.dwlhm.finan.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.ui.common.BaseActivity;
import com.dwlhm.finan.ui.common.FinanIntents;
import com.dwlhm.finan.ui.common.ServicesProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SettingsActivity extends BaseActivity {

  private ActivityResultLauncher<Intent> exportLauncher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    exportLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                return;
              }
              Uri uri = result.getData().getData();
              if (uri == null) {
                Toast.makeText(this, R.string.settings_export_failed, Toast.LENGTH_SHORT).show();
                return;
              }
              if (writeCsvExport(uri)) {
                Toast.makeText(this, R.string.settings_export_success, Toast.LENGTH_SHORT).show();
              } else {
                Toast.makeText(this, R.string.settings_export_failed, Toast.LENGTH_SHORT).show();
              }
            });
    super.onCreate(savedInstanceState);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_settings;
  }

  @Override
  protected void onReady() {
    Button exportButton = findViewById(R.id.settings_export);
    Button categoriesButton = findViewById(R.id.settings_categories);

    exportButton.setOnClickListener(v -> launchExportPicker());
    categoriesButton.setOnClickListener(
        v ->
            startActivity(
                FinanIntents.categories(this).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)));
  }

  private void launchExportPicker() {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("text/csv");
    intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.settings_export_filename));
    exportLauncher.launch(intent);
  }

  private boolean writeCsvExport(Uri destination) {
    List<Transaction> transactions =
        ServicesProvider.get(this).transactionGateway.findAll();
    String csv = ServicesProvider.get(this).exportService.toCsv(transactions);
    try (OutputStream out = getContentResolver().openOutputStream(destination)) {
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
