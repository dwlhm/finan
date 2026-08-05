package com.dwlhm.finan.service.wallet;

import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.entity.Wallet;
import java.util.List;

public final class WalletService {

    private final SQLiteDatabase db;
    private final WalletDao walletDao;

    public WalletService(SQLiteDatabase db, WalletDao walletDao) {
        this.db = db;
        this.walletDao = walletDao;
    }

    public Wallet create(String name, String currencyCode, boolean makeDefault, long openingBalanceMinor, String icon) {
        String trimmed = name == null ? "" : name.trim();
        if (walletDao.findByNameIgnoreCase(trimmed) != null) {
            throw new IllegalArgumentException("Wallet name already exists");
        }
        db.beginTransaction();
        try {
            long walletId = walletDao.insert(trimmed, currencyCode, makeDefault, openingBalanceMinor, System.currentTimeMillis(), icon);
            if (walletId <= 0L) {
                throw new IllegalStateException("Failed to create wallet");
            }
            if (makeDefault) {
                walletDao.clearDefaultWalletsExcept(walletId);
            }
            Wallet wallet = walletDao.findById(walletId);
            db.setTransactionSuccessful();
            return wallet;
        } finally {
            db.endTransaction();
        }
    }

    public void updateNameAndDefault(long walletId, String name, boolean makeDefault) {
        String trimmed = name == null ? "" : name.trim();
        Wallet existing = walletDao.findByNameIgnoreCase(trimmed);
        if (existing != null && existing.getId() != walletId) {
            throw new IllegalArgumentException("Wallet name already exists");
        }
        db.beginTransaction();
        try {
            walletDao.updateNameAndDefault(walletId, trimmed, makeDefault);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateNameDefaultAndIcon(long walletId, String name, boolean makeDefault, String icon) {
        String trimmed = name == null ? "" : name.trim();
        Wallet existing = walletDao.findByNameIgnoreCase(trimmed);
        if (existing != null && existing.getId() != walletId) {
            throw new IllegalArgumentException("Wallet name already exists");
        }
        db.beginTransaction();
        try {
            walletDao.updateNameDefaultAndIcon(walletId, trimmed, makeDefault, icon);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public boolean delete(long walletId) {
        return walletDao.delete(walletId);
    }

    public long insertUndo(String name, String currencyCode, boolean isDefault, long openingBalanceMinor, long createdAt, String icon) {
        return walletDao.insert(name, currencyCode, isDefault, openingBalanceMinor, createdAt, icon);
    }

    public Wallet findById(long id) {
        return walletDao.findById(id);
    }

    public Wallet findDefault() {
        return walletDao.findDefault();
    }

    public List<Wallet> findAll() {
        return walletDao.findAll();
    }
}
