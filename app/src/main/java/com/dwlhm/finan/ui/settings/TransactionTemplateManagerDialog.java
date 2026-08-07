package com.dwlhm.finan.ui.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.transaction.TransactionTemplateService;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyParser;

import java.util.List;

public final class TransactionTemplateManagerDialog extends BottomSheetDialog {

  private final AppServices services;
  private final Runnable onDismissListener;

  private LinearLayout listContainer;
  private TextView emptyView;

  public TransactionTemplateManagerDialog(
      @NonNull Context context,
      @NonNull AppServices services,
      @Nullable Runnable onDismissListener) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.services = services;
    this.onDismissListener = onDismissListener;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_transaction_template_manager);

    listContainer = findViewById(R.id.template_manager_list_container);
    emptyView = findViewById(R.id.template_manager_empty_view);

    Button btnAdd = findViewById(R.id.template_manager_btn_add);
    if (btnAdd != null) {
      btnAdd.setOnClickListener(v -> showEditDialog(null));
    }

    setOnDismissListener(dialog -> {
      if (onDismissListener != null) {
        onDismissListener.run();
      }
    });

    refreshList();
  }

  private void refreshList() {
    if (listContainer == null) return;
    listContainer.removeAllViews();

    List<TransactionTemplate> templates = services.transactionTemplateDao.findAll();
    if (templates.isEmpty()) {
      if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
      return;
    }

    if (emptyView != null) emptyView.setVisibility(View.GONE);

    LayoutInflater inflater = LayoutInflater.from(getContext());
    for (TransactionTemplate template : templates) {
      View itemView = inflater.inflate(R.layout.item_transaction_template_manage, listContainer, false);
      TextView iconView = itemView.findViewById(R.id.manage_template_icon);
      TextView nameView = itemView.findViewById(R.id.manage_template_name);
      TextView detailsView = itemView.findViewById(R.id.manage_template_details);
      ImageButton btnEdit = itemView.findViewById(R.id.manage_template_btn_edit);
      ImageButton btnDelete = itemView.findViewById(R.id.manage_template_btn_delete);

      iconView.setText(template.getIcon());
      nameView.setText(template.getName());

      String amountStr = template.getAmountMinor() > 0
          ? MoneyFormatter.format(template.getAmountMinor())
          : "Nominal Fleksibel";
      detailsView.setText(TransactionTemplateService.getTypeDisplayName(template.getType()) + " • " + amountStr);

      btnEdit.setOnClickListener(v -> showEditDialog(template));
      btnDelete.setOnClickListener(v -> confirmDelete(template));

      listContainer.addView(itemView);
    }
  }

  private void confirmDelete(TransactionTemplate template) {
    BottomSheetHelper.showConfirmation(
        getContext(),
        "Hapus Shortcut",
        "Apakah kamu yakin ingin menghapus shortcut \"" + template.getName() + "\"?",
        "Hapus",
        null,
        "Batal",
        () -> {
          services.transactionTemplateDao.delete(template.getId());
          Toast.makeText(getContext(), "Shortcut dihapus", Toast.LENGTH_SHORT).show();
          refreshList();
        });
  }

  private void showEditDialog(@Nullable TransactionTemplate existingTemplate) {
    TransactionTemplateEditorDialog dialog = new TransactionTemplateEditorDialog(
        getContext(), services, existingTemplate, this::refreshList
    );
    dialog.show();
  }
}
