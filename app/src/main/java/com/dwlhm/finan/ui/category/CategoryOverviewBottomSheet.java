package com.dwlhm.finan.ui.category;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.ScreenNavigator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CategoryOverviewBottomSheet extends Dialog {

  private final AppServices services;
  private final Category category;
  private final Runnable onDataChanged;
  private final ScreenNavigator navigator;

  public CategoryOverviewBottomSheet(
      @NonNull Context context,
      @NonNull AppServices services,
      @NonNull Category category,
      @NonNull Runnable onDataChanged,
      @NonNull ScreenNavigator navigator) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.services = services;
    this.category = category;
    this.onDataChanged = onDataChanged;
    this.navigator = navigator;
    setContentView(R.layout.dialog_category_overview);
    setCancelable(true);
    setupViews();
  }

  private void setupViews() {
    BottomSheetHelper.show(this);

    TextView iconView = findViewById(R.id.category_overview_icon);
    TextView nameView = findViewById(R.id.category_overview_name);
    TextView typeBadge = findViewById(R.id.category_overview_type_badge);
    TextView usageView = findViewById(R.id.category_overview_usage);
    TextView lastUsedView = findViewById(R.id.category_overview_last_used);
    View lastUsedRow = findViewById(R.id.category_overview_last_used_row);
    TextView activityView = findViewById(R.id.category_overview_activity);

    String icon = category.getIcon();
    iconView.setText(icon == null || icon.trim().isEmpty() ? "📂" : icon);
    nameView.setText(category.getName());

    String typeFilter = category.getTypeFilter();
    if ("EXPENSE".equals(typeFilter)) {
      typeBadge.setText("Pengeluaran");
    } else if ("INCOME".equals(typeFilter)) {
      typeBadge.setText("Pemasukan");
    } else {
      typeBadge.setText("Campuran");
    }

    usageView.setText(category.getUsageCount() + "x");

    Long lastUsedAt = category.getLastUsedAt();
    if (lastUsedAt != null && lastUsedAt > 0L) {
      lastUsedRow.setVisibility(View.VISIBLE);
      String dateStr =
          new SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"))
              .format(new Date(lastUsedAt));
      lastUsedView.setText(dateStr);
    }

    String activity = category.getCashFlowActivity();
    if ("OPERATING".equals(activity)) {
      activityView.setText("Aktivitas harian");
    } else if ("INVESTING".equals(activity)) {
      activityView.setText("Aset & investasi");
    } else if ("FINANCING".equals(activity)) {
      activityView.setText("Pendanaan");
    } else {
      activityView.setText("Belum diklasifikasi");
    }

    TextView editAction = findViewById(R.id.category_action_edit);
    TextView viewTransactionsAction = findViewById(R.id.category_action_view_transactions);
    TextView deleteAction = findViewById(R.id.category_action_delete);

    editAction.setOnClickListener(v -> openEditor());
    viewTransactionsAction.setOnClickListener(v -> {
      dismiss();
      navigator.openHistoryForCategory(category.getId());
    });
    deleteAction.setOnClickListener(v -> confirmDelete());
  }

  private void openEditor() {
    services.dbWorker.compute(
        () -> services.categoryDao.countTransactions(category.getId()),
        count -> {
          if (!isShowing() || count == null) return;
          dismiss();
          new CategoryEditorDialog(
              getContext(),
              services,
              category,
              count,
              saved -> onDataChanged.run(),
              () -> {
                navigator.openHistoryForCategory(category.getId());
              });
        });
  }

  private void confirmDelete() {
    new AlertDialog.Builder(getContext())
        .setTitle("Hapus kategori")
        .setMessage(
            "Apakah Anda yakin ingin menghapus kategori \""
                + category.getName()
                + "\"?")
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton("Hapus", (d, which) -> deleteCategory())
        .show();
  }

  private void deleteCategory() {
    services.dbWorker.compute(
        () -> services.categoryDao.delete(category.getId()),
        success -> {
          if (!isShowing()) return;
          if (Boolean.TRUE.equals(success)) {
            Toast.makeText(getContext(), "Kategori dihapus", Toast.LENGTH_SHORT).show();
            dismiss();
            onDataChanged.run();
          } else {
            Toast.makeText(getContext(), "Gagal menghapus kategori", Toast.LENGTH_SHORT).show();
          }
        });
  }
}
