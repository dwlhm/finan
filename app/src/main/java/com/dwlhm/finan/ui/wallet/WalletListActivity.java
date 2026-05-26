package com.dwlhm.finan.ui.wallet;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BaseActivity;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.util.ArrayList;
import java.util.List;

public class WalletListActivity extends BaseActivity {

  private AppServices services;
  private WalletAdapter adapter;
  private ListView listView;
  private TextView emptyView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    services = ServicesProvider.get(this);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_wallet_list;
  }

  @Override
  protected void onReady() {
    listView = findViewById(R.id.wallet_list);
    emptyView = findViewById(R.id.wallet_empty);
    adapter = new WalletAdapter(this);
    listView.setAdapter(adapter);
    reload();
  }

  @Override
  protected void onResume() {
    super.onResume();
    reload();
  }

  private void reload() {
    adapter.setWallets(services.walletDao.findAll());
    boolean empty = adapter.getCount() == 0;
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    listView.setVisibility(empty ? View.GONE : View.VISIBLE);
  }

  private class WalletAdapter extends BaseAdapter {

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
      TextView name = convertView.findViewById(R.id.item_wallet_name);
      TextView balance = convertView.findViewById(R.id.item_wallet_balance);
      Wallet wallet = getItem(position);
      name.setText(wallet.getName());
      balance.setText(
          context.getString(
              R.string.wallet_balance_format, MoneyFormatter.format(wallet.getCachedBalanceMinor())));
      return convertView;
    }
  }
}
