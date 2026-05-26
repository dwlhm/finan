package com.dwlhm.finan.service.transaction;

import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.rule.ValidationRules;
import com.dwlhm.finan.domain.rule.ValidationResult;
import com.dwlhm.finan.service.balance.BalanceService;
import com.dwlhm.finan.service.category.CategoryUsageService;
import com.dwlhm.finan.util.date.OccurredAtHelper;
import com.dwlhm.finan.util.date.TimeProvider;

import java.util.List;

public class TransactionService {

    private final TransactionGateway transactionDao;
    private final BalanceService balanceService;
    private final CategoryUsageService categoryUsageService;
    private final TimeProvider timeProvider;

    public TransactionService(
            TransactionGateway transactionDao,
            BalanceService balanceService,
            CategoryUsageService categoryUsageService,
            TimeProvider timeProvider
    ) {
        this.transactionDao = transactionDao;
        this.balanceService = balanceService;
        this.categoryUsageService = categoryUsageService;
        this.timeProvider = timeProvider;
    }

    public long save(Transaction transaction) {
        prepareForWrite(transaction);
        ValidationResult validation = ValidationRules.isValid(transaction);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }
        long id = transactionDao.insert(transaction);
        transaction.setId(id);
        balanceService.applyTransaction(transaction);
        categoryUsageService.bumpUsage(transaction.getCategoryId());
        return id;
    }

    public void edit(Transaction transaction) {
        if (transaction.getId() <= 0L) {
            throw new IllegalArgumentException("Transaction id is required for edit");
        }
        Transaction existing = transactionDao.findById(transaction.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Transaction not found");
        }
        prepareForWrite(transaction);
        ValidationResult validation = ValidationRules.isValid(transaction);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }
        transactionDao.update(transaction);
        balanceService.recalculate(transaction.getWalletId());
        if (existing.getWalletId() != transaction.getWalletId()) {
            balanceService.recalculate(existing.getWalletId());
        }
        categoryUsageService.bumpUsage(transaction.getCategoryId());
    }

    public void delete(long transactionId) {
        Transaction existing = transactionDao.findById(transactionId);
        if (existing == null) {
            return;
        }
        transactionDao.delete(transactionId);
        balanceService.recalculate(existing.getWalletId());
    }

    public boolean undoLast() {
        Transaction last = transactionDao.findLast();
        if (last == null) {
            return false;
        }
        long walletId = last.getWalletId();
        transactionDao.delete(last.getId());
        balanceService.recalculate(walletId);
        return true;
    }

    public List<Transaction> getRecent(int limit) {
        return transactionDao.findRecent(limit);
    }

    private void prepareForWrite(Transaction transaction) {
        transaction.setOccurredAt(OccurredAtHelper.resolve(transaction.getOccurredAt(), timeProvider));
    }
}
