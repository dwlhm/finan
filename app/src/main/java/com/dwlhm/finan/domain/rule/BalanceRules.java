package com.dwlhm.finan.domain.rule;

import com.dwlhm.finan.domain.model.TransactionType;

public final class BalanceRules {

    private BalanceRules() {
    }

    public static long deltaFor(TransactionType type, long amountMinor) {
        if (type == TransactionType.EXPENSE) {
            return -amountMinor;
        }
        if (type == TransactionType.INCOME) {
            return amountMinor;
        }
        throw new IllegalArgumentException("Unknown transaction type: " + type);
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
