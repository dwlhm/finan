package com.dwlhm.finan.data.entity;

import androidx.annotation.Nullable;

public final class Wallet {

  private final long id;
  private final String name;
  private final String currencyCode;
  private final boolean isDefault;
  private final long openingBalanceMinor;
  private final long cachedBalanceMinor;
  private final long createdAt;
  private final int usageCount;
  @Nullable private final String icon;

  public Wallet(
      long id,
      String name,
      String currencyCode,
      boolean isDefault,
      long openingBalanceMinor,
      long cachedBalanceMinor,
      long createdAt) {
    this(id, name, currencyCode, isDefault, openingBalanceMinor, cachedBalanceMinor, createdAt, 0, null);
  }

  public Wallet(
      long id,
      String name,
      String currencyCode,
      boolean isDefault,
      long openingBalanceMinor,
      long cachedBalanceMinor,
      long createdAt,
      int usageCount) {
    this(id, name, currencyCode, isDefault, openingBalanceMinor, cachedBalanceMinor, createdAt, usageCount, null);
  }

  public Wallet(
      long id,
      String name,
      String currencyCode,
      boolean isDefault,
      long openingBalanceMinor,
      long cachedBalanceMinor,
      long createdAt,
      int usageCount,
      @Nullable String icon) {
    this.id = id;
    this.name = name;
    this.currencyCode = currencyCode;
    this.isDefault = isDefault;
    this.openingBalanceMinor = openingBalanceMinor;
    this.cachedBalanceMinor = cachedBalanceMinor;
    this.createdAt = createdAt;
    this.usageCount = usageCount;
    this.icon = icon;
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

  public long getOpeningBalanceMinor() {
    return openingBalanceMinor;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public int getUsageCount() {
    return usageCount;
  }

  @Nullable
  public String getIcon() {
    return icon;
  }
}
