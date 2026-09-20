package com.dwlhm.finan;

import android.app.Application;

import com.dwlhm.finan.ui.common.AppServices;

public final class FinanApplication extends Application {

  private AppServices services;

  @Override
  public void onCreate() {
    super.onCreate();
    registerActivityLifecycleCallbacks(new com.dwlhm.finan.service.privacy.AppLock());
    new com.dwlhm.finan.service.backup.BackupRecoveryGate(this);
    services = AppServices.create(this);
    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(services.defaultsStore.getThemeMode());
    try {
      new com.dwlhm.finan.service.backup.BackupService(this, services.databaseHelper.getWritableDatabase())
          .recoverPendingPreferences();
      com.dwlhm.finan.service.reminder.ReminderScheduler.reconcile(this);
    } catch (Exception e) {
      com.dwlhm.finan.service.backup.BackupRecoveryGate.requireRecovery(this);
    }
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
