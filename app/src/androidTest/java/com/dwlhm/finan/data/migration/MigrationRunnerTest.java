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
public class MigrationRunnerTest {

    private SQLiteDatabase db;

    @Before
    public void setUp() {
        db = SQLiteDatabase.create(null);
    }

    @After
    public void tearDown() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    @Test
    public void migrate_fromZeroToOne_appliesInitialMigration() {
        MigrationRunner.migrate(db, 0, 1, new Migration[]{new Migration001Initial()});

        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM wallets", null)) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getInt(0));
        }
    }

    @Test
    public void migrate_sameVersion_isNoOp() {
        MigrationRunner.migrate(db, 1, 1, new Migration[]{new Migration001Initial()});

        try (Cursor c = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='wallets'",
                null)) {
            assertTrue(!c.moveToFirst());
        }
    }

    @Test
    public void migrate_freshToThirteen_createsCoverageIndexes() {
        MigrationRunner.migrate(db, 0, 13, new Migration[]{
                new Migration001Initial(),
                new Migration013Indexes()});

        assertTrue(indexExists(Migration013Indexes.IDX_TRANSACTIONS_CAT_OCCURRED));
        assertTrue(indexExists(Migration013Indexes.IDX_TRANSACTIONS_WALLET_CAT_OCCURRED));
    }

    @Test
    public void migrate_twelveToThirteen_createsCoverageIndexes() {
        MigrationRunner.migrate(db, 0, 12, new Migration[]{new Migration001Initial()});
        MigrationRunner.migrate(db, 12, 13, new Migration[]{new Migration013Indexes()});

        assertTrue(indexExists(Migration013Indexes.IDX_TRANSACTIONS_CAT_OCCURRED));
        assertTrue(indexExists(Migration013Indexes.IDX_TRANSACTIONS_WALLET_CAT_OCCURRED));
    }

    @Test
    public void migrate_thirteen_isIdempotent() {
        MigrationRunner.migrate(db, 0, 13, new Migration[]{
                new Migration001Initial(),
                new Migration013Indexes()});
        MigrationRunner.migrate(db, 12, 13, new Migration[]{new Migration013Indexes()});

        assertTrue(indexExists(Migration013Indexes.IDX_TRANSACTIONS_CAT_OCCURRED));
        assertTrue(indexExists(Migration013Indexes.IDX_TRANSACTIONS_WALLET_CAT_OCCURRED));
    }

    private boolean indexExists(String indexName) {
        try (Cursor c = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='index' AND name=?",
                new String[]{indexName})) {
            return c.moveToFirst();
        }
    }
}
