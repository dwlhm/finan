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
    reload();
  }

  @Override
  public void onResume() {
    super.onResume();
    reload();
  }

  private void reload() {
    List<Wallet> wallets = services.walletDao.findAll();
    adapter.setWallets(wallets);
    bindSummary(wallets);
    boolean empty = wallets.isEmpty();
    summaryView.setVisibility(empty ? View.GONE : View.VISIBLE);
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    listView.setVisibility(empty ? View.GONE : View.VISIBLE);
  }

  private void bindSummary(List<Wallet> wallets) {
    Map<String, Long> totalsByCurrency = new LinkedHashMap<>();
    Wallet defaultWallet = null;
    for (Wallet wallet : wallets) {
      String currencyCode = MoneyFormatter.normalizeCurrencyCode(wallet.getCurrencyCode());
      Long current = totalsByCurrency.get(currencyCode);
      totalsByCurrency.put(
          currencyCode, (current == null ? 0L : current) + wallet.getCachedBalanceMinor());
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
    walletCountView.setText(getString(R.string.wallet_count_format, wallets.size()));
    String defaultWalletName =
        defaultWallet == null ? getString(R.string.wallet_default_none) : defaultWallet.getName();
    defaultWalletView.setText(getString(R.string.wallet_default_format, defaultWalletName));
  }

  private void goBack() {
    requireActivity().getSupportFragmentManager().popBackStack();
  }

  private void showAddWalletDialog() {
    Dialog dialog = new Dialog(requireContext());
    View content =
        LayoutInflater.from(requireContext()).inflate(R.layout.dialog_wallet_input, null, false);
    LabeledEditTextView nameField = content.findViewById(R.id.wallet_name_field);
    LabeledEditTextView balanceField = content.findViewById(R.id.wallet_balance_field);
    EditText nameInput = nameField.getEditText();
    EditText balanceInput = balanceField.getEditText();
    MoneyInputFormatter.attach(balanceInput, true);
    CheckBox defaultInput = content.findViewById(R.id.wallet_default_input);
    DialogActionsView actionsView = content.findViewById(R.id.wallet_actions);
    boolean firstWallet = services.walletDao.findAll().isEmpty();

    defaultInput.setChecked(firstWallet);
    defaultInput.setEnabled(!firstWallet);

    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(content);
    actionsView.setOnCancelClickListener(v -> dialog.dismiss());
    actionsView.setOnPrimaryClickListener(
        v -> {
          if (createWallet(nameInput, balanceInput, defaultInput)) {
            dialog.dismiss();
          }
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
    View content =
        LayoutInflater.from(requireContext()).inflate(R.layout.dialog_wallet_name_input, null, false);
    LabeledEditTextView nameField = content.findViewById(R.id.wallet_name_field);
    EditText nameInput = nameField.getEditText();
    CheckBox defaultInput = content.findViewById(R.id.wallet_default_input);
    DialogActionsView actionsView = content.findViewById(R.id.wallet_actions);

    nameInput.setText(wallet.getName());
    nameInput.setSelection(nameInput.getText().length());
    defaultInput.setChecked(wallet.isDefault());
    defaultInput.setEnabled(!wallet.isDefault());
    defaultInput.setAlpha(wallet.isDefault() ? 0.72f : 1f);

    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(content);
    actionsView.setOnCancelClickListener(v -> dialog.dismiss());
    actionsView.setOnPrimaryClickListener(
        v -> {
          if (updateWallet(wallet.getId(), nameInput, defaultInput)) {
            dialog.dismiss();
          }
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

  private boolean createWallet(
      EditText nameInput,
      EditText balanceInput,
      CheckBox defaultInput) {
    String name = nameInput.getText().toString().trim();
    if (name.isEmpty()) {
      nameInput.setError(getString(R.string.wallet_error_name));
      return false;
    }

    long initialBalanceMinor = 0L;
    String balanceText = balanceInput.getText().toString().trim();
    if (!balanceText.isEmpty()) {
      try {
        initialBalanceMinor = MoneyParser.parse(balanceText);
      } catch (IllegalArgumentException e) {
        balanceInput.setError(getString(R.string.wallet_error_balance));
        return false;
      }
    }

    boolean makeDefault = defaultInput.isChecked();
    long walletId =
        services.walletDao.insert(
            name,
            MoneyFormatter.DEFAULT_CURRENCY_CODE,
            makeDefault,
            initialBalanceMinor,
            System.currentTimeMillis());
    if (walletId <= 0) {
      Toast.makeText(requireContext(), R.string.wallet_error_create, Toast.LENGTH_SHORT).show();
      return false;
    }

    if (makeDefault) {
      services.walletDao.clearDefaultWalletsExcept(walletId);
      services.defaultsStore.setDefaultWalletId(walletId);
    }
    Toast.makeText(requireContext(), R.string.wallet_created, Toast.LENGTH_SHORT).show();
    reload();
    return true;
  }

  private boolean updateWallet(
      long walletId,
      EditText nameInput,
      CheckBox defaultInput) {
    String name = nameInput.getText().toString().trim();
    if (name.isEmpty()) {
      nameInput.setError(getString(R.string.wallet_error_name));
      return false;
    }
    boolean makeDefault = defaultInput.isChecked();
    if (!services.walletDao.updateNameAndDefault(walletId, name, makeDefault)) {
      Toast.makeText(requireContext(), R.string.wallet_error_update, Toast.LENGTH_SHORT).show();
      return false;
    }
    if (makeDefault) {
      services.defaultsStore.setDefaultWalletId(walletId);
    }
    Toast.makeText(requireContext(), R.string.wallet_updated, Toast.LENGTH_SHORT).show();
    reload();
    return true;
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
