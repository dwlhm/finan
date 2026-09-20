package com.dwlhm.finan.service.reminder;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import com.dwlhm.finan.R;
import com.dwlhm.finan.data.prefs.ReminderPreferences;
import com.dwlhm.finan.ui.MainActivity;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/** Device-local, opt-in periodic reminders with generic notification content. */
public final class ReminderScheduler {
  public static final int JOB_ID = 4101;
  public static final int NOTIFICATION_ID = 4102;
  public static final String CHANNEL_ID = "due_reminders";
  private static final long INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);
  private ReminderScheduler() {}

  /** Checks both app notification permission and the reminder channel. */
  public static boolean notificationsAllowed(Context context) {
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (manager == null || !manager.areNotificationsEnabled()) return false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return false;
    NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
    return channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
  }

  /** Changes consent and immediately reconciles the system job. */
  public static void setEnabled(Context context, boolean enabled) {
    new ReminderPreferences(context).setEnabled(enabled);
    reconcile(context);
  }

  /** Retains an existing valid job, or disables reminders if scheduling is unavailable. */
  public static void reconcile(Context context) {
    if (com.dwlhm.finan.service.backup.BackupRecoveryGate.isBlocked()) {
      JobScheduler scheduler = context.getSystemService(JobScheduler.class);
      if (scheduler != null) scheduler.cancel(JOB_ID);
      cancelNotification(context);
      return;
    }
    ReminderPreferences preferences = new ReminderPreferences(context);
    JobScheduler scheduler = context.getSystemService(JobScheduler.class);
    if (!preferences.isEnabled() || !notificationsAllowed(context) || scheduler == null) {
      preferences.setEnabled(false);
      if (scheduler != null) scheduler.cancel(JOB_ID);
      cancelNotification(context);
      return;
    }
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
        context.getString(R.string.reminder_title), NotificationManager.IMPORTANCE_DEFAULT));
    ComponentName service = new ComponentName(context, DueReminderJobService.class);
    JobInfo existing = scheduler.getPendingJob(JOB_ID);
    if (existing != null && existing.isPersisted() && existing.isPeriodic()
        && existing.getIntervalMillis() == INTERVAL_MILLIS && existing.getService().equals(service)
        && existing.getNetworkType() == JobInfo.NETWORK_TYPE_NONE) return;
    try {
      if (scheduler.schedule(new JobInfo.Builder(JOB_ID, service)
          .setPeriodic(INTERVAL_MILLIS).setPersisted(true).build()) == JobScheduler.RESULT_SUCCESS) return;
    } catch (RuntimeException ignored) {
      // A rejected schedule revokes the opt-in instead of claiming an active reminder.
    }
    preferences.setEnabled(false);
    scheduler.cancel(JOB_ID);
    cancelNotification(context);
  }

  /** Removes any outstanding reminder without changing the daily delivery marker. */
  public static void cancelNotification(Context context) {
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (manager != null) manager.cancel(NOTIFICATION_ID);
  }

  /** Delivers generic content and advances the day marker only after successful posting. */
  public static void notifyDueToday(Context context, LocalDate date) {
    if (com.dwlhm.finan.service.backup.BackupRecoveryGate.isBlocked()) return;
    ReminderPreferences preferences = new ReminderPreferences(context);
    if (!preferences.isEnabled() || !notificationsAllowed(context)
        || date.toEpochDay() <= preferences.getLastNotifiedEpochDay()) return;
    Intent intent = new Intent(context, MainActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(MainActivity.EXTRA_OPEN_DUE_REMINDERS, true);
    PendingIntent contentIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification notification = new Notification.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_summary_calendar)
        .setContentTitle(context.getString(R.string.reminder_title))
        .setContentText(context.getString(R.string.reminder_notification_body))
        .setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true)
        .setContentIntent(contentIntent).build();
    try {
      context.getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification);
      preferences.setLastNotifiedEpochDay(date.toEpochDay());
    } catch (SecurityException denied) {
      setEnabled(context, false);
    }
  }
}
