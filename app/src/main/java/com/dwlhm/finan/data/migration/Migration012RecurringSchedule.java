package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration012RecurringSchedule implements Migration {

  @Override
  public int getVersion() {
    return 12;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    db.execSQL("ALTER TABLE transaction_templates ADD COLUMN frequency TEXT NOT NULL DEFAULT 'NONE'");
    db.execSQL("ALTER TABLE transaction_templates ADD COLUMN due_day INTEGER NOT NULL DEFAULT 0");
    db.execSQL("ALTER TABLE transaction_templates ADD COLUMN is_scheduled INTEGER NOT NULL DEFAULT 0");
    db.execSQL("ALTER TABLE transaction_templates ADD COLUMN last_recorded_at INTEGER NOT NULL DEFAULT 0");
  }
}
