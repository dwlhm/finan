package com.dwlhm.finan.service.balance;

import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.data.dao.WalletBalanceDao;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.util.date.TimeProvider;

public final class AdjustmentService {

  private final SQLiteDatabase db;
  private final TransactionGateway transactionGateway;
  private final WalletBalanceDao walletBalanceDao;
  private final BalanceService balanceService;
  private final TimeProvider timeProvider;

  public AdjustmentService(
      SQLiteDatabase db,
      TransactionGateway transactionGateway,
      WalletBalanceDao walletBalanceDao,
      BalanceService balanceService,
      TimeProvider timeProvider) {
    this.db = db;
    this.transactionGateway = transactionGateway;
    this.walletBalanceDao = walletBalanceDao;
    this.balanceService = balanceService;
    this.timeProvider = timeProvider;
  }

  public long adjustTo(long walletId, long targetBalanceMinor, long occurredAt, String note) {
    long currentBalance = walletBalanceDao.getCachedBalance(walletId);
    long difference;
    try {
      difference = Math.subtractExact(targetBalanceMinor, currentBalance);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Adjustment amount is too large", e);
    }
    if (difference == 0L) {
      return 0L;
    }
    if (difference == Long.MIN_VALUE) {
      throw new IllegalArgumentException("Adjustment amount is too large");
    }

    TransactionType type =
        difference > 0L
            ? TransactionType.ADJUSTMENT_INCREASE
            : TransactionType.ADJUSTMENT_DECREASE;
    Transaction adjustment =
        new Transaction(
            0L,
            Math.abs(difference),
            type,
            walletId,
            0L,
            occurredAt > 0L ? occurredAt : timeProvider.currentTimeMillis(),
            note);

    db.beginTransaction();
    try {
      long transactionId = transactionGateway.insert(adjustment);
      if (transactionId <= 0L) {
        throw new IllegalStateException("Failed to save adjustment");
      }
      balanceService.applyTransaction(adjustment);
      db.setTransactionSuccessful();
      return transactionId;
    } finally {
      db.endTransaction();
    }
  }

  public void delete(long transactionId) {
    Transaction adjustment = transactionGateway.findById(transactionId);
    if (adjustment == null) {
      return;
    }
    if (adjustment.getType() != TransactionType.ADJUSTMENT_INCREASE
        && adjustment.getType() != TransactionType.ADJUSTMENT_DECREASE) {
      throw new IllegalArgumentException("Transaction is not an adjustment");
    }

    db.beginTransaction();
    try {
      transactionGateway.delete(transactionId);
      balanceService.recalculate(adjustment.getWalletId());
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }
}
