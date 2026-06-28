package com.dwlhm.finan.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.ui.category.CategoryEditorDialog;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ScreenNavigator;
import com.dwlhm.finan.ui.common.ServicesProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
  }

  @Override
  public void onResume() {
    super.onResume();
    loadTopCategories();
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

    List<Category> topCategories = new ArrayList<>();
    for (Category category : allCategories) {
      topCategories.add(category);
      if (topCategories.size() == 6) {
        break;
      }
    }

    if (topCategories.isEmpty()) {
      categoriesEmpty.setVisibility(View.VISIBLE);
      categoriesGrid.setVisibility(View.GONE);
      return;
    }

    categoriesEmpty.setVisibility(View.GONE);
    categoriesGrid.setVisibility(View.VISIBLE);

    LayoutInflater inflater = LayoutInflater.from(requireContext());
    float density = getResources().getDisplayMetrics().density;
    int margin = (int) (4 * density + 0.5f);

    for (Category category : topCategories) {
      View item = inflater.inflate(R.layout.item_settings_category_grid, categoriesGrid, false);
      
      TextView emojiView = item.findViewById(R.id.grid_category_emoji);
      TextView nameView = item.findViewById(R.id.grid_category_name);
      TextView usageView = item.findViewById(R.id.grid_category_usage);

      String icon = category.getIcon();
      emojiView.setText(icon == null || icon.trim().isEmpty() ? "📂" : icon);
      nameView.setText(category.getName());
      
      int usage = category.getUsageCount();
      usageView.setText(usage + "x");

      item.setOnClickListener(v -> openCategoryEditor(category));

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

  private void openCategoryEditor(Category category) {
    AppServices services = ServicesProvider.get(requireContext());
    services.dbWorker.compute(
        () -> services.categoryDao.countTransactions(category.getId()),
        count -> {
          if (isAdded() && count != null) {
            new CategoryEditorDialog(
                requireContext(),
                services,
                category,
                count,
                saved -> loadTopCategories(),
                () -> {
                    if (requireActivity() instanceof ScreenNavigator) {
                      ((ScreenNavigator) requireActivity())
                          .openHistoryForCategory(category.getId());
                    }
                }
            );
          }
        });
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
