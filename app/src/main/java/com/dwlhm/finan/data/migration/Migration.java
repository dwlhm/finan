package com.dwlhm.finan.data.migration;

import android.database.sqlite.SQLiteDatabase;

public interface Migration {

    int getVersion();

    void migrate(SQLiteDatabase db);
}
