package com.dwlhm.finan.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TransactionTagDao {

  private final SQLiteDatabase db;

  public TransactionTagDao(SQLiteDatabase db) {
    this.db = db;
  }

  public void replaceAll(long transactionId, List<Long> tagIds) {
    db.delete(
        "transaction_tags", "transaction_id = ?", new String[] {String.valueOf(transactionId)});
    if (tagIds == null || tagIds.isEmpty()) {
      return;
    }
    Set<Long> unique = new LinkedHashSet<>();
    for (Long tagId : tagIds) {
      if (tagId != null && tagId > 0L) {
        unique.add(tagId);
      }
    }
    for (Long tagId : unique) {
      ContentValues values = new ContentValues();
      values.put("transaction_id", transactionId);
      values.put("tag_id", tagId);
      db.insert("transaction_tags", null, values);
    }
  }

  public List<Long> findTagIdsByTransaction(long transactionId) {
    List<Long> tagIds = new ArrayList<>();
    try (Cursor c =
        db.query(
            "transaction_tags",
            new String[] {"tag_id"},
            "transaction_id = ?",
            new String[] {String.valueOf(transactionId)},
            null,
            null,
            "tag_id ASC")) {
      while (c.moveToNext()) {
        tagIds.add(c.getLong(0));
      }
    }
    return tagIds;
  }

  public Map<Long, List<Long>> findTagIdsByTransactions(List<Long> transactionIds) {
    Map<Long, List<Long>> result = new HashMap<>();
    if (transactionIds == null || transactionIds.isEmpty()) {
      return result;
    }
    StringBuilder placeholders = new StringBuilder();
    String[] args = new String[transactionIds.size()];
    for (int i = 0; i < transactionIds.size(); i++) {
      if (i > 0) {
        placeholders.append(',');
      }
      placeholders.append('?');
      args[i] = String.valueOf(transactionIds.get(i));
    }
    try (Cursor c =
        db.query(
            "transaction_tags",
            new String[] {"transaction_id", "tag_id"},
            "transaction_id IN (" + placeholders + ")",
            args,
            null,
            null,
            "transaction_id ASC, tag_id ASC")) {
      while (c.moveToNext()) {
        long transactionId = c.getLong(0);
        long tagId = c.getLong(1);
        List<Long> tagIds = result.get(transactionId);
        if (tagIds == null) {
          tagIds = new ArrayList<>();
          result.put(transactionId, tagIds);
        }
        tagIds.add(tagId);
      }
    }
    return result;
  }
}
