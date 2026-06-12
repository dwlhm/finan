package com.dwlhm.finan.service.transaction;

import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.rule.ValidationRules;
import com.dwlhm.finan.domain.rule.ValidationResult;
import com.dwlhm.finan.service.balance.BalanceService;
import com.dwlhm.finan.service.category.CategoryUsageService;
import com.dwlhm.finan.service.merchant.MerchantUsageService;
import com.dwlhm.finan.service.tag.TagUsageService;
import com.dwlhm.finan.util.date.OccurredAtHelper;
import com.dwlhm.finan.util.date.TimeProvider;

import java.util.List;

public class TransactionService {

    private final TransactionGateway transactionDao;
    private final SQLiteDatabase db;
    private final BalanceService balanceService;
    private final CategoryUsageService categoryUsageService;
    private final TagUsageService tagUsageService;
    private final MerchantUsageService merchantUsageService;
    private final TimeProvider timeProvider;

    public TransactionService(
            SQLiteDatabase db,
            TransactionGateway transactionDao,
            BalanceService balanceService,
            CategoryUsageService categoryUsageService,
            TagUsageService tagUsageService,
            MerchantUsageService merchantUsageService,
            TimeProvider timeProvider
    ) {
        this.db = db;
        this.transactionDao = transactionDao;
        this.balanceService = balanceService;
        this.categoryUsageService = categoryUsageService;
        this.tagUsageService = tagUsageService;
        this.merchantUsageService = merchantUsageService;
        this.timeProvider = timeProvider;
    }

    public long save(Transaction transaction) {
        prepareForWrite(transaction);
        requireRegularTransaction(transaction);
        ValidationResult validation = ValidationRules.isValid(transaction);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }
        db.beginTransaction();
        try {
            long id = transactionDao.insert(transaction);
            if (id <= 0L) {
                throw new IllegalStateException("Failed to save transaction");
            }
            transaction.setId(id);
            balanceService.applyTransaction(transaction);
            categoryUsageService.bumpUsage(transaction.getCategoryId());
            tagUsageService.bumpUsageForTags(transaction.getTagIds());
            Long merchantId = transaction.getMerchantId();
            if (merchantId != null) {
                merchantUsageService.bumpUsage(merchantId);
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public void edit(Transaction transaction) {
        if (transaction.getId() <= 0L) {
            throw new IllegalArgumentException("Transaction id is required for edit");
        }
        Transaction existing = transactionDao.findById(transaction.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Transaction not found");
        }
        requireRegularTransaction(existing);
        prepareForWrite(transaction);
        requireRegularTransaction(transaction);
        ValidationResult validation = ValidationRules.isValid(transaction);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }
        db.beginTransaction();
        try {
            transactionDao.update(transaction);
            balanceService.recalculate(transaction.getWalletId());
            if (existing.getWalletId() != transaction.getWalletId()) {
                balanceService.recalculate(existing.getWalletId());
            }
            categoryUsageService.bumpUsage(transaction.getCategoryId());
            tagUsageService.bumpUsageForTags(transaction.getTagIds());
            Long merchantId = transaction.getMerchantId();
            if (merchantId != null) {
                merchantUsageService.bumpUsage(merchantId);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void delete(long transactionId) {
        Transaction existing = transactionDao.findById(transactionId);
        if (existing == null) {
            return;
        }
        requireRegularTransaction(existing);
        db.beginTransaction();
        try {
            transactionDao.delete(transactionId);
            balanceService.recalculate(existing.getWalletId());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Transaction> getRecent(int limit) {
        return transactionDao.findRecent(limit);
    }

    private void prepareForWrite(Transaction transaction) {
        transaction.setOccurredAt(OccurredAtHelper.resolve(transaction.getOccurredAt(), timeProvider));
    }

    private static void requireRegularTransaction(Transaction transaction) {
        if (transaction.getType() == null || !transaction.getType().isRegular()) {
            throw new IllegalArgumentException("System transactions require their dedicated service");
        }
    }
}
