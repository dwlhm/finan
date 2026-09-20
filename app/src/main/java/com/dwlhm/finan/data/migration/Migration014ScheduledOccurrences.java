package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration014ScheduledOccurrences implements Migration {
  @Override public int getVersion() { return 14; }

  @Override public void migrate(SQLiteDatabase db) {
    db.execSQL("ALTER TABLE transaction_templates ADD COLUMN due_month INTEGER NOT NULL DEFAULT 0");
    db.execSQL("CREATE TABLE scheduled_occurrences ("
        + "template_id INTEGER NOT NULL REFERENCES transaction_templates(id) ON DELETE CASCADE, "
        + "due_date TEXT NOT NULL, "
        + "status TEXT NOT NULL CHECK(status IN ('RECORDED','SKIPPED')), "
        + "transaction_id INTEGER REFERENCES transactions(id) ON DELETE CASCADE, "
        + "PRIMARY KEY(template_id, due_date))");
  }
}
