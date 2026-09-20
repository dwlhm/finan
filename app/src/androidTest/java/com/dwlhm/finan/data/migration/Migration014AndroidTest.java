package com.dwlhm.finan.data.migration;

import static org.junit.Assert.*;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.dwlhm.finan.data.dao.TransactionTemplateDao;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class Migration014AndroidTest {
  @Test public void occurrences_areUniqueAtomicAndCascade() {
    try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
      db.setForeignKeyConstraintsEnabled(true);
      db.execSQL("CREATE TABLE transaction_templates(id INTEGER PRIMARY KEY, last_recorded_at INTEGER NOT NULL DEFAULT 123)");
      db.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY)");
      db.execSQL("INSERT INTO transaction_templates(id) VALUES (1)");
      new Migration014ScheduledOccurrences().migrate(db);
      try (Cursor c = db.rawQuery("SELECT due_month FROM transaction_templates", null)) {
        assertTrue(c.moveToFirst());
        assertEquals(0, c.getInt(0));
      }
      TransactionTemplateDao dao = new TransactionTemplateDao(db);
      db.beginTransaction();
      try {
        db.execSQL("INSERT INTO transactions VALUES (10)");
        assertTrue(dao.markOccurrence(1, "2026-08-01", "RECORDED", 10L));
        // Deliberately fail the encompassing action: both writes roll back.
      } finally { db.endTransaction(); }
      assertFalse(dao.isOccurrenceHandled(1, "2026-08-01"));
      try (Cursor c = db.rawQuery("SELECT count(*) FROM transactions", null)) {
        c.moveToFirst(); assertEquals(0, c.getInt(0));
      }
      db.execSQL("INSERT INTO transactions VALUES (10)");
      assertTrue(dao.markOccurrence(1, "2026-08-01", "RECORDED", 10L));
      assertFalse(dao.markOccurrence(1, "2026-08-01", "SKIPPED", null));
      assertTrue(dao.hasOccurrenceHistory(1));
      db.delete("transactions", "id=10", null);
      assertFalse(dao.isOccurrenceHandled(1, "2026-08-01"));
      assertTrue(dao.markOccurrence(1, "2026-08-02", "SKIPPED", null));
      db.delete("transaction_templates", "id=1", null);
      assertFalse(dao.hasOccurrenceHistory(1));
    }
  }
}
