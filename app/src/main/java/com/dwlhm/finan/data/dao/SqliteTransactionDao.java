package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.data.entity.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;

import java.util.ArrayList;
import java.util.List;

public final class SqliteTransactionDao implements TransactionGateway {

  private final TransactionDao table;

  public SqliteTransactionDao(TransactionDao table) {
    this.table = table;
  }

  @Override
  public long insert(com.dwlhm.finan.domain.model.Transaction transaction) {
    long now = System.currentTimeMillis();
    long occurredAt = transaction.getOccurredAt() > 0L ? transaction.getOccurredAt() : now;
    return table.insert(
        transaction.getAmountMinor(),
        transaction.getType().name(),
        transaction.getWalletId(),
        transaction.getCategoryId(),
        occurredAt,
        transaction.getNote(),
        null,
        null,
        now,
        now);
  }

  @Override
  public void update(com.dwlhm.finan.domain.model.Transaction transaction) {
    Transaction existing = table.findById(transaction.getId());
    if (existing == null) {
      throw new IllegalArgumentException("Transaction not found");
    }
    long now = System.currentTimeMillis();
    table.update(
        transaction.getId(),
        transaction.getAmountMinor(),
        transaction.getType().name(),
        transaction.getWalletId(),
        transaction.getCategoryId(),
        transaction.getOccurredAt(),
        transaction.getNote(),
        existing.getTag(),
        existing.getMerchant(),
        existing.getCreatedAt(),
        now);
  }

  @Override
  public void delete(long transactionId) {
    table.delete(transactionId);
  }

  @Override
  public com.dwlhm.finan.domain.model.Transaction findById(long transactionId) {
    return toDomain(table.findById(transactionId));
  }

  @Override
  public com.dwlhm.finan.domain.model.Transaction findLast() {
    return toDomain(table.findLast());
  }

  @Override
  public List<com.dwlhm.finan.domain.model.Transaction> findRecent(int limit) {
    List<com.dwlhm.finan.domain.model.Transaction> result = new ArrayList<>();
    for (Transaction entity : table.findRecent(limit)) {
      result.add(toDomain(entity));
    }
    return result;
  }

  @Override
  public List<com.dwlhm.finan.domain.model.Transaction> findAll() {
    List<com.dwlhm.finan.domain.model.Transaction> result = new ArrayList<>();
    for (Transaction entity : table.findAll()) {
      result.add(toDomain(entity));
    }
    return result;
  }

  @Override
  public List<com.dwlhm.finan.domain.model.Transaction> findByWalletId(long walletId) {
    List<com.dwlhm.finan.domain.model.Transaction> result = new ArrayList<>();
    for (Transaction entity : table.findByWalletId(walletId)) {
      result.add(toDomain(entity));
    }
    return result;
  }

  private static com.dwlhm.finan.domain.model.Transaction toDomain(Transaction entity) {
    if (entity == null) {
      return null;
    }
    return new com.dwlhm.finan.domain.model.Transaction(
        entity.getId(),
        entity.getAmountMinor(),
        TransactionType.valueOf(entity.getType()),
        entity.getWalletId(),
        entity.getCategoryId(),
        entity.getOccurredAt(),
        entity.getNote());
  }
}
