package com.dwlhm.finan.ui.transaction;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dwlhm.finan.R;

public final class TransactionItemViewHolder extends RecyclerView.ViewHolder {

  final TextView dateHeader;
  final ImageView icon;
  final TextView emoji;
  final TextView category;
  final TextView wallet;
  final TextView amount;
  final TextView meta;
  final TextView note;

  TransactionItemViewHolder(@NonNull View itemView) {
    super(itemView);
    dateHeader = itemView.findViewById(R.id.item_transaction_date_header);
    icon = itemView.findViewById(R.id.item_transaction_icon);
    emoji = itemView.findViewById(R.id.item_transaction_emoji);
    category = itemView.findViewById(R.id.item_transaction_category);
    wallet = itemView.findViewById(R.id.item_transaction_wallet);
    amount = itemView.findViewById(R.id.item_transaction_amount);
    meta = itemView.findViewById(R.id.item_transaction_meta);
    note = itemView.findViewById(R.id.item_transaction_note);
  }
}
