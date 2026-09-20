package com.dwlhm.finan.service.privacy;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.SystemClock;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.MainActivity;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Device authentication guards UI and navigation; it does not encrypt the local database. */
public final class AppLock implements Application.ActivityLifecycleCallbacks {
  public static final String PREFS = "finan_security";
  /** Fixed grace period after the last activity leaves the foreground. */
  static final long LOCK_TIMEOUT_MS = 60_000L;
  private static final int AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG
      | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
  private static final WeakHashMap<Activity, Dialog> covers = new WeakHashMap<>();
  private static final WeakHashMap<Activity, Runnable> continuations = new WeakHashMap<>();
  private static final WeakHashMap<Activity, CancellationSignal> prompts = new WeakHashMap<>();
  private static final Set<Activity> resumedActivities = Collections.newSetFromMap(new WeakHashMap<>());
  private static final SessionState session = new SessionState();
  private int started;

  /** In-memory session clock shared by production lifecycle callbacks and unit tests. */
  static final class SessionState {
    private boolean authenticated;
    private long backgroundAt = -1;
    boolean isLocked(long now, boolean enabled) {
      if (backgroundAt >= 0 && now - backgroundAt >= LOCK_TIMEOUT_MS) authenticated = false;
      return enabled && !authenticated;
    }
    void onAuthenticated() { authenticated = true; backgroundAt = -1; }
    void onBackground(long now) { if (backgroundAt < 0) backgroundAt = now; }
    void onForeground(long now) { isLocked(now, true); backgroundAt = -1; }
  }

  /** Returns the persisted opt-in setting. */
  public static boolean enabled(Context context) {
    return context.getSharedPreferences(PREFS, 0).getBoolean("app_lock", false);
  }
  /** App lock forces widget privacy even during an authenticated session. */
  public static boolean privateWidgets(Context context) {
    return com.dwlhm.finan.service.backup.BackupRecoveryGate.isBlocked()
        || enabled(context) || context.getSharedPreferences(PREFS, 0).getBoolean("private_widgets", false);
  }
  /** Changes the independent widget setting and refreshes all surfaces. */
  public static void setPrivateWidgets(Context context, boolean value) {
    context.getSharedPreferences(PREFS, 0).edit().putBoolean("private_widgets", value).apply();
    refreshWidgets(context);
  }
  private static boolean locked(Context context) {
    return session.isLocked(SystemClock.elapsedRealtime(), enabled(context));
  }
  /** Runs the latest pending continuation only while its live owner is resumed and unlocked. */
  public static void afterUnlock(Activity activity, Runnable action) {
    if (activity.isDestroyed() || activity.isFinishing()) return;
    continuations.put(activity, action);
    reconcile(activity);
  }
  /** Authenticates before changing the optional lock setting. */
  public static void setEnabled(Activity activity, boolean value, Runnable done) {
    setEnabled(activity, value, done, null);
  }
  /** Leaves preferences unchanged if authentication is unavailable or cancelled. */
  public static void setEnabled(Activity activity, boolean value, Runnable done, Runnable failure) {
    authenticate(activity, () -> {
      activity.getSharedPreferences(PREFS, 0).edit().putBoolean("app_lock", value).apply();
      session.onAuthenticated();
      reconcile(activity);
      refreshWidgets(activity);
      if (done != null) done.run();
    }, failure);
  }
  /** Replaces private widgets with an inert generic entry into the authenticated app. */
  public static RemoteViews privateWidgetViews(Context context, int widgetId) {
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_private);
    Intent intent = new Intent(context, MainActivity.class);
    PendingIntent open = PendingIntent.getActivity(context, widgetId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    views.setOnClickPendingIntent(R.id.widget_private_open, open);
    return views;
  }
  /** Refreshes both widget providers after privacy changes. */
  public static void refreshWidgets(Context context) {
    com.dwlhm.finan.widget.QuickTransactionWidgetProvider.updateAllWidgets(context);
    com.dwlhm.finan.widget.ShortcutWidgetProvider.updateAllWidgets(context);
  }
  private static void authenticate(Activity activity, Runnable success, Runnable failure) {
    if (activity.isDestroyed() || activity.isFinishing() || prompts.containsKey(activity)) {
      if (failure != null) failure.run();
      return;
    }
    BiometricManager manager = activity.getSystemService(BiometricManager.class);
    if (manager == null || manager.canAuthenticate(AUTHENTICATORS) != BiometricManager.BIOMETRIC_SUCCESS) {
      Toast.makeText(activity, R.string.privacy_auth_unavailable, Toast.LENGTH_LONG).show();
      if (failure != null) failure.run();
      return;
    }
    CancellationSignal signal = new CancellationSignal();
    prompts.put(activity, signal);
    new BiometricPrompt.Builder(activity).setTitle(activity.getString(R.string.privacy_unlock))
        .setSubtitle(activity.getString(R.string.privacy_auth_subtitle))
        .setAllowedAuthenticators(AUTHENTICATORS).build()
        .authenticate(signal, activity.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
          @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
            if (prompts.get(activity) != signal || signal.isCanceled() || activity.isDestroyed()) return;
            prompts.remove(activity);
            if (activity.isFinishing() || !resumedActivities.contains(activity)) {
              if (failure != null) failure.run();
              return;
            }
            success.run();
          }
          @Override public void onAuthenticationError(int code, CharSequence message) {
            if (prompts.get(activity) != signal || activity.isDestroyed()) return;
            prompts.remove(activity);
            if (failure != null) failure.run();
          }
        });
  }
  private static void cover(Activity activity) {
    if (covers.containsKey(activity) || activity.isDestroyed() || activity.isFinishing()) return;
    Dialog dialog = new Dialog(activity, android.R.style.Theme_Material_Light_NoActionBar);
    dialog.setCancelable(false);
    FrameLayout content = new FrameLayout(activity);
    content.setBackgroundColor(activity.getColor(R.color.finan_background));
    Button unlock = new Button(activity);
    unlock.setText(R.string.privacy_unlock);
    content.addView(unlock, new FrameLayout.LayoutParams(-2, -2, android.view.Gravity.CENTER));
    unlock.setOnClickListener(v -> unlock(activity));
    dialog.setContentView(content);
    dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    covers.put(activity, dialog);
    dialog.show();
    dialog.getWindow().setLayout(-1, -1);
  }
  private static void releaseCover(Activity activity) {
    Dialog dialog = covers.remove(activity);
    if (dialog != null) dialog.dismiss();
  }
  private static void reconcile(Activity activity) {
    if (activity.isDestroyed() || activity.isFinishing()) return;
    if (com.dwlhm.finan.service.backup.BackupRecoveryGate.isBlocked()) {
      releaseCover(activity);
      return;
    }
    if (enabled(activity)) activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    else activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    if (locked(activity)) cover(activity);
    else { releaseCover(activity); dispatchContinuation(activity); }
  }
  private static void dispatchContinuation(Activity activity) {
    if (com.dwlhm.finan.service.backup.BackupRecoveryGate.isBlocked()) return;
    if (!resumedActivities.contains(activity) || locked(activity)) return;
    Runnable action = continuations.remove(activity);
    if (action != null) action.run();
  }
  private static void unlock(Activity activity) {
    authenticate(activity, () -> {
      session.onAuthenticated();
      reconcile(activity);
      refreshWidgets(activity);
    }, null);
  }
  @Override public void onActivityCreated(Activity activity, Bundle state) {
    if (enabled(activity)) activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  }
  @Override public void onActivityPostCreated(Activity activity, Bundle state) { reconcile(activity); }
  @Override public void onActivityStarted(Activity activity) {
    if (started++ == 0) session.onForeground(SystemClock.elapsedRealtime());
    reconcile(activity);
  }
  @Override public void onActivityResumed(Activity activity) {
    resumedActivities.add(activity);
    reconcile(activity);
    if (!com.dwlhm.finan.service.backup.BackupRecoveryGate.isBlocked() && locked(activity)) unlock(activity);
  }
  @Override public void onActivityPaused(Activity activity) { resumedActivities.remove(activity); }
  @Override public void onActivityStopped(Activity activity) {
    started = Math.max(0, started - 1);
    if (started == 0 && !activity.isChangingConfigurations()) session.onBackground(SystemClock.elapsedRealtime());
  }
  @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
  @Override public void onActivityDestroyed(Activity activity) {
    CancellationSignal signal = prompts.remove(activity);
    if (signal != null) signal.cancel();
    releaseCover(activity);
    resumedActivities.remove(activity);
    continuations.remove(activity);
  }
}
