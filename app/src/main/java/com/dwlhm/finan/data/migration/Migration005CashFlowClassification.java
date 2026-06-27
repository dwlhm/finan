package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public final class Migration005CashFlowClassification implements Migration {

  @Override
  public int getVersion() {
    return 5;
  }

  @Override
  public void migrate(SQLiteDatabase db) {
    db.execSQL(
        "ALTER TABLE categories ADD COLUMN cash_flow_activity TEXT NOT NULL "
            + "DEFAULT 'UNCLASSIFIED' CHECK (cash_flow_activity IN "
            + "('OPERATING', 'INVESTING', 'FINANCING', 'UNCLASSIFIED'))");
    db.execSQL(
        "ALTER TABLE transactions ADD COLUMN cash_flow_activity TEXT NOT NULL "
            + "DEFAULT 'UNCLASSIFIED' CHECK (cash_flow_activity IN "
            + "('OPERATING', 'INVESTING', 'FINANCING', 'UNCLASSIFIED'))");
    db.execSQL(
        "ALTER TABLE transactions ADD COLUMN cash_flow_activity_overridden INTEGER NOT NULL "
            + "DEFAULT 0 CHECK (cash_flow_activity_overridden IN (0, 1))");
    db.execSQL(
        "UPDATE categories SET cash_flow_activity = 'OPERATING' "
            + "WHERE name IN ('Makanan', 'Transport', 'Kopi', 'Tagihan', 'Belanja', 'Gaji')");
    db.execSQL(
        "UPDATE transactions SET cash_flow_activity = COALESCE("
            + "(SELECT cash_flow_activity FROM categories WHERE id = category_id), "
            + "'UNCLASSIFIED')");
  }
}
