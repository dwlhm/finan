package com.dwlhm.finan.domain.model;

public final class WalletBalance {

  private final long walletId;
  private final String walletName;
  private final long balanceMinor;

  public WalletBalance(long walletId, String walletName, long balanceMinor) {
    this.walletId = walletId;
    this.walletName = walletName;
    this.balanceMinor = balanceMinor;
  }

  public long getWalletId() {
    return walletId;
  }

  public String getWalletName() {
    return walletName;
  }

  public long getBalanceMinor() {
    return balanceMinor;
  }
}
