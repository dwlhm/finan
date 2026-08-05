package com.dwlhm.finan.ui.settings;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.transaction.TransactionTemplateService;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.EmojiPickerBottomSheet;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;
public final class TransactionTemplateEditorDialog extends BottomSheetDialog {

  private final AppServices services;
  @Nullable private final TransactionTemplate existingTemplate;
  private final Runnable onSavedCallback;

  private String selectedIcon = "⚡";
  private TransactionType selectedType = TransactionType.EXPENSE;

  public TransactionTemplateEditorDialog(
      @NonNull Context context,
      @NonNull AppServices services,
      @Nullable TransactionTemplate existingTemplate,
      @Nullable Runnable onSavedCallback) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.services = services;
    this.existingTemplate = existingTemplate;
    this.onSavedCallback = onSavedCallback;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_edit_template);

    TextView titleView = findViewById(R.id.dialog_edit_template_title);
    FrameLayout iconContainer = findViewById(R.id.template_icon_container);
    TextView iconPreview = findViewById(R.id.template_icon_preview);
    LabeledEditTextView nameField = findViewById(R.id.template_name_field);

    LinearLayout expenseCard = findViewById(R.id.template_type_expense_card);
    LinearLayout incomeCard = findViewById(R.id.template_type_income_card);
    LinearLayout transferCard = findViewById(R.id.template_type_transfer_card);
    TextView expenseLabel = findViewById(R.id.template_type_expense_label);
    TextView incomeLabel = findViewById(R.id.template_type_income_label);
    TextView transferLabel = findViewById(R.id.template_type_transfer_label);

    LabeledEditTextView amountField = findViewById(R.id.template_amount_field);
    LabeledEditTextView noteField = findViewById(R.id.template_note_field);
    Button deleteButton = findViewById(R.id.template_delete);
    DialogActionsView actionsView = findViewById(R.id.template_editor_actions);

    if (amountField != null && amountField.getEditText() != null) {
      MoneyInputFormatter.attach(amountField.getEditText(), true);
    }
    boolean editing = existingTemplate != null && existingTemplate.getId() > 0;
    if (titleView != null) {
      titleView.setText(editing ? "Edit Shortcut Transaksi" : "Tambah Shortcut Transaksi");
    }
    if (actionsView != null) {
      actionsView.setPrimaryText(editing ? "Simpan Perubahan" : "Simpan");
      actionsView.setOnPrimaryClickListener(v -> saveShortcut(nameField, amountField, noteField));
      actionsView.setOnCancelClickListener(v -> dismiss());
    }
    if (existingTemplate != null) {
      selectedIcon = existingTemplate.getIcon() != null && !existingTemplate.getIcon().isEmpty()
          ? existingTemplate.getIcon()
          : "⚡";
      if (existingTemplate.getType() != null) {
        selectedType = existingTemplate.getType();
      }
      if (nameField != null && nameField.getEditText() != null) {
        nameField.getEditText().setText(existingTemplate.getName());
      }
      if (amountField != null && amountField.getEditText() != null && existingTemplate.getAmountMinor() > 0) {
        amountField.getEditText().setText(MoneyFormatter.format(existingTemplate.getAmountMinor()));
      }
      if (noteField != null && noteField.getEditText() != null && existingTemplate.getNote() != null) {
        noteField.getEditText().setText(existingTemplate.getNote());
      }
      if (editing && deleteButton != null) {
        deleteButton.setVisibility(View.VISIBLE);
        deleteButton.setOnClickListener(v -> confirmDelete());
      } else if (deleteButton != null) {
        deleteButton.setVisibility(View.GONE);
      }
    }

    if (iconPreview != null) {
      iconPreview.setText(selectedIcon);
    }

    if (iconContainer != null) {
      iconContainer.setOnClickListener(v -> {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
          imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        EmojiPickerBottomSheet picker = new EmojiPickerBottomSheet(
            getContext(),
            selectedIcon,
            emoji -> {
              selectedIcon = emoji;
              if (iconPreview != null) {
                iconPreview.setText(emoji);
              }
            });
        BottomSheetHelper.show(picker);
      });
    }

    updateTypeCardsUI(expenseCard, expenseLabel, incomeCard, incomeLabel, transferCard, transferLabel);

    View.OnClickListener typeClickListener = v -> {
      if (v == expenseCard) {
        selectedType = TransactionType.EXPENSE;
      } else if (v == incomeCard) {
        selectedType = TransactionType.INCOME;
      } else if (v == transferCard) {
        selectedType = TransactionType.TRANSFER_OUT;
      }
      updateTypeCardsUI(expenseCard, expenseLabel, incomeCard, incomeLabel, transferCard, transferLabel);
    };

    if (expenseCard != null) expenseCard.setOnClickListener(typeClickListener);
    if (incomeCard != null) incomeCard.setOnClickListener(typeClickListener);
    if (transferCard != null) transferCard.setOnClickListener(typeClickListener);
  }

  private void updateTypeCardsUI(
      @Nullable LinearLayout expenseCard, @Nullable TextView expenseLabel,
      @Nullable LinearLayout incomeCard, @Nullable TextView incomeLabel,
      @Nullable LinearLayout transferCard, @Nullable TextView transferLabel) {
    if (expenseCard == null || incomeCard == null || transferCard == null) return;

    boolean isExpense = selectedType == TransactionType.EXPENSE;
    boolean isIncome = selectedType == TransactionType.INCOME;
    boolean isTransfer = selectedType == TransactionType.TRANSFER_OUT || selectedType == TransactionType.TRANSFER_IN;

    expenseCard.setBackgroundResource(isExpense ? R.drawable.bg_category_type_expense_active : R.drawable.bg_category_type_inactive);
    if (expenseLabel != null) {
      expenseLabel.setTextColor(ContextCompat.getColor(getContext(), isExpense ? R.color.finan_expense : R.color.finan_text_primary));
    }

    incomeCard.setBackgroundResource(isIncome ? R.drawable.bg_category_type_income_active : R.drawable.bg_category_type_inactive);
    if (incomeLabel != null) {
      incomeLabel.setTextColor(ContextCompat.getColor(getContext(), isIncome ? R.color.finan_income : R.color.finan_text_primary));
    }

    transferCard.setBackgroundResource(isTransfer ? R.drawable.bg_category_type_income_active : R.drawable.bg_category_type_inactive);
    if (transferLabel != null) {
      transferLabel.setTextColor(ContextCompat.getColor(getContext(), isTransfer ? R.color.finan_primary : R.color.finan_text_primary));
    }
  }

  private void confirmDelete() {
    if (existingTemplate == null) return;
    new MaterialAlertDialogBuilder(getContext())
        .setTitle("Hapus Shortcut")
        .setMessage("Apakah kamu yakin ingin menghapus shortcut \"" + existingTemplate.getName() + "\"?")
        .setPositiveButton("Hapus", (dialog, which) -> {
          services.transactionTemplateDao.delete(existingTemplate.getId());
          Toast.makeText(getContext(), "Shortcut dihapus", Toast.LENGTH_SHORT).show();
          if (onSavedCallback != null) {
            onSavedCallback.run();
          }
          dismiss();
        })
        .setNegativeButton("Batal", null)
        .show();
  }

  private void saveShortcut(
      @Nullable LabeledEditTextView nameField,
      @Nullable LabeledEditTextView amountField,
      @Nullable LabeledEditTextView noteField) {

    String name = "";
    if (nameField != null && nameField.getEditText() != null) {
      name = nameField.getEditText().getText().toString().trim();
    }
    if (TextUtils.isEmpty(name)) {
      if (nameField != null && nameField.getEditText() != null) {
        nameField.getEditText().setError("Nama shortcut tidak boleh kosong");
        nameField.getEditText().requestFocus();
      } else {
        Toast.makeText(getContext(), "Nama shortcut tidak boleh kosong", Toast.LENGTH_SHORT).show();
      }
      return;
    }

    TransactionTemplate duplicate = services.transactionTemplateDao.findByNameIgnoreCase(name);
    if (duplicate != null && (existingTemplate == null || duplicate.getId() != existingTemplate.getId())) {
      if (nameField != null && nameField.getEditText() != null) {
        nameField.getEditText().setError("Shortcut dengan nama ini sudah ada");
        nameField.getEditText().requestFocus();
      } else {
        Toast.makeText(getContext(), "Shortcut dengan nama ini sudah ada", Toast.LENGTH_SHORT).show();
      }
      return;
    }

    long amountMinor = 0L;
    if (amountField != null && amountField.getEditText() != null) {
      String rawAmount = amountField.getEditText().getText().toString().trim();
      if (!TextUtils.isEmpty(rawAmount)) {
        try {
          amountMinor = MoneyParser.parse(rawAmount);
        } catch (Exception ignored) {}
      }
    }

    if (amountMinor <= 0) {
      if (amountField != null && amountField.getEditText() != null) {
        amountField.getEditText().setError("Nominal shortcut wajib diisi");
        amountField.getEditText().requestFocus();
      } else {
        Toast.makeText(getContext(), "Nominal shortcut wajib diisi", Toast.LENGTH_SHORT).show();
      }
      return;
    }

    String note = "";
    if (noteField != null && noteField.getEditText() != null) {
      note = noteField.getEditText().getText().toString().trim();
    }

    if (existingTemplate != null && existingTemplate.getId() > 0) {
      existingTemplate.setName(name);
      existingTemplate.setIcon(selectedIcon);
      existingTemplate.setType(selectedType);
      existingTemplate.setAmountMinor(amountMinor);
      existingTemplate.setNote(note);
      services.transactionTemplateDao.update(existingTemplate);
      Toast.makeText(getContext(), "Shortcut diperbarui", Toast.LENGTH_SHORT).show();
    } else {
      TransactionTemplate newTemplate = TransactionTemplateService.createFromInput(
          name, selectedType, amountMinor, null, null, null, note, selectedIcon
      );
      services.transactionTemplateDao.insert(newTemplate);
      Toast.makeText(getContext(), "Shortcut baru disimpan", Toast.LENGTH_SHORT).show();
    }

    if (onSavedCallback != null) {
      onSavedCallback.run();
    }
    dismiss();
  }
}
