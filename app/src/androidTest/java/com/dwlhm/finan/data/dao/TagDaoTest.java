package com.dwlhm.finan.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.entity.Tag;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TagDaoTest {

  private FinanDatabaseHelper helper;
  private TagDao dao;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
    helper = new FinanDatabaseHelper(context);
    dao = new TagDao(helper.getWritableDatabase());
  }

  @After
  public void tearDown() {
    helper.close();
  }

  @Test
  public void insertIfAbsent_isCaseInsensitive() {
    Tag first = dao.insertIfAbsent("Liburan");
    Tag second = dao.insertIfAbsent("liburan");
    assertEquals(first.getId(), second.getId());
    assertNotNull(dao.findByNameIgnoreCase("LIBURAN"));
  }

  @Test
  public void incrementUsage_updatesCount() {
    long id = dao.insert("reimburs", 0, null);
    dao.incrementUsage(id, 100L);
    Tag tag = dao.findById(id);
    assertEquals(1, tag.getUsageCount());
  }
}
