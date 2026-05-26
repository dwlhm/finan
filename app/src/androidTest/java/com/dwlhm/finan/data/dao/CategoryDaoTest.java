package com.dwlhm.finan.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.entity.Category;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CategoryDaoTest {

    private FinanDatabaseHelper helper;
    private CategoryDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
        helper = new FinanDatabaseHelper(context);
        dao = new CategoryDao(helper.getWritableDatabase());
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void findAllOrdered_returnsSevenSeededCategories() {
        List<Category> categories = dao.findAllOrdered();
        assertEquals(7, categories.size());
        assertEquals("Makanan", categories.get(0).getName());
        assertEquals("EXPENSE", categories.get(0).getTypeFilter());
    }

    @Test
    public void findByTypeFilter_expenseExcludesIncomeOnly() {
        List<Category> expense = dao.findByTypeFilter("EXPENSE");
        assertTrue(expense.size() >= 5);
        for (Category c : expense) {
            String filter = c.getTypeFilter();
            assertTrue(filter.equals("EXPENSE") || filter.equals("BOTH"));
        }
    }

    @Test
    public void incrementUsage_updatesCountAndLastUsed() {
        Category makanan = dao.findByName("Makanan");
        assertNotNull(makanan);
        long usedAt = 1_700_000_000_000L;
        dao.incrementUsage(makanan.getId(), usedAt);
        Category updated = dao.findById(makanan.getId());
        assertEquals(1, updated.getUsageCount());
        assertEquals(Long.valueOf(usedAt), updated.getLastUsedAt());
    }

    @Test
    public void insertAndFindById_roundTrip() {
        long id = dao.insert("Hobi", "EXPENSE", 99, 0, null);
        Category category = dao.findById(id);
        assertEquals("Hobi", category.getName());
        assertEquals("EXPENSE", category.getTypeFilter());
        assertEquals(99, category.getSortOrder());
    }
}
