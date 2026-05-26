package com.dwlhm.finan.data.entity;

public final class Wallet {

  private final long id;
  private final String name;
  private final String currencyCode;
  private final boolean isDefault;
  private final long cachedBalanceMinor;
  private final long createdAt;

  public Wallet(
      long id,
      String name,
      String currencyCode,
      boolean isDefault,
      long cachedBalanceMinor,
      long createdAt) {
    this.id = id;
    this.name = name;
    this.currencyCode = currencyCode;
    this.isDefault = isDefault;
    this.cachedBalanceMinor = cachedBalanceMinor;
    this.createdAt = createdAt;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public long getCachedBalanceMinor() {
    return cachedBalanceMinor;
  }

  public long getCreatedAt() {
    return createdAt;
  }
}
