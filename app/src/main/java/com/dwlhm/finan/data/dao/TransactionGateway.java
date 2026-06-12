package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.domain.model.HistoryPageCursor;
import com.dwlhm.finan.domain.model.HistoryQuery;
import com.dwlhm.finan.domain.model.PageResult;
import com.dwlhm.finan.domain.model.HistoryTotals;
import com.dwlhm.finan.domain.model.Transaction;

import java.util.List;
import java.util.function.Consumer;

public interface TransactionGateway {

    long insert(Transaction transaction);

    void update(Transaction transaction);

    void delete(long transactionId);

    Transaction findById(long transactionId);

    Transaction findLast();

    List<Transaction> findRecent(int limit);

    PageResult<Transaction, HistoryPageCursor> findHistoryPage(
            HistoryQuery query,
            HistoryPageCursor cursor,
            int limit
    );

    HistoryTotals findHistoryTotals(HistoryQuery query);

    List<Transaction> findByWalletId(long walletId);

    void forEachTransaction(Consumer<Transaction> consumer);

    List<Transaction> findAll();
}
