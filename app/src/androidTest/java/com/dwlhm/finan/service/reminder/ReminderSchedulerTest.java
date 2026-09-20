package com.dwlhm.finan.service.reminder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import com.dwlhm.finan.data.prefs.ReminderPreferences;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

/** Exercises consent against Android's real scheduler and notification manager. */
@RunWith(AndroidJUnit4.class)
public final class ReminderSchedulerTest {
  @Rule public final TestRule notificationPermission =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
          ? GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
          : (statement, description) -> statement;
  private Context context;
  private ReminderPreferences preferences;
  private JobScheduler scheduler;

  @Before public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    preferences = new ReminderPreferences(context);
    scheduler = context.getSystemService(JobScheduler.class);
    ReminderScheduler.setEnabled(context, false);
  }

  @After public void tearDown() { ReminderScheduler.setEnabled(context, false); }

  @Test public void offCancelsJobAndCannotPublish() {
    ReminderScheduler.reconcile(context);
    ReminderScheduler.notifyDueToday(context, LocalDate.now());
    assertFalse(preferences.isEnabled());
    assertNull(scheduler.getPendingJob(ReminderScheduler.JOB_ID));
    assertEquals(0, context.getSystemService(NotificationManager.class).getActiveNotifications().length);
  }

  @Test public void deniedPermissionRevokesConsent() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU);
    Context denied = new ContextWrapper(context) {
      @Override public int checkSelfPermission(String permission) {
        return Manifest.permission.POST_NOTIFICATIONS.equals(permission)
            ? PackageManager.PERMISSION_DENIED : super.checkSelfPermission(permission);
      }
    };
    ReminderScheduler.setEnabled(denied, true);
    assertFalse(preferences.isEnabled());
    assertNull(scheduler.getPendingJob(ReminderScheduler.JOB_ID));
  }

  @Test public void toggledConsentInvalidatesPendingReadButRefreshDoesNot() {
    long before = preferences.getConsentGeneration();
    preferences.setEnabled(true);
    long enabledGeneration = preferences.getConsentGeneration();
    assertTrue(enabledGeneration > before);
    preferences.setEnabled(true);
    assertEquals(enabledGeneration, preferences.getConsentGeneration());
    preferences.setEnabled(false);
    preferences.setEnabled(true);
    assertTrue(preferences.getConsentGeneration() > enabledGeneration);
  }

  @Test public void allowedConsentRetainsOnePersistedPeriodicJobAndOffCancelsIt() {
    assumeTrue(ReminderScheduler.notificationsAllowed(context));
    ReminderScheduler.setEnabled(context, true);
    assertTrue(preferences.isEnabled());
    JobInfo job = scheduler.getPendingJob(ReminderScheduler.JOB_ID);
    assertNotNull(job);
    assertTrue(job.isPeriodic());
    assertTrue(job.isPersisted());
    assertEquals(TimeUnit.HOURS.toMillis(1), job.getIntervalMillis());
    ReminderScheduler.reconcile(context);
    assertEquals(1, scheduler.getAllPendingJobs().stream()
        .filter(pending -> pending.getId() == ReminderScheduler.JOB_ID).count());
    ReminderScheduler.setEnabled(context, false);
    assertNull(scheduler.getPendingJob(ReminderScheduler.JOB_ID));
  }
}
