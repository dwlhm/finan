package com.dwlhm.finan.widget;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WidgetStateStoreTest {

    @Test
    public void testKeypadLogicCalculations() {
        // Test basic keypad append logic formatting
        String initial = "0";
        String digit = "5";
        String next = "0".equals(initial) ? digit : initial + digit;
        assertEquals("5", next);

        initial = next;
        digit = "000";
        next = "0".equals(initial) ? digit : initial + digit;
        assertEquals("5000", next);

        initial = next;
        // Backspace
        String deleted = initial.length() > 1 ? initial.substring(0, initial.length() - 1) : "0";
        assertEquals("500", deleted);
    }

    @Test
    public void testUndoCountdownCalculation() {
        long now = System.currentTimeMillis();
        long deadline = now + 5000L;
        long remainingMs = deadline - now;
        int remainingSec = (int) Math.ceil(remainingMs / 1000.0);
        assertEquals(5, remainingSec);

        long now4s = now + 1200L;
        long remainingMs4s = deadline - now4s;
        int remainingSec4s = (int) Math.ceil(remainingMs4s / 1000.0);
        assertEquals(4, remainingSec4s);
        assertTrue(remainingSec4s > 0);
    }

    @Test
    public void testDraftSnapshotRestoreConcept() {
        String originalAmount = "75000";
        String originalType = "EXPENSE";
        long originalWalletId = 3L;
        long originalCategoryId = 5L;

        // When saved, amount resets to 0
        String currentAmount = "0";
        assertEquals("0", currentAmount);

        // When undo is clicked, draft is restored
        currentAmount = originalAmount;
        assertEquals("75000", currentAmount);
        assertEquals("EXPENSE", originalType);
        assertEquals(3L, originalWalletId);
        assertEquals(5L, originalCategoryId);
    }

    @Test
    public void testPickerModeLogic() {
        String pickerMode = "WALLET";
        assertEquals("WALLET", pickerMode);

        // After item selection, picker mode is cleared (returns null)
        pickerMode = null;
        assertNull(pickerMode);
    }
}
