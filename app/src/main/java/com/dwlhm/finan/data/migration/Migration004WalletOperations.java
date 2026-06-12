package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration004WalletOperations implements Migration {

  @Override
  public int getVersion() {
    return 4;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    db.execSQL(
        "ALTER TABLE wallets ADD COLUMN opening_balance_minor INTEGER NOT NULL DEFAULT 0");
    db.execSQL(
        "UPDATE wallets SET opening_balance_minor = cached_balance_minor - "
            + "COALESCE((SELECT SUM(CASE type "
            + "WHEN 'INCOME' THEN amount_minor "
            + "WHEN 'EXPENSE' THEN -amount_minor "
            + "ELSE 0 END) FROM transactions WHERE wallet_id = wallets.id), 0)");

    db.execSQL(
        "CREATE TABLE transfers ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "source_wallet_id INTEGER NOT NULL, "
            + "destination_wallet_id INTEGER NOT NULL, "
            + "amount_minor INTEGER NOT NULL, "
            + "occurred_at INTEGER NOT NULL, "
            + "note TEXT, "
            + "created_at INTEGER NOT NULL, "
            + "updated_at INTEGER NOT NULL, "
            + "FOREIGN KEY (source_wallet_id) REFERENCES wallets(id), "
            + "FOREIGN KEY (destination_wallet_id) REFERENCES wallets(id), "
            + "CHECK (source_wallet_id <> destination_wallet_id), "
            + "CHECK (amount_minor > 0)"
            + ")");

    db.execSQL(
        "CREATE TEMP TABLE transaction_tags_backup AS "
            + "SELECT transaction_id, tag_id FROM transaction_tags");
    db.execSQL("DROP TABLE transaction_tags");

    db.execSQL(
        "CREATE TABLE transactions_new ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "amount_minor INTEGER NOT NULL, "
            + "type TEXT NOT NULL, "
            + "wallet_id INTEGER NOT NULL, "
            + "category_id INTEGER, "
            + "occurred_at INTEGER NOT NULL, "
            + "note TEXT, "
            + "merchant_id INTEGER, "
            + "transfer_id INTEGER, "
            + "created_at INTEGER NOT NULL, "
            + "updated_at INTEGER NOT NULL, "
            + "FOREIGN KEY (wallet_id) REFERENCES wallets(id), "
            + "FOREIGN KEY (category_id) REFERENCES categories(id), "
            + "FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE SET NULL, "
            + "FOREIGN KEY (transfer_id) REFERENCES transfers(id) ON DELETE CASCADE"
            + ")");
    db.execSQL(
        "INSERT INTO transactions_new "
            + "(id, amount_minor, type, wallet_id, category_id, occurred_at, note, merchant_id, "
            + "transfer_id, created_at, updated_at) "
            + "SELECT id, amount_minor, type, wallet_id, category_id, occurred_at, note, "
            + "merchant_id, NULL, created_at, updated_at FROM transactions");
    db.execSQL("DROP TABLE transactions");
    db.execSQL("ALTER TABLE transactions_new RENAME TO transactions");

    db.execSQL(
        "CREATE TABLE transaction_tags ("
            + "transaction_id INTEGER NOT NULL, "
            + "tag_id INTEGER NOT NULL, "
            + "PRIMARY KEY (transaction_id, tag_id), "
            + "FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE, "
            + "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE"
            + ")");
    db.execSQL(
        "INSERT INTO transaction_tags (transaction_id, tag_id) "
            + "SELECT transaction_id, tag_id FROM transaction_tags_backup");
    db.execSQL("DROP TABLE transaction_tags_backup");

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
