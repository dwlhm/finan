package com.dwlhm.finan.service.backup;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.MainActivity;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ServicesProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Application-owned barrier while a committed restore awaits preference journal replay. */
public final class BackupRecoveryGate implements Application.ActivityLifecycleCallbacks {
  private static volatile boolean blocked;
  private static BackupRecoveryGate instance;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final Application context;
  private final Set<Activity> started = Collections.newSetFromMap(new WeakHashMap<>());
  private final WeakHashMap<Activity, AlertDialog> covers = new WeakHashMap<>();
  private boolean retrying;

  /** Registers exactly one process-wide gate before activities are created. */
  public BackupRecoveryGate(Application application) {
    context = application;
    instance = this;
    application.registerActivityLifecycleCallbacks(this);
  }

  /** True immediately after replay failure, including before the main-thread cover is shown. */
  public static boolean isBlocked() { return blocked; }

  /** Signals committed-data recovery from any thread without retaining a fragment or activity. */
  public static void requireRecovery(Context context) {
    blocked = true;
    BackupRecoveryGate gate = instance;
    if (gate == null) throw new IllegalStateException("Backup recovery gate is not initialized");
    gate.main.post(gate::coverStarted);
  }

  private void coverStarted() {
    for (Activity activity : new ArrayList<>(started)) cover(activity);
  }

  private void cover(Activity activity) {
    if (!blocked || activity.isFinishing() || activity.isDestroyed() || covers.containsKey(activity)) return;
    AlertDialog dialog = new AlertDialog.Builder(activity)
        .setMessage(R.string.backup_preferences_pending)
        .setCancelable(false).setPositiveButton(R.string.backup_retry, null).create();
    covers.put(activity, dialog);
    dialog.setOnShowListener(d -> {
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!retrying);
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> retry());
    });
    dialog.show();
    dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    dialog.getWindow().setLayout(-1, -1);
  }

  private void retry() {
    if (!blocked || retrying) return;
    retrying = true;
    enableRetry(false);
    AppServices services = ServicesProvider.get(context);
    services.dbWorker.run(() -> {
      boolean recovered;
      try {
        new BackupService(context, services.databaseHelper.getWritableDatabase()).recoverPendingPreferences();
        recovered = true;
      } catch (Exception error) { recovered = false; }
      final boolean success = recovered;
      // This handler is independent of screen-owned callback cancellation.
      main.post(() -> finishRetry(success));
    });
  }

  private void finishRetry(boolean success) {
    retrying = false;
    if (!success) { enableRetry(true); return; }
    blocked = false;
    com.dwlhm.finan.service.reminder.ReminderScheduler.reconcile(context);
    com.dwlhm.finan.service.privacy.AppLock.refreshWidgets(context);
    context.startActivity(new Intent(context, MainActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    for (Activity activity : new ArrayList<>(covers.keySet())) release(activity);
  }

  private void enableRetry(boolean enabled) {
    for (AlertDialog dialog : new ArrayList<>(covers.values())) {
      if (dialog.isShowing()) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
    }
  }

  private void release(Activity activity) {
    AlertDialog dialog = covers.remove(activity);
    if (dialog != null) dialog.dismiss();
  }

  @Override public void onActivityCreated(Activity activity, Bundle state) {}
  @Override public void onActivityPostCreated(Activity activity, Bundle state) { cover(activity); }
  @Override public void onActivityStarted(Activity activity) { started.add(activity); cover(activity); }
  @Override public void onActivityResumed(Activity activity) { cover(activity); }
  @Override public void onActivityPaused(Activity activity) {}
  @Override public void onActivityStopped(Activity activity) { started.remove(activity); release(activity); }
  @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
  @Override public void onActivityDestroyed(Activity activity) { started.remove(activity); release(activity); }
}
