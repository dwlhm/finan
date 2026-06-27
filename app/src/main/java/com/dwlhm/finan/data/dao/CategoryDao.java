package com.dwlhm.finan.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.CashFlowActivity;

import java.util.ArrayList;
import java.util.List;

public final class CategoryDao {

  private final SQLiteDatabase db;

  public CategoryDao(SQLiteDatabase db) {
    this.db = db;
  }

  public long insert(
      String name, String icon, String typeFilter, int sortOrder, int usageCount, Long lastUsedAt) {
    return insert(
        name,
        icon,
        typeFilter,
        sortOrder,
        usageCount,
        lastUsedAt,
        CashFlowActivity.UNCLASSIFIED.name());
  }

  public long insert(
      String name,
      String icon,
      String typeFilter,
      int sortOrder,
      int usageCount,
      Long lastUsedAt,
      String cashFlowActivity) {
    ContentValues values = new ContentValues();
    values.put("name", name);
    values.put("icon", icon);
    values.put("type_filter", typeFilter);
    values.put("sort_order", sortOrder);
    values.put("usage_count", usageCount);
    if (lastUsedAt == null) {
      values.putNull("last_used_at");
    } else {
      values.put("last_used_at", lastUsedAt);
    }
    values.put("cash_flow_activity", cashFlowActivity);
    return db.insert("categories", null, values);
  }

  public Category findById(long id) {
    try (Cursor c =
        db.query(
            "categories",
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

  public Category findByName(String name) {
    try (Cursor c =
        db.query(
            "categories",
            null,
            "name = ?",
            new String[]{name},
            null,
            null,
            null,
            "1")) {
      if (!c.moveToFirst()) {
        return null;
      }
      return map(c);
    }
  }

  public List<Category> findAllOrdered() {
    List<Category> categories = new ArrayList<>();
    try (Cursor c =
        db.query("categories", null, null, null, null, null, "sort_order ASC, id ASC")) {
      while (c.moveToNext()) {
        categories.add(map(c));
      }
    }
    return categories;
  }

  public List<Category> findAllForManage() {
    List<Category> categories = new ArrayList<>();
    try (Cursor c =
        db.query(
            "categories",
            null,
            null,
            null,
            null,
            null,
            "CASE cash_flow_activity WHEN 'UNCLASSIFIED' THEN 0 ELSE 1 END, "
                + "usage_count DESC, last_used_at DESC, name COLLATE NOCASE")) {
      while (c.moveToNext()) {
        categories.add(map(c));
      }
    }
    return categories;
  }

  public List<Category> findByTypeFilter(String transactionType) {
    return queryByTypeFilter(transactionType, "sort_order ASC, id ASC");
  }

  /** Most-used categories first — for capture quick picks. */
  public List<Category> findByTypeFilterOrderByUsage(String transactionType) {
    return queryByTypeFilter(
        transactionType, "usage_count DESC, sort_order ASC, id ASC");
  }

  private List<Category> queryByTypeFilter(String transactionType, String orderBy) {
    List<Category> categories = new ArrayList<>();
    String selection;
    String[] args;
    if ("INCOME".equals(transactionType)) {
      selection = "type_filter IN (?, ?)";
      args = new String[] {"INCOME", "BOTH"};
    } else if ("EXPENSE".equals(transactionType)) {
      selection = "type_filter IN (?, ?)";
      args = new String[] {"EXPENSE", "BOTH"};
    } else {
      selection = null;
      args = null;
    }
    try (Cursor c =
        db.query("categories", null, selection, args, null, null, orderBy)) {
      while (c.moveToNext()) {
        categories.add(map(c));
      }
    }
    return categories;
  }

  public Category findByNameIgnoreCase(String name) {
    if (name == null || name.trim().isEmpty()) {
      return null;
    }
    try (Cursor c =
        db.query(
            "categories",
            null,
            "name = ? COLLATE NOCASE",
            new String[] {name.trim()},
            null,
            null,
            null,
            "1")) {
      if (!c.moveToFirst()) {
        return null;
      }
      return map(c);
    }
  }

  public int nextSortOrder() {
    try (Cursor c =
        db.rawQuery("SELECT COALESCE(MAX(sort_order), -1) FROM categories", null)) {
      if (!c.moveToFirst()) {
        return 0;
      }
      return c.getInt(0) + 1;
    }
  }

  /**
   * Creates a category for the given transaction type, or returns existing if name matches
   * (case-insensitive).
   */
  public Category insertForTransactionType(String name, String transactionType) {
    String trimmed = name == null ? "" : name.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Category name is empty");
    }
    Category existing = findByNameIgnoreCase(trimmed);
    if (existing != null) {
      return existing;
    }
    String typeFilter = "INCOME".equals(transactionType) ? "INCOME" : "EXPENSE";
    long id = insert(trimmed, null, typeFilter, nextSortOrder(), 0, null);
    return findById(id);
  }

  public boolean update(long id, String name, String icon, String typeFilter, String cashFlowActivity) {
    ContentValues values = new ContentValues();
    values.put("name", name);
    values.put("icon", icon);
    values.put("type_filter", typeFilter);
    values.put("cash_flow_activity", cashFlowActivity);
    return db.update("categories", values, "id = ?", new String[] {String.valueOf(id)}) > 0;
  }

  public int countTransactions(long categoryId) {
    try (Cursor c =
        db.rawQuery(
            "SELECT COUNT(*) FROM transactions WHERE category_id = ?",
            new String[] {String.valueOf(categoryId)})) {
      return c.moveToFirst() ? c.getInt(0) : 0;
    }
  }

  public void incrementUsage(long id, long usedAt) {
    db.execSQL(
        "UPDATE categories SET usage_count = usage_count + 1, last_used_at = ? WHERE id = ?",
        new Object[]{usedAt, id});
  }

  private static Category map(Cursor c) {
    int lastUsedIndex = c.getColumnIndex("last_used_at");
    Long lastUsedAt = null;
    if (lastUsedIndex >= 0 && !c.isNull(lastUsedIndex)) {
      lastUsedAt = c.getLong(lastUsedIndex);
    }
    int iconIndex = c.getColumnIndex("icon");
    String icon = iconIndex >= 0 ? c.getString(iconIndex) : null;
    return new Category(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("name")),
        icon,
        c.getString(c.getColumnIndexOrThrow("type_filter")),
        c.getInt(c.getColumnIndexOrThrow("sort_order")),
        c.getInt(c.getColumnIndexOrThrow("usage_count")),
        lastUsedAt,
        c.getString(c.getColumnIndexOrThrow("cash_flow_activity")));
  }
}
