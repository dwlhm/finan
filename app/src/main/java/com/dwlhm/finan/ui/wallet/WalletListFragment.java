package com.dwlhm.finan.ui.wallet;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ScreenHeaderView;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.TransactionOccurredAtPicker;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;
import com.dwlhm.finan.util.ui.ViewPressAnimator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WalletListFragment extends ScreenFragment {

  public static final String TAG = "wallet_list";

  private AppServices services;
  private int reloadGeneration;
  private WalletAdapter adapter;
  private ListView listView;
  private TextView emptyView;
  private View summaryView;
  private TextView totalBalanceView;
  private TextView walletCountView;
  private TextView defaultWalletView;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    services = ServicesProvider.get(requireContext());
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_wallet_list;
  }

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    listView = view.findViewById(R.id.wallet_list);
    emptyView = view.findViewById(R.id.wallet_empty);
    summaryView = view.findViewById(R.id.wallet_summary);
    totalBalanceView = view.findViewById(R.id.wallet_total_balance);
    walletCountView = view.findViewById(R.id.wallet_count);
    defaultWalletView = view.findViewById(R.id.wallet_default);
    ScreenHeaderView headerView = view.findViewById(R.id.wallet_header);
    headerView.setOnBackClickListener(v -> goBack());
    headerView.setOnActionClickListener(v -> showAddWalletDialog());
    adapter = new WalletAdapter(requireContext());
    listView.setAdapter(adapter);
  }

  @Override
  public void onResume() {
    super.onResume();
    reload();
  }

  @Override
  public void onDestroyView() {
    reloadGeneration++;
    super.onDestroyView();
  }

  private void reload() {
    int generation = ++reloadGeneration;
    services.dbWorker.compute(
        () -> services.walletDao.findAll(),
        wallets -> {
          if (!isAdded() || generation != reloadGeneration || wallets == null) {
            return;
          }
          adapter.setWallets(wallets);
          bindSummary(wallets);
          boolean empty = wallets.isEmpty();
          summaryView.setVisibility(empty ? View.GONE : View.VISIBLE);
          emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
          listView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
  }

  private void bindSummary(List<Wallet> wallets) {
    Map<String, Long> totalsByCurrency = new LinkedHashMap<>();
    Wallet defaultWallet = null;
    for (Wallet wallet : wallets) {
      String currencyCode = MoneyFormatter.normalizeCurrencyCode(wallet.getCurrencyCode());
      totalsByCurrency.compute(
          currencyCode,
          (ignored, current) ->
              (current == null ? 0L : current) + wallet.getCachedBalanceMinor());
      if (wallet.isDefault()) {
        defaultWallet = wallet;
      }
    }

    if (BottomSheetHelper.isMasked(requireContext())) {
      totalBalanceView.setText("***");
      totalBalanceView.setTextColor(
          ContextCompat.getColor(requireContext(), R.color.finan_text_primary));
    } else {
      totalBalanceView.setText(MoneyFormatter.formatTotalsByCurrency(totalsByCurrency));
      int balanceColor =
          MoneyFormatter.containsOnlyNegativeTotals(totalsByCurrency)
              ? R.color.finan_expense
              : R.color.finan_primary;
      totalBalanceView.setTextColor(
          ContextCompat.getColor(requireContext(), balanceColor));
    }
    walletCountView.setText(
        getResources().getQuantityString(R.plurals.wallet_count, wallets.size(), wallets.size()));
    String defaultWalletName =
        defaultWallet == null ? getString(R.string.wallet_default_none) : defaultWallet.getName();
    defaultWalletView.setText(getString(R.string.wallet_default_format, defaultWalletName));
  }

  private void goBack() {
    requireActivity().getSupportFragmentManager().popBackStack();
  }

  private void showAddWalletDialog() {
    new WalletInputDialog(requireContext(), services, this::reload);
  }

  private void showEditWalletDialog(Wallet wallet) {
    Dialog dialog = new Dialog(requireContext());
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.dialog_wallet_name_input);
    LabeledEditTextView nameField = dialog.findViewById(R.id.wallet_name_field);
    EditText nameInput = nameField.getEditText();
    CheckBox defaultInput = dialog.findViewById(R.id.wallet_default_input);
    DialogActionsView actionsView = dialog.findViewById(R.id.wallet_actions);

    nameInput.setText(wallet.getName());
    nameInput.setSelection(nameInput.getText().length());
    defaultInput.setChecked(wallet.isDefault());
    defaultInput.setEnabled(!wallet.isDefault());
    defaultInput.setAlpha(wallet.isDefault() ? 0.72f : 1f);

    actionsView.setOnCancelClickListener(v -> dialog.dismiss());
    actionsView.setOnPrimaryClickListener(
        v -> submitUpdateWallet(dialog, wallet.getId(), nameInput, defaultInput));
    BottomSheetHelper.show(dialog);
    nameInput.requestFocus();
  }

  private void showAdjustmentDialog(Wallet wallet) {
    Dialog dialog = new Dialog(requireContext());
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.dialog_balance_adjustment);
    TextView currentBalanceView = dialog.findViewById(R.id.wallet_adjust_current_balance);
    TextView differenceView = dialog.findViewById(R.id.wallet_adjust_difference);
    LabeledEditTextView targetField = dialog.findViewById(R.id.wallet_adjust_target_field);
    EditText targetInput = targetField.getEditText();
    EditText noteInput = dialog.findViewById(R.id.wallet_adjust_note);
    DialogActionsView actionsView = dialog.findViewById(R.id.wallet_adjust_actions);
    TransactionOccurredAtPicker occurredAtPicker =
        new TransactionOccurredAtPicker(
            requireContext(),
            dialog.findViewById(R.id.transaction_occurred_date),
            dialog.findViewById(R.id.transaction_occurred_time),
            System.currentTimeMillis());

    if (BottomSheetHelper.isMasked(requireContext())) {
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
            bindAdjustmentDifference(wallet, targetInput, differenceView);
          }
        });
    bindAdjustmentDifference(wallet, targetInput, differenceView);

    actionsView.setOnCancelClickListener(v -> dialog.dismiss());
    actionsView.setOnPrimaryClickListener(
        v ->
            submitAdjustment(
                dialog, wallet, targetInput, noteInput, occurredAtPicker.getOccurredAtMillis()));
    BottomSheetHelper.show(dialog);
    targetInput.requestFocus();
  }

  private void bindAdjustmentDifference(
      Wallet wallet, EditText targetInput, TextView differenceView) {
    try {
      if (BottomSheetHelper.isMasked(requireContext())) {
        differenceView.setText("***");
        differenceView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.finan_text_secondary));
        return;
      }
      long target = MoneyParser.parse(targetInput.getText().toString());
      long difference = Math.subtractExact(target, wallet.getCachedBalanceMinor());
      if (difference == Long.MIN_VALUE) {
        throw new ArithmeticException();
      }
      if (difference == 0L) {
        differenceView.setText(R.string.wallet_adjust_no_change);
        differenceView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.finan_text_secondary));
        return;
      }
      boolean increase = difference > 0L;
      differenceView.setText(
          getString(
              increase
                  ? R.string.transaction_income_amount_format
                  : R.string.transaction_expense_amount_format,
              MoneyFormatter.format(Math.abs(difference))));
      differenceView.setTextColor(
          ContextCompat.getColor(
              requireContext(), increase ? R.color.finan_income : R.color.finan_expense));
    } catch (IllegalArgumentException | ArithmeticException e) {
      differenceView.setText(R.string.wallet_adjust_no_change);
      differenceView.setTextColor(
          ContextCompat.getColor(requireContext(), R.color.finan_text_secondary));
    }
  }

  private void submitAdjustment(
      Dialog dialog,
      Wallet wallet,
      EditText targetInput,
      EditText noteInput,
      long occurredAt) {
    long targetBalance;
    try {
      targetBalance = MoneyParser.parse(targetInput.getText().toString());
    } catch (IllegalArgumentException e) {
      targetInput.setError(getString(R.string.wallet_adjust_error_target));
      return;
    }
    String note = noteInput.getText().toString().trim();
    services.dbWorker.compute(
        () -> {
          try {
            return services.adjustmentService.adjustTo(
                wallet.getId(),
                targetBalance,
                occurredAt,
                TextUtils.isEmpty(note) ? null : note);
          } catch (RuntimeException e) {
            return -1L;
          }
        },
        transactionId -> {
          if (!isAdded() || !dialog.isShowing()) {
            return;
          }
          if (transactionId == null || transactionId < 0L) {
            Toast.makeText(requireContext(), R.string.wallet_adjust_error_save, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          Toast.makeText(
                  requireContext(),
                  transactionId == 0L
                      ? R.string.wallet_adjust_no_change
                      : R.string.wallet_adjust_saved,
                  Toast.LENGTH_SHORT)
              .show();
          dialog.dismiss();
          reload();
        });
  }

  private void showTransferDialog(Wallet sourceWallet) {
    List<Wallet> wallets = adapter.getWallets();
    if (wallets.size() < 2) {
      Toast.makeText(requireContext(), R.string.wallet_transfer_need_two, Toast.LENGTH_SHORT).show();
      return;
    }

    Dialog dialog = new Dialog(requireContext());
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.dialog_wallet_transfer);
    Spinner sourceSpinner = dialog.findViewById(R.id.wallet_transfer_source);
    Spinner destinationSpinner = dialog.findViewById(R.id.wallet_transfer_destination);
    LabeledEditTextView amountField = dialog.findViewById(R.id.wallet_transfer_amount_field);
    EditText amountInput = amountField.getEditText();
    EditText noteInput = dialog.findViewById(R.id.wallet_transfer_note);
    DialogActionsView actionsView = dialog.findViewById(R.id.wallet_transfer_actions);
    TransactionOccurredAtPicker occurredAtPicker =
        new TransactionOccurredAtPicker(
            requireContext(),
            dialog.findViewById(R.id.transaction_occurred_date),
            dialog.findViewById(R.id.transaction_occurred_time),
            System.currentTimeMillis());

    boolean masked = BottomSheetHelper.isMasked(requireContext());
    List<String> labels = new ArrayList<>();
    int sourceIndex = 0;
    int destinationIndex = 0;
    for (int i = 0; i < wallets.size(); i++) {
      Wallet wallet = wallets.get(i);
      labels.add(
          wallet.getName()
              + " · "
              + (masked
                  ? "***"
                  : MoneyFormatter.formatWithCurrencyCode(
                      wallet.getCurrencyCode(), wallet.getCachedBalanceMinor())));
      if (wallet.getId() == sourceWallet.getId()) {
        sourceIndex = i;
      } else if (destinationIndex == sourceIndex) {
        destinationIndex = i;
      }
    }
    if (destinationIndex == sourceIndex) {
      destinationIndex = sourceIndex == 0 ? 1 : 0;
    }
    ArrayAdapter<String> spinnerAdapter =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
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
                dialog,
                wallets,
                sourceSpinner,
                destinationSpinner,
                amountInput,
                noteInput,
                occurredAtPicker.getOccurredAtMillis()));
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
    if (sourceIndex < 0
        || destinationIndex < 0
        || sourceIndex >= wallets.size()
        || destinationIndex >= wallets.size()) {
      return;
    }
    Wallet source = wallets.get(sourceIndex);
    Wallet destination = wallets.get(destinationIndex);
    if (source.getId() == destination.getId()) {
      Toast.makeText(requireContext(), R.string.wallet_transfer_same_wallet, Toast.LENGTH_SHORT)
          .show();
      return;
    }
    long amountMinor;
    try {
      amountMinor = MoneyParser.parse(amountInput.getText().toString());
      if (amountMinor <= 0L) {
        throw new IllegalArgumentException("Amount must be positive");
      }
    } catch (IllegalArgumentException e) {
      amountInput.setError(getString(R.string.wallet_transfer_error_amount));
      return;
    }
    String note = noteInput.getText().toString().trim();
    services.dbWorker.compute(
        () -> {
          try {
            return services.transferService.create(
                source.getId(),
                destination.getId(),
                amountMinor,
                occurredAt,
                TextUtils.isEmpty(note) ? null : note);
          } catch (RuntimeException e) {
            return -1L;
          }
        },
        transferId -> {
          if (!isAdded() || !dialog.isShowing()) {
            return;
          }
          if (transferId == null || transferId <= 0L) {
            Toast.makeText(requireContext(), R.string.wallet_transfer_error_save, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          Toast.makeText(requireContext(), R.string.wallet_transfer_saved, Toast.LENGTH_SHORT).show();
          dialog.dismiss();
          reload();
        });
  }

  private void submitUpdateWallet(
      Dialog dialog, long walletId, EditText nameInput, CheckBox defaultInput) {
    String name = nameInput.getText().toString().trim();
    if (name.isEmpty()) {
      nameInput.setError(getString(R.string.wallet_error_name));
      return;
    }
    boolean makeDefault = defaultInput.isChecked();
    services.dbWorker.compute(
        () -> services.walletDao.updateNameAndDefault(walletId, name, makeDefault),
        updated -> {
          if (!isAdded() || !dialog.isShowing()) {
            return;
          }
          if (!Boolean.TRUE.equals(updated)) {
            Toast.makeText(requireContext(), R.string.wallet_error_update, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          if (makeDefault) {
            services.defaultsStore.setDefaultWalletId(walletId);
          }
          Toast.makeText(requireContext(), R.string.wallet_updated, Toast.LENGTH_SHORT).show();
          dialog.dismiss();
          reload();
        });
  }

  private final class WalletAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private List<Wallet> wallets = new ArrayList<>();

    WalletAdapter(Context context) {
      this.context = context;
      this.inflater = LayoutInflater.from(context);
    }

    void setWallets(List<Wallet> wallets) {
      this.wallets = wallets != null ? wallets : new ArrayList<>();
      notifyDataSetChanged();
    }

    List<Wallet> getWallets() {
      return new ArrayList<>(wallets);
    }

    @Override
    public int getCount() {
      return wallets.size();
    }

    @Override
    public Wallet getItem(int position) {
      return wallets.get(position);
    }

    @Override
    public long getItemId(int position) {
      return getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.item_wallet, parent, false);
      }
      View card = convertView.findViewById(R.id.item_wallet_card);
      TextView name = convertView.findViewById(R.id.item_wallet_name);
      TextView defaultBadge = convertView.findViewById(R.id.item_wallet_default_badge);
      TextView balance = convertView.findViewById(R.id.item_wallet_balance);
      View editButton = convertView.findViewById(R.id.item_wallet_edit);
      View adjustButton = convertView.findViewById(R.id.item_wallet_adjust);
      View transferButton = convertView.findViewById(R.id.item_wallet_transfer);
      Wallet wallet = getItem(position);
      String currencyCode = MoneyFormatter.normalizeCurrencyCode(wallet.getCurrencyCode());
      name.setText(wallet.getName());
      card.setBackgroundResource(
          wallet.isDefault() ? R.drawable.bg_card_wallet_default : R.drawable.bg_card);
      defaultBadge.setVisibility(wallet.isDefault() ? View.VISIBLE : View.GONE);
      if (BottomSheetHelper.isMasked(requireContext())) {
        balance.setText("***");
        balance.setTextColor(
            ContextCompat.getColor(context, R.color.finan_text_primary));
      } else {
        balance.setText(
            MoneyFormatter.formatWithCurrencyCode(currencyCode, wallet.getCachedBalanceMinor()));
        balance.setTextColor(
            ContextCompat.getColor(
                context,
                wallet.getCachedBalanceMinor() < 0 ? R.color.finan_expense : R.color.finan_primary));
      }
      ViewPressAnimator.bindScale(editButton);
      editButton.setOnClickListener(v -> showEditWalletDialog(wallet));
      adjustButton.setOnClickListener(v -> showAdjustmentDialog(wallet));
      transferButton.setEnabled(wallets.size() > 1);
      transferButton.setAlpha(wallets.size() > 1 ? 1f : 0.5f);
      transferButton.setOnClickListener(v -> showTransferDialog(wallet));
      return convertView;
    }
  }
}
