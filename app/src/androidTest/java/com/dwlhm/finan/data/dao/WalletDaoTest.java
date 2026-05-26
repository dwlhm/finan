package com.dwlhm.finan.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.entity.Wallet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WalletDaoTest {

    private FinanDatabaseHelper helper;
    private WalletDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
        helper = new FinanDatabaseHelper(context);
        dao = new WalletDao(helper.getWritableDatabase());
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void findDefault_returnsSeededWallet() {
        Wallet wallet = dao.findDefault();
        assertNotNull(wallet);
        assertEquals("Dompet Utama", wallet.getName());
        assertEquals("IDR", wallet.getCurrencyCode());
        assertTrue(wallet.isDefault());
        assertEquals(0, wallet.getCachedBalanceMinor());
    }

    @Test
    public void insertAndFindById_roundTrip() {
        long id = dao.insert("Tabungan", "USD", false, 5000, 1_700_000_000_000L);
        Wallet wallet = dao.findById(id);
        assertNotNull(wallet);
        assertEquals("Tabungan", wallet.getName());
        assertEquals("USD", wallet.getCurrencyCode());
        assertEquals(false, wallet.isDefault());
        assertEquals(5000, wallet.getCachedBalanceMinor());
    }

    @Test
    public void update_changesFields() {
        Wallet seeded = dao.findDefault();
        dao.update(seeded.getId(), "Dompet Baru", "SGD", true, 100, seeded.getCreatedAt());
        Wallet updated = dao.findById(seeded.getId());
        assertEquals("Dompet Baru", updated.getName());
        assertEquals("SGD", updated.getCurrencyCode());
        assertTrue(updated.isDefault());
        assertEquals(100, updated.getCachedBalanceMinor());
    }

    @Test
    public void delete_removesRow() {
        long id = dao.insert("Hapus", "IDR", false, 0, 1L);
        assertTrue(dao.delete(id));
        assertNull(dao.findById(id));
    }

    @Test
    public void findAll_includesSeededAndInserted() {
        dao.insert("Kedua", "IDR", false, 0, 2L);
        List<Wallet> wallets = dao.findAll();
        assertEquals(2, wallets.size());
    }
}
