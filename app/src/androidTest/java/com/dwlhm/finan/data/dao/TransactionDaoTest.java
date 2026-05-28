package com.dwlhm.finan.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.entity.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TransactionDaoTest {

    private FinanDatabaseHelper helper;
    private WalletDao walletDao;
    private CategoryDao categoryDao;
    private TransactionDao dao;

    private long walletId;
    private long categoryId;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
        helper = new FinanDatabaseHelper(context);
        walletDao = new WalletDao(helper.getWritableDatabase());
        categoryDao = new CategoryDao(helper.getWritableDatabase());
        dao = new TransactionDao(helper.getWritableDatabase());

        walletId = walletDao.findDefault().getId();
        categoryId = categoryDao.findByName("Makanan").getId();
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void insertAndFindById_roundTrip() {
        long now = System.currentTimeMillis();
        long id = dao.insert(
                25_000,
                "EXPENSE",
                walletId,
                categoryId,
                now,
                "makan siang",
                "food",
                "Warung",
                now,
                now);
        Transaction tx = dao.findById(id);
        assertNotNull(tx);
        assertEquals(25_000, tx.getAmountMinor());
        assertEquals("EXPENSE", tx.getType());
        assertEquals(walletId, tx.getWalletId());
        assertEquals(categoryId, tx.getCategoryId());
        assertEquals("makan siang", tx.getNote());
        assertEquals("food", tx.getTag());
        assertEquals("Warung", tx.getMerchant());
    }

    @Test
    public void update_changesFields() {
        long now = System.currentTimeMillis();
        long id = dao.insert(10_000, "EXPENSE", walletId, categoryId, now, null, null, null, now, now);
        long updatedAt = now + 1000;
        dao.update(id, 12_000, "EXPENSE", walletId, categoryId, now, "updated", null, null, now, updatedAt);
        Transaction tx = dao.findById(id);
        assertEquals(12_000, tx.getAmountMinor());
        assertEquals("updated", tx.getNote());
        assertEquals(updatedAt, tx.getUpdatedAt());
    }

    @Test
    public void delete_removesRow() {
        long now = System.currentTimeMillis();
        long id = dao.insert(5_000, "EXPENSE", walletId, categoryId, now, null, null, null, now, now);
        assertTrue(dao.delete(id));
        assertNull(dao.findById(id));
    }

    @Test
    public void findRecentByWallet_ordersNewestFirst() {
        long base = System.currentTimeMillis();
        dao.insert(1_000, "EXPENSE", walletId, categoryId, base, null, null, null, base, base);
        dao.insert(2_000, "EXPENSE", walletId, categoryId, base + 1, null, null, null, base + 1, base + 1);
        List<Transaction> recent = dao.findRecentByWallet(walletId, 10);
        assertEquals(2, recent.size());
        assertEquals(2_000, recent.get(0).getAmountMinor());
        assertEquals(1_000, recent.get(1).getAmountMinor());
    }

    @Test
    public void findHistory_filtersAndSortsInQuery() {
        long base = System.currentTimeMillis();
        long otherWalletId = walletDao.insert("Tabungan", "IDR", false, 0L, base);
        long otherCategoryId = categoryDao.insert("Bonus", "BOTH", 100, 0, null);

        dao.insert(1_000, "EXPENSE", walletId, categoryId, base, null, null, null, base, base);
        dao.insert(
                2_000, "INCOME", walletId, categoryId, base + 1, null, null, null, base + 1, base + 1);
        dao.insert(
                3_000, "EXPENSE", otherWalletId, categoryId, base + 2, null, null, null, base + 2, base + 2);
        dao.insert(
                4_000, "EXPENSE", walletId, otherCategoryId, base + 3, null, null, null, base + 3, base + 3);
        dao.insert(5_000, "EXPENSE", walletId, categoryId, base + 4, null, null, null, base + 4, base + 4);

        List<Transaction> newestFirst =
                dao.findHistory(walletId, categoryId, "EXPENSE", null, null, false);
        assertEquals(2, newestFirst.size());
        assertEquals(5_000, newestFirst.get(0).getAmountMinor());
        assertEquals(1_000, newestFirst.get(1).getAmountMinor());

        List<Transaction> oldestFirst =
                dao.findHistory(walletId, categoryId, "EXPENSE", null, null, true);
        assertEquals(2, oldestFirst.size());
        assertEquals(1_000, oldestFirst.get(0).getAmountMinor());
        assertEquals(5_000, oldestFirst.get(1).getAmountMinor());
    }

    @Test
    public void findHistory_filtersByOccurredAtRange() {
        long base = System.currentTimeMillis();
        dao.insert(1_000, "EXPENSE", walletId, categoryId, base, null, null, null, base, base);
        dao.insert(
                2_000, "EXPENSE", walletId, categoryId, base + 10, null, null, null, base + 10, base + 10);
        dao.insert(
                3_000, "EXPENSE", walletId, categoryId, base + 20, null, null, null, base + 20, base + 20);

        List<Transaction> history =
                dao.findHistory(null, null, null, base + 10, base + 20, true);

        assertEquals(1, history.size());
        assertEquals(2_000, history.get(0).getAmountMinor());
    }
}
