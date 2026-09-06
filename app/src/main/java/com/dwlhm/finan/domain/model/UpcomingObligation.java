package com.dwlhm.finan.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public final class UpcomingObligation {

  private final long templateId;
  @NonNull private final String name;
  @NonNull private final TransactionType type;
  private final long amountMinor;
  @Nullable private final Long walletId;
  @Nullable private final String walletName;
  @Nullable private final Long categoryId;
  @Nullable private final String categoryName;
  @NonNull private final String icon;
  private final long dueEpochMillis;
  private final int dueDay;
  private final int daysRemaining;
  @NonNull private final RecurringFrequency frequency;

  public UpcomingObligation(
      long templateId,
      @NonNull String name,
      @NonNull TransactionType type,
      long amountMinor,
      @Nullable Long walletId,
      @Nullable String walletName,
      @Nullable Long categoryId,
      @Nullable String categoryName,
      @Nullable String icon,
      long dueEpochMillis,
      int dueDay,
      int daysRemaining,
      @Nullable RecurringFrequency frequency) {
    this.templateId = templateId;
    this.name = name;
    this.type = type;
    this.amountMinor = amountMinor;
    this.walletId = walletId;
    this.walletName = walletName;
    this.categoryId = categoryId;
    this.categoryName = categoryName;
    this.icon = (icon != null && !icon.isEmpty()) ? icon : "⚡";
    this.dueEpochMillis = dueEpochMillis;
    this.dueDay = dueDay;
    this.daysRemaining = daysRemaining;
    this.frequency = frequency != null ? frequency : RecurringFrequency.NONE;
  }

  public long getTemplateId() {
    return templateId;
  }

  @NonNull
  public String getName() {
    return name;
  }

  @NonNull
  public TransactionType getType() {
    return type;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  @Nullable
  public Long getWalletId() {
    return walletId;
  }

  @Nullable
  public String getWalletName() {
    return walletName;
  }

  @Nullable
  public Long getCategoryId() {
    return categoryId;
  }

  @Nullable
  public String getCategoryName() {
    return categoryName;
  }

  @NonNull
  public String getIcon() {
    return icon;
  }

  public long getDueEpochMillis() {
    return dueEpochMillis;
  }

  public int getDueDay() {
    return dueDay;
  }

  public int getDaysRemaining() {
    return daysRemaining;
  }

  @NonNull
  public RecurringFrequency getFrequency() {
    return frequency;
  }

  public boolean isExpense() {
    return type == TransactionType.EXPENSE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UpcomingObligation that = (UpcomingObligation) o;
    return templateId == that.templateId
        && amountMinor == that.amountMinor
        && dueEpochMillis == that.dueEpochMillis
        && dueDay == that.dueDay
        && daysRemaining == that.daysRemaining
        && Objects.equals(name, that.name)
        && type == that.type
        && Objects.equals(walletId, that.walletId)
        && Objects.equals(walletName, that.walletName)
        && Objects.equals(categoryId, that.categoryId)
        && Objects.equals(categoryName, that.categoryName)
        && Objects.equals(icon, that.icon)
        && frequency == that.frequency;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        templateId,
        name,
        type,
        amountMinor,
        walletId,
        walletName,
        categoryId,
        categoryName,
        icon,
        dueEpochMillis,
        dueDay,
        daysRemaining,
        frequency);
  }
}
