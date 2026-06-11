package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration002TransactionIndexes implements Migration {

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
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
}
