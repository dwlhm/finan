package com.dwlhm.finan.widget;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.domain.model.RecurringFrequency;
import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.privacy.AppLock;
import com.dwlhm.finan.ui.MainActivity;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ServicesProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ShortcutWidgetDraftAndroidTest {
  private static final long FIXED_AMOUNT = 42500L;

  @Test public void fixedScheduledVariableAndTransferShortcutsOnlyOpenCapture() {
    Context context = ApplicationProvider.getApplicationContext();
    SharedPreferences prefs = context.getSharedPreferences(AppLock.PREFS, Context.MODE_PRIVATE);
    boolean oldPrivate = prefs.getBoolean("private_widgets", false);
    boolean oldLock = prefs.getBoolean("app_lock", false);
    AppServices services = ServicesProvider.get(context);
    try {
      prefs.edit().putBoolean("private_widgets", false).putBoolean("app_lock", false).commit();
      assertDraftOnly(context, services, TransactionType.EXPENSE, FIXED_AMOUNT, false);
      assertDraftOnly(context, services, TransactionType.EXPENSE, FIXED_AMOUNT, true);
      assertDraftOnly(context, services, TransactionType.INCOME, 0L, false);
      assertDraftOnly(context, services, TransactionType.TRANSFER_OUT, FIXED_AMOUNT, false);
    } finally {
      prefs.edit().putBoolean("private_widgets", oldPrivate).putBoolean("app_lock", oldLock).commit();
    }
  }

  @Test public void invalidShortcutIdsDoNotLaunchOrWrite() {
    Context context = ApplicationProvider.getApplicationContext();
    SharedPreferences prefs = context.getSharedPreferences(AppLock.PREFS, Context.MODE_PRIVATE);
    boolean oldPrivate = prefs.getBoolean("private_widgets", false);
    boolean oldLock = prefs.getBoolean("app_lock", false);
    AppServices services = ServicesProvider.get(context);
    long before = transactionCount(services);
    RecordingContext recording = new RecordingContext(context);
    try {
      prefs.edit().putBoolean("private_widgets", false).putBoolean("app_lock", false).commit();
      ShortcutWidgetProvider provider = new ShortcutWidgetProvider();
      provider.onReceive(recording, new Intent(ShortcutWidgetProvider.ACTION_EXECUTE_SHORTCUT));
      provider.onReceive(recording, new Intent(ShortcutWidgetProvider.ACTION_EXECUTE_SHORTCUT)
          .putExtra(ShortcutWidgetProvider.EXTRA_SHORTCUT_ID, 0L));
      assertNull(recording.startedIntent);
      assertEquals(before, transactionCount(services));
    } finally {
      prefs.edit().putBoolean("private_widgets", oldPrivate).putBoolean("app_lock", oldLock).commit();
    }
  }

  private static void assertDraftOnly(Context context, AppServices services,
      TransactionType type, long amount, boolean scheduled) {
    TransactionTemplate template = new TransactionTemplate(
        "Widget draft regression", type, amount, null, null, null, "Review before saving", "");
    template.setScheduled(scheduled);
    template.setFrequency(scheduled ? RecurringFrequency.MONTHLY : RecurringFrequency.NONE);
    template.setDueDay(java.time.LocalDate.now().getDayOfMonth());
    long id = services.transactionTemplateDao.insert(template);
    assertTrue(id > 0);
    long before = transactionCount(services);
    RecordingContext recording = new RecordingContext(context);
    try {
      new ShortcutWidgetProvider().onReceive(recording,
          new Intent(ShortcutWidgetProvider.ACTION_EXECUTE_SHORTCUT)
              .putExtra(ShortcutWidgetProvider.EXTRA_SHORTCUT_ID, id));
      assertEquals(before, transactionCount(services));
      Intent launched = recording.startedIntent;
      assertNotNull(launched);
      assertEquals(MainActivity.class.getName(), launched.getComponent().getClassName());
      assertEquals(MainActivity.NAV_TARGET_CAPTURE, launched.getStringExtra(MainActivity.EXTRA_NAV_TARGET));
      assertEquals(id, launched.getLongExtra(MainActivity.EXTRA_TEMPLATE_ID, -1L));
      assertEquals(Intent.ACTION_MAIN, launched.getAction());
      assertTrue(launched.hasCategory(Intent.CATEGORY_LAUNCHER));
      assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP, launched.getFlags());
      assertEquals(template.getLastRecordedAt(), services.transactionTemplateDao.findById(id).getLastRecordedAt());
    } finally {
      services.transactionTemplateDao.delete(id);
    }
  }

  private static long transactionCount(AppServices services) {
    return DatabaseUtils.queryNumEntries(services.databaseHelper.getReadableDatabase(), "transactions");
  }

  private static final class RecordingContext extends ContextWrapper {
    private Intent startedIntent;

    RecordingContext(Context base) {
      super(base);
    }

    @Override public void startActivity(Intent intent) {
      assertNull("A shortcut must launch capture only once", startedIntent);
      startedIntent = intent;
    }
  }
}
