package com.dwlhm.finan.service.transaction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;

import java.util.List;

public final class TransactionTemplateService {

  public static boolean isValidTemplate(@Nullable TransactionTemplate template) {
    if (template == null) return false;
    if (template.getName().trim().isEmpty()) return false;
    if (template.getType() == null) return false;
    return true;
  }

  @NonNull
  public static String getTypeDisplayName(@Nullable TransactionType type) {
    if (type == null) return "";
    switch (type) {
      case EXPENSE:
        return "Pengeluaran";
      case INCOME:
        return "Pemasukan";
      case TRANSFER_OUT:
      case TRANSFER_IN:
        return "Transfer";
      case ADJUSTMENT_INCREASE:
      case ADJUSTMENT_DECREASE:
        return "Penyesuaian";
      default:
        return type.name();
    }
  }

  @NonNull
  public static TransactionTemplate createFromInput(
      @NonNull String name,
      @NonNull TransactionType type,
      long amountMinor,
      @Nullable Long categoryId,
      @Nullable Long walletId,
      @Nullable Long destinationWalletId,
      @Nullable String note,
      @Nullable String icon) {
    String cleanName = name.trim();
    if (cleanName.isEmpty()) {
      cleanName = note != null && !note.trim().isEmpty() ? note.trim() : "Template Transaksi";
    }
    String cleanIcon = icon == null || icon.trim().isEmpty() ? "⚡" : icon.trim();
    return new TransactionTemplate(
        0L,
        cleanName,
        type,
        amountMinor,
        categoryId,
        walletId,
        destinationWalletId,
        note != null ? note.trim() : "",
        cleanIcon,
        0
    );
  }
}
