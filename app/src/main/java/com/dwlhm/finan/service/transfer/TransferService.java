package com.dwlhm.finan.service.transfer;

import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.data.dao.TransferDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.domain.model.Transfer;
import com.dwlhm.finan.service.balance.BalanceService;
import com.dwlhm.finan.util.date.TimeProvider;

import java.util.LinkedHashSet;
import java.util.Set;

public final class TransferService {

  private final SQLiteDatabase db;
  private final TransferDao transferDao;
  private final TransactionGateway transactionGateway;
  private final WalletDao walletDao;
  private final BalanceService balanceService;
  private final TimeProvider timeProvider;

  public TransferService(
      SQLiteDatabase db,
      TransferDao transferDao,
      TransactionGateway transactionGateway,
      WalletDao walletDao,
      BalanceService balanceService,
      TimeProvider timeProvider) {
    this.db = db;
    this.transferDao = transferDao;
    this.transactionGateway = transactionGateway;
    this.walletDao = walletDao;
    this.balanceService = balanceService;
    this.timeProvider = timeProvider;
  }

  public long create(
      long sourceWalletId,
      long destinationWalletId,
      long amountMinor,
      long occurredAt,
      String note) {
    validate(sourceWalletId, destinationWalletId, amountMinor);
    long resolvedOccurredAt =
        occurredAt > 0L ? occurredAt : timeProvider.currentTimeMillis();
    long now = timeProvider.currentTimeMillis();

    db.beginTransaction();
    try {
      long transferId =
          transferDao.insert(
              sourceWalletId,
              destinationWalletId,
              amountMinor,
              resolvedOccurredAt,
              note,
              now);
      insertEntries(
          transferId,
          sourceWalletId,
          destinationWalletId,
          amountMinor,
          resolvedOccurredAt,
          note,
          true);
      db.setTransactionSuccessful();
      return transferId;
    } finally {
      db.endTransaction();
    }
  }

  public void edit(Transfer updated) {
    Transfer existing = transferDao.findById(updated.getId());
    if (existing == null) {
      throw new IllegalArgumentException("Transfer not found");
    }
    validate(
        updated.getSourceWalletId(),
        updated.getDestinationWalletId(),
        updated.getAmountMinor());

    db.beginTransaction();
    try {
      transferDao.update(updated, timeProvider.currentTimeMillis());
      for (Transaction transaction : transactionGateway.findByTransferId(updated.getId())) {
        transactionGateway.delete(transaction.getId());
      }
      insertEntries(
          updated.getId(),
          updated.getSourceWalletId(),
          updated.getDestinationWalletId(),
          updated.getAmountMinor(),
          updated.getOccurredAt(),
          updated.getNote(),
          false);
      recalculateAffectedWallets(existing, updated);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public void delete(long transferId) {
    Transfer transfer = transferDao.findById(transferId);
    if (transfer == null) {
      return;
    }
    db.beginTransaction();
    try {
      transferDao.delete(transferId);
      balanceService.recalculate(transfer.getSourceWalletId());
      balanceService.recalculate(transfer.getDestinationWalletId());
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  private void validate(long sourceWalletId, long destinationWalletId, long amountMinor) {
    if (sourceWalletId <= 0L || destinationWalletId <= 0L) {
      throw new IllegalArgumentException("Both wallets are required");
    }
    if (sourceWalletId == destinationWalletId) {
      throw new IllegalArgumentException("Source and destination wallets must differ");
    }
    if (amountMinor <= 0L) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    Wallet source = walletDao.findById(sourceWalletId);
    Wallet destination = walletDao.findById(destinationWalletId);
    if (source == null || destination == null) {
      throw new IllegalArgumentException("Wallet not found");
    }
    if (!source.getCurrencyCode().equals(destination.getCurrencyCode())) {
      throw new IllegalArgumentException("Wallet currencies must match");
    }
  }

  private void insertEntries(
      long transferId,
      long sourceWalletId,
      long destinationWalletId,
      long amountMinor,
      long occurredAt,
      String note,
      boolean updateBalance) {
    Transaction outgoing =
        transferEntry(
            transferId,
            sourceWalletId,
            amountMinor,
            TransactionType.TRANSFER_OUT,
            occurredAt,
            note);
    Transaction incoming =
        transferEntry(
            transferId,
            destinationWalletId,
            amountMinor,
            TransactionType.TRANSFER_IN,
            occurredAt,
            note);
    if (transactionGateway.insert(outgoing) <= 0L
        || transactionGateway.insert(incoming) <= 0L) {
      throw new IllegalStateException("Failed to save transfer entries");
    }
    if (updateBalance) {
      balanceService.applyTransaction(outgoing);
      balanceService.applyTransaction(incoming);
    }
  }

  private static Transaction transferEntry(
      long transferId,
      long walletId,
      long amountMinor,
      TransactionType type,
      long occurredAt,
      String note) {
    Transaction transaction =
        new Transaction(0L, amountMinor, type, walletId, 0L, occurredAt, note);
    transaction.setTransferId(transferId);
    return transaction;
  }

  private void recalculateAffectedWallets(Transfer existing, Transfer updated) {
    Set<Long> walletIds = new LinkedHashSet<>();
    walletIds.add(existing.getSourceWalletId());
    walletIds.add(existing.getDestinationWalletId());
    walletIds.add(updated.getSourceWalletId());
    walletIds.add(updated.getDestinationWalletId());
    for (Long walletId : walletIds) {
      balanceService.recalculate(walletId);
    }
  }
}
