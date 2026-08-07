package com.dwlhm.finan.ui.category;

import android.annotation.SuppressLint;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.ScreenNavigator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public final class CategoryOverviewBottomSheet extends BottomSheetDialog {

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
    TextView defaultBadge = findViewById(R.id.category_overview_default_badge);
    TextView typeBadge = findViewById(R.id.category_overview_type_badge);
    TextView usageView = findViewById(R.id.category_overview_usage);
    TextView lastUsedView = findViewById(R.id.category_overview_last_used);
    View lastUsedRow = findViewById(R.id.category_overview_last_used_row);
    TextView activityView = findViewById(R.id.category_overview_activity);

    String icon = category.getIcon();
    if (iconView != null) iconView.setText(icon == null || icon.trim().isEmpty() ? "📂" : icon);
    if (nameView != null) nameView.setText(category.getName());
    if (defaultBadge != null) defaultBadge.setVisibility(category.isDefault() ? View.VISIBLE : View.GONE);

    if (typeBadge != null) {
      String typeFilter = category.getTypeFilter();
      if ("EXPENSE".equals(typeFilter)) {
        typeBadge.setText(R.string.java_CategoryOverviewBottomSheet_pengeluaran);
      } else if ("INCOME".equals(typeFilter)) {
        typeBadge.setText(R.string.java_CategoryOverviewBottomSheet_pemasukan);
      } else {
        typeBadge.setText(R.string.java_CategoryOverviewBottomSheet_campuran);
      }
    }

    if (usageView != null) usageView.setText(category.getUsageCount() + "x");

    Long lastUsedAt = category.getLastUsedAt();
    if (lastUsedAt != null && lastUsedAt > 0L) {
      if (lastUsedRow != null) lastUsedRow.setVisibility(View.VISIBLE);
      String dateStr =
          new SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"))
              .format(new Date(lastUsedAt));
      if (lastUsedView != null) lastUsedView.setText(dateStr);
    }

    if (activityView != null) {
      String activity = category.getCashFlowActivity();
      if ("OPERATING".equals(activity)) {
        activityView.setText(R.string.java_CategoryOverviewBottomSheet_aktivitas_harian);
      } else if ("INVESTING".equals(activity)) {
        activityView.setText(R.string.java_CategoryOverviewBottomSheet_aset_investasi);
      } else if ("FINANCING".equals(activity)) {
        activityView.setText(R.string.java_CategoryOverviewBottomSheet_pendanaan);
      } else {
        activityView.setText(R.string.java_CategoryOverviewBottomSheet_belum_diklasifikasi);
      }
    }

    TextView editAction = findViewById(R.id.category_action_edit);
    TextView viewTransactionsAction = findViewById(R.id.category_action_view_transactions);
    TextView deleteAction = findViewById(R.id.category_action_delete);

    if (editAction != null) editAction.setOnClickListener(v -> openEditor());
    if (viewTransactionsAction != null) {
      viewTransactionsAction.setOnClickListener(v -> {
        dismiss();
        navigator.openHistoryForCategory(category.getId());
      });
    }
    if (deleteAction != null) deleteAction.setOnClickListener(v -> confirmDelete());
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
              () -> navigator.openHistoryForCategory(category.getId()));
        });
  }

  private void confirmDelete() {
    services.dbWorker.compute(
        () -> services.categoryDao.countTransactions(category.getId()),
        count -> {
          if (!isShowing() || count == null) return;
          String message;
          if (count > 0) {
            message = "Kategori ini digunakan dalam " + count + " transaksi. Menghapus kategori ini juga akan memengaruhi transaksi tersebut. Hapus?";
          } else {
            message = "Apakah Anda yakin ingin menghapus kategori \"" + category.getName() + "\"?";
          }
          BottomSheetHelper.showConfirmation(
              getContext(),
              "Hapus Kategori",
              message,
              "Hapus",
              null,
              getContext().getString(android.R.string.cancel),
              this::deleteCategory);
        });
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
