package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.data.entity.Transaction;
import com.dwlhm.finan.domain.model.HistoryPageCursor;
import com.dwlhm.finan.domain.model.PageResult;
import com.dwlhm.finan.domain.model.HistoryTotals;
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
  public List<com.dwlhm.finan.domain.model.Transaction> findHistory(
      Long walletId,
      Long categoryId,
      TransactionType type,
      Long startInclusiveMillis,
      Long endExclusiveMillis,
      boolean oldestFirst) {
    List<com.dwlhm.finan.domain.model.Transaction> result = new ArrayList<>();
    String typeName = type == null ? null : type.name();
    for (Transaction entity :
        table.findHistory(
            walletId, categoryId, typeName, startInclusiveMillis, endExclusiveMillis, oldestFirst)) {
      result.add(toDomain(entity));
    }
    return result;
  }

  @Override
  public PageResult<com.dwlhm.finan.domain.model.Transaction, HistoryPageCursor> findHistoryPage(
      Long walletId,
      Long categoryId,
      TransactionType type,
      Long startInclusiveMillis,
      Long endExclusiveMillis,
      boolean oldestFirst,
      HistoryPageCursor cursor,
      int limit) {
    String typeName = type == null ? null : type.name();
    Long cursorOccurredAt = cursor == null ? null : cursor.occurredAt();
    Long cursorId = cursor == null ? null : cursor.id();
    List<com.dwlhm.finan.domain.model.Transaction> items = new ArrayList<>();
    for (Transaction entity :
        table.findHistoryPage(
            walletId,
            categoryId,
            typeName,
            startInclusiveMillis,
            endExclusiveMillis,
            oldestFirst,
            cursorOccurredAt,
            cursorId,
            limit + 1)) {
      items.add(toDomain(entity));
    }
    return PageResult.fromLimitPlusOne(
        items, limit, t -> new HistoryPageCursor(t.getOccurredAt(), t.getId()));
  }

  @Override
  public HistoryTotals findHistoryTotals(
      Long walletId,
      Long categoryId,
      TransactionType type,
      Long startInclusiveMillis,
      Long endExclusiveMillis) {
    String typeName = type == null ? null : type.name();
    TransactionDao.HistoryTotalsRow row =
        table.findHistoryTotals(
            walletId, categoryId, typeName, startInclusiveMillis, endExclusiveMillis);
    return new HistoryTotals(row.count, row.incomeMinor, row.expenseMinor);
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
