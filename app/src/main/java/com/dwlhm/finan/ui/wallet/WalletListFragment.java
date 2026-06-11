package com.dwlhm.finan.ui.wallet;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ScreenHeaderView;
import com.dwlhm.finan.ui.common.ServicesProvider;
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

    totalBalanceView.setText(MoneyFormatter.formatTotalsByCurrency(totalsByCurrency));
    int balanceColor =
        MoneyFormatter.containsOnlyNegativeTotals(totalsByCurrency)
            ? R.color.finan_expense
            : R.color.finan_primary;
    totalBalanceView.setTextColor(
        ContextCompat.getColor(requireContext(), balanceColor));
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
    Dialog dialog = new Dialog(requireContext());
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.dialog_wallet_input);
    LabeledEditTextView nameField = dialog.findViewById(R.id.wallet_name_field);
    LabeledEditTextView balanceField = dialog.findViewById(R.id.wallet_balance_field);
    EditText nameInput = nameField.getEditText();
    EditText balanceInput = balanceField.getEditText();
    MoneyInputFormatter.attach(balanceInput, true);
    CheckBox defaultInput = dialog.findViewById(R.id.wallet_default_input);
    DialogActionsView actionsView = dialog.findViewById(R.id.wallet_actions);
    actionsView.setOnCancelClickListener(v -> dialog.dismiss());
    actionsView.setOnPrimaryClickListener(v -> submitCreateWallet(dialog, nameInput, balanceInput, defaultInput));
    services.dbWorker.compute(
        () -> services.walletDao.findAll().isEmpty(),
        firstWallet -> {
          if (!dialog.isShowing() || firstWallet == null) {
            return;
          }
          defaultInput.setChecked(firstWallet);
          defaultInput.setEnabled(!firstWallet);
        });
    dialog.show();
    Window window = dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      window.setLayout(
          WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
    nameInput.requestFocus();
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
    dialog.show();
    Window window = dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      window.setLayout(
          WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
    nameInput.requestFocus();
  }

  private void submitCreateWallet(
      Dialog dialog, EditText nameInput, EditText balanceInput, CheckBox defaultInput) {
    String name = nameInput.getText().toString().trim();
    if (name.isEmpty()) {
      nameInput.setError(getString(R.string.wallet_error_name));
      return;
    }

    long initialBalanceMinor = 0L;
    String balanceText = balanceInput.getText().toString().trim();
    if (!balanceText.isEmpty()) {
      try {
        initialBalanceMinor = MoneyParser.parse(balanceText);
      } catch (IllegalArgumentException e) {
        balanceInput.setError(getString(R.string.wallet_error_balance));
        return;
      }
    }

    boolean makeDefault = defaultInput.isChecked();
    long parsedBalance = initialBalanceMinor;
    services.dbWorker.compute(
        () -> {
          long walletId =
              services.walletDao.insert(
                  name,
                  MoneyFormatter.DEFAULT_CURRENCY_CODE,
                  makeDefault,
                  parsedBalance,
                  System.currentTimeMillis());
          if (walletId <= 0L) {
            return Boolean.FALSE;
          }
          if (makeDefault) {
            services.walletDao.clearDefaultWalletsExcept(walletId);
            services.defaultsStore.setDefaultWalletId(walletId);
          }
          return Boolean.TRUE;
        },
        created -> {
          if (!isAdded() || !dialog.isShowing()) {
            return;
          }
          if (!Boolean.TRUE.equals(created)) {
            Toast.makeText(requireContext(), R.string.wallet_error_create, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          Toast.makeText(requireContext(), R.string.wallet_created, Toast.LENGTH_SHORT).show();
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
      Wallet wallet = getItem(position);
      String currencyCode = MoneyFormatter.normalizeCurrencyCode(wallet.getCurrencyCode());
      name.setText(wallet.getName());
      card.setBackgroundResource(
          wallet.isDefault() ? R.drawable.bg_card_wallet_default : R.drawable.bg_card);
      defaultBadge.setVisibility(wallet.isDefault() ? View.VISIBLE : View.GONE);
      balance.setText(
          MoneyFormatter.formatWithCurrencyCode(currencyCode, wallet.getCachedBalanceMinor()));
      balance.setTextColor(
          ContextCompat.getColor(
              context,
              wallet.getCachedBalanceMinor() < 0 ? R.color.finan_expense : R.color.finan_primary));
      ViewPressAnimator.bindScale(editButton);
      editButton.setOnClickListener(v -> showEditWalletDialog(wallet));
      return convertView;
    }
  }
}
