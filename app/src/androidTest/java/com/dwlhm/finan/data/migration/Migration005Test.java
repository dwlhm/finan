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
public class Migration005Test {

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
  public void freshDatabase_hasClassificationSchemaAndBackfill() {
    helper = new FinanDatabaseHelper(context);
    SQLiteDatabase db = helper.getWritableDatabase();

    assertTrue(columnExists(db, "categories", "cash_flow_activity"));
    assertTrue(columnExists(db, "transactions", "cash_flow_activity"));
    assertTrue(columnExists(db, "transactions", "cash_flow_activity_overridden"));
    assertEquals(
        "OPERATING",
        scalarText(db, "SELECT cash_flow_activity FROM categories WHERE name = 'Makanan'"));
    assertEquals(
        "UNCLASSIFIED",
        scalarText(db, "SELECT cash_flow_activity FROM categories WHERE name = 'Lainnya'"));
  }

  @Test
  public void upgradeFromV4_backfillsTransactionFromCategory() {
    SQLiteDatabase v4 = context.openOrCreateDatabase(FinanDatabaseHelper.DATABASE_NAME, 0, null);
    v4.setForeignKeyConstraintsEnabled(true);
    MigrationRunner.migrate(
        v4,
        0,
        4,
        new Migration[] {
          new Migration001Initial(),
          new Migration002TransactionIndexes(),
          new Migration003TagMerchantEntities(),
          new Migration004WalletOperations()
        });
    long walletId = scalarLong(v4, "SELECT id FROM wallets LIMIT 1");
    long categoryId = scalarLong(v4, "SELECT id FROM categories WHERE name = 'Makanan'");
    long now = System.currentTimeMillis();
    v4.execSQL(
        "INSERT INTO transactions "
            + "(amount_minor, type, wallet_id, category_id, occurred_at, created_at, updated_at) "
            + "VALUES (10000, 'EXPENSE', ?, ?, ?, ?, ?)",
        new Object[] {walletId, categoryId, now, now, now});
    v4.setVersion(4);
    v4.close();

    helper = new FinanDatabaseHelper(context);
    SQLiteDatabase upgraded = helper.getWritableDatabase();

    assertEquals(
        "OPERATING",
        scalarText(upgraded, "SELECT cash_flow_activity FROM transactions LIMIT 1"));
    assertEquals(
        0L,
        scalarLong(
            upgraded, "SELECT cash_flow_activity_overridden FROM transactions LIMIT 1"));
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

  private static long scalarLong(SQLiteDatabase db, String sql) {
    try (Cursor c = db.rawQuery(sql, null)) {
      assertTrue(c.moveToFirst());
      return c.getLong(0);
    }
  }

  private static String scalarText(SQLiteDatabase db, String sql) {
    try (Cursor c = db.rawQuery(sql, null)) {
      assertTrue(c.moveToFirst());
      return c.getString(0);
    }
  }
}
