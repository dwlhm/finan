package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.domain.model.Transaction;

import java.util.List;

public interface TransactionGateway {

    long insert(Transaction transaction);

    void update(Transaction transaction);

    void delete(long transactionId);

    Transaction findById(long transactionId);

    Transaction findLast();

    List<Transaction> findRecent(int limit);

    List<Transaction> findByWalletId(long walletId);

    List<Transaction> findAll();
}
