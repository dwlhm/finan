package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;
import com.dwlhm.finan.data.dao.TransactionTemplateDao;

public final class Migration011TransactionTemplate implements Migration {

  @Override
  public int getVersion() {
    return 11;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    TransactionTemplateDao.createTable(db);
    TransactionTemplateDao.seedDefaults(db);
  }
}
