package com.dwlhm.finan.service.privacy;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.appwidget.AppWidgetManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WidgetPrivacyAndroidTest {
  @Test public void privateStaleActionsCannotMutateAndCollectionIsEmpty() {
    Context context = ApplicationProvider.getApplicationContext();
    android.content.SharedPreferences prefs = context.getSharedPreferences(AppLock.PREFS, 0);
    boolean old = prefs.getBoolean("private_widgets", false);
    AppServices services = com.dwlhm.finan.ui.common.ServicesProvider.get(context);
    long before = android.database.DatabaseUtils.queryNumEntries(
        services.databaseHelper.getReadableDatabase(), "transactions");
    final int widgetId = 917;
    String oldAmount = WidgetStateStore.getAmountStr(context, widgetId);
    try {
      prefs.edit().putBoolean("private_widgets", true).commit();
      WidgetStateStore.setAmountStr(context, widgetId, "38123");
      for (String action : new String[] {QuickTransactionWidgetProvider.ACTION_SAVE_TRANSACTION,
          QuickTransactionWidgetProvider.ACTION_UNDO_TRANSACTION,
          QuickTransactionWidgetProvider.ACTION_KEYPAD_CLEAR}) {
        new QuickTransactionWidgetProvider().onReceive(context, new Intent(action)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
      }
      for (String action : new String[] {ShortcutWidgetProvider.ACTION_EXECUTE_SHORTCUT,
          ShortcutWidgetProvider.ACTION_UNDO_SHORTCUT}) {
        new ShortcutWidgetProvider().onReceive(context, new Intent(action)
            .putExtra(ShortcutWidgetProvider.EXTRA_SHORTCUT_ID, 1L));
      }
      assertEquals(before, android.database.DatabaseUtils.queryNumEntries(
          services.databaseHelper.getReadableDatabase(), "transactions"));
      assertEquals("38123", WidgetStateStore.getAmountStr(context, widgetId));
      assertEquals(R.layout.widget_private, AppLock.privateWidgetViews(context, widgetId).getLayoutId());
      ShortcutWidgetService.ShortcutRemoteViewsFactory factory =
          new ShortcutWidgetService.ShortcutRemoteViewsFactory(context, new Intent());
      factory.onCreate();
      factory.onDataSetChanged();
      assertEquals(0, factory.getCount());
      assertNull(factory.getViewAt(0));
      factory.onDestroy();
    } finally {
      prefs.edit().putBoolean("private_widgets", old).commit();
      WidgetStateStore.setAmountStr(context, widgetId, oldAmount);
    }
  }
}
