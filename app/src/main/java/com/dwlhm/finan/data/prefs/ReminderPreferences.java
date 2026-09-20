package com.dwlhm.finan.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/** Device-local reminder consent and forward-only daily delivery marker. */
public final class ReminderPreferences {
  private static final String FILE = "finan_reminders";
  private static final String ENABLED = "enabled";
  private static final String LAST_NOTIFIED = "last_notified_epoch_day";
  private static final String CONSENT_GENERATION = "consent_generation";
  private final SharedPreferences preferences;

  /** Opens device-local reminder preferences, disabled until explicitly enabled. */
  public ReminderPreferences(Context context) {
    preferences = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
  }

  /** Returns the user's current opt-in state. */
  public boolean isEnabled() { return preferences.getBoolean(ENABLED, false); }

  /** Updates consent without resetting the daily delivery marker. */
  public void setEnabled(boolean enabled) {
    if (isEnabled() == enabled) return;
    preferences.edit().putBoolean(ENABLED, enabled)
        .putLong(CONSENT_GENERATION, getConsentGeneration() + 1).apply();
  }

  /** Identifies the consent that authorized a pending read, including an off/on transition. */
  public long getConsentGeneration() { return preferences.getLong(CONSENT_GENERATION, 0); }

  /** Returns the last delivered local epoch day, or the sentinel before first delivery. */
  public long getLastNotifiedEpochDay() { return preferences.getLong(LAST_NOTIFIED, Long.MIN_VALUE); }

  /** Records a successful delivery. */
  public void setLastNotifiedEpochDay(long day) { preferences.edit().putLong(LAST_NOTIFIED, day).apply(); }
}
