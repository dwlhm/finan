package com.dwlhm.finan.ui.history;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.transaction.TransactionDetailDialog;
import com.dwlhm.finan.ui.transaction.TransactionListAdapter;

public final class HistoryFragment extends ScreenFragment {

  private AppServices services;
  private TransactionListAdapter adapter;
  private ListView listView;
  private TextView emptyView;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    services = ServicesProvider.get(requireContext());
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_history;
  }

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    listView = view.findViewById(R.id.history_list);
    emptyView = view.findViewById(R.id.history_empty);
    adapter =
        new TransactionListAdapter(requireContext(), services.categoryDao, services.walletDao);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(
        (parent, itemView, position, id) ->
            new TransactionDetailDialog(
                    requireContext(), services, adapter.getItem(position), this::reload)
                .show());
    reload();
  }

  @Override
  public void onResume() {
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
