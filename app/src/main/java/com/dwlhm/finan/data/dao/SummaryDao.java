package com.dwlhm.finan.data.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public final class SummaryDao {

  public static final class CategorySumRow {
    public final long categoryId;
    public final long totalMinor;

    public CategorySumRow(long categoryId, long totalMinor) {
      this.categoryId = categoryId;
      this.totalMinor = totalMinor;
    }
  }

  private final SQLiteDatabase db;

  public SummaryDao(SQLiteDatabase db) {
    this.db = db;
  }

  public long sumByTypeBetween(String type, long startInclusive, long endExclusive) {
    return sumByTypeBetween(type, startInclusive, endExclusive, null, null);
  }

  public long sumByTypeBetween(
      String type, long startInclusive, long endExclusive, Long walletId, Long categoryId) {
    List<String> args = new ArrayList<>();
    args.add(type);
    args.add(String.valueOf(startInclusive));
    args.add(String.valueOf(endExclusive));
    String sql =
        "SELECT COALESCE(SUM(amount_minor), 0) FROM transactions "
            + "WHERE type = ? AND occurred_at >= ? AND occurred_at < ?";
    sql = appendFilterWhere(sql, args, walletId, categoryId);
    try (Cursor c =
        db.rawQuery(
            sql,
            args.toArray(new String[0]))) {
      if (!c.moveToFirst()) {
        return 0L;
      }
      return c.getLong(0);
    }
  }

  public long walletBalanceBefore(long walletId, long endExclusive) {
    try (Cursor c =
        db.rawQuery(
            "SELECT wallets.opening_balance_minor + COALESCE(SUM("
                + "CASE transactions.type "
                + "WHEN 'INCOME' THEN amount_minor "
                + "WHEN 'ADJUSTMENT_INCREASE' THEN amount_minor "
                + "WHEN 'TRANSFER_IN' THEN amount_minor "
                + "WHEN 'EXPENSE' THEN -amount_minor "
                + "WHEN 'ADJUSTMENT_DECREASE' THEN -amount_minor "
                + "WHEN 'TRANSFER_OUT' THEN -amount_minor "
                + "ELSE 0 END), 0) FROM wallets "
                + "LEFT JOIN transactions ON transactions.wallet_id = wallets.id "
                + "AND transactions.occurred_at < ? "
                + "WHERE wallets.id = ? GROUP BY wallets.id",
            new String[] {String.valueOf(endExclusive), String.valueOf(walletId)})) {
      if (!c.moveToFirst()) {
        return 0L;
      }
      return c.getLong(0);
    }
  }

  public List<CategorySumRow> expenseByCategory(long startInclusive, long endExclusive) {
    return expenseByCategory(startInclusive, endExclusive, null, null);
  }

  public List<CategorySumRow> expenseByCategory(
      long startInclusive, long endExclusive, Long walletId, Long categoryId) {
    List<CategorySumRow> rows = new ArrayList<>();
    List<String> args = new ArrayList<>();
    args.add(String.valueOf(startInclusive));
    args.add(String.valueOf(endExclusive));
    String sql =
        "SELECT category_id, SUM(amount_minor) AS total_minor FROM transactions "
            + "WHERE type = 'EXPENSE' AND occurred_at >= ? AND occurred_at < ? "
            + "AND category_id IS NOT NULL";
    sql = appendFilterWhere(sql, args, walletId, categoryId);
    sql += " GROUP BY category_id ORDER BY total_minor DESC";
    try (Cursor c =
        db.rawQuery(
            sql,
            args.toArray(new String[0]))) {
      while (c.moveToNext()) {
        rows.add(
            new CategorySumRow(
                c.getLong(c.getColumnIndexOrThrow("category_id")),
                c.getLong(c.getColumnIndexOrThrow("total_minor"))));
      }
    }
    return rows;
  }

  private static String appendFilterWhere(
      String sql, List<String> args, Long walletId, Long categoryId) {
    String filteredSql = sql;
    if (walletId != null) {
      filteredSql += " AND wallet_id = ?";
      args.add(String.valueOf(walletId));
    }
    if (categoryId != null) {
      filteredSql += " AND category_id = ?";
      args.add(String.valueOf(categoryId));
    }
    return filteredSql;
  }
}
