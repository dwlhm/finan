package com.dwlhm.finan.ui.transaction;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;

public final class TransactionRowLabels {

  private TransactionRowLabels() {}

  public static String title(
      @NonNull Context context, @NonNull Transaction transaction, @Nullable Category category) {
    if (transaction.getType().isRegular()) {
      return category != null
          ? category.getName()
          : context.getString(R.string.transaction_system_category);
    }
    return context.getString(typeLabel(transaction.getType()));
  }

  @StringRes
  public static int typeLabel(TransactionType type) {
    return switch (type) {
      case INCOME -> R.string.capture_type_income;
      case EXPENSE -> R.string.capture_type_expense;
      case ADJUSTMENT_INCREASE -> R.string.transaction_adjustment_increase;
      case ADJUSTMENT_DECREASE -> R.string.transaction_adjustment_decrease;
      case TRANSFER_IN -> R.string.transaction_transfer_in;
      case TRANSFER_OUT -> R.string.transaction_transfer_out;
    };
  }

  @StringRes
  public static int amountFormat(TransactionType type) {
    return type.increasesBalance()
        ? R.string.transaction_income_amount_format
        : R.string.transaction_expense_amount_format;
  }

  @ColorRes
  public static int amountColor(TransactionType type) {
    return type.increasesBalance() ? R.color.finan_income : R.color.finan_expense;
  }


}
