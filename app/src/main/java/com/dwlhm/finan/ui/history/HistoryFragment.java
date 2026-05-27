package com.dwlhm.finan.ui.history;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.transaction.TransactionDetailDialog;
import com.dwlhm.finan.ui.transaction.TransactionListAdapter;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.util.List;

public final class HistoryFragment extends ScreenFragment {

  private static final int HISTORY_LIMIT = 200;

  private AppServices services;
  private TransactionListAdapter adapter;
  private ListView listView;
  private View emptyView;
  private View summaryView;
  private TextView countView;
  private TextView totalTransactionsView;
  private TextView incomeTotalView;
  private TextView expenseTotalView;

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
    summaryView = view.findViewById(R.id.history_summary);
    countView = view.findViewById(R.id.history_count);
    totalTransactionsView = view.findViewById(R.id.history_total_transactions);
    incomeTotalView = view.findViewById(R.id.history_income_total);
    expenseTotalView = view.findViewById(R.id.history_expense_total);
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
    List<Transaction> transactions = services.transactionService.getRecent(HISTORY_LIMIT);
    adapter.setTransactions(transactions);
    renderSummary(transactions);
    boolean empty = transactions.isEmpty();
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    summaryView.setVisibility(empty ? View.GONE : View.VISIBLE);
    countView.setVisibility(empty ? View.GONE : View.VISIBLE);
    listView.setVisibility(empty ? View.GONE : View.VISIBLE);
  }

  private void renderSummary(List<Transaction> transactions) {
    long incomeMinor = 0L;
    long expenseMinor = 0L;

    for (Transaction transaction : transactions) {
      if (transaction.getType() == TransactionType.INCOME) {
        incomeMinor += transaction.getAmountMinor();
      } else {
        expenseMinor += transaction.getAmountMinor();
      }
    }

    int count = transactions.size();
    countView.setText(getString(R.string.history_count_format, count));
    totalTransactionsView.setText(String.valueOf(count));
    incomeTotalView.setText(MoneyFormatter.format(incomeMinor));
    expenseTotalView.setText(MoneyFormatter.format(expenseMinor));
  }
}
