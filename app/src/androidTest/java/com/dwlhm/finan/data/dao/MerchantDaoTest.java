package com.dwlhm.finan.data.dao;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.entity.Merchant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MerchantDaoTest {

  private FinanDatabaseHelper helper;
  private MerchantDao dao;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
    helper = new FinanDatabaseHelper(context);
    dao = new MerchantDao(helper.getWritableDatabase());
  }

  @After
  public void tearDown() {
    helper.close();
  }

  @Test
  public void insertIfAbsent_reusesExistingName() {
    Merchant first = dao.insertIfAbsent("Indomaret");
    Merchant second = dao.insertIfAbsent("indomaret");
    assertEquals(first.getId(), second.getId());
  }
}
