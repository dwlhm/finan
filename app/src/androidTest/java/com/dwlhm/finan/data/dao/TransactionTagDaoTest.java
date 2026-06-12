package com.dwlhm.finan.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.db.FinanDatabaseHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RunWith(AndroidJUnit4.class)
public class TransactionTagDaoTest {

  private FinanDatabaseHelper helper;
  private TagDao tagDao;
  private TransactionTagDao dao;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
    helper = new FinanDatabaseHelper(context);
    tagDao = new TagDao(helper.getWritableDatabase());
    dao = new TransactionTagDao(helper.getWritableDatabase());
  }

  @After
  public void tearDown() {
    helper.close();
  }

  @Test
  public void replaceAll_storesMultipleTagsPerTransaction() {
    long tagA = tagDao.insert("liburan", 0, null);
    long tagB = tagDao.insert("pacar", 0, null);
    WalletDao walletDao = new WalletDao(helper.getWritableDatabase());
    CategoryDao categoryDao = new CategoryDao(helper.getWritableDatabase());
    long now = System.currentTimeMillis();
    long transactionId =
        new TransactionDao(helper.getWritableDatabase())
            .insert(
                1_000L,
                "EXPENSE",
                Objects.requireNonNull(walletDao.findDefault()).getId(),
                Objects.requireNonNull(categoryDao.findByName("Makanan")).getId(),
                now,
                null,
                null,
                now,
                now);

    dao.replaceAll(transactionId, Arrays.asList(tagA, tagB, tagA));

    List<Long> tagIds = dao.findTagIdsByTransaction(transactionId);
    assertEquals(2, tagIds.size());
    assertTrue(tagIds.contains(tagA));
    assertTrue(tagIds.contains(tagB));
  }
}
