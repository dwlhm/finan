package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration008RemoveTagsMerchants implements Migration {

  @Override
  public int getVersion() {
    return 8;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE transactions_new ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "amount_minor INTEGER NOT NULL, "
            + "type TEXT NOT NULL, "
            + "wallet_id INTEGER NOT NULL, "
            + "category_id INTEGER, "
            + "occurred_at INTEGER NOT NULL, "
            + "note TEXT, "
            + "transfer_id INTEGER, "
            + "cash_flow_activity TEXT NOT NULL DEFAULT 'UNCLASSIFIED', "
            + "cash_flow_activity_overridden INTEGER NOT NULL DEFAULT 0, "
            + "created_at INTEGER NOT NULL, "
            + "updated_at INTEGER NOT NULL, "
            + "FOREIGN KEY (wallet_id) REFERENCES wallets(id), "
            + "FOREIGN KEY (category_id) REFERENCES categories(id), "
            + "FOREIGN KEY (transfer_id) REFERENCES transfers(id) ON DELETE CASCADE"
            + ")");
    db.execSQL(
        "INSERT INTO transactions_new "
            + "(id, amount_minor, type, wallet_id, category_id, occurred_at, note, "
            + "transfer_id, cash_flow_activity, cash_flow_activity_overridden, "
            + "created_at, updated_at) "
            + "SELECT id, amount_minor, type, wallet_id, category_id, occurred_at, note, "
            + "transfer_id, cash_flow_activity, cash_flow_activity_overridden, "
            + "created_at, updated_at FROM transactions");
    db.execSQL("DROP TABLE transactions");
    db.execSQL("ALTER TABLE transactions_new RENAME TO transactions");

    db.execSQL("DROP TABLE IF EXISTS transaction_tags");
    db.execSQL("DROP TABLE IF EXISTS tags");
    db.execSQL("DROP TABLE IF EXISTS merchants");

    db.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_transactions_occurred_at_id "
            + "ON transactions(occurred_at, id)");
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_transactions_wallet_occurred "
            + "ON transactions(wallet_id, occurred_at)");
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_transactions_type_occurred "
            + "ON transactions(type, occurred_at)");
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_transactions_transfer "
            + "ON transactions(transfer_id)");
  }
}
