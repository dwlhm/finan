package com.dwlhm.finan.data.dao;

public interface WalletBalanceDao {

    long getCachedBalance(long walletId);

    void setCachedBalance(long walletId, long balanceMinor);
}
