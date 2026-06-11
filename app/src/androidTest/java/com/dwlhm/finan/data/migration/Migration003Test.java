package com.dwlhm.finan.data.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.MerchantDao;
import com.dwlhm.finan.data.dao.TagDao;
import com.dwlhm.finan.data.dao.TransactionDao;
import com.dwlhm.finan.data.dao.TransactionTagDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.entity.Merchant;
import com.dwlhm.finan.data.entity.Tag;
import com.dwlhm.finan.data.entity.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class Migration003Test {

  private FinanDatabaseHelper helper;
  private SQLiteDatabase db;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
    helper = new FinanDatabaseHelper(context);
    db = helper.getWritableDatabase();
  }

  @After
  public void tearDown() {
    helper.close();
  }

  @Test
  public void freshDatabase_hasTagAndMerchantTables() {
    assertTableExists("tags");
    assertTableExists("merchants");
    assertTableExists("transaction_tags");
    assertNoColumn("transactions", "tag");
    assertNoColumn("transactions", "merchant");
    assertHasColumn("transactions", "merchant_id");
  }

  @Test
  public void upgradeFromV2_backfillsMerchantAndTags() {
    Context context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);

    SQLiteDatabase v2Db = context.openOrCreateDatabase(FinanDatabaseHelper.DATABASE_NAME, 0, null);
    MigrationRunner.migrate(
        v2Db, 0, 2, new Migration001Initial(), new Migration002TransactionIndexes());
    WalletDao walletDao = new WalletDao(v2Db);
    CategoryDao categoryDao = new CategoryDao(v2Db);
    TransactionDao transactionDao = new TransactionDao(v2Db);
    long walletId = walletDao.findDefault().getId();
    long categoryId = categoryDao.findByName("Makanan").getId();
    long now = System.currentTimeMillis();
    transactionDao.insert(
        25_000, "EXPENSE", walletId, categoryId, now, "note", "liburan,pacar", "Warung", now, now);
    v2Db.setVersion(2);
    v2Db.close();

    FinanDatabaseHelper upgraded = new FinanDatabaseHelper(context);
    SQLiteDatabase upgradedDb = upgraded.getWritableDatabase();
    TransactionDao upgradedTxDao = new TransactionDao(upgradedDb);
    TransactionTagDao transactionTagDao = new TransactionTagDao(upgradedDb);
    MerchantDao merchantDao = new MerchantDao(upgradedDb);
    TagDao tagDao = new TagDao(upgradedDb);

    Transaction tx = upgradedTxDao.findById(1L);
    assertNotNull(tx);
    Merchant merchant = merchantDao.findById(tx.getMerchantId());
    assertNotNull(merchant);
    assertEquals("Warung", merchant.getName());

    List<Long> tagIds = transactionTagDao.findTagIdsByTransaction(tx.getId());
    assertEquals(2, tagIds.size());
    Tag first = tagDao.findById(tagIds.get(0));
    Tag second = tagDao.findById(tagIds.get(1));
    assertNotNull(first);
    assertNotNull(second);
    assertTrue(
        (first.getName().equals("liburan") && second.getName().equals("pacar"))
            || (first.getName().equals("pacar") && second.getName().equals("liburan")));

    upgraded.close();
  }

  private void assertTableExists(String table) {
    try (Cursor cursor =
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            new String[] {table})) {
      assertTrue(cursor.moveToFirst());
    }
  }

  private void assertNoColumn(String table, String column) {
    try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
      while (cursor.moveToNext()) {
        if (column.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
          throw new AssertionError("Column should not exist: " + column);
        }
      }
    }
  }

  private void assertHasColumn(String table, String column) {
    boolean found = false;
    try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
      while (cursor.moveToNext()) {
        if (column.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
          found = true;
          break;
        }
      }
    }
    assertTrue(found);
  }
}
