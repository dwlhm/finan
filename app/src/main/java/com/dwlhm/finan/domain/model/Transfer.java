package com.dwlhm.finan.domain.model;

public final class Transfer {

  private final long id;
  private final long sourceWalletId;
  private final long destinationWalletId;
  private final long amountMinor;
  private final long occurredAt;
  private final String note;

  public Transfer(
      long id,
      long sourceWalletId,
      long destinationWalletId,
      long amountMinor,
      long occurredAt,
      String note) {
    this.id = id;
    this.sourceWalletId = sourceWalletId;
    this.destinationWalletId = destinationWalletId;
    this.amountMinor = amountMinor;
    this.occurredAt = occurredAt;
    this.note = note;
  }

  public long getId() {
    return id;
  }

  public long getSourceWalletId() {
    return sourceWalletId;
  }

  public long getDestinationWalletId() {
    return destinationWalletId;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public long getOccurredAt() {
    return occurredAt;
  }

  public String getNote() {
    return note;
  }
}
