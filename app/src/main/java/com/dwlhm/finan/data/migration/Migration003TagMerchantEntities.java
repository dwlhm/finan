package com.dwlhm.finan.data.migration;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Migration003TagMerchantEntities implements Migration {

  @Override
  public int getVersion() {
    return 3;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE tags ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "name TEXT NOT NULL COLLATE NOCASE UNIQUE, "
            + "usage_count INTEGER NOT NULL DEFAULT 0, "
            + "last_used_at INTEGER"
            + ")");

    db.execSQL(
        "CREATE TABLE merchants ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "name TEXT NOT NULL COLLATE NOCASE UNIQUE, "
            + "usage_count INTEGER NOT NULL DEFAULT 0, "
            + "last_used_at INTEGER"
            + ")");

    db.execSQL(
        "CREATE TABLE transactions_new ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "amount_minor INTEGER NOT NULL, "
            + "type TEXT NOT NULL, "
            + "wallet_id INTEGER NOT NULL, "
            + "category_id INTEGER NOT NULL, "
            + "occurred_at INTEGER NOT NULL, "
            + "note TEXT, "
            + "merchant_id INTEGER, "
            + "created_at INTEGER NOT NULL, "
            + "updated_at INTEGER NOT NULL, "
            + "FOREIGN KEY (wallet_id) REFERENCES wallets(id), "
            + "FOREIGN KEY (category_id) REFERENCES categories(id), "
            + "FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE SET NULL"
            + ")");

    Map<String, Long> tagIdsByName = new HashMap<>();
    Map<String, Long> merchantIdsByName = new HashMap<>();
    List<TagLink> pendingTagLinks = new ArrayList<>();

    try (Cursor c =
        db.query(
            "transactions",
            new String[] {
              "id",
              "amount_minor",
              "type",
              "wallet_id",
              "category_id",
              "occurred_at",
              "note",
              "tag",
              "merchant",
              "created_at",
              "updated_at"
            },
            null,
            null,
            null,
            null,
            "id ASC")) {
      while (c.moveToNext()) {
        ContentValues values = new ContentValues();
        values.put("amount_minor", c.getLong(1));
        values.put("type", c.getString(2));
        values.put("wallet_id", c.getLong(3));
        values.put("category_id", c.getLong(4));
        values.put("occurred_at", c.getLong(5));
        if (c.isNull(6)) {
          values.putNull("note");
        } else {
          values.put("note", c.getString(6));
        }
        String tagText = c.isNull(7) ? null : c.getString(7);
        String merchantText = c.isNull(8) ? null : c.getString(8);
        values.put("created_at", c.getLong(9));
        values.put("updated_at", c.getLong(10));

        Long merchantId = resolveMerchantId(db, merchantIdsByName, merchantText);
        if (merchantId == null) {
          values.putNull("merchant_id");
        } else {
          values.put("merchant_id", merchantId);
        }

        long newId = db.insert("transactions_new", null, values);
        collectTagLinks(db, tagIdsByName, pendingTagLinks, newId, tagText);
      }
    }

    db.execSQL("DROP TABLE transactions");
    db.execSQL("ALTER TABLE transactions_new RENAME TO transactions");

    db.execSQL(
        "CREATE TABLE transaction_tags ("
            + "transaction_id INTEGER NOT NULL, "
            + "tag_id INTEGER NOT NULL, "
            + "PRIMARY KEY (transaction_id, tag_id), "
            + "FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE, "
            + "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE"
            + ")");

    for (TagLink link : pendingTagLinks) {
      ContentValues values = new ContentValues();
      values.put("transaction_id", link.transactionId);
      values.put("tag_id", link.tagId);
      db.insert("transaction_tags", null, values);
    }

    recreateTransactionIndexes(db);
  }

  private static void recreateTransactionIndexes(SQLiteDatabase db) {
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_transactions_occurred_at_id "
            + "ON transactions(occurred_at, id)");
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_transactions_wallet_occurred "
            + "ON transactions(wallet_id, occurred_at)");
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_transactions_type_occurred "
            + "ON transactions(type, occurred_at)");
  }

  private static Long resolveMerchantId(
      SQLiteDatabase db, Map<String, Long> cache, String merchantText) {
    if (merchantText == null || merchantText.trim().isEmpty()) {
      return null;
    }
    String key = merchantText.trim().toLowerCase(Locale.ROOT);
    Long cached = cache.get(key);
    if (cached != null) {
      return cached;
    }
    long id = insertNamedEntity(db, "merchants", merchantText.trim());
    cache.put(key, id);
    return id;
  }

  private static void collectTagLinks(
      SQLiteDatabase db,
      Map<String, Long> cache,
      List<TagLink> pending,
      long transactionId,
      String tagText) {
    if (tagText == null || tagText.trim().isEmpty()) {
      return;
    }
    for (String name : splitTagNames(tagText)) {
      String key = name.toLowerCase(Locale.ROOT);
      Long tagId = cache.get(key);
      if (tagId == null) {
        tagId = insertNamedEntity(db, "tags", name);
        cache.put(key, tagId);
      }
      pending.add(new TagLink(transactionId, tagId));
    }
  }

  private static List<String> splitTagNames(String tagText) {
    List<String> names = new ArrayList<>();
    for (String part : tagText.split(",")) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        names.add(trimmed);
      }
    }
    if (names.isEmpty()) {
      String trimmed = tagText.trim();
      if (!trimmed.isEmpty()) {
        names.add(trimmed);
      }
    }
    return names;
  }

  private static long insertNamedEntity(SQLiteDatabase db, String table, String name) {
    ContentValues values = new ContentValues();
    values.put("name", name);
    values.put("usage_count", 0);
    values.putNull("last_used_at");
    return db.insert(table, null, values);
  }

  private static final class TagLink {
    private final long transactionId;
    private final long tagId;

    private TagLink(long transactionId, long tagId) {
      this.transactionId = transactionId;
      this.tagId = tagId;
    }
  }
}
