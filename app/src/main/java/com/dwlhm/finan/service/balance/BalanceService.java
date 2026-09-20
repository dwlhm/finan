package com.dwlhm.finan.service.balance;

import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.data.dao.WalletBalanceDao;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.rule.BalanceRules;

public class BalanceService {

    private final TransactionGateway transactionDao;
    private final WalletBalanceDao walletBalanceDao;

    public BalanceService(TransactionGateway transactionDao, WalletBalanceDao walletBalanceDao) {
        this.transactionDao = transactionDao;
        this.walletBalanceDao = walletBalanceDao;
    }

    public void applyTransaction(Transaction transaction) {
        long current = walletBalanceDao.getCachedBalance(transaction.getWalletId());
        long delta = BalanceRules.deltaFor(transaction.getType(), transaction.getAmountMinor());
        long updated = BalanceRules.apply(current, delta);
        walletBalanceDao.setCachedBalance(transaction.getWalletId(), updated);
    }

    public long recalculate(long walletId) {
        long balance =
                BalanceRules.apply(
                        walletBalanceDao.getOpeningBalance(walletId),
                        transactionDao.sumDeltaByWallet(walletId));
        walletBalanceDao.setCachedBalance(walletId, balance);
        return balance;
    }
}
