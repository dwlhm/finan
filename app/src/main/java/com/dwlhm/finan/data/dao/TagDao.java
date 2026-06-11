package com.dwlhm.finan.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.entity.Tag;

import java.util.ArrayList;
import java.util.List;

public final class TagDao {

  private final SQLiteDatabase db;

  public TagDao(SQLiteDatabase db) {
    this.db = db;
  }

  public long insert(String name, int usageCount, Long lastUsedAt) {
    ContentValues values = new ContentValues();
    values.put("name", name);
    values.put("usage_count", usageCount);
    if (lastUsedAt == null) {
      values.putNull("last_used_at");
    } else {
      values.put("last_used_at", lastUsedAt);
    }
    return db.insert("tags", null, values);
  }

  public Tag findById(long id) {
    try (Cursor c =
        db.query("tags", null, "id = ?", new String[] {String.valueOf(id)}, null, null, null)) {
      if (!c.moveToFirst()) {
        return null;
      }
      return map(c);
    }
  }

  public Tag findByNameIgnoreCase(String name) {
    if (name == null || name.trim().isEmpty()) {
      return null;
    }
    try (Cursor c =
        db.query(
            "tags",
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

  public List<Tag> findAllOrderByUsage() {
    List<Tag> tags = new ArrayList<>();
    try (Cursor c =
        db.query("tags", null, null, null, null, null, "usage_count DESC, name ASC, id ASC")) {
      while (c.moveToNext()) {
        tags.add(map(c));
      }
    }
    return tags;
  }

  public Tag insertIfAbsent(String name) {
    String trimmed = name == null ? "" : name.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Tag name is empty");
    }
    Tag existing = findByNameIgnoreCase(trimmed);
    if (existing != null) {
      return existing;
    }
    long id = insert(trimmed, 0, null);
    return findById(id);
  }

  public void incrementUsage(long id, long usedAt) {
    db.execSQL(
        "UPDATE tags SET usage_count = usage_count + 1, last_used_at = ? WHERE id = ?",
        new Object[] {usedAt, id});
  }

  private static Tag map(Cursor c) {
    int lastUsedIndex = c.getColumnIndex("last_used_at");
    Long lastUsedAt = null;
    if (lastUsedIndex >= 0 && !c.isNull(lastUsedIndex)) {
      lastUsedAt = c.getLong(lastUsedIndex);
    }
    return new Tag(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("name")),
        c.getInt(c.getColumnIndexOrThrow("usage_count")),
        lastUsedAt);
  }
}
