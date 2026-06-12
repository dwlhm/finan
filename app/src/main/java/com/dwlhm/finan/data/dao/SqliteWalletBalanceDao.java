package com.dwlhm.finan.data.dao;

public final class SqliteWalletBalanceDao implements WalletBalanceDao {

  private final WalletDao table;

  public SqliteWalletBalanceDao(WalletDao table) {
    this.table = table;
  }

  @Override
  public long getCachedBalance(long walletId) {
    com.dwlhm.finan.data.entity.Wallet wallet = table.findById(walletId);
    if (wallet == null) {
      throw new IllegalArgumentException("Wallet not found");
    }
    return wallet.getCachedBalanceMinor();
  }

  @Override
  public long getOpeningBalance(long walletId) {
    com.dwlhm.finan.data.entity.Wallet wallet = table.findById(walletId);
    if (wallet == null) {
      throw new IllegalArgumentException("Wallet not found");
    }
    return wallet.getOpeningBalanceMinor();
  }

  @Override
  public void setCachedBalance(long walletId, long balanceMinor) {
    com.dwlhm.finan.data.entity.Wallet wallet = table.findById(walletId);
    if (wallet == null) {
      throw new IllegalArgumentException("Wallet not found");
    }
    table.update(
        walletId,
        wallet.getName(),
        wallet.getCurrencyCode(),
        wallet.isDefault(),
        wallet.getOpeningBalanceMinor(),
        balanceMinor,
        wallet.getCreatedAt());
  }
}
