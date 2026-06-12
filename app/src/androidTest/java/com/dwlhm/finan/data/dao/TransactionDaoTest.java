package com.dwlhm.finan.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.entity.Merchant;
import com.dwlhm.finan.data.entity.Tag;
import com.dwlhm.finan.data.entity.Transaction;
import com.dwlhm.finan.domain.model.HistoryQuery;
import com.dwlhm.finan.domain.model.HistorySearch;
import com.dwlhm.finan.domain.model.TransactionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class TransactionDaoTest {

    private FinanDatabaseHelper helper;
    private WalletDao walletDao;
    private CategoryDao categoryDao;
    private TransactionDao dao;
    private TagDao tagDao;
    private MerchantDao merchantDao;
    private TransactionTagDao transactionTagDao;

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
        tagDao = new TagDao(helper.getWritableDatabase());
        merchantDao = new MerchantDao(helper.getWritableDatabase());
        transactionTagDao = new TransactionTagDao(helper.getWritableDatabase());

        walletId = Objects.requireNonNull(walletDao.findDefault()).getId();
        categoryId = Objects.requireNonNull(categoryDao.findByName("Makanan")).getId();
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void insertAndFindById_roundTripWithMerchant() {
        long now = System.currentTimeMillis();
        Merchant merchant = merchantDao.insertIfAbsent("Warung");
        long id =
            dao.insert(
                25_000,
                "EXPENSE",
                walletId,
                categoryId,
                now,
                "makan siang",
                merchant.getId(),
                now,
                now);
        Tag tag = tagDao.insertIfAbsent("food");
        transactionTagDao.replaceAll(id, java.util.Collections.singletonList(tag.getId()));
        Transaction tx = Objects.requireNonNull(dao.findById(id));
        assertEquals(25_000, tx.getAmountMinor());
        assertEquals("EXPENSE", tx.getType());
        assertEquals(walletId, tx.getWalletId());
        assertEquals(categoryId, tx.getCategoryId());
        assertEquals("makan siang", tx.getNote());
        assertEquals(merchant.getId(), tx.getMerchantId().longValue());
        assertEquals(1, transactionTagDao.findTagIdsByTransaction(id).size());
    }

    @Test
    public void update_changesFields() {
        long now = System.currentTimeMillis();
        long id = dao.insert(10_000, "EXPENSE", walletId, categoryId, now, null, null, now, now);
        long updatedAt = now + 1000;
        dao.update(id, 12_000, "EXPENSE", walletId, categoryId, now, "updated", null, now, updatedAt);
        Transaction tx = Objects.requireNonNull(dao.findById(id));
        assertEquals(12_000, tx.getAmountMinor());
        assertEquals("updated", tx.getNote());
        assertEquals(updatedAt, tx.getUpdatedAt());
    }

    @Test
    public void delete_removesRow() {
        long now = System.currentTimeMillis();
        long id = dao.insert(5_000, "EXPENSE", walletId, categoryId, now, null, null, now, now);
        assertTrue(dao.delete(id));
        assertNull(dao.findById(id));
    }

    @Test
    public void findRecentByWallet_ordersNewestFirst() {
        long base = System.currentTimeMillis();
        dao.insert(1_000, "EXPENSE", walletId, categoryId, base, null, null, base, base);
        dao.insert(2_000, "EXPENSE", walletId, categoryId, base + 1, null, null, base + 1, base + 1);
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

        dao.insert(1_000, "EXPENSE", walletId, categoryId, base, null, null, base, base);
        dao.insert(
                2_000, "INCOME", walletId, categoryId, base + 1, null, null, base + 1, base + 1);
        dao.insert(
                3_000, "EXPENSE", otherWalletId, categoryId, base + 2, null, null, base + 2, base + 2);
        dao.insert(
                4_000, "EXPENSE", walletId, otherCategoryId, base + 3, null, null, base + 3, base + 3);
        dao.insert(5_000, "EXPENSE", walletId, categoryId, base + 4, null, null, base + 4, base + 4);

        List<Transaction> newestFirst =
                dao.findHistory(query(walletId, categoryId, TransactionType.EXPENSE, null, null, false));
        assertEquals(2, newestFirst.size());
        assertEquals(5_000, newestFirst.get(0).getAmountMinor());
        assertEquals(1_000, newestFirst.get(1).getAmountMinor());

        List<Transaction> oldestFirst =
                dao.findHistory(query(walletId, categoryId, TransactionType.EXPENSE, null, null, true));
        assertEquals(2, oldestFirst.size());
        assertEquals(1_000, oldestFirst.get(0).getAmountMinor());
        assertEquals(5_000, oldestFirst.get(1).getAmountMinor());
    }

    @Test
    public void findHistory_filtersByOccurredAtRange() {
        long base = System.currentTimeMillis();
        dao.insert(1_000, "EXPENSE", walletId, categoryId, base, null, null, base, base);
        dao.insert(
                2_000, "EXPENSE", walletId, categoryId, base + 10, null, null, base + 10, base + 10);
        dao.insert(
                3_000, "EXPENSE", walletId, categoryId, base + 20, null, null, base + 20, base + 20);

        List<Transaction> history =
                dao.findHistory(query(null, null, null, base + 10, base + 20, true));

        assertEquals(1, history.size());
        assertEquals(2_000, history.get(0).getAmountMinor());
    }

    @Test
    public void findHistoryPage_usesCursorKeysetPagination() {
        long base = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            dao.insert(
                    (i + 1) * 1_000L,
                    "EXPENSE",
                    walletId,
                    categoryId,
                    base + i,
                    null,
                    null,
                    base + i,
                    base + i);
        }

        List<Transaction> firstPage =
                dao.findHistoryPage(query(null, null, null, null, null, false), null, null, 2);
        assertEquals(2, firstPage.size());
        assertEquals(5_000, firstPage.get(0).getAmountMinor());
        assertEquals(4_000, firstPage.get(1).getAmountMinor());

        Transaction last = firstPage.get(1);
        List<Transaction> secondPage =
                dao.findHistoryPage(
                        query(null, null, null, null, null, false),
                        last.getOccurredAt(),
                        last.getId(),
                        2);
        assertEquals(2, secondPage.size());
        assertEquals(3_000, secondPage.get(0).getAmountMinor());
        assertEquals(2_000, secondPage.get(1).getAmountMinor());

        Transaction lastSecond = secondPage.get(1);
        List<Transaction> thirdPage =
                dao.findHistoryPage(
                        query(null, null, null, null, null, false),
                        lastSecond.getOccurredAt(),
                        lastSecond.getId(),
                        2);
        assertEquals(1, thirdPage.size());
        assertEquals(1_000, thirdPage.get(0).getAmountMinor());
    }

    @Test
    public void findHistoryTotals_aggregatesFilteredRows() {
        long base = System.currentTimeMillis();
        dao.insert(1_000, "EXPENSE", walletId, categoryId, base, null, null, base, base);
        dao.insert(2_000, "INCOME", walletId, categoryId, base + 1, null, null, base + 1, base + 1);
        dao.insert(3_000, "EXPENSE", walletId, categoryId, base + 2, null, null, base + 2, base + 2);

        TransactionDao.HistoryTotalsRow totals =
                dao.findHistoryTotals(query(null, null, null, null, null, false));
        assertEquals(3, totals.count);
        assertEquals(2_000, totals.incomeMinor);
        assertEquals(4_000, totals.expenseMinor);
    }

    @Test
    public void historySearch_matchesResolvedEntityIdsAndKeepsTotalsAccurate() {
        long base = System.currentTimeMillis();
        Merchant starbucks = merchantDao.insertIfAbsent("Starbucks");
        Merchant warung = merchantDao.insertIfAbsent("Warung");
        long matchingId =
                dao.insert(
                        25_000,
                        "EXPENSE",
                        walletId,
                        categoryId,
                        base,
                        "kopi pagi",
                        starbucks.getId(),
                        base,
                        base);
        Tag coffee = tagDao.insertIfAbsent("Coffee");
        transactionTagDao.replaceAll(
                matchingId, java.util.Collections.singletonList(coffee.getId()));
        dao.insert(
                10_000,
                "EXPENSE",
                walletId,
                categoryId,
                base + 1,
                "sarapan",
                warung.getId(),
                base + 1,
                base + 1);

        HistorySearch search =
                new HistorySearch(
                        "starbuks",
                        null,
                        null,
                        null,
                        java.util.Collections.singletonList(starbucks.getId()),
                        null);
        HistoryQuery query = new HistoryQuery(null, null, null, null, null, false, search);
        HistorySearch tagSearch =
                new HistorySearch(
                        "cofee",
                        null,
                        null,
                        null,
                        null,
                        java.util.Collections.singletonList(coffee.getId()));

        List<Transaction> result = dao.findHistory(query);
        TransactionDao.HistoryTotalsRow totals = dao.findHistoryTotals(query);

        assertEquals(1, result.size());
        assertEquals(matchingId, result.get(0).getId());
        assertEquals(1, totals.count);
        assertEquals(25_000, totals.expenseMinor);
        assertEquals(
                matchingId,
                dao.findHistory(
                                new HistoryQuery(
                                        null, null, null, null, null, false, tagSearch))
                        .get(0)
                        .getId());
    }

    @Test
    public void historySearch_matchesNoteAndExactAmount() {
        long base = System.currentTimeMillis();
        dao.insert(
                25_000, "EXPENSE", walletId, categoryId, base, "Makan Siang", null, base, base);
        dao.insert(
                15_000,
                "EXPENSE",
                walletId,
                categoryId,
                base + 1,
                "Camilan",
                null,
                base + 1,
                base + 1);

        HistorySearch noteSearch =
                new HistorySearch("makan", null, null, null, null, null);
        HistorySearch amountSearch =
                new HistorySearch("Rp 15.000", 15_000L, null, null, null, null);

        assertEquals(
                1,
                dao.findHistory(
                                new HistoryQuery(
                                        null, null, null, null, null, false, noteSearch))
                        .size());
        assertEquals(
                15_000,
                dao.findHistory(
                                new HistoryQuery(
                                        null, null, null, null, null, false, amountSearch))
                        .get(0)
                        .getAmountMinor());
    }

    @Test
    public void forEachOrdered_visitsTransactionsInExportOrder() {
        long base = System.currentTimeMillis();
        dao.insert(1_000, "EXPENSE", walletId, categoryId, base, null, null, base, base);
        dao.insert(2_000, "EXPENSE", walletId, categoryId, base + 1, null, null, base + 1, base + 1);

        List<Long> ids = new ArrayList<>();
        dao.forEachOrdered(transaction -> ids.add(transaction.getId()));

        assertEquals(2, ids.size());
        assertTrue(ids.get(0) > ids.get(1));
    }

    @Test
    public void migration002_createsTransactionIndexes() {
        Set<String> indexes = new HashSet<>();
        try (Cursor cursor =
            helper
                .getWritableDatabase()
                .rawQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'transactions'",
                    null)) {
            while (cursor.moveToNext()) {
                indexes.add(cursor.getString(0));
            }
        }

        assertTrue(indexes.contains("idx_transactions_occurred_at_id"));
        assertTrue(indexes.contains("idx_transactions_wallet_occurred"));
        assertTrue(indexes.contains("idx_transactions_type_occurred"));
    }

    private static HistoryQuery query(
            Long walletId,
            Long categoryId,
            TransactionType type,
            Long startInclusiveMillis,
            Long endExclusiveMillis,
            boolean oldestFirst) {
        return new HistoryQuery(
                walletId,
                categoryId,
                type,
                startInclusiveMillis,
                endExclusiveMillis,
                oldestFirst,
                HistorySearch.empty());
    }
}
