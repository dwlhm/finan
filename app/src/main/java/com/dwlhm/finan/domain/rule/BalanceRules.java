package com.dwlhm.finan.domain.rule;

import com.dwlhm.finan.domain.model.TransactionType;

public final class BalanceRules {

    private BalanceRules() {
    }

    public static long deltaFor(TransactionType type, long amountMinor) {
        return switch (type) {
            case EXPENSE, ADJUSTMENT_DECREASE, TRANSFER_OUT -> -amountMinor;
            case INCOME, ADJUSTMENT_INCREASE, TRANSFER_IN -> amountMinor;
        };
    }

    public static long apply(long currentBalanceMinor, long deltaMinor) {
        return currentBalanceMinor + deltaMinor;
    }

    public static long sumDeltas(long[] deltas) {
        long total = 0L;
        for (long delta : deltas) {
            total += delta;
        }
        return total;
    }
}
