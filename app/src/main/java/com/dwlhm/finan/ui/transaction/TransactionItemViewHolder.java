package com.dwlhm.finan.ui.transaction;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dwlhm.finan.R;

final class TransactionItemViewHolder extends RecyclerView.ViewHolder {

  final View categoryIndicator;
  final View walletIndicator;
  final TextView category;
  final TextView amount;
  final TextView meta;
  final TextView note;

  TransactionItemViewHolder(@NonNull View itemView) {
    super(itemView);
    categoryIndicator = itemView.findViewById(R.id.item_transaction_category_indicator);
    walletIndicator = itemView.findViewById(R.id.item_transaction_wallet_indicator);
    category = itemView.findViewById(R.id.item_transaction_category);
    amount = itemView.findViewById(R.id.item_transaction_amount);
    meta = itemView.findViewById(R.id.item_transaction_meta);
    note = itemView.findViewById(R.id.item_transaction_note);
  }
}
