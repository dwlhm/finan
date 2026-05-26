package com.dwlhm.finan.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.entity.Transaction;

import java.util.ArrayList;
import java.util.List;

public final class TransactionDao {

  private final SQLiteDatabase db;

  public TransactionDao(SQLiteDatabase db) {
    this.db = db;
  }

  public long insert(
      long amountMinor,
      String type,
      long walletId,
      long categoryId,
      long occurredAt,
      String note,
      String tag,
      String merchant,
      long createdAt,
      long updatedAt) {
    ContentValues values = new ContentValues();
    values.put("amount_minor", amountMinor);
    values.put("type", type);
    values.put("wallet_id", walletId);
    values.put("category_id", categoryId);
    values.put("occurred_at", occurredAt);
    putNullable(values, "note", note);
    putNullable(values, "tag", tag);
    putNullable(values, "merchant", merchant);
    values.put("created_at", createdAt);
    values.put("updated_at", updatedAt);
    return db.insert("transactions", null, values);
  }

  public boolean update(
      long id,
      long amountMinor,
      String type,
      long walletId,
      long categoryId,
      long occurredAt,
      String note,
      String tag,
      String merchant,
      long createdAt,
      long updatedAt) {
    ContentValues values = new ContentValues();
    values.put("amount_minor", amountMinor);
    values.put("type", type);
    values.put("wallet_id", walletId);
    values.put("category_id", categoryId);
    values.put("occurred_at", occurredAt);
    putNullable(values, "note", note);
    putNullable(values, "tag", tag);
    putNullable(values, "merchant", merchant);
    values.put("created_at", createdAt);
    values.put("updated_at", updatedAt);
    return db.update("transactions", values, "id = ?", new String[]{String.valueOf(id)}) > 0;
  }

  public boolean delete(long id) {
    return db.delete("transactions", "id = ?", new String[]{String.valueOf(id)}) > 0;
  }

  public Transaction findById(long id) {
    try (Cursor c =
        db.query(
            "transactions",
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

  public List<Transaction> findAll() {
    List<Transaction> transactions = new ArrayList<>();
    try (Cursor c =
        db.query(
            "transactions",
            null,
            null,
            null,
            null,
            null,
            "occurred_at DESC, id DESC")) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  public List<Transaction> findRecent(int limit) {
    List<Transaction> transactions = new ArrayList<>();
    try (Cursor c =
        db.query(
            "transactions",
            null,
            null,
            null,
            null,
            null,
            "occurred_at DESC, id DESC",
            String.valueOf(limit))) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  public Transaction findLast() {
    List<Transaction> recent = findRecent(1);
    return recent.isEmpty() ? null : recent.get(0);
  }

  public List<Transaction> findByWalletId(long walletId) {
    List<Transaction> transactions = new ArrayList<>();
    try (Cursor c =
        db.query(
            "transactions",
            null,
            "wallet_id = ?",
            new String[] {String.valueOf(walletId)},
            null,
            null,
            "occurred_at ASC, id ASC",
            null)) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  public List<Transaction> findRecentByWallet(long walletId, int limit) {
    List<Transaction> transactions = new ArrayList<>();
    try (Cursor c =
        db.query(
            "transactions",
            null,
            "wallet_id = ?",
            new String[]{String.valueOf(walletId)},
            null,
            null,
            "occurred_at DESC, id DESC",
            String.valueOf(limit))) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  private static void putNullable(ContentValues values, String key, String value) {
    if (value == null) {
      values.putNull(key);
    } else {
      values.put(key, value);
    }
  }

  private static Transaction map(Cursor c) {
    return new Transaction(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getLong(c.getColumnIndexOrThrow("amount_minor")),
        c.getString(c.getColumnIndexOrThrow("type")),
        c.getLong(c.getColumnIndexOrThrow("wallet_id")),
        c.getLong(c.getColumnIndexOrThrow("category_id")),
        c.getLong(c.getColumnIndexOrThrow("occurred_at")),
        c.getString(c.getColumnIndexOrThrow("note")),
        c.getString(c.getColumnIndexOrThrow("tag")),
        c.getString(c.getColumnIndexOrThrow("merchant")),
        c.getLong(c.getColumnIndexOrThrow("created_at")),
        c.getLong(c.getColumnIndexOrThrow("updated_at")));
  }
}
