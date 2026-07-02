package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration007WalletIcon implements Migration {

  @Override
  public int getVersion() {
    return 7;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    db.execSQL("ALTER TABLE wallets ADD COLUMN icon TEXT");
  }
}
