package com.dwlhm.finan.service.reminder;

import android.app.job.JobParameters;
import android.app.job.JobService;
import com.dwlhm.finan.FinanApplication;
import com.dwlhm.finan.data.prefs.ReminderPreferences;
import com.dwlhm.finan.ui.common.AppServices;
import java.time.LocalDate;
import java.time.ZoneId;

/** Queries current obligations without performing any financial writes. */
public final class DueReminderJobService extends JobService {
  private Run activeRun;

  @Override public boolean onStartJob(JobParameters params) {
    if (com.dwlhm.finan.service.backup.BackupRecoveryGate.isBlocked()) {
      ReminderScheduler.reconcile(this);
      return false;
    }
    ReminderPreferences preferences = new ReminderPreferences(this);
    if (!preferences.isEnabled() || !ReminderScheduler.notificationsAllowed(this)) {
      ReminderScheduler.reconcile(this);
      return false;
    }
    if (activeRun != null) finish(activeRun);
    Run run = new Run(params, ZoneId.systemDefault(), preferences.getConsentGeneration());
    activeRun = run;
    AppServices services = ((FinanApplication) getApplication()).getServices();
    services.dbWorker.compute(() -> {
      try {
        boolean hasDue = !services.createUpcomingCashFlowService(run.zone)
            .calculateForwardSummary(run.date, run.date, null).getUpcomingObligations().isEmpty();
        return new QueryResult(hasDue, null);
      } catch (Exception error) {
        return new QueryResult(false, error);
      }
    }, result -> {
      if (activeRun != run || run.canceled) return;
      ZoneId currentZone = ZoneId.systemDefault();
      LocalDate today = LocalDate.now(currentZone);
      try {
        if (com.dwlhm.finan.service.backup.BackupRecoveryGate.isBlocked()) return;
        if (result.error != null) return;
        if (preferences.getConsentGeneration() != run.consentGeneration) return;
        if (!preferences.isEnabled() || !ReminderScheduler.notificationsAllowed(this)) {
          ReminderScheduler.reconcile(this);
          return;
        }
        if (!run.date.equals(today) || !run.zone.equals(currentZone)) return;
        if (!result.hasDue) ReminderScheduler.cancelNotification(this);
        else if (ReminderPolicy.shouldNotify(preferences.isEnabled(), true, run.date, run.zone,
            today, currentZone, preferences.getLastNotifiedEpochDay(), true)) {
          ReminderScheduler.notifyDueToday(this, today);
        }
      } finally {
        finish(run);
      }
    });
    return true;
  }

  private void finish(Run run) {
    if (activeRun != run || run.canceled) return;
    run.canceled = true;
    activeRun = null;
    jobFinished(run.params, false);
  }

  @Override public boolean onStopJob(JobParameters params) {
    if (activeRun != null && activeRun.params == params) {
      activeRun.canceled = true;
      activeRun = null;
    }
    return false;
  }

  @Override public void onDestroy() {
    if (activeRun != null) {
      activeRun.canceled = true;
      activeRun = null;
    }
    super.onDestroy();
  }

  private static final class Run {
    final JobParameters params;
    final ZoneId zone;
    final LocalDate date;
    final long consentGeneration;
    boolean canceled;
    Run(JobParameters params, ZoneId zone, long consentGeneration) {
      this.params = params;
      this.zone = zone;
      this.consentGeneration = consentGeneration;
      date = LocalDate.now(zone);
    }
  }

  private static final class QueryResult {
    final boolean hasDue;
    final Exception error;
    QueryResult(boolean hasDue, Exception error) { this.hasDue = hasDue; this.error = error; }
  }
}
