package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.domain.model.HistoryPageCursor;
import com.dwlhm.finan.domain.model.PageResult;
import com.dwlhm.finan.domain.model.HistoryTotals;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;

import java.util.List;
import java.util.function.Consumer;

public interface TransactionGateway {

    long insert(Transaction transaction);

    void update(Transaction transaction);

    void delete(long transactionId);

    Transaction findById(long transactionId);

    Transaction findLast();

    List<Transaction> findRecent(int limit);

    List<Transaction> findHistory(
            Long walletId,
            Long categoryId,
            TransactionType type,
            Long startInclusiveMillis,
            Long endExclusiveMillis,
            boolean oldestFirst
    );

    PageResult<Transaction, HistoryPageCursor> findHistoryPage(
            Long walletId,
            Long categoryId,
            TransactionType type,
            Long startInclusiveMillis,
            Long endExclusiveMillis,
            boolean oldestFirst,
            HistoryPageCursor cursor,
            int limit
    );

    HistoryTotals findHistoryTotals(
            Long walletId,
            Long categoryId,
            TransactionType type,
            Long startInclusiveMillis,
            Long endExclusiveMillis
    );

    List<Transaction> findByWalletId(long walletId);

    void forEachTransaction(Consumer<Transaction> consumer);

    List<Transaction> findAll();
}
