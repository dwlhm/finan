package com.dwlhm.finan.ui.wallet;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.dwlhm.finan.ui.components.FinanToast;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;
import com.dwlhm.finan.util.ui.ViewPressAnimator;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WalletListFragment extends ScreenFragment {

  public static final String TAG = "wallet_list";

  private AppServices services;
  private int reloadGeneration;
  private WalletListAdapter adapter;
  private RecyclerView recyclerView;

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
    ScreenHeaderView headerView = view.findViewById(R.id.wallet_header);
    if (headerView != null) {
      headerView.setOnBackClickListener(v -> goBack());
    }
    recyclerView = view.findViewById(R.id.wallet_recycler);
    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    adapter = new WalletListAdapter();
    recyclerView.setAdapter(adapter);
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
        () -> services.walletService.findAll(),
        wallets -> {
          if (!isAdded() || generation != reloadGeneration || wallets == null) {
            return;
          }
          adapter.setWallets(wallets);
        });
  }

  private void goBack() {
    requireActivity().getSupportFragmentManager().popBackStack();
  }

  private void showAddWalletDialog() {
    new WalletInputDialog(requireContext(), services, this::reload);
  }

  private void showEditWalletDialog(Wallet wallet) {
    new WalletEditBottomSheet(requireContext(), services, wallet, this::reload);
  }

  private void showAdjustmentDialog(Wallet wallet) {
    BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.Finan_BottomSheetDialog);
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
            requireContext(),
            dateBtn,
            timeBtn,
            System.currentTimeMillis());

    if (currentBalanceView != null) {
      if (BottomSheetHelper.isMasked(requireContext())) {
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
            bindAdjustmentDifference(wallet, targetInput, differenceView);
          }
        });
    bindAdjustmentDifference(wallet, targetInput, differenceView);

    if (actionsView != null) {
      actionsView.setOnCancelClickListener(v -> dialog.dismiss());
      actionsView.setOnPrimaryClickListener(
          v ->
              submitAdjustment(
                  dialog, wallet, targetInput, noteInput, occurredAtPicker.getOccurredAtMillis()));
    }
    BottomSheetHelper.show(dialog);
    targetInput.requestFocus();
  }

  private void bindAdjustmentDifference(
      Wallet wallet, EditText targetInput, TextView differenceView) {
    if (differenceView == null) return;
    try {
      if (BottomSheetHelper.isMasked(requireContext())) {
        differenceView.setText("***");
        differenceView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.finan_text_secondary));
        return;
      }
      long target = MoneyParser.parse(targetInput.getText().toString());
      long difference = Math.subtractExact(target, wallet.getCachedBalanceMinor());
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
      BottomSheetDialog dialog,
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

  private void showTransferDialog(@Nullable Wallet sourceWallet) {
    List<Wallet> wallets = adapter.getWallets();
    if (wallets.size() < 2) {
      Toast.makeText(requireContext(), R.string.wallet_transfer_need_two, Toast.LENGTH_SHORT).show();
      return;
    }

    BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.Finan_BottomSheetDialog);
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
            requireContext(),
            dateBtn,
            timeBtn,
            System.currentTimeMillis());

    boolean masked = BottomSheetHelper.isMasked(requireContext());
    List<String> labels = new ArrayList<>();
    int sourceIndex = 0;
    int destinationIndex = 0;
    long targetSourceId = sourceWallet != null ? sourceWallet.getId() : -1L;
    for (int i = 0; i < wallets.size(); i++) {
      Wallet wallet = wallets.get(i);
      labels.add(
          wallet.getName()
              + " · "
              + (masked
                  ? "***"
                  : MoneyFormatter.formatWithCurrencyCode(
                      wallet.getCurrencyCode(), wallet.getCachedBalanceMinor())));
      if (wallet.getId() == targetSourceId) {
        sourceIndex = i;
      }
    }
    destinationIndex = sourceIndex == 0 ? 1 : 0;

    ArrayAdapter<String> spinnerAdapter =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
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
                  dialog,
                  wallets,
                  sourceSpinner,
                  destinationSpinner,
                  amountInput,
                  noteInput,
                  occurredAtPicker.getOccurredAtMillis()));
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

  private void showDeleteWalletDialog(Wallet wallet) {
    if (adapter.getWallets().size() <= 1) {
      Toast.makeText(requireContext(), R.string.wallet_error_delete_last, Toast.LENGTH_SHORT).show();
      return;
    }
    BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.Finan_BottomSheetDialog);
    dialog.setContentView(R.layout.dialog_confirm_delete);
    TextView titleView = dialog.findViewById(R.id.confirm_delete_title);
    TextView messageView = dialog.findViewById(R.id.confirm_delete_message);
    View cancelBtn = dialog.findViewById(R.id.confirm_delete_cancel);
    View confirmBtn = dialog.findViewById(R.id.confirm_delete_confirm);

    if (titleView != null) titleView.setText(R.string.java_WalletListFragment_hapus_dompet);
    if (messageView != null) messageView.setText(getString(R.string.delete_wallet_confirmation, wallet.getName()));
    if (cancelBtn != null) cancelBtn.setOnClickListener(v -> dialog.dismiss());
    if (confirmBtn != null) {
      confirmBtn.setOnClickListener(v -> {
          final long deleteWalletId = wallet.getId();
          final String deleteName = wallet.getName();
          final String deleteCurrencyCode = wallet.getCurrencyCode();
          final boolean deleteIsDefault = wallet.isDefault();
          final long deleteOpeningBalanceMinor = wallet.getOpeningBalanceMinor();
          final long deleteCreatedAt = wallet.getCreatedAt();
          final String deleteIcon = wallet.getIcon();

          dialog.dismiss();

          services.dbWorker.compute(
              () -> services.walletService.delete(deleteWalletId),
              success -> {
                if (!isAdded()) return;
                if (Boolean.TRUE.equals(success)) {
                  FinanToast.show(
                      requireActivity(),
                      "Dompet \"" + deleteName + "\" dihapus",
                      "Urungkan",
                      () -> services.dbWorker.compute(
                          () -> services.walletService.insertUndo(
                              deleteName, deleteCurrencyCode, deleteIsDefault,
                              deleteOpeningBalanceMinor, deleteCreatedAt, deleteIcon),
                          id -> reload()));
                  reload();
                } else {
                  Toast.makeText(requireContext(), "Gagal menghapus dompet", Toast.LENGTH_SHORT).show();
                }
              });
        });
    }
    BottomSheetHelper.show(dialog);
  }

  private final class WalletListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HERO = 0;
    private static final int TYPE_SECTION = 1;
    private static final int TYPE_WALLET = 2;
    private static final int TYPE_EMPTY = 3;

    private List<Wallet> wallets = new ArrayList<>();

    void setWallets(List<Wallet> newWallets) {
      this.wallets = newWallets != null ? newWallets : new ArrayList<>();
      notifyDataSetChanged();
    }

    List<Wallet> getWallets() {
      return new ArrayList<>(wallets);
    }

    @Override
    public int getItemViewType(int position) {
      if (position == 0) return TYPE_HERO;
      if (position == 1) return TYPE_SECTION;
      if (wallets.isEmpty()) return TYPE_EMPTY;
      return TYPE_WALLET;
    }

    @Override
    public int getItemCount() {
      return 2 + Math.max(wallets.size(), 1);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      if (viewType == TYPE_HERO) {
        View view = inflater.inflate(R.layout.item_wallet_hero, parent, false);
        return new HeroViewHolder(view);
      } else if (viewType == TYPE_SECTION) {
        View view = inflater.inflate(R.layout.item_wallet_section_label, parent, false);
        return new SectionViewHolder(view);
      } else if (viewType == TYPE_EMPTY) {
        View view = inflater.inflate(R.layout.item_wallet_empty, parent, false);
        return new EmptyViewHolder(view);
      } else {
        View view = inflater.inflate(R.layout.item_wallet, parent, false);
        return new WalletViewHolder(view);
      }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
      int viewType = getItemViewType(position);
      if (viewType == TYPE_HERO) {
        ((HeroViewHolder) holder).bind(wallets);
      } else if (viewType == TYPE_WALLET) {
        ((WalletViewHolder) holder).bind(wallets.get(position - 2));
      } else if (viewType == TYPE_EMPTY) {
        ((EmptyViewHolder) holder).bind();
      }
    }

    final class HeroViewHolder extends RecyclerView.ViewHolder {
      private final TextView totalBalanceView;
      private final TextView countView;
      private final TextView defaultWalletView;
      private final Button addButton;
      private final Button transferButton;

      HeroViewHolder(@NonNull View itemView) {
        super(itemView);
        totalBalanceView = itemView.findViewById(R.id.wallet_hero_total_balance);
        countView = itemView.findViewById(R.id.wallet_hero_count);
        defaultWalletView = itemView.findViewById(R.id.wallet_hero_default);
        addButton = itemView.findViewById(R.id.wallet_hero_add_btn);
        transferButton = itemView.findViewById(R.id.wallet_hero_transfer_btn);
        ViewPressAnimator.bindScale(addButton);
        ViewPressAnimator.bindScale(transferButton);
      }

      void bind(List<Wallet> walletList) {
        Map<String, Long> totalsByCurrency = new LinkedHashMap<>();
        Wallet defaultWallet = null;
        for (Wallet w : walletList) {
          String currencyCode = MoneyFormatter.normalizeCurrencyCode(w.getCurrencyCode());
          totalsByCurrency.compute(
              currencyCode,
              (k, current) -> (current == null ? 0L : current) + w.getCachedBalanceMinor());
          if (w.isDefault()) {
            defaultWallet = w;
          }
        }

        Context context = itemView.getContext();
        boolean masked = BottomSheetHelper.isMasked(context);
        if (masked) {
          totalBalanceView.setText(R.string.wallet_balance_masked);
        } else if (totalsByCurrency.isEmpty()) {
          totalBalanceView.setText(MoneyFormatter.formatWithCurrencyCode("IDR", 0L));
        } else {
          StringBuilder sb = new StringBuilder();
          int i = 0;
          for (Map.Entry<String, Long> entry : totalsByCurrency.entrySet()) {
            if (i++ > 0) sb.append("\n");
            sb.append(MoneyFormatter.formatWithCurrencyCode(entry.getKey(), entry.getValue()));
          }
          totalBalanceView.setText(sb.toString());
        }

        countView.setText(
            context.getResources().getQuantityString(R.plurals.wallet_count, walletList.size(), walletList.size()));
        String defaultName = defaultWallet != null ? defaultWallet.getName() : context.getString(R.string.wallet_default_none);
        defaultWalletView.setText(context.getString(R.string.wallet_hero_default_indicator, defaultName));

        addButton.setOnClickListener(v -> showAddWalletDialog());
        transferButton.setEnabled(walletList.size() > 1);
        transferButton.setAlpha(walletList.size() > 1 ? 1.0f : 0.5f);
        transferButton.setOnClickListener(v -> showTransferDialog(walletList.isEmpty() ? null : walletList.get(0)));
      }
    }

    final class SectionViewHolder extends RecyclerView.ViewHolder {
      SectionViewHolder(@NonNull View itemView) {
        super(itemView);
      }
    }

    final class EmptyViewHolder extends RecyclerView.ViewHolder {
      private final Button emptyActionButton;

      EmptyViewHolder(@NonNull View itemView) {
        super(itemView);
        emptyActionButton = itemView.findViewById(R.id.wallet_empty_action);
        ViewPressAnimator.bindScale(emptyActionButton);
      }

      void bind() {
        emptyActionButton.setOnClickListener(v -> showAddWalletDialog());
      }
    }

    final class WalletViewHolder extends RecyclerView.ViewHolder {
      private final View cardView;
      private final ImageView iconImage;
      private final TextView iconEmoji;
      private final TextView nameView;
      private final TextView defaultBadge;
      private final TextView currencyView;
      private final TextView balanceView;
      private final ImageButton editButton;
      private final ImageButton deleteButton;
      private final Button adjustButton;
      private final Button transferButton;

      WalletViewHolder(@NonNull View itemView) {
        super(itemView);
        cardView = itemView.findViewById(R.id.item_wallet_card);
        iconImage = itemView.findViewById(R.id.item_wallet_icon_image);
        iconEmoji = itemView.findViewById(R.id.item_wallet_icon_emoji);
        nameView = itemView.findViewById(R.id.item_wallet_name);
        defaultBadge = itemView.findViewById(R.id.item_wallet_default_badge);
        currencyView = itemView.findViewById(R.id.item_wallet_currency);
        balanceView = itemView.findViewById(R.id.item_wallet_balance);
        editButton = itemView.findViewById(R.id.item_wallet_edit);
        deleteButton = itemView.findViewById(R.id.item_wallet_delete);
        adjustButton = itemView.findViewById(R.id.item_wallet_adjust);
        transferButton = itemView.findViewById(R.id.item_wallet_transfer);

        ViewPressAnimator.bindScale(cardView);
        ViewPressAnimator.bindScale(editButton);
        ViewPressAnimator.bindScale(deleteButton);
        ViewPressAnimator.bindScale(adjustButton);
        ViewPressAnimator.bindScale(transferButton);
      }

      void bind(Wallet wallet) {
        Context context = itemView.getContext();
        cardView.setBackgroundResource(
            wallet.isDefault() ? R.drawable.bg_wallet_card_default : R.drawable.bg_wallet_card);

        String icon = wallet.getIcon();
        if (!TextUtils.isEmpty(icon)) {
          iconEmoji.setText(icon);
          iconEmoji.setVisibility(View.VISIBLE);
          iconImage.setVisibility(View.GONE);
        } else {
          iconEmoji.setVisibility(View.GONE);
          iconImage.setVisibility(View.VISIBLE);
        }

        nameView.setText(wallet.getName());
        defaultBadge.setVisibility(wallet.isDefault() ? View.VISIBLE : View.GONE);
        currencyView.setText(MoneyFormatter.normalizeCurrencyCode(wallet.getCurrencyCode()));

        if (BottomSheetHelper.isMasked(context)) {
          balanceView.setText(R.string.wallet_balance_masked);
        } else {
          balanceView.setText(
              MoneyFormatter.formatWithCurrencyCode(
                  wallet.getCurrencyCode(), wallet.getCachedBalanceMinor()));
        }

        editButton.setOnClickListener(v -> showEditWalletDialog(wallet));
        boolean canDelete = wallets.size() > 1;
        deleteButton.setAlpha(canDelete ? 1.0f : 0.4f);
        deleteButton.setOnClickListener(v -> showDeleteWalletDialog(wallet));
        adjustButton.setOnClickListener(v -> showAdjustmentDialog(wallet));

        boolean canTransfer = wallets.size() > 1;
        transferButton.setEnabled(canTransfer);
        transferButton.setAlpha(canTransfer ? 1.0f : 0.5f);
        transferButton.setOnClickListener(v -> showTransferDialog(wallet));
      }
    }
  }
}
