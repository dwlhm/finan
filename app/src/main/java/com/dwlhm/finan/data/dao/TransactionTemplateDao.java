package com.dwlhm.finan.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;

import java.util.ArrayList;
import java.util.List;

public final class TransactionTemplateDao {

  public static final String TABLE_NAME = "transaction_templates";

  private final SQLiteDatabase db;

  public TransactionTemplateDao(SQLiteDatabase db) {
    this.db = db;
  }

  public static void createTable(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "name TEXT NOT NULL, "
            + "type TEXT NOT NULL, "
            + "amount_minor INTEGER NOT NULL DEFAULT 0, "
            + "category_id INTEGER, "
            + "wallet_id INTEGER, "
            + "destination_wallet_id INTEGER, "
            + "note TEXT, "
            + "icon TEXT, "
            + "sort_order INTEGER NOT NULL DEFAULT 0, "
            + "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL, "
            + "FOREIGN KEY(wallet_id) REFERENCES wallets(id) ON DELETE SET NULL, "
            + "FOREIGN KEY(destination_wallet_id) REFERENCES wallets(id) ON DELETE SET NULL"
            + ");");
  }

  public static void seedDefaults(SQLiteDatabase db) {
    Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
    boolean empty = true;
    if (cursor != null) {
      if (cursor.moveToFirst()) {
        empty = cursor.getInt(0) == 0;
      }
      cursor.close();
    }
    if (empty) {
      insertDefault(db, "Makan Siang", "EXPENSE", 2500000L, null, null, null, "Makan Siang", "🍱", 1);
      insertDefault(db, "Kopi / Minuman", "EXPENSE", 1800000L, null, null, null, "Kopi", "☕", 2);
      insertDefault(db, "Bensin", "EXPENSE", 3000000L, null, null, null, "Bensin", "⛽", 3);
      insertDefault(db, "Belanja Harian", "EXPENSE", 5000000L, null, null, null, "Belanja", "🛒", 4);
    }
  }

  private static void insertDefault(
      SQLiteDatabase db,
      String name,
      String type,
      long amountMinor,
      Long categoryId,
      Long walletId,
      Long destWalletId,
      String note,
      String icon,
      int sortOrder) {
    ContentValues values = new ContentValues();
    values.put("name", name);
    values.put("type", type);
    values.put("amount_minor", amountMinor);
    if (categoryId != null) values.put("category_id", categoryId);
    if (walletId != null) values.put("wallet_id", walletId);
    if (destWalletId != null) values.put("destination_wallet_id", destWalletId);
    if (note != null) values.put("note", note);
    values.put("icon", icon);
    values.put("sort_order", sortOrder);
    db.insert(TABLE_NAME, null, values);
  }

  public long insert(TransactionTemplate template) {
    ContentValues values = toContentValues(template);
    long id = db.insert(TABLE_NAME, null, values);
    if (id > 0) {
      template.setId(id);
    }
    return id;
  }

  public boolean update(TransactionTemplate template) {
    ContentValues values = toContentValues(template);
    return db.update(TABLE_NAME, values, "id = ?", new String[] {String.valueOf(template.getId())}) > 0;
  }

  public boolean delete(long id) {
    return db.delete(TABLE_NAME, "id = ?", new String[] {String.valueOf(id)}) > 0;
  }

  public List<TransactionTemplate> findAll() {
    List<TransactionTemplate> list = new ArrayList<>();
    Cursor cursor =
        db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "sort_order ASC, id ASC");
    if (cursor != null) {
      try {
        while (cursor.moveToNext()) {
          list.add(fromCursor(cursor));
        }
      } finally {
        cursor.close();
      }
    }
    return list;
  }

  @Nullable
  public TransactionTemplate findById(long id) {
    Cursor cursor =
        db.query(
            TABLE_NAME,
            null,
            "id = ?",
            new String[] {String.valueOf(id)},
            null,
            null,
            null);
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          return fromCursor(cursor);
        }
      } finally {
        cursor.close();
      }
    }
    return null;
  }

  @Nullable
  public TransactionTemplate findByNameIgnoreCase(@Nullable String name) {
    if (name == null || name.trim().isEmpty()) {
      return null;
    }
    Cursor cursor =
        db.query(
            TABLE_NAME,
            null,
            "name = ? COLLATE NOCASE",
            new String[] {name.trim()},
            null,
            null,
            null,
            "1");
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          return fromCursor(cursor);
        }
      } finally {
        cursor.close();
      }
    }
    return null;
  }

  private static ContentValues toContentValues(TransactionTemplate t) {
    ContentValues values = new ContentValues();
    values.put("name", t.getName());
    values.put("type", t.getType().name());
    values.put("amount_minor", t.getAmountMinor());
    values.put("category_id", t.getCategoryId());
    values.put("wallet_id", t.getWalletId());
    values.put("destination_wallet_id", t.getDestinationWalletId());
    values.put("note", t.getNote());
    values.put("icon", t.getIcon());
    values.put("sort_order", t.getSortOrder());
    return values;
  }

  private static TransactionTemplate fromCursor(Cursor c) {
    long id = c.getLong(c.getColumnIndexOrThrow("id"));
    String name = c.getString(c.getColumnIndexOrThrow("name"));
    String typeStr = c.getString(c.getColumnIndexOrThrow("type"));
    TransactionType type = TransactionType.EXPENSE;
    try {
      type = TransactionType.valueOf(typeStr);
    } catch (Exception ignored) {}

    long amountMinor = c.getLong(c.getColumnIndexOrThrow("amount_minor"));
    Long categoryId = c.isNull(c.getColumnIndexOrThrow("category_id")) ? null : c.getLong(c.getColumnIndexOrThrow("category_id"));
    Long walletId = c.isNull(c.getColumnIndexOrThrow("wallet_id")) ? null : c.getLong(c.getColumnIndexOrThrow("wallet_id"));
    Long destWalletId = c.isNull(c.getColumnIndexOrThrow("destination_wallet_id")) ? null : c.getLong(c.getColumnIndexOrThrow("destination_wallet_id"));
    String note = c.getString(c.getColumnIndexOrThrow("note"));
    String icon = c.getString(c.getColumnIndexOrThrow("icon"));
    int sortOrder = c.getInt(c.getColumnIndexOrThrow("sort_order"));

    return new TransactionTemplate(
        id, name, type, amountMinor, categoryId, walletId, destWalletId, note, icon != null ? icon : "⚡", sortOrder);
  }
}
