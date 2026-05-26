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
    try (Cursor c =
        db.rawQuery(
            "SELECT COALESCE(SUM(amount_minor), 0) FROM transactions "
                + "WHERE type = ? AND occurred_at >= ? AND occurred_at < ?",
            new String[] {type, String.valueOf(startInclusive), String.valueOf(endExclusive)})) {
      if (!c.moveToFirst()) {
        return 0L;
      }
      return c.getLong(0);
    }
  }

  public List<CategorySumRow> topExpenseByCategory(long startInclusive, long endExclusive, int limit) {
    List<CategorySumRow> rows = new ArrayList<>();
    try (Cursor c =
        db.rawQuery(
            "SELECT category_id, SUM(amount_minor) AS total_minor FROM transactions "
                + "WHERE type = 'EXPENSE' AND occurred_at >= ? AND occurred_at < ? "
                + "GROUP BY category_id ORDER BY total_minor DESC LIMIT ?",
            new String[] {
              String.valueOf(startInclusive),
              String.valueOf(endExclusive),
              String.valueOf(limit)
            })) {
      while (c.moveToNext()) {
        rows.add(
            new CategorySumRow(
                c.getLong(c.getColumnIndexOrThrow("category_id")),
                c.getLong(c.getColumnIndexOrThrow("total_minor"))));
      }
    }
    return rows;
  }
}
