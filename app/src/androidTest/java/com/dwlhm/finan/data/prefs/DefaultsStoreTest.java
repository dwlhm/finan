package com.dwlhm.finan.data.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
}
