package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration009CategoryDefault implements Migration {

  @Override
  public int getVersion() {
    return 9;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    db.execSQL("ALTER TABLE categories ADD COLUMN is_default INTEGER NOT NULL DEFAULT 0");
    db.execSQL("UPDATE categories SET is_default = 1 WHERE name = 'Makanan'");
  }
}
