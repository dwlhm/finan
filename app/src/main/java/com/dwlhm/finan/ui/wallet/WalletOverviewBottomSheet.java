package com.dwlhm.finan.ui.wallet;

import android.app.Dialog;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class WalletOverviewBottomSheet extends Dialog {

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
    if (walletIcon != null && !walletIcon.trim().isEmpty()) {
      iconView.setText(walletIcon);
    } else {
      iconView.setText(wallet.isDefault() ? "⭐" : "💳");
    }
    nameView.setText(wallet.getName());
    defaultBadge.setVisibility(wallet.isDefault() ? View.VISIBLE : View.GONE);

    String formattedBalance =
        MoneyFormatter.formatWithCurrencyCode(
            wallet.getCurrencyCode(), wallet.getCachedBalanceMinor());
    if (BottomSheetHelper.isMasked(getContext())) {
      balanceView.setText("***");
      balanceLabelView.setText("***");
      balanceView.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_text_primary));
      balanceLabelView.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_text_primary));
    } else {
      int balanceColorId =
          wallet.getCachedBalanceMinor() < 0 ? R.color.finan_expense : R.color.finan_primary;
      int balanceColor = ContextCompat.getColor(getContext(), balanceColorId);
      balanceView.setText(formattedBalance);
      balanceLabelView.setText(formattedBalance);
      balanceView.setTextColor(balanceColor);
      balanceLabelView.setTextColor(balanceColor);
    }

    int usage = wallet.getUsageCount();
    if (usage > 0) {
      transactionRow.setVisibility(View.VISIBLE);
      transactionCountView.setText(usage + " transaksi");
    }

    TextView createdAtView = findViewById(R.id.wallet_overview_created_at);
    String dateStr =
        new SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"))
            .format(new Date(wallet.getCreatedAt()));
    createdAtView.setText(dateStr);

    TextView editAction = findViewById(R.id.wallet_action_edit);
    TextView adjustAction = findViewById(R.id.wallet_action_adjust);
    TextView transferAction = findViewById(R.id.wallet_action_transfer);
    TextView deleteAction = findViewById(R.id.wallet_action_delete);

    editAction.setOnClickListener(v -> {
      dismiss();
      new WalletEditBottomSheet(getContext(), services, wallet, onDataChanged);
    });
    adjustAction.setOnClickListener(v -> showAdjustDialog());
    transferAction.setOnClickListener(v -> showTransferDialog());
    deleteAction.setOnClickListener(v -> confirmDelete());
  }

  private void showAdjustDialog() {
    Dialog dialog = new Dialog(getContext(), R.style.Finan_BottomSheetDialog);
    dialog.setContentView(R.layout.dialog_adjustment_bottom_sheet);
    TextView currentBalanceView = dialog.findViewById(R.id.wallet_adjust_current_balance);
    TextView differenceView = dialog.findViewById(R.id.wallet_adjust_difference);
    LabeledEditTextView targetField = dialog.findViewById(R.id.wallet_adjust_target_field);
    EditText targetInput = targetField.getEditText();
    EditText noteInput = dialog.findViewById(R.id.wallet_adjust_note);
    DialogActionsView actionsView = dialog.findViewById(R.id.wallet_adjust_actions);
    TransactionOccurredAtPicker occurredAtPicker =
        new TransactionOccurredAtPicker(
            getContext(),
            dialog.findViewById(R.id.transaction_occurred_date),
            dialog.findViewById(R.id.transaction_occurred_time),
            System.currentTimeMillis());

    if (BottomSheetHelper.isMasked(getContext())) {
      currentBalanceView.setText("***");
    } else {
      currentBalanceView.setText(
          MoneyFormatter.formatWithCurrencyCode(
              wallet.getCurrencyCode(), wallet.getCachedBalanceMinor()));
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

    actionsView.setOnCancelClickListener(v -> dialog.dismiss());
    actionsView.setOnPrimaryClickListener(
        v ->
            submitAdjust(
                dialog, wallet, targetInput, noteInput, occurredAtPicker.getOccurredAtMillis()));

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
      Dialog dialog,
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

    Dialog dialog = new Dialog(getContext(), R.style.Finan_BottomSheetDialog);
    dialog.setContentView(R.layout.dialog_transfer_bottom_sheet);
    Spinner sourceSpinner = dialog.findViewById(R.id.wallet_transfer_source);
    Spinner destinationSpinner = dialog.findViewById(R.id.wallet_transfer_destination);
    LabeledEditTextView amountField = dialog.findViewById(R.id.wallet_transfer_amount_field);
    EditText amountInput = amountField.getEditText();
    EditText noteInput = dialog.findViewById(R.id.wallet_transfer_note);
    DialogActionsView actionsView = dialog.findViewById(R.id.wallet_transfer_actions);
    TransactionOccurredAtPicker occurredAtPicker =
        new TransactionOccurredAtPicker(
            getContext(),
            dialog.findViewById(R.id.transaction_occurred_date),
            dialog.findViewById(R.id.transaction_occurred_time),
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

    actionsView.setOnCancelClickListener(v -> dialog.dismiss());
    actionsView.setOnPrimaryClickListener(
        v ->
            submitTransfer(
                dialog, allWallets, sourceSpinner, destinationSpinner,
                amountInput, noteInput, occurredAtPicker.getOccurredAtMillis()));

    BottomSheetHelper.show(dialog);
    amountInput.requestFocus();
  }

  private void submitTransfer(
      Dialog dialog,
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
    Dialog dialog = new Dialog(getContext(), R.style.Finan_BottomSheetDialog);
    dialog.setContentView(R.layout.dialog_confirm_delete);
    ((TextView) dialog.findViewById(R.id.confirm_delete_title))
        .setText("Hapus dompet");
    ((TextView) dialog.findViewById(R.id.confirm_delete_message))
        .setText(
            "Apakah Anda yakin ingin menghapus dompet \""
                + wallet.getName()
                + "\"? Semua transaksi terkait juga akan dihapus.");
    dialog.findViewById(R.id.confirm_delete_cancel)
        .setOnClickListener(v -> dialog.dismiss());
    dialog.findViewById(R.id.confirm_delete_confirm)
        .setOnClickListener(v -> {
          final long deleteWalletId = wallet.getId();
          final String deleteName = wallet.getName();
          final String deleteCurrencyCode = wallet.getCurrencyCode();
          final boolean deleteIsDefault = wallet.isDefault();
          final long deleteOpeningBalanceMinor = wallet.getOpeningBalanceMinor();
          final long deleteCreatedAt = wallet.getCreatedAt();
          final String deleteIcon = wallet.getIcon();

          dialog.dismiss();
          dismiss();

          services.dbWorker.compute(
              () -> services.walletDao.delete(deleteWalletId),
              success -> {
                if (Boolean.TRUE.equals(success)) {
                  FinanToast.show(
                      activity,
                      "Dompet \"" + deleteName + "\" dihapus",
                      "Urungkan",
                      () -> {
                        services.dbWorker.compute(
                            () -> services.walletDao.insert(
                                deleteName, deleteCurrencyCode, deleteIsDefault,
                                deleteOpeningBalanceMinor, deleteCreatedAt, deleteIcon),
                            id -> onDataChanged.run());
                      });
                  onDataChanged.run();
                } else {
                  Toast.makeText(getContext(), "Gagal menghapus dompet", Toast.LENGTH_SHORT).show();
                }
              });
        });

    BottomSheetHelper.show(dialog);
  }
}
