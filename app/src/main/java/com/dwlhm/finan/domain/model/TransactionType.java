package com.dwlhm.finan.domain.model;

public enum TransactionType {
    EXPENSE,
    INCOME,
    ADJUSTMENT_INCREASE,
    ADJUSTMENT_DECREASE,
    TRANSFER_OUT,
    TRANSFER_IN;

    public boolean isRegular() {
        return this == EXPENSE || this == INCOME;
    }

    public boolean isTransfer() {
        return this == TRANSFER_OUT || this == TRANSFER_IN;
    }

    public boolean isSystem() {
        return !isRegular();
    }

    public boolean increasesBalance() {
        return this == INCOME || this == ADJUSTMENT_INCREASE || this == TRANSFER_IN;
    }
}
