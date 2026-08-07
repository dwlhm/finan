package com.dwlhm.finan.ui.wallet;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dwlhm.finan.ui.components.FinanToast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
import com.dwlhm.finan.ui.common.TransactionOccurredAtPicker;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;
import com.dwlhm.finan.util.ui.ViewPressAnimator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class WalletOverviewBottomSheet extends BottomSheetDialog {

  private final Activity activity;
  private final AppServices services;
  private final Wallet wallet;
  private final List<Wallet> allWallets;
  private final Runnable onDataChanged;

  public WalletOverviewBottomSheet(
      @NonNull Context context,
      @NonNull AppServices services,
      @NonNull Wallet wallet,
      @NonNull List<Wallet> allWallets,
      @NonNull Runnable onDataChanged) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.activity = (Activity) context;
    this.services = services;
    this.wallet = wallet;
    this.allWallets = allWallets;
    this.onDataChanged = onDataChanged;
    setContentView(R.layout.dialog_wallet_overview);
    setCancelable(true);
    setupViews();
  }

  private void setupViews() {
    BottomSheetHelper.show(this);

    TextView iconView = findViewById(R.id.wallet_overview_icon);
    TextView nameView = findViewById(R.id.wallet_overview_name);
    TextView defaultBadge = findViewById(R.id.wallet_overview_default_badge);
    TextView balanceView = findViewById(R.id.wallet_overview_balance);
    TextView balanceLabelView = findViewById(R.id.wallet_overview_balance_label);
    TextView transactionCountView = findViewById(R.id.wallet_overview_transaction_count);
    LinearLayout transactionRow = findViewById(R.id.wallet_overview_transaction_row);

    String walletIcon = wallet.getIcon();
    if (iconView != null) {
      if (walletIcon != null && !walletIcon.trim().isEmpty()) {
        iconView.setText(walletIcon);
      } else {
        iconView.setText(wallet.isDefault() ? "⭐" : "💳");
      }
    }
    if (nameView != null) nameView.setText(wallet.getName());
    if (defaultBadge != null) defaultBadge.setVisibility(wallet.isDefault() ? View.VISIBLE : View.GONE);

    String formattedBalance =
        MoneyFormatter.formatWithCurrencyCode(
            wallet.getCurrencyCode(), wallet.getCachedBalanceMinor());
    if (BottomSheetHelper.isMasked(getContext())) {
      if (balanceView != null) {
        balanceView.setText("***");
        balanceView.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_text_primary));
      }
      if (balanceLabelView != null) {
        balanceLabelView.setText("***");
        balanceLabelView.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_text_primary));
      }
    } else {
      int balanceColorId =
          wallet.getCachedBalanceMinor() < 0 ? R.color.finan_expense : R.color.finan_primary;
      int balanceColor = ContextCompat.getColor(getContext(), balanceColorId);
      if (balanceView != null) {
        balanceView.setText(formattedBalance);
        balanceView.setTextColor(balanceColor);
      }
      if (balanceLabelView != null) {
        balanceLabelView.setText(formattedBalance);
        balanceLabelView.setTextColor(balanceColor);
      }
    }

    int usage = wallet.getUsageCount();
    if (transactionRow != null && transactionCountView != null) {
      if (usage > 0) {
        transactionRow.setVisibility(View.VISIBLE);
        transactionCountView.setText(
            getContext().getString(R.string.java_WalletOverviewBottomSheet_transaksi, usage));
      } else {
        transactionRow.setVisibility(View.GONE);
      }
    }

    TextView createdAtView = findViewById(R.id.wallet_overview_created_at);
    if (createdAtView != null) {
      String dateStr =
          new SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"))
              .format(new Date(wallet.getCreatedAt()));
      createdAtView.setText(dateStr);
    }

    TextView editAction = findViewById(R.id.wallet_action_edit);
    TextView adjustAction = findViewById(R.id.wallet_action_adjust);
    TextView transferAction = findViewById(R.id.wallet_action_transfer);
    TextView deleteAction = findViewById(R.id.wallet_action_delete);

    if (editAction != null) {
      ViewPressAnimator.bindScale(editAction);
      editAction.setOnClickListener(
          v -> {
            dismiss();
            new WalletEditBottomSheet(getContext(), services, wallet, onDataChanged);
          });
    }
    if (adjustAction != null) {
      ViewPressAnimator.bindScale(adjustAction);
      adjustAction.setOnClickListener(v -> showAdjustDialog());
    }
    if (transferAction != null) {
      ViewPressAnimator.bindScale(transferAction);
      if (allWallets.size() < 2) {
        transferAction.setAlpha(0.4f);
        transferAction.setOnClickListener(
            v ->
                Toast.makeText(
                        getContext(), R.string.wallet_transfer_need_two, Toast.LENGTH_SHORT)
                    .show());
      } else {
        transferAction.setAlpha(1.0f);
        transferAction.setOnClickListener(v -> showTransferDialog());
      }
    }
    if (deleteAction != null) {
      ViewPressAnimator.bindScale(deleteAction);
      if (allWallets.size() <= 1) {
        deleteAction.setAlpha(0.4f);
        deleteAction.setOnClickListener(
            v ->
                Toast.makeText(
                        getContext(), R.string.wallet_error_delete_last, Toast.LENGTH_SHORT)
                    .show());
      } else {
        deleteAction.setAlpha(1.0f);
        deleteAction.setOnClickListener(v -> confirmDelete());
      }
    }
  }

  private void showAdjustDialog() {
    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.Finan_BottomSheetDialog);
    dialog.setContentView(R.layout.dialog_adjustment_bottom_sheet);
    TextView currentBalanceView = dialog.findViewById(R.id.wallet_adjust_current_balance);
    TextView differenceView = dialog.findViewById(R.id.wallet_adjust_difference);
    LabeledEditTextView targetField = dialog.findViewById(R.id.wallet_adjust_target_field);
    EditText targetInput = targetField != null ? targetField.getEditText() : null;
    EditText noteInput = dialog.findViewById(R.id.wallet_adjust_note);
    DialogActionsView actionsView = dialog.findViewById(R.id.wallet_adjust_actions);
    TextView dateBtn = dialog.findViewById(R.id.transaction_occurred_date);
    TextView timeBtn = dialog.findViewById(R.id.transaction_occurred_time);
    if (targetInput == null || dateBtn == null || timeBtn == null) return;

    TransactionOccurredAtPicker occurredAtPicker =
        new TransactionOccurredAtPicker(
            getContext(),
            dateBtn,
            timeBtn,
            System.currentTimeMillis());

    if (currentBalanceView != null) {
      if (BottomSheetHelper.isMasked(getContext())) {
        currentBalanceView.setText("***");
      } else {
        currentBalanceView.setText(
            MoneyFormatter.formatWithCurrencyCode(
                wallet.getCurrencyCode(), wallet.getCachedBalanceMinor()));
      }
    }
    MoneyInputFormatter.attach(targetInput, true);
    targetInput.setText(MoneyFormatter.format(wallet.getCachedBalanceMinor()));
    targetInput.setSelection(targetInput.length());
    targetInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            bindDifference(wallet, targetInput, differenceView);
          }
        });
    bindDifference(wallet, targetInput, differenceView);

    if (actionsView != null) {
      actionsView.setOnCancelClickListener(v -> dialog.dismiss());
      actionsView.setOnPrimaryClickListener(
          v ->
              submitAdjust(
                  dialog, wallet, targetInput, noteInput, occurredAtPicker.getOccurredAtMillis()));
    }

    BottomSheetHelper.show(dialog);
    targetInput.requestFocus();
  }

  private void bindDifference(Wallet wallet, EditText targetInput, TextView differenceView) {
    try {
      if (BottomSheetHelper.isMasked(getContext())) {
        differenceView.setText("***");
        differenceView.setTextColor(
            ContextCompat.getColor(getContext(), R.color.finan_text_secondary));
        return;
      }
      long target = MoneyParser.parse(targetInput.getText().toString());
      long difference = Math.subtractExact(target, wallet.getCachedBalanceMinor());
      if (difference == Long.MIN_VALUE) throw new ArithmeticException();
      if (difference == 0L) {
        differenceView.setText(R.string.wallet_adjust_no_change);
        differenceView.setTextColor(
            ContextCompat.getColor(getContext(), R.color.finan_text_secondary));
        return;
      }
      boolean increase = difference > 0L;
      differenceView.setText(
          getContext().getString(
              increase
                  ? R.string.transaction_income_amount_format
                  : R.string.transaction_expense_amount_format,
              MoneyFormatter.format(Math.abs(difference))));
      differenceView.setTextColor(
          ContextCompat.getColor(
              getContext(), increase ? R.color.finan_income : R.color.finan_expense));
    } catch (IllegalArgumentException | ArithmeticException e) {
      differenceView.setText(R.string.wallet_adjust_no_change);
      differenceView.setTextColor(
          ContextCompat.getColor(getContext(), R.color.finan_text_secondary));
    }
  }

  private void submitAdjust(
      BottomSheetDialog dialog,
      Wallet wallet,
      EditText targetInput,
      EditText noteInput,
      long occurredAt) {
    long targetBalance;
    try {
      targetBalance = MoneyParser.parse(targetInput.getText().toString());
    } catch (IllegalArgumentException e) {
      targetInput.setError(getContext().getString(R.string.wallet_adjust_error_target));
      return;
    }
    String note = noteInput.getText().toString().trim();
    services.dbWorker.compute(
        () -> {
          try {
            return services.adjustmentService.adjustTo(
                wallet.getId(), targetBalance, occurredAt,
                TextUtils.isEmpty(note) ? null : note);
          } catch (RuntimeException e) {
            return -1L;
          }
        },
        transactionId -> {
          if (!isShowing() && !dialog.isShowing()) return;
          if (transactionId == null || transactionId < 0L) {
            Toast.makeText(getContext(), R.string.wallet_adjust_error_save, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          Toast.makeText(
                  getContext(),
                  transactionId == 0L
                      ? R.string.wallet_adjust_no_change
                      : R.string.wallet_adjust_saved,
                  Toast.LENGTH_SHORT)
              .show();
          dialog.dismiss();
          dismiss();
          onDataChanged.run();
        });
  }

  private void showTransferDialog() {
    if (allWallets.size() < 2) {
      Toast.makeText(getContext(), R.string.wallet_transfer_need_two, Toast.LENGTH_SHORT).show();
      return;
    }

    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.Finan_BottomSheetDialog);
    dialog.setContentView(R.layout.dialog_transfer_bottom_sheet);
    Spinner sourceSpinner = dialog.findViewById(R.id.wallet_transfer_source);
    Spinner destinationSpinner = dialog.findViewById(R.id.wallet_transfer_destination);
    LabeledEditTextView amountField = dialog.findViewById(R.id.wallet_transfer_amount_field);
    EditText amountInput = amountField != null ? amountField.getEditText() : null;
    EditText noteInput = dialog.findViewById(R.id.wallet_transfer_note);
    DialogActionsView actionsView = dialog.findViewById(R.id.wallet_transfer_actions);
    TextView dateBtn = dialog.findViewById(R.id.transaction_occurred_date);
    TextView timeBtn = dialog.findViewById(R.id.transaction_occurred_time);
    if (sourceSpinner == null || destinationSpinner == null || amountInput == null || dateBtn == null || timeBtn == null) return;

    TransactionOccurredAtPicker occurredAtPicker =
        new TransactionOccurredAtPicker(
            getContext(),
            dateBtn,
            timeBtn,
            System.currentTimeMillis());

    boolean masked = BottomSheetHelper.isMasked(getContext());
    List<String> labels = new ArrayList<>();
    int sourceIndex = 0;
    int destinationIndex = 0;
    for (int i = 0; i < allWallets.size(); i++) {
      Wallet w = allWallets.get(i);
      labels.add(
          w.getName()
              + " · "
              + (masked
                  ? "***"
                  : MoneyFormatter.formatWithCurrencyCode(
                      w.getCurrencyCode(), w.getCachedBalanceMinor())));
      if (w.getId() == wallet.getId()) {
        sourceIndex = i;
      } else if (destinationIndex == sourceIndex) {
        destinationIndex = i;
      }
    }
    if (destinationIndex == sourceIndex) {
      destinationIndex = sourceIndex == 0 ? 1 : 0;
    }
    ArrayAdapter<String> spinnerAdapter =
        new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, labels);
    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    sourceSpinner.setAdapter(spinnerAdapter);
    destinationSpinner.setAdapter(spinnerAdapter);
    sourceSpinner.setSelection(sourceIndex);
    destinationSpinner.setSelection(destinationIndex);
    MoneyInputFormatter.attach(amountInput, true);

    if (actionsView != null) {
      actionsView.setOnCancelClickListener(v -> dialog.dismiss());
      actionsView.setOnPrimaryClickListener(
          v ->
              submitTransfer(
                  dialog, allWallets, sourceSpinner, destinationSpinner,
                  amountInput, noteInput, occurredAtPicker.getOccurredAtMillis()));
    }

    BottomSheetHelper.show(dialog);
    amountInput.requestFocus();
  }

  private void submitTransfer(
      BottomSheetDialog dialog,
      List<Wallet> wallets,
      Spinner sourceSpinner,
      Spinner destinationSpinner,
      EditText amountInput,
      EditText noteInput,
      long occurredAt) {
    int sourceIndex = sourceSpinner.getSelectedItemPosition();
    int destinationIndex = destinationSpinner.getSelectedItemPosition();
    if (sourceIndex < 0 || destinationIndex < 0
        || sourceIndex >= wallets.size() || destinationIndex >= wallets.size()) {
      return;
    }
    Wallet source = wallets.get(sourceIndex);
    Wallet destination = wallets.get(destinationIndex);
    if (source.getId() == destination.getId()) {
      Toast.makeText(getContext(), R.string.wallet_transfer_same_wallet, Toast.LENGTH_SHORT).show();
      return;
    }
    long amountMinor;
    try {
      amountMinor = MoneyParser.parse(amountInput.getText().toString());
      if (amountMinor <= 0L) throw new IllegalArgumentException();
    } catch (IllegalArgumentException e) {
      amountInput.setError(getContext().getString(R.string.wallet_transfer_error_amount));
      return;
    }
    String note = noteInput.getText().toString().trim();
    services.dbWorker.compute(
        () -> {
          try {
            return services.transferService.create(
                source.getId(), destination.getId(), amountMinor, occurredAt,
                TextUtils.isEmpty(note) ? null : note);
          } catch (RuntimeException e) {
            return -1L;
          }
        },
        transferId -> {
          if (!isShowing() && !dialog.isShowing()) return;
          if (transferId == null || transferId <= 0L) {
            Toast.makeText(getContext(), R.string.wallet_transfer_error_save, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          Toast.makeText(getContext(), R.string.wallet_transfer_saved, Toast.LENGTH_SHORT).show();
          dialog.dismiss();
          dismiss();
          onDataChanged.run();
        });
  }

  private void confirmDelete() {
    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.Finan_BottomSheetDialog);
    dialog.setContentView(R.layout.dialog_confirm_delete);
    TextView titleView = dialog.findViewById(R.id.confirm_delete_title);
    TextView messageView = dialog.findViewById(R.id.confirm_delete_message);
    View cancelBtn = dialog.findViewById(R.id.confirm_delete_cancel);
    View confirmBtn = dialog.findViewById(R.id.confirm_delete_confirm);

    if (titleView != null) titleView.setText(R.string.java_WalletOverviewBottomSheet_hapus_dompet);
    if (messageView != null) messageView.setText(getContext().getString(R.string.delete_wallet_confirmation, wallet.getName()));
    if (cancelBtn != null) cancelBtn.setOnClickListener(v -> dialog.dismiss());
    if (confirmBtn != null) {
      confirmBtn.setOnClickListener(v -> {
          final String deleteName = wallet.getName();
          final String deleteCurrencyCode = wallet.getCurrencyCode();
          final boolean deleteIsDefault = wallet.isDefault();
          final long deleteOpeningBalanceMinor = wallet.getOpeningBalanceMinor();
          final long deleteCreatedAt = wallet.getCreatedAt();
          final String deleteIcon = wallet.getIcon();

          dialog.dismiss();
          dismiss();

          services.dbWorker.compute(
              () -> services.walletService.delete(wallet.getId()),
              success -> {
                if (Boolean.TRUE.equals(success)) {
                  FinanToast.show(
                      activity,
                      getContext().getString(R.string.wallet_deleted, deleteName),
                      getContext().getString(R.string.wallet_delete_undo),
                      () -> services.dbWorker.compute(
                          () -> services.walletService.insertUndo(
                              deleteName, deleteCurrencyCode, deleteIsDefault,
                              deleteOpeningBalanceMinor, deleteCreatedAt, deleteIcon),
                          id -> onDataChanged.run()));
                  onDataChanged.run();
                } else {
                  Toast.makeText(getContext(), getContext().getString(R.string.wallet_delete_error), Toast.LENGTH_SHORT).show();
                }
              });
        });
    }
    BottomSheetHelper.show(dialog);
  }
}
