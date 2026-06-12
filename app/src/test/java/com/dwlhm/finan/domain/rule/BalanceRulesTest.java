package com.dwlhm.finan.domain.rule;

import com.dwlhm.finan.domain.model.TransactionType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BalanceRulesTest {

    @Test
    public void expense_subtracts_from_balance() {
        assertEquals(-25_000L, BalanceRules.deltaFor(TransactionType.EXPENSE, 25_000L));
    }

    @Test
    public void income_adds_to_balance() {
        assertEquals(50_000L, BalanceRules.deltaFor(TransactionType.INCOME, 50_000L));
    }

    @Test
    public void system_transactions_apply_correct_direction() {
        assertEquals(
                10_000L,
                BalanceRules.deltaFor(TransactionType.ADJUSTMENT_INCREASE, 10_000L));
        assertEquals(
                -10_000L,
                BalanceRules.deltaFor(TransactionType.ADJUSTMENT_DECREASE, 10_000L));
        assertEquals(10_000L, BalanceRules.deltaFor(TransactionType.TRANSFER_IN, 10_000L));
        assertEquals(-10_000L, BalanceRules.deltaFor(TransactionType.TRANSFER_OUT, 10_000L));
    }

    @Test
    public void apply_delta_updates_balance() {
        assertEquals(75_000L, BalanceRules.apply(100_000L, -25_000L));
    }

    @Test
    public void recalculate_sums_deltas() {
        long[] deltas = {-10_000L, 50_000L, -5_000L};
        assertEquals(35_000L, BalanceRules.sumDeltas(deltas));
    }

    @Test
    public void recalculate_empty_is_zero() {
        assertEquals(0L, BalanceRules.sumDeltas(new long[0]));
    }
}
