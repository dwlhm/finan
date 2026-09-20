package com.dwlhm.finan.service.backup;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.MainActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Exercises the recovery barrier without a Settings fragment or its callback queue. */
@RunWith(AndroidJUnit4.class)
public final class BackupRecoveryGateTest {
  private static final long UI_TIMEOUT_MILLIS = java.util.concurrent.TimeUnit.SECONDS.toMillis(10);
  private static final long POLL_INTERVAL_MILLIS = 25;

  @Test public void applicationSignalSurvivesRecreationAndRetryCompletes() {
    Context context = ApplicationProvider.getApplicationContext();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      Throwable failure = null;
      try {
        BackupRecoveryGate.requireRecovery(context);
        assertTrue(BackupRecoveryGate.isBlocked());
        assertNotNull(findRetry(context));
        scenario.recreate();
        assertTrue(BackupRecoveryGate.isBlocked());
        AccessibilityNodeInfo retry = findRetry(context);
        assertNotNull(retry);
        assertTrue(retry.performAction(AccessibilityNodeInfo.ACTION_CLICK));
        awaitRecovery();
        assertFalse(BackupRecoveryGate.isBlocked());
      } catch (RuntimeException | Error error) {
        failure = error;
        throw error;
      } finally {
        try {
          if (BackupRecoveryGate.isBlocked()) {
            AccessibilityNodeInfo retry = findRetry(context);
            assertNotNull("Recovery retry must be available for cleanup", retry);
            assertTrue(retry.performAction(AccessibilityNodeInfo.ACTION_CLICK));
            awaitRecovery();
            assertFalse("Recovery gate must be cleared after cleanup", BackupRecoveryGate.isBlocked());
          }
        } catch (RuntimeException | Error cleanupError) {
          if (failure != null) failure.addSuppressed(cleanupError);
          else throw cleanupError;
        }
      }
    }
  }

  private static void awaitRecovery() {
    long deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS;
    while (BackupRecoveryGate.isBlocked() && SystemClock.uptimeMillis() < deadline) {
      SystemClock.sleep(POLL_INTERVAL_MILLIS);
    }
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }

  private static AccessibilityNodeInfo findRetry(Context context) {
    long deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS;
    do {
      AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation()
          .getUiAutomation().getRootInActiveWindow();
      if (root != null) {
        for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(
            context.getString(R.string.backup_retry))) {
          if (node.isVisibleToUser() && node.isEnabled() && node.isClickable()) return node;
        }
      }
      SystemClock.sleep(POLL_INTERVAL_MILLIS);
    } while (SystemClock.uptimeMillis() < deadline);
    return null;
  }
}
