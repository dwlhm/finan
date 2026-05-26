package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

import java.util.Arrays;
import java.util.Comparator;

public final class MigrationRunner {

  private MigrationRunner() {}

  public static void migrate(SQLiteDatabase db, int fromVersion, int toVersion, Migration[] migrations) {
    if (fromVersion >= toVersion) {
      return;
    }

    Migration[] sorted = Arrays.copyOf(migrations, migrations.length);
    Arrays.sort(sorted, Comparator.comparingInt(Migration::getVersion));

    for (Migration migration : sorted) {
      int version = migration.getVersion();
      if (version > fromVersion && version <= toVersion) {
        migration.migrate(db);
      }
    }
  }
}
