package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.data.entity.Transaction;
import com.dwlhm.finan.domain.model.HistoryPageCursor;
import com.dwlhm.finan.domain.model.HistoryQuery;
import com.dwlhm.finan.domain.model.HistoryTotals;
import com.dwlhm.finan.domain.model.PageResult;
import com.dwlhm.finan.domain.model.TransactionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SqliteTransactionDao implements TransactionGateway {

  private final TransactionDao table;
  private final TransactionTagDao transactionTags;

  public SqliteTransactionDao(TransactionDao table, TransactionTagDao transactionTags) {
    this.table = table;
    this.transactionTags = transactionTags;
  }

  @Override
  public long insert(com.dwlhm.finan.domain.model.Transaction transaction) {
    long now = System.currentTimeMillis();
    long occurredAt = transaction.getOccurredAt() > 0L ? transaction.getOccurredAt() : now;
    long id =
        table.insert(
            transaction.getAmountMinor(),
            transaction.getType().name(),
            transaction.getWalletId(),
            transaction.getCategoryId(),
            occurredAt,
            transaction.getNote(),
            transaction.getMerchantId(),
            transaction.getTransferId(),
            now,
            now);
    transactionTags.replaceAll(id, transaction.getTagIds());
    return id;
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
        transaction.getMerchantId(),
        transaction.getTransferId(),
        existing.getCreatedAt(),
        now);
    transactionTags.replaceAll(transaction.getId(), transaction.getTagIds());
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
  public List<com.dwlhm.finan.domain.model.Transaction> findRecent(int limit) {
    List<Transaction> entities = table.findRecent(limit);
    return toDomainList(entities);
  }

  @Override
  public PageResult<com.dwlhm.finan.domain.model.Transaction, HistoryPageCursor> findHistoryPage(
      HistoryQuery query,
      HistoryPageCursor cursor,
      int limit) {
    Long cursorOccurredAt = cursor == null ? null : cursor.occurredAt();
    Long cursorId = cursor == null ? null : cursor.id();
    List<com.dwlhm.finan.domain.model.Transaction> items =
        toDomainList(
            table.findHistoryPage(
                query, cursorOccurredAt, cursorId, limit + 1));
    return PageResult.fromLimitPlusOne(
        items, limit, t -> new HistoryPageCursor(t.getOccurredAt(), t.getId()));
  }

  @Override
  public HistoryTotals findHistoryTotals(HistoryQuery query) {
    TransactionDao.HistoryTotalsRow row = table.findHistoryTotals(query);
    return new HistoryTotals(row.count, row.incomeMinor, row.expenseMinor);
  }

  @Override
  public void forEachTransaction(Consumer<com.dwlhm.finan.domain.model.Transaction> consumer) {
    List<Transaction> entities = table.findAll();
    List<com.dwlhm.finan.domain.model.Transaction> domain = toDomainList(entities);
    for (com.dwlhm.finan.domain.model.Transaction transaction : domain) {
      consumer.accept(transaction);
    }
  }

  @Override
  public List<com.dwlhm.finan.domain.model.Transaction> findAll() {
    return toDomainList(table.findAll());
  }

  @Override
  public List<com.dwlhm.finan.domain.model.Transaction> findByWalletId(long walletId) {
    return toDomainList(table.findByWalletId(walletId));
  }

  @Override
  public List<com.dwlhm.finan.domain.model.Transaction> findByTransferId(long transferId) {
    return toDomainList(table.findByTransferId(transferId));
  }

  private List<com.dwlhm.finan.domain.model.Transaction> toDomainList(List<Transaction> entities) {
    List<com.dwlhm.finan.domain.model.Transaction> result = new ArrayList<>();
    if (entities.isEmpty()) {
      return result;
    }
    List<Long> transactionIds = new ArrayList<>();
    for (Transaction entity : entities) {
      if (entity != null) {
        transactionIds.add(entity.getId());
      }
    }
    Map<Long, List<Long>> tagIdsByTransaction =
        transactionTags.findTagIdsByTransactions(transactionIds);
    for (Transaction entity : entities) {
      com.dwlhm.finan.domain.model.Transaction domain = map(entity);
      if (domain != null) {
        List<Long> tagIds = tagIdsByTransaction.get(entity.getId());
        if (tagIds != null) {
          domain.setTagIds(tagIds);
        }
        result.add(domain);
      }
    }
    return result;
  }

  private com.dwlhm.finan.domain.model.Transaction toDomain(Transaction entity) {
    com.dwlhm.finan.domain.model.Transaction domain = map(entity);
    if (domain != null) {
      domain.setTagIds(transactionTags.findTagIdsByTransaction(entity.getId()));
    }
    return domain;
  }

  private static com.dwlhm.finan.domain.model.Transaction map(Transaction entity) {
    if (entity == null) {
      return null;
    }
    com.dwlhm.finan.domain.model.Transaction domain =
        new com.dwlhm.finan.domain.model.Transaction(
            entity.getId(),
            entity.getAmountMinor(),
            TransactionType.valueOf(entity.getType()),
            entity.getWalletId(),
            entity.getCategoryId(),
            entity.getOccurredAt(),
            entity.getNote());
    domain.setMerchantId(entity.getMerchantId());
    domain.setTransferId(entity.getTransferId());
    return domain;
  }
}
