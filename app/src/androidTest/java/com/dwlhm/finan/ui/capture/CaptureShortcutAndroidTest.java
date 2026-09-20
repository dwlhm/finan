package com.dwlhm.finan.ui.capture;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.dwlhm.finan.R;
import com.dwlhm.finan.data.prefs.TransactionFormDraft;
import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.privacy.AppLock;
import com.dwlhm.finan.ui.MainActivity;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ServicesProvider;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CaptureShortcutAndroidTest {
  private static final String NOTE = "CaptureShortcutAndroidTest";
  private Context context;
  private AppServices services;
  private long walletId, destinationId, categoryId;
  private long before;
  private boolean oldLock;
  private String oldDraft;
  private Long oldWallet;
  private TransactionTemplate routine, ordinary, transfer;

  @Before public void seed() {
    context = ApplicationProvider.getApplicationContext();
    services = ServicesProvider.get(context);
    oldLock = context.getSharedPreferences(AppLock.PREFS, 0).getBoolean("app_lock", false);
    context.getSharedPreferences(AppLock.PREFS, 0).edit().putBoolean("app_lock", false).commit();
    oldDraft = services.defaultsStore.getDraftJson();
    oldWallet = services.defaultsStore.getLastWalletId();
    walletId = services.walletDao.insert(NOTE, "IDR", false, 0, System.currentTimeMillis());
    destinationId = services.walletDao.insert(NOTE + " destination", "IDR", false, 0, System.currentTimeMillis());
    categoryId = services.categoryDao.insert(NOTE, "test", "EXPENSE", 0, 0, null);
    ordinary = new TransactionTemplate(0, "Kopi sore", TransactionType.EXPENSE, 0,
        categoryId, walletId, null, NOTE, "☕", -20);
    routine = new TransactionTemplate(0, "Langganan bulanan", TransactionType.EXPENSE, 739,
        categoryId, walletId, null, NOTE, "↻", -19);
    routine.setScheduled(true);
    transfer = new TransactionTemplate(0, "Pindah tabungan", TransactionType.TRANSFER_OUT, 947,
        null, walletId, destinationId, NOTE, "↗", -18);
    services.transactionTemplateDao.insert(ordinary);
    services.transactionTemplateDao.insert(routine);
    services.transactionTemplateDao.insert(transfer);
    TransactionFormDraft draft = new TransactionFormDraft();
    draft.setWalletId(walletId);
    draft.setCategoryId(categoryId);
    draft.setOccurredAtMillis(System.currentTimeMillis());
    draft.setNote("Previous draft");
    services.defaultsStore.setCaptureDraft(draft);
    services.defaultsStore.setLastWalletId(walletId);
    before = count();
  }

  @After public void cleanup() throws Exception {
    drain();
    try (android.database.Cursor rows = services.databaseHelper.getReadableDatabase().rawQuery(
        "SELECT id FROM transactions WHERE note = ?", new String[] {NOTE})) {
      while (rows.moveToNext()) services.transactionService.delete(rows.getLong(0));
    }
    services.transactionTemplateDao.delete(ordinary.getId());
    services.transactionTemplateDao.delete(routine.getId());
    services.transactionTemplateDao.delete(transfer.getId());
    services.walletDao.delete(walletId);
    services.walletDao.delete(destinationId);
    services.categoryDao.delete(categoryId);
    services.defaultsStore.setDraftJson(oldDraft);
    if (oldWallet != null) services.defaultsStore.setLastWalletId(oldWallet);
    else context.getSharedPreferences("finan_defaults", 0).edit().remove("last_wallet_id").commit();
    context.getSharedPreferences(AppLock.PREFS, 0).edit().putBoolean("app_lock", oldLock).commit();
  }

  private long count() {
    return DatabaseUtils.queryNumEntries(services.databaseHelper.getReadableDatabase(), "transactions");
  }

  private void drain() throws Exception {
    // The form pipeline has refresh, restoration, template and reference-resolution stages.
    for (int stage = 0; stage < 5; stage++) {
      CountDownLatch idle = new CountDownLatch(1);
      services.dbWorker.run(() -> {}, idle::countDown);
      assertTrue(idle.await(10, TimeUnit.SECONDS));
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
  }

  private static CaptureFragment capture(MainActivity activity) {
    return (CaptureFragment) activity.getSupportFragmentManager().getFragments().stream()
        .filter(f -> f instanceof CaptureFragment).findFirst().get();
  }

  private static Object field(Object owner, String name) {
    try {
      Field field = owner.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(owner);
    } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
  }

  private static void tapTemplate(MainActivity activity, TransactionTemplate template) {
    ViewGroup rail = activity.findViewById(R.id.capture_template_chips_layout);
    for (int i = 0; i < rail.getChildCount(); i++) {
      if (Long.valueOf(template.getId()).equals(rail.getChildAt(i).getTag())) {
        rail.getChildAt(i).performClick();
        return;
      }
    }
    fail("Missing template in rail");
  }

  private static void activate(MainActivity activity, TransactionTemplate template) {
    try {
      Method method = CaptureFragment.class.getDeclaredMethod("executeShortcut", TransactionTemplate.class);
      method.setAccessible(true);
      method.invoke(capture(activity), template);
    } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
  }

  @Test public void visibleRoutineShortcutPrefillsAndOnlyExplicitSaveWrites() throws Exception {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      drain();
      scenario.onActivity(activity -> {
        assertTrue(activity.findViewById(R.id.capture_template_container).isShown());
        ViewGroup rail = activity.findViewById(R.id.capture_template_chips_layout);
        int routineIndex = -1, ordinaryIndex = -1;
        for (int i = 0; i < rail.getChildCount(); i++) {
          View chip = rail.getChildAt(i);
          if (Long.valueOf(routine.getId()).equals(chip.getTag())) {
            routineIndex = i;
            assertEquals(View.VISIBLE, chip.findViewById(R.id.chip_template_routine).getVisibility());
          }
          if (Long.valueOf(ordinary.getId()).equals(chip.getTag())) {
            ordinaryIndex = i;
            assertEquals(View.GONE, chip.findViewById(R.id.chip_template_routine).getVisibility());
          }
        }
        assertTrue(routineIndex >= 0 && routineIndex < ordinaryIndex);
        tapTemplate(activity, routine);
      });
      drain();
      assertEquals(before, count());
      scenario.onActivity(activity -> {
        assertEquals("739", ((EditText) activity.findViewById(R.id.capture_amount)).getText().toString());
        assertEquals(NOTE, ((EditText) activity.findViewById(R.id.capture_note)).getText().toString());
        assertEquals(walletId, ((com.dwlhm.finan.data.entity.Wallet) field(capture(activity), "activeWallet")).getId());
        assertEquals(categoryId, ((com.dwlhm.finan.data.entity.Category) field(capture(activity), "selectedCategory")).getId());
      });
      android.graphics.Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
      try (java.io.FileOutputStream output = new java.io.FileOutputStream(new java.io.File(context.getCacheDir(), "capture-shortcuts.png"))) {
        screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
      } finally { screenshot.recycle(); }
      scenario.onActivity(activity -> activity.findViewById(R.id.capture_save_button_area).performClick());
      drain();
      assertEquals(before + 1, count());
      try (android.database.Cursor rows = services.databaseHelper.getReadableDatabase().rawQuery(
          "SELECT amount_minor, wallet_id, category_id FROM transactions WHERE note = ?", new String[] {NOTE})) {
        assertTrue(rows.moveToFirst());
        assertEquals(routine.getAmountMinor(), rows.getLong(0));
        assertEquals(walletId, rows.getLong(1));
        assertEquals(categoryId, rows.getLong(2));
        assertFalse(rows.moveToNext());
      }
    }
  }

  @Test public void variableTransferMissingReferencesAndStaleReadsRemainDraftOnly() throws Exception {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      drain();
      scenario.onActivity(activity -> {
        ((EditText) activity.findViewById(R.id.capture_amount)).setText("1837");
        tapTemplate(activity, ordinary);
      });
      drain();
      scenario.onActivity(activity -> {
        assertEquals("1.837", ((EditText) activity.findViewById(R.id.capture_amount)).getText().toString());
        tapTemplate(activity, transfer);
      });
      drain();
      assertEquals(before, count());
      scenario.onActivity(activity -> {
        assertEquals(TransactionType.TRANSFER_OUT, field(capture(activity), "selectedType"));
        assertEquals(destinationId, ((com.dwlhm.finan.data.entity.Wallet) field(capture(activity), "destinationWallet")).getId());
        transfer.setDestinationWalletId(Long.MAX_VALUE);
        activate(activity, transfer);
      });
      drain();
      scenario.onActivity(activity -> {
        assertNull(field(capture(activity), "destinationWallet"));
        activity.findViewById(R.id.capture_save_button_area).performClick();
        routine.setCategoryId(Long.MAX_VALUE);
        routine.setWalletId(Long.MAX_VALUE);
        activate(activity, routine);
      });
      drain();
      scenario.onActivity(activity -> {
        assertNull(field(capture(activity), "activeWallet"));
        assertNull(field(capture(activity), "selectedCategory"));
        activity.findViewById(R.id.capture_save_button_area).performClick();
      });
      drain();
      assertEquals(before, count());
      CountDownLatch release = new CountDownLatch(1);
      services.dbWorker.run(() -> {
        try { release.await(10, TimeUnit.SECONDS); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); }
      });
      scenario.onActivity(activity -> {
        tapTemplate(activity, ordinary);
        ((EditText) activity.findViewById(R.id.capture_note)).setText("Keep my new edit");
      });
      release.countDown();
      drain();
      scenario.onActivity(activity -> assertEquals("Keep my new edit",
          ((EditText) activity.findViewById(R.id.capture_note)).getText().toString()));
      scenario.recreate();
      drain();
      scenario.onActivity(activity -> assertTrue(activity.findViewById(R.id.capture_template_container).isShown()));
      assertEquals(before, count());
    }
  }

  @Test public void coldWidgetDeepLinkWaitsForInitialDraftRestoreAndNeverRecords() throws Exception {
    Intent intent = new Intent(context, MainActivity.class).putExtra(MainActivity.EXTRA_TEMPLATE_ID, routine.getId());
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
      drain();
      scenario.onActivity(activity -> {
        assertEquals("739", ((EditText) activity.findViewById(R.id.capture_amount)).getText().toString());
        assertEquals(NOTE, ((EditText) activity.findViewById(R.id.capture_note)).getText().toString());
        assertFalse(activity.getIntent().hasExtra(MainActivity.EXTRA_TEMPLATE_ID));
      });
      assertEquals(before, count());
      scenario.recreate();
      drain();
      assertEquals(before, count());
    }
  }
}
