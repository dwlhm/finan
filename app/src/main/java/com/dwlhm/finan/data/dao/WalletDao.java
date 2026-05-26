package com.dwlhm.finan.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.entity.Wallet;

import java.util.ArrayList;
import java.util.List;

public final class WalletDao {

  private final SQLiteDatabase db;

  public WalletDao(SQLiteDatabase db) {
    this.db = db;
  }

  public long insert(
      String name, String currencyCode, boolean isDefault, long cachedBalanceMinor, long createdAt) {
    ContentValues values = new ContentValues();
    values.put("name", name);
    values.put("currency_code", currencyCode);
    values.put("is_default", isDefault ? 1 : 0);
    values.put("cached_balance_minor", cachedBalanceMinor);
    values.put("created_at", createdAt);
    return db.insert("wallets", null, values);
  }

  public boolean update(
      long id,
      String name,
      String currencyCode,
      boolean isDefault,
      long cachedBalanceMinor,
      long createdAt) {
    ContentValues values = new ContentValues();
    values.put("name", name);
    values.put("currency_code", currencyCode);
    values.put("is_default", isDefault ? 1 : 0);
    values.put("cached_balance_minor", cachedBalanceMinor);
    values.put("created_at", createdAt);
    return db.update("wallets", values, "id = ?", new String[]{String.valueOf(id)}) > 0;
  }

  public boolean delete(long id) {
    return db.delete("wallets", "id = ?", new String[]{String.valueOf(id)}) > 0;
  }

  public Wallet findById(long id) {
    try (Cursor c =
        db.query(
            "wallets",
            null,
            "id = ?",
            new String[]{String.valueOf(id)},
            null,
            null,
            null)) {
      if (!c.moveToFirst()) {
        return null;
      }
      return map(c);
    }
  }

  public Wallet findDefault() {
    try (Cursor c =
        db.query(
            "wallets",
            null,
            "is_default = 1",
            null,
            null,
            null,
            "id ASC",
            "1")) {
      if (!c.moveToFirst()) {
        return null;
      }
      return map(c);
    }
  }

  public List<Wallet> findAll() {
    List<Wallet> wallets = new ArrayList<>();
    try (Cursor c = db.query("wallets", null, null, null, null, null, "id ASC")) {
      while (c.moveToNext()) {
        wallets.add(map(c));
      }
    }
    return wallets;
  }

  private static Wallet map(Cursor c) {
    return new Wallet(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("name")),
        c.getString(c.getColumnIndexOrThrow("currency_code")),
        c.getInt(c.getColumnIndexOrThrow("is_default")) == 1,
        c.getLong(c.getColumnIndexOrThrow("cached_balance_minor")),
        c.getLong(c.getColumnIndexOrThrow("created_at")));
  }
}
