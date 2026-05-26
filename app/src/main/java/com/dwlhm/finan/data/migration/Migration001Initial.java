package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration001Initial implements Migration {

  @Override
  public int getVersion() {
    return 1;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE wallets ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "name TEXT NOT NULL, "
            + "currency_code TEXT NOT NULL DEFAULT 'IDR', "
            + "is_default INTEGER NOT NULL, "
            + "cached_balance_minor INTEGER NOT NULL DEFAULT 0, "
            + "created_at INTEGER NOT NULL"
            + ")");

    db.execSQL(
        "CREATE TABLE categories ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "name TEXT NOT NULL, "
            + "type_filter TEXT NOT NULL, "
            + "sort_order INTEGER NOT NULL, "
            + "usage_count INTEGER NOT NULL DEFAULT 0, "
            + "last_used_at INTEGER"
            + ")");

    db.execSQL(
        "CREATE TABLE transactions ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "amount_minor INTEGER NOT NULL, "
            + "type TEXT NOT NULL, "
            + "wallet_id INTEGER NOT NULL, "
            + "category_id INTEGER NOT NULL, "
            + "occurred_at INTEGER NOT NULL, "
            + "note TEXT, "
            + "tag TEXT, "
            + "merchant TEXT, "
            + "created_at INTEGER NOT NULL, "
            + "updated_at INTEGER NOT NULL, "
            + "FOREIGN KEY (wallet_id) REFERENCES wallets(id), "
            + "FOREIGN KEY (category_id) REFERENCES categories(id)"
            + ")");

    long now = System.currentTimeMillis();
    db.execSQL(
        "INSERT INTO wallets (name, currency_code, is_default, cached_balance_minor, created_at) "
            + "VALUES ('Dompet Utama', 'IDR', 1, 0, "
            + now
            + ")");

    seedCategory(db, "Makanan", "EXPENSE", 0);
    seedCategory(db, "Transport", "EXPENSE", 1);
    seedCategory(db, "Kopi", "EXPENSE", 2);
    seedCategory(db, "Tagihan", "EXPENSE", 3);
    seedCategory(db, "Belanja", "EXPENSE", 4);
    seedCategory(db, "Gaji", "INCOME", 5);
    seedCategory(db, "Lainnya", "BOTH", 6);
  }

  private static void seedCategory(SQLiteDatabase db, String name, String typeFilter, int sortOrder) {
    db.execSQL(
        "INSERT INTO categories (name, type_filter, sort_order, usage_count, last_used_at) "
            + "VALUES ('"
            + escape(name)
            + "', '"
            + typeFilter
            + "', "
            + sortOrder
            + ", 0, NULL)");
  }

  private static String escape(String value) {
    return value.replace("'", "''");
  }
}
