package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;

import java.util.List;

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

    List<Transaction> findByWalletId(long walletId);

    List<Transaction> findAll();
}
