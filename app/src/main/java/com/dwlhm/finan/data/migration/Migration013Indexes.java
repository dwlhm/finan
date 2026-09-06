package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration013Indexes implements Migration {

  public static final String IDX_TRANSACTIONS_CAT_OCCURRED =
      "idx_transactions_cat_occurred";
  public static final String IDX_TRANSACTIONS_WALLET_CAT_OCCURRED =
      "idx_transactions_wallet_cat_occurred";

  public static final String SQL_IDX_TRANSACTIONS_CAT_OCCURRED =
      "CREATE INDEX IF NOT EXISTS idx_transactions_cat_occurred "
          + "ON transactions(category_id, occurred_at)";
  public static final String SQL_IDX_TRANSACTIONS_WALLET_CAT_OCCURRED =
      "CREATE INDEX IF NOT EXISTS idx_transactions_wallet_cat_occurred "
          + "ON transactions(wallet_id, category_id, occurred_at)";

  @Override
  public int getVersion() {
    return 13;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    db.execSQL(SQL_IDX_TRANSACTIONS_CAT_OCCURRED);
    db.execSQL(SQL_IDX_TRANSACTIONS_WALLET_CAT_OCCURRED);
  }
}
