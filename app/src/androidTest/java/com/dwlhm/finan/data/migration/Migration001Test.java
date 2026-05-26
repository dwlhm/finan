package com.dwlhm.finan.data.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class Migration001Test {

    private SQLiteDatabase db;

    @Before
    public void setUp() {
        db = SQLiteDatabase.create(null);
        new Migration001Initial().migrate(db);
    }

    @After
    public void tearDown() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    @Test
    public void migrate_createsWalletsCategoriesTransactionsTables() {
        assertTrue(tableExists("wallets"));
        assertTrue(tableExists("categories"));
        assertTrue(tableExists("transactions"));
    }

    @Test
    public void migrate_seedsDefaultWallet() {
        try (Cursor c = db.rawQuery(
                "SELECT name, currency_code, is_default, cached_balance_minor FROM wallets",
                null)) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getCount());
            assertEquals("Dompet Utama", c.getString(0));
            assertEquals("IDR", c.getString(1));
            assertEquals(1, c.getInt(2));
            assertEquals(0, c.getInt(3));
        }
    }

    @Test
    public void migrate_seedsDefaultCategories() {
        try (Cursor c = db.rawQuery(
                "SELECT name, type_filter FROM categories ORDER BY sort_order ASC",
                null)) {
            assertEquals(7, c.getCount());
            assertCategory(c, "Makanan", "EXPENSE");
            assertCategory(c, "Transport", "EXPENSE");
            assertCategory(c, "Kopi", "EXPENSE");
            assertCategory(c, "Tagihan", "EXPENSE");
            assertCategory(c, "Belanja", "EXPENSE");
            assertCategory(c, "Gaji", "INCOME");
            assertCategory(c, "Lainnya", "BOTH");
        }
    }

    @Test
    public void getVersion_isOne() {
        assertEquals(1, new Migration001Initial().getVersion());
    }

    private void assertCategory(Cursor c, String name, String typeFilter) {
        assertTrue("Missing category: " + name, c.moveToNext());
        assertEquals(name, c.getString(0));
        assertEquals(typeFilter, c.getString(1));
    }

    private boolean tableExists(String tableName) {
        try (Cursor c = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName})) {
            return c.moveToFirst();
        }
    }
}
