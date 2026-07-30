package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

/**
 * No-op migration to version 10.
 *
 * The device database was already bumped to version 10 by a prior build.
 * This stub migration keeps the declared DATABASE_VERSION in sync so that
 * SQLiteOpenHelper does not throw a "Can't downgrade" exception.
 */
public final class Migration010NoOp implements Migration {

  @Override
  public int getVersion() {
    return 10;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    // No schema changes — this entry exists solely to align DATABASE_VERSION
    // with the version already stored on devices that received a prior build.
  }
}
