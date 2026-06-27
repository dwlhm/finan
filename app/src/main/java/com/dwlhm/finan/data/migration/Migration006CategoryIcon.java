package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration006CategoryIcon implements Migration {

  @Override
  public int getVersion() {
    return 6;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    db.execSQL("ALTER TABLE categories ADD COLUMN icon TEXT");
  }
}
