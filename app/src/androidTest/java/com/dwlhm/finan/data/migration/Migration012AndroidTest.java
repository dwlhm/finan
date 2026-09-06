package com.dwlhm.finan.data.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.dao.TransactionTemplateDao;
import com.dwlhm.finan.data.db.FinanDatabaseHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class Migration012AndroidTest {

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
  public void freshDatabase_createsRecurringScheduleColumns() {
    helper = new FinanDatabaseHelper(context);
    SQLiteDatabase db = helper.getWritableDatabase();

    assertTrue(columnExists(db, TransactionTemplateDao.TABLE_NAME, "frequency"));
    assertTrue(columnExists(db, TransactionTemplateDao.TABLE_NAME, "due_day"));
    assertTrue(columnExists(db, TransactionTemplateDao.TABLE_NAME, "is_scheduled"));
    assertTrue(columnExists(db, TransactionTemplateDao.TABLE_NAME, "last_recorded_at"));
  }

  @Test
  public void upgradeFromV11_appliesRecurringScheduleMigration() {
    SQLiteDatabase v11Db = context.openOrCreateDatabase(FinanDatabaseHelper.DATABASE_NAME, 0, null);
    v11Db.setForeignKeyConstraintsEnabled(true);
    MigrationRunner.migrate(
        v11Db,
        0,
        11,
        new Migration[] {
          new Migration001Initial(),
          new Migration002TransactionIndexes(),
          new Migration003TagMerchantEntities(),
          new Migration004WalletOperations(),
          new Migration005CashFlowClassification(),
          new Migration006CategoryIcon(),
          new Migration007WalletIcon(),
          new Migration008RemoveTagsMerchants(),
          new Migration009CategoryDefault(),
          new Migration010NoOp(),
          new Migration011TransactionTemplate()
        });
    v11Db.setVersion(11);
    v11Db.close();

    helper = new FinanDatabaseHelper(context);
    SQLiteDatabase upgraded = helper.getWritableDatabase();

    assertTrue(columnExists(upgraded, TransactionTemplateDao.TABLE_NAME, "frequency"));
    assertTrue(columnExists(upgraded, TransactionTemplateDao.TABLE_NAME, "due_day"));
    assertTrue(columnExists(upgraded, TransactionTemplateDao.TABLE_NAME, "is_scheduled"));
    assertTrue(columnExists(upgraded, TransactionTemplateDao.TABLE_NAME, "last_recorded_at"));

    try (Cursor cursor =
        upgraded.rawQuery(
            "SELECT name, frequency, due_day, is_scheduled, last_recorded_at FROM "
                + TransactionTemplateDao.TABLE_NAME
                + " ORDER BY sort_order ASC",
            null)) {
      assertEquals(4, cursor.getCount());
      assertTrue(cursor.moveToFirst());
      assertEquals("Makan Siang", cursor.getString(0));
      assertEquals("NONE", cursor.getString(1));
      assertEquals(0, cursor.getInt(2));
      assertEquals(0, cursor.getInt(3));
      assertEquals(0L, cursor.getLong(4));

      assertTrue(cursor.moveToNext());
      assertEquals("Kopi / Minuman", cursor.getString(0));
      assertEquals("NONE", cursor.getString(1));
      assertEquals(0, cursor.getInt(2));
      assertEquals(0, cursor.getInt(3));
      assertEquals(0L, cursor.getLong(4));

      assertTrue(cursor.moveToNext());
      assertEquals("Bensin", cursor.getString(0));
      assertEquals("NONE", cursor.getString(1));
      assertEquals(0, cursor.getInt(2));
      assertEquals(0, cursor.getInt(3));
      assertEquals(0L, cursor.getLong(4));

      assertTrue(cursor.moveToNext());
      assertEquals("Belanja Harian", cursor.getString(0));
      assertEquals("NONE", cursor.getString(1));
      assertEquals(0, cursor.getInt(2));
      assertEquals(0, cursor.getInt(3));
      assertEquals(0L, cursor.getLong(4));
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
}
