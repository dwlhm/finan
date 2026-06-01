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

  public List<Transaction> findHistory(
      Long walletId,
      Long categoryId,
      String type,
      Long startInclusiveMillis,
      Long endExclusiveMillis,
      boolean oldestFirst) {
    List<Transaction> transactions = new ArrayList<>();
    List<String> args = new ArrayList<>();
    String selection =
        historySelection(walletId, categoryId, type, startInclusiveMillis, endExclusiveMillis, args);
    try (Cursor c =
        db.query(
            "transactions",
            null,
            selection,
            args.isEmpty() ? null : args.toArray(new String[0]),
            null,
            null,
            oldestFirst ? "occurred_at ASC, id ASC" : "occurred_at DESC, id DESC")) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  public List<Transaction> findHistoryPage(
      Long walletId,
      Long categoryId,
      String type,
      Long startInclusiveMillis,
      Long endExclusiveMillis,
      boolean oldestFirst,
      Long cursorOccurredAt,
      Long cursorId,
      int limit) {
    List<Transaction> transactions = new ArrayList<>();
    List<String> args = new ArrayList<>();
    String selection =
        historyPageSelection(
            walletId,
            categoryId,
            type,
            startInclusiveMillis,
            endExclusiveMillis,
            oldestFirst,
            cursorOccurredAt,
            cursorId,
            args);
    try (Cursor c =
        db.query(
            "transactions",
            null,
            selection,
            args.isEmpty() ? null : args.toArray(new String[0]),
            null,
            null,
            oldestFirst ? "occurred_at ASC, id ASC" : "occurred_at DESC, id DESC",
            String.valueOf(limit))) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  public HistoryTotalsRow findHistoryTotals(
      Long walletId,
      Long categoryId,
      String type,
      Long startInclusiveMillis,
      Long endExclusiveMillis) {
    List<String> args = new ArrayList<>();
    String selection =
        historySelection(walletId, categoryId, type, startInclusiveMillis, endExclusiveMillis, args);
    String[] selectionArgs = args.isEmpty() ? null : args.toArray(new String[0]);
    int count = 0;
    long incomeMinor = 0L;
    long expenseMinor = 0L;
    try (Cursor c =
        db.rawQuery(
            "SELECT COUNT(*) AS tx_count,"
                + " COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount_minor ELSE 0 END), 0)"
                + " AS income_minor,"
                + " COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount_minor ELSE 0 END), 0)"
                + " AS expense_minor"
                + " FROM transactions"
                + (selection == null ? "" : " WHERE " + selection),
            selectionArgs)) {
      if (c.moveToFirst()) {
        count = c.getInt(c.getColumnIndexOrThrow("tx_count"));
        incomeMinor = c.getLong(c.getColumnIndexOrThrow("income_minor"));
        expenseMinor = c.getLong(c.getColumnIndexOrThrow("expense_minor"));
      }
    }
    return new HistoryTotalsRow(count, incomeMinor, expenseMinor);
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

  private static String historySelection(
      Long walletId,
      Long categoryId,
      String type,
      Long startInclusiveMillis,
      Long endExclusiveMillis,
      List<String> args) {
    StringBuilder selection = new StringBuilder();
    appendHistoryFilter(selection, args, "wallet_id = ?", walletId);
    appendHistoryFilter(selection, args, "category_id = ?", categoryId);
    appendHistoryFilter(selection, args, "type = ?", type);
    appendHistoryFilter(selection, args, "occurred_at >= ?", startInclusiveMillis);
    appendHistoryFilter(selection, args, "occurred_at < ?", endExclusiveMillis);
    return selection.length() == 0 ? null : selection.toString();
  }

  private static void appendHistoryFilter(
      StringBuilder selection, List<String> args, String clause, Object value) {
    if (value == null) {
      return;
    }
    if (selection.length() > 0) {
      selection.append(" AND ");
    }
    selection.append(clause);
    args.add(String.valueOf(value));
  }

  private static String historyPageSelection(
      Long walletId,
      Long categoryId,
      String type,
      Long startInclusiveMillis,
      Long endExclusiveMillis,
      boolean oldestFirst,
      Long cursorOccurredAt,
      Long cursorId,
      List<String> args) {
    StringBuilder selection = new StringBuilder();
    String base =
        historySelection(walletId, categoryId, type, startInclusiveMillis, endExclusiveMillis, args);
    if (base != null) {
      selection.append(base);
    }
    if (cursorOccurredAt != null && cursorId != null) {
      if (selection.length() > 0) {
        selection.append(" AND ");
      }
      if (oldestFirst) {
        selection.append("(occurred_at > ? OR (occurred_at = ? AND id > ?))");
      } else {
        selection.append("(occurred_at < ? OR (occurred_at = ? AND id < ?))");
      }
      args.add(String.valueOf(cursorOccurredAt));
      args.add(String.valueOf(cursorOccurredAt));
      args.add(String.valueOf(cursorId));
    }
    return selection.length() == 0 ? null : selection.toString();
  }

  public static final class HistoryTotalsRow {
    public final int count;
    public final long incomeMinor;
    public final long expenseMinor;

    public HistoryTotalsRow(int count, long incomeMinor, long expenseMinor) {
      this.count = count;
      this.incomeMinor = incomeMinor;
      this.expenseMinor = expenseMinor;
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
