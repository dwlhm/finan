package com.dwlhm.finan.service.category;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.TransactionDao;
import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.CashFlowActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CategoryClassificationServiceTest {

  private FinanDatabaseHelper helper;
  private SQLiteDatabase db;
  private CategoryDao categories;
  private TransactionDao transactions;
  private CategoryClassificationService service;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
    helper = new FinanDatabaseHelper(context);
    db = helper.getWritableDatabase();
    categories = new CategoryDao(db);
    transactions = new TransactionDao(db);
    service = new CategoryClassificationService(db, categories, transactions);
  }

  @After
  public void tearDown() {
    helper.close();
  }

  @Test
  public void updateWithHistory_doesNotReplaceOverrides() {
    Category category =
        service.create("Deposito", "INCOME", CashFlowActivity.UNCLASSIFIED);
    long walletId = scalar("SELECT id FROM wallets LIMIT 1");
    long now = System.currentTimeMillis();
    long regular =
        transactions.insert(
            10000,
            "INCOME",
            walletId,
            category.getId(),
            now,
            null,
            null,
            null,
            CashFlowActivity.UNCLASSIFIED.name(),
            false,
            now,
            now);
    long overridden =
        transactions.insert(
            20000,
            "INCOME",
            walletId,
            category.getId(),
            now,
            null,
            null,
            null,
            CashFlowActivity.FINANCING.name(),
            true,
            now,
            now);

    service.update(
        category.getId(), "Deposito", "INCOME", CashFlowActivity.INVESTING, true);

    assertEquals(
        CashFlowActivity.INVESTING.name(),
        transactions.findById(regular).getCashFlowActivity());
    assertEquals(
        CashFlowActivity.FINANCING.name(),
        transactions.findById(overridden).getCashFlowActivity());
  }

  @Test
  public void updateFutureOnly_keepsHistoricalSnapshot() {
    Category category =
        service.create("Aset", "EXPENSE", CashFlowActivity.UNCLASSIFIED);
    long walletId = scalar("SELECT id FROM wallets LIMIT 1");
    long now = System.currentTimeMillis();
    long transactionId =
        transactions.insert(
            10000,
            "EXPENSE",
            walletId,
            category.getId(),
            now,
            null,
            null,
            null,
            CashFlowActivity.UNCLASSIFIED.name(),
            false,
            now,
            now);

    service.update(
        category.getId(), "Aset", "EXPENSE", CashFlowActivity.INVESTING, false);

    assertEquals(
        CashFlowActivity.UNCLASSIFIED.name(),
        transactions.findById(transactionId).getCashFlowActivity());
  }

  private long scalar(String sql) {
    try (android.database.Cursor c = db.rawQuery(sql, null)) {
      c.moveToFirst();
      return c.getLong(0);
    }
  }
}
