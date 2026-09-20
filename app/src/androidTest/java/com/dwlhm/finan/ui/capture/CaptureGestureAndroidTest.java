package com.dwlhm.finan.ui.capture;

import static org.junit.Assert.*;
import android.animation.ValueAnimator;
import android.content.Context;
import android.database.DatabaseUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.dwlhm.finan.R;
import com.dwlhm.finan.service.privacy.AppLock;
import com.dwlhm.finan.ui.MainActivity;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ServicesProvider;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CaptureGestureAndroidTest {
  private static final String TEST_NOTE = "CaptureGestureAndroidTest";
  private static Object field(Object owner, String name) {
    try { Field f = owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner); }
    catch (ReflectiveOperationException e) { throw new AssertionError(e); }
  }
  private static void drain(AppServices services) throws Exception {
    CountDownLatch idle = new CountDownLatch(1);
    services.dbWorker.run(() -> {}, idle::countDown);
    assertTrue(idle.await(10, TimeUnit.SECONDS));
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }
  private static void touch(View button, int action, float x, float y) {
    long now = android.os.SystemClock.uptimeMillis();
    MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
    button.dispatchTouchEvent(event);
    event.recycle();
  }
  @Test public void cancellationDoesNotSaveAndClickAndCompleteHoldSaveOnce() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    android.content.SharedPreferences prefs = context.getSharedPreferences(AppLock.PREFS, 0);
    boolean oldLock = prefs.getBoolean("app_lock", false);
    prefs.edit().putBoolean("app_lock", false).commit();
    AppServices services = ServicesProvider.get(context);
    String oldDraft = services.defaultsStore.getDraftJson();
    Long oldWallet = services.defaultsStore.getLastWalletId();
    long walletId = services.walletDao.insert(TEST_NOTE, "IDR", false, 0, System.currentTimeMillis());
    long categoryId = services.categoryDao.insert(TEST_NOTE, "test", "EXPENSE", 0, 0, null);
    com.dwlhm.finan.data.prefs.TransactionFormDraft draft = new com.dwlhm.finan.data.prefs.TransactionFormDraft();
    draft.setType(com.dwlhm.finan.domain.model.TransactionType.EXPENSE);
    draft.setWalletId(walletId);
    draft.setCategoryId(categoryId);
    draft.setOccurredAtMillis(System.currentTimeMillis());
    services.defaultsStore.setCaptureDraft(draft);
    services.defaultsStore.setLastWalletId(walletId);
    long before = DatabaseUtils.queryNumEntries(services.databaseHelper.getReadableDatabase(), "transactions");
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      drain(services);
      scenario.onActivity(activity -> {
        CaptureFragment fragment = (CaptureFragment) activity.getSupportFragmentManager().getFragments().stream()
            .filter(f -> f instanceof CaptureFragment).findFirst().get();
        View button = activity.findViewById(R.id.capture_save_button_area);
        ((EditText) activity.findViewById(R.id.capture_amount)).setText("739");
        ((EditText) activity.findViewById(R.id.capture_note)).setText(TEST_NOTE);
        assertNotNull(field(fragment, "activeWallet"));
        assertNotNull(field(fragment, "selectedCategory"));
        button.setEnabled(true);
        touch(button, MotionEvent.ACTION_DOWN, 1, 1);
        ((Runnable) field(fragment, "startHoldRunnable")).run();
        touch(button, MotionEvent.ACTION_MOVE, -1000, -1000);
        assertNull(field(fragment, "holdAnimator"));
        assertFalse((Boolean) field(fragment, "saveInProgress"));
        touch(button, MotionEvent.ACTION_UP, -1000, -1000);
        touch(button, MotionEvent.ACTION_DOWN, 1, 1);
        ((Runnable) field(fragment, "startHoldRunnable")).run();
        touch(button, MotionEvent.ACTION_CANCEL, 1, 1);
        assertFalse((Boolean) field(fragment, "saveInProgress"));
      });
      drain(services);
      assertEquals(before, DatabaseUtils.queryNumEntries(services.databaseHelper.getReadableDatabase(), "transactions"));
      scenario.onActivity(activity -> assertTrue(activity.findViewById(R.id.capture_save_button_area).performClick()));
      drain(services);
      assertEquals(before + 1, DatabaseUtils.queryNumEntries(services.databaseHelper.getReadableDatabase(), "transactions"));
      scenario.onActivity(activity -> {
        CaptureFragment fragment = (CaptureFragment) activity.getSupportFragmentManager().getFragments().stream()
            .filter(f -> f instanceof CaptureFragment).findFirst().get();
        View button = activity.findViewById(R.id.capture_save_button_area);
        assertEquals(View.VISIBLE, button.getVisibility());
        ((EditText) activity.findViewById(R.id.capture_amount)).setText("947");
        ((EditText) activity.findViewById(R.id.capture_note)).setText(TEST_NOTE);
        button.setEnabled(true);
        touch(button, MotionEvent.ACTION_DOWN, 1, 1);
        ((Runnable) field(fragment, "startHoldRunnable")).run();
        ((ValueAnimator) field(fragment, "holdAnimator")).end();
        touch(button, MotionEvent.ACTION_UP, 1, 1);
      });
      drain(services);
      assertEquals(before + 2, DatabaseUtils.queryNumEntries(services.databaseHelper.getReadableDatabase(), "transactions"));
      CountDownLatch undoRelease = new CountDownLatch(1);
      services.dbWorker.run(() -> {
        try { undoRelease.await(10, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      });
      scenario.onActivity(activity -> {
        View undo = activity.findViewById(R.id.capture_undo_action);
        undo.performClick();
        undo.performClick();
        ((EditText) activity.findViewById(R.id.capture_amount)).setText("1837");
        ((EditText) activity.findViewById(R.id.capture_note)).setText("New unsaved draft");
      });
      undoRelease.countDown();
      drain(services);
      assertEquals(before + 1, DatabaseUtils.queryNumEntries(services.databaseHelper.getReadableDatabase(), "transactions"));
      scenario.onActivity(activity -> assertEquals("New unsaved draft",
          ((EditText) activity.findViewById(R.id.capture_note)).getText().toString()));
    } finally {
      try (android.database.Cursor rows = services.databaseHelper.getReadableDatabase().rawQuery(
          "SELECT id FROM transactions WHERE note = ?", new String[] {TEST_NOTE})) {
        while (rows.moveToNext()) services.transactionService.delete(rows.getLong(0));
      }
      prefs.edit().putBoolean("app_lock", oldLock).commit();
      services.walletDao.delete(walletId);
      services.categoryDao.delete(categoryId);
      services.defaultsStore.setDraftJson(oldDraft);
      if (oldWallet != null) services.defaultsStore.setLastWalletId(oldWallet);
      else context.getSharedPreferences("finan_defaults", 0).edit().remove("last_wallet_id").commit();
    }
  }
}
