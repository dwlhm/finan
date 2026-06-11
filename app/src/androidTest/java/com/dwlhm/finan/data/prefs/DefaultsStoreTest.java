package com.dwlhm.finan.data.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.domain.model.TransactionType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DefaultsStoreTest {

    private DefaultsStore store;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("finan_defaults_test", Context.MODE_PRIVATE).edit().clear().commit();
        store = new DefaultsStore(context, "finan_defaults_test");
    }

    @Test
    public void defaultWalletId_roundTrip() {
        assertFalse(store.hasDefaultWalletId());
        store.setDefaultWalletId(42L);
        assertTrue(store.hasDefaultWalletId());
        assertEquals(42L, store.getDefaultWalletId());
    }

    @Test
    public void lastWalletId_roundTrip() {
        assertNull(store.getLastWalletId());
        store.setLastWalletId(7L);
        assertEquals(Long.valueOf(7L), store.getLastWalletId());
    }

    @Test
    public void draft_roundTripAndClear() {
        assertNull(store.getDraftJson());
        store.setDraftJson("{\"amount\":25000}");
        assertEquals("{\"amount\":25000}", store.getDraftJson());
        store.clearDraft();
        assertNull(store.getDraftJson());
    }

    @Test
    public void captureDraft_roundTripAndClear() {
        assertNull(store.getCaptureDraft());

        TransactionFormDraft draft = new TransactionFormDraft();
        draft.setAmountMinor(25_000L);
        draft.setType(TransactionType.EXPENSE);
        draft.setWalletId(3L);
        draft.setCategoryId(9L);
        draft.setOccurredAtMillis(1_700_000_000_000L);
        draft.setNote("coffee");
        store.setCaptureDraft(draft);

        TransactionFormDraft restored = store.getCaptureDraft();
        assertEquals(25_000L, restored.getAmountMinor());
        assertEquals(TransactionType.EXPENSE, restored.getType());
        assertEquals(Long.valueOf(3L), restored.getWalletId());
        assertEquals(Long.valueOf(9L), restored.getCategoryId());
        assertEquals(1_700_000_000_000L, restored.getOccurredAtMillis());
        assertEquals("coffee", restored.getNote());

        store.clearCaptureDraft();
        assertNull(store.getCaptureDraft());
    }

    @Test
    public void editDraft_roundTripAndClear() {
        assertNull(store.getEditDraft(42L));

        TransactionFormDraft draft = new TransactionFormDraft();
        draft.setAmountMinor(50_000L);
        draft.setType(TransactionType.INCOME);
        draft.setWalletId(2L);
        draft.setCategoryId(5L);
        draft.setOccurredAtMillis(1_800_000_000_000L);
        store.setEditDraft(42L, draft);

        TransactionFormDraft restored = store.getEditDraft(42L);
        assertEquals(Long.valueOf(42L), restored.getTransactionId());
        assertEquals(50_000L, restored.getAmountMinor());
        assertEquals(TransactionType.INCOME, restored.getType());

        store.clearEditDraft(42L);
        assertNull(store.getEditDraft(42L));
    }

    @Test
    public void transactionFormDraft_jsonRoundTrip() {
        TransactionFormDraft draft = new TransactionFormDraft();
        draft.setAmountMinor(10_000L);
        draft.setType(TransactionType.EXPENSE);
        draft.setCategoryId(1L);
        draft.setOccurredAtMillis(123L);

        TransactionFormDraft parsed = TransactionFormDraft.fromJson(draft.toJson());
        assertEquals(10_000L, parsed.getAmountMinor());
        assertEquals(TransactionType.EXPENSE, parsed.getType());
        assertEquals(Long.valueOf(1L), parsed.getCategoryId());
        assertEquals(123L, parsed.getOccurredAtMillis());
    }
}
