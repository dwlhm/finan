package com.dwlhm.finan.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.entity.Category;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CategoryDaoInsertTest {

  private CategoryDao dao;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
    FinanDatabaseHelper helper = new FinanDatabaseHelper(context);
    dao = new CategoryDao(helper.getWritableDatabase());
  }

  @Test
  public void insertForTransactionType_createsNewCategory() {
    Category created = dao.insertForTransactionType("Hobi Baru", "EXPENSE");
    assertNotNull(created);
    assertEquals("Hobi Baru", created.getName());
    assertEquals("EXPENSE", created.getTypeFilter());
  }

  @Test
  public void insertForTransactionType_returnsExistingByName() {
    Category first = dao.insertForTransactionType("Unik", "EXPENSE");
    Category second = dao.insertForTransactionType("unik", "EXPENSE");
    assertEquals(first.getId(), second.getId());
  }
}
