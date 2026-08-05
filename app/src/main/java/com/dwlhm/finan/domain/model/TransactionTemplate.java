package com.dwlhm.finan.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class TransactionTemplate {

  private long id;
  @NonNull private String name;
  @NonNull private TransactionType type;
  private long amountMinor;
  @Nullable private Long categoryId;
  @Nullable private Long walletId;
  @Nullable private Long destinationWalletId;
  @Nullable private String note;
  @NonNull private String icon;
  private int sortOrder;

  public TransactionTemplate(
      long id,
      @NonNull String name,
      @NonNull TransactionType type,
      long amountMinor,
      @Nullable Long categoryId,
      @Nullable Long walletId,
      @Nullable Long destinationWalletId,
      @Nullable String note,
      @NonNull String icon,
      int sortOrder) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.amountMinor = amountMinor;
    this.categoryId = categoryId;
    this.walletId = walletId;
    this.destinationWalletId = destinationWalletId;
    this.note = note;
    this.icon = icon.isEmpty() ? "⚡" : icon;
    this.sortOrder = sortOrder;
  }

  public TransactionTemplate(
      @NonNull String name,
      @NonNull TransactionType type,
      long amountMinor,
      @Nullable Long categoryId,
      @Nullable Long walletId,
      @Nullable Long destinationWalletId,
      @Nullable String note,
      @NonNull String icon) {
    this(0L, name, type, amountMinor, categoryId, walletId, destinationWalletId, note, icon, 0);
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @NonNull
  public String getName() {
    return name;
  }

  public void setName(@NonNull String name) {
    this.name = name;
  }

  @NonNull
  public TransactionType getType() {
    return type;
  }

  public void setType(@NonNull TransactionType type) {
    this.type = type;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public void setAmountMinor(long amountMinor) {
    this.amountMinor = amountMinor;
  }

  @Nullable
  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(@Nullable Long categoryId) {
    this.categoryId = categoryId;
  }

  @Nullable
  public Long getWalletId() {
    return walletId;
  }

  public void setWalletId(@Nullable Long walletId) {
    this.walletId = walletId;
  }

  @Nullable
  public Long getDestinationWalletId() {
    return destinationWalletId;
  }

  public void setDestinationWalletId(@Nullable Long destinationWalletId) {
    this.destinationWalletId = destinationWalletId;
  }

  @Nullable
  public String getNote() {
    return note;
  }

  public void setNote(@Nullable String note) {
    this.note = note;
  }

  @NonNull
  public String getIcon() {
    return icon;
  }

  public void setIcon(@NonNull String icon) {
    this.icon = icon;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TransactionTemplate that = (TransactionTemplate) o;
    return id == that.id &&
        amountMinor == that.amountMinor &&
        sortOrder == that.sortOrder &&
        Objects.equals(name, that.name) &&
        type == that.type &&
        Objects.equals(categoryId, that.categoryId) &&
        Objects.equals(walletId, that.walletId) &&
        Objects.equals(destinationWalletId, that.destinationWalletId) &&
        Objects.equals(note, that.note) &&
        Objects.equals(icon, that.icon);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, type, amountMinor, categoryId, walletId, destinationWalletId, note, icon, sortOrder);
  }
}
