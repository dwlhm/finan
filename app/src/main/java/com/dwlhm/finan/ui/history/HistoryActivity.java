package com.dwlhm.finan.ui.history;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BaseActivity;
import com.dwlhm.finan.ui.common.ServicesProvider;

public class HistoryActivity extends BaseActivity {

  private AppServices services;
  private TransactionListAdapter adapter;
  private ListView listView;
  private TextView emptyView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    services = ServicesProvider.get(this);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_history;
  }

  @Override
  protected void onReady() {
    listView = findViewById(R.id.history_list);
    emptyView = findViewById(R.id.history_empty);
    adapter =
        new TransactionListAdapter(this, services.categoryDao, services.walletDao);
    listView.setAdapter(adapter);
    reload();
  }

  @Override
  protected void onResume() {
    super.onResume();
    reload();
  }

  private void reload() {
    adapter.setTransactions(services.transactionService.getRecent(200));
    boolean empty = adapter.getCount() == 0;
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    listView.setVisibility(empty ? View.GONE : View.VISIBLE);
  }
}
