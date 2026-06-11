package com.dwlhm.finan.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dwlhm.finan.data.migration.Migration;
import com.dwlhm.finan.data.migration.Migration001Initial;
import com.dwlhm.finan.data.migration.Migration002TransactionIndexes;
import com.dwlhm.finan.data.migration.Migration003TagMerchantEntities;
import com.dwlhm.finan.data.migration.MigrationRunner;

public final class FinanDatabaseHelper extends SQLiteOpenHelper {

  public static final String DATABASE_NAME = "finan.db";
  public static final int DATABASE_VERSION = 3;

  private static final Migration[] MIGRATIONS = {
    new Migration001Initial(),
    new Migration002TransactionIndexes(),
    new Migration003TagMerchantEntities()
  };

  public FinanDatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    MigrationRunner.migrate(db, 0, DATABASE_VERSION, MIGRATIONS);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    MigrationRunner.migrate(db, oldVersion, newVersion, MIGRATIONS);
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    super.onConfigure(db);
    db.setForeignKeyConstraintsEnabled(true);
  }
}
