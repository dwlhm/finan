package com.dwlhm.finan.data.entity;

public final class Transaction {

  private final long id;
  private final long amountMinor;
  private final String type;
  private final long walletId;
  private final long categoryId;
  private final long occurredAt;
  private final String note;
  private final String tag;
  private final String merchant;
  private final long createdAt;
  private final long updatedAt;

  public Transaction(
      long id,
      long amountMinor,
      String type,
      long walletId,
      long categoryId,
      long occurredAt,
      String note,
      String tag,
      String merchant,
      long createdAt,
      long updatedAt) {
    this.id = id;
    this.amountMinor = amountMinor;
    this.type = type;
    this.walletId = walletId;
    this.categoryId = categoryId;
    this.occurredAt = occurredAt;
    this.note = note;
    this.tag = tag;
    this.merchant = merchant;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public long getId() {
    return id;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public String getType() {
    return type;
  }

  public long getWalletId() {
    return walletId;
  }

  public long getCategoryId() {
    return categoryId;
  }

  public long getOccurredAt() {
    return occurredAt;
  }

  public String getNote() {
    return note;
  }

  public String getTag() {
    return tag;
  }

  public String getMerchant() {
    return merchant;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }
}
