package com.dwlhm.finan.data.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.db.FinanDatabaseHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class Migration004Test {

  private Context context;
  private FinanDatabaseHelper helper;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
  }

  @After
  public void tearDown() {
    if (helper != null) {
      helper.close();
    }
  }

  @Test
  public void freshDatabase_hasWalletOperationSchema() {
    helper = new FinanDatabaseHelper(context);
    SQLiteDatabase db = helper.getWritableDatabase();

    assertTrue(transfersTableExists(db));
    assertTrue(columnExists(db, "wallets", "opening_balance_minor"));
    assertTrue(columnExists(db, "transactions", "transfer_id"));
  }

  @Test
  public void upgradeFromV3_preservesCachedBalanceAndBackfillsOpeningBalance() {
    SQLiteDatabase v3 = context.openOrCreateDatabase(FinanDatabaseHelper.DATABASE_NAME, 0, null);
    v3.setForeignKeyConstraintsEnabled(true);
    MigrationRunner.migrate(
        v3,
        0,
        3,
        new Migration[] {
          new Migration001Initial(),
          new Migration002TransactionIndexes(),
          new Migration003TagMerchantEntities()
        });
    long walletId = scalar(v3, "SELECT id FROM wallets LIMIT 1");
    long categoryId = scalar(v3, "SELECT id FROM categories LIMIT 1");
    long now = System.currentTimeMillis();
    v3.execSQL(
        "INSERT INTO transactions "
            + "(amount_minor, type, wallet_id, category_id, occurred_at, created_at, updated_at) "
            + "VALUES (100000, 'EXPENSE', ?, ?, ?, ?, ?)",
        new Object[] {walletId, categoryId, now, now, now});
    v3.execSQL(
        "UPDATE wallets SET cached_balance_minor = 900000 WHERE id = ?",
        new Object[] {walletId});
    v3.setVersion(3);
    v3.close();

    helper = new FinanDatabaseHelper(context);
    SQLiteDatabase upgraded = helper.getWritableDatabase();

    assertEquals(
        1_000_000L,
        scalar(
            upgraded,
            "SELECT opening_balance_minor FROM wallets WHERE id = " + walletId));
    assertEquals(
        900_000L,
        scalar(
            upgraded,
            "SELECT cached_balance_minor FROM wallets WHERE id = " + walletId));
    assertTrue(transfersTableExists(upgraded));
    assertTrue(columnExists(upgraded, "transactions", "transfer_id"));
  }

  private static boolean transfersTableExists(SQLiteDatabase db) {
    try (Cursor c =
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
            new String[] {"transfers"})) {
      return c.moveToFirst();
    }
  }

  private static boolean columnExists(SQLiteDatabase db, String table, String column) {
    try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
      while (c.moveToNext()) {
        if (column.equals(c.getString(c.getColumnIndexOrThrow("name")))) {
          return true;
        }
      }
      return false;
    }
  }

  private static long scalar(SQLiteDatabase db, String sql) {
    try (Cursor c = db.rawQuery(sql, null)) {
      assertTrue(c.moveToFirst());
      return c.getLong(0);
    }
  }
}
