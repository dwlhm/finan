package com.dwlhm.finan;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.dwlhm.finan.ui.common.AppServices;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class FinanApplication extends Application {

  private volatile AppServices services;
  private final CountDownLatch servicesLatch = new CountDownLatch(1);

  @Override
  public void onCreate() {
    super.onCreate();
    registerActivityLifecycleCallbacks(new com.dwlhm.finan.service.privacy.AppLock());
    new com.dwlhm.finan.service.backup.BackupRecoveryGate(this);
    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
        new com.dwlhm.finan.data.prefs.DefaultsStore(this).getThemeMode());
    Thread init = new Thread(() -> {
      AppServices created;
      try {
        created = AppServices.create(this);
      } catch (Exception e) {
        servicesLatch.countDown();
        postRequireRecovery();
        return;
      }
      services = created;
      servicesLatch.countDown();
      try {
        new com.dwlhm.finan.service.backup.BackupService(
            this, services.databaseHelper.getWritableDatabase()).recoverPendingPreferences();
        com.dwlhm.finan.service.reminder.ReminderScheduler.reconcile(this);
      } catch (Exception e) {
        postRequireRecovery();
      }
    }, "finan-init");
    init.setDaemon(true);
    init.start();
  }

  private void postRequireRecovery() {
    new Handler(Looper.getMainLooper()).post(
        () -> com.dwlhm.finan.service.backup.BackupRecoveryGate.requireRecovery(this));
  }

  public AppServices awaitServices() {
    AppServices current = getServices();
    if (current != null) return current;
    try {
      servicesLatch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    AppServices afterWait = getServices();
    if (afterWait == null) {
      throw new IllegalStateException("Services unavailable");
    }
    return afterWait;
  }

  @Override
  public void onTerminate() {
    if (services != null) {
      services.dbWorker.shutdown();
    }
    super.onTerminate();
  }

  public AppServices getServices() {
    return services;
  }
}
