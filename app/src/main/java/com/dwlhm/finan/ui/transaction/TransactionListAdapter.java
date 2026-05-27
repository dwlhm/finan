package com.dwlhm.finan.ui.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionListAdapter extends BaseAdapter {

  private final Context context;
  private final LayoutInflater inflater;
  private final CategoryDao categoryTable;
  private final WalletDao walletTable;
  private final List<Transaction> items = new ArrayList<>();
  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("d MMM yyyy, HH:mm", new Locale("id", "ID"));

  public TransactionListAdapter(
      Context context, CategoryDao categoryTable, WalletDao walletTable) {
    this.context = context;
    this.inflater = LayoutInflater.from(context);
    this.categoryTable = categoryTable;
    this.walletTable = walletTable;
  }

  public void setTransactions(List<Transaction> transactions) {
    items.clear();
    if (transactions != null) {
      items.addAll(transactions);
    }
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return items.size();
  }

  @Override
  public Transaction getItem(int position) {
    return items.get(position);
  }

  @Override
  public long getItemId(int position) {
    return items.get(position).getId();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ViewHolder holder;
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.item_transaction, parent, false);
      holder = new ViewHolder();
      holder.category = convertView.findViewById(R.id.item_transaction_category);
      holder.amount = convertView.findViewById(R.id.item_transaction_amount);
      holder.meta = convertView.findViewById(R.id.item_transaction_meta);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    Transaction transaction = getItem(position);
    Category category = categoryTable.findById(transaction.getCategoryId());
    Wallet wallet = walletTable.findById(transaction.getWalletId());

    holder.category.setText(category != null ? category.getName() : "—");

    String formatted = MoneyFormatter.format(transaction.getAmountMinor());
    if (transaction.getType() == TransactionType.INCOME) {
      holder.amount.setText("+" + formatted);
      holder.amount.setTextColor(ContextCompat.getColor(context, R.color.finan_income));
    } else {
      holder.amount.setText("-" + formatted);
      holder.amount.setTextColor(ContextCompat.getColor(context, R.color.finan_expense));
    }

    String walletName = wallet != null ? wallet.getName() : "";
    String when = dateFormat.format(new Date(transaction.getOccurredAt()));
    holder.meta.setText(walletName + " · " + when);

    return convertView;
  }

  private static class ViewHolder {
    TextView category;
    TextView amount;
    TextView meta;
  }
}
