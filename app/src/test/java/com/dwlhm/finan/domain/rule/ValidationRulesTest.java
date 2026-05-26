package com.dwlhm.finan.domain.rule;

import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidationRulesTest {

    private Transaction validExpense() {
        return new Transaction(
                0L,
                25_000L,
                TransactionType.EXPENSE,
                1L,
                2L,
                1_700_000_000_000L,
                null
        );
    }

    @Test
    public void accepts_valid_transaction() {
        assertTrue(ValidationRules.isValid(validExpense()).isValid());
    }

    @Test
    public void rejects_zero_amount() {
        Transaction t = validExpense();
        t.setAmountMinor(0L);
        assertFalse(ValidationRules.isValid(t).isValid());
    }

    @Test
    public void rejects_negative_amount() {
        Transaction t = validExpense();
        t.setAmountMinor(-100L);
        assertFalse(ValidationRules.isValid(t).isValid());
    }

    @Test
    public void rejects_missing_wallet() {
        Transaction t = validExpense();
        t.setWalletId(0L);
        assertFalse(ValidationRules.isValid(t).isValid());
    }

    @Test
    public void rejects_missing_category() {
        Transaction t = validExpense();
        t.setCategoryId(0L);
        assertFalse(ValidationRules.isValid(t).isValid());
    }

    @Test
    public void rejects_missing_type() {
        Transaction t = validExpense();
        t.setType(null);
        assertFalse(ValidationRules.isValid(t).isValid());
    }

    @Test
    public void rejects_missing_occurred_at() {
        Transaction t = validExpense();
        t.setOccurredAt(0L);
        assertFalse(ValidationRules.isValid(t).isValid());
    }
}
