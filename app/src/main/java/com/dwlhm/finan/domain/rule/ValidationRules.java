package com.dwlhm.finan.domain.rule;

import com.dwlhm.finan.domain.model.Transaction;

public final class ValidationRules {

    private ValidationRules() {
    }

    public static ValidationResult isValid(Transaction transaction) {
        if (transaction == null) {
            return ValidationResult.error("Transaction is required");
        }
        if (transaction.getAmountMinor() <= 0L) {
            return ValidationResult.error("Amount must be positive");
        }
        if (transaction.getType() == null) {
            return ValidationResult.error("Transaction type is required");
        }
        if (transaction.getWalletId() <= 0L) {
            return ValidationResult.error("Wallet is required");
        }
        if (transaction.getCategoryId() <= 0L) {
            return ValidationResult.error("Category is required");
        }
        if (transaction.getOccurredAt() <= 0L) {
            return ValidationResult.error("Occurred at is required");
        }
        return ValidationResult.ok();
    }
}
