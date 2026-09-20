package com.dwlhm.finan.service.reminder;

import java.time.LocalDate;
import java.time.ZoneId;

/** Pure eligibility boundary for a freshly queried due-date reminder. */
public final class ReminderPolicy {
  private ReminderPolicy() {}

  /** Allows delivery only for an unchanged local day/zone with consent and an unhandled due item. */
  public static boolean shouldNotify(boolean enabled, boolean notificationsAllowed,
      LocalDate queriedDate, ZoneId queriedZone, LocalDate currentDate, ZoneId currentZone,
      long lastNotifiedEpochDay, boolean hasUnhandledDue) {
    return enabled && notificationsAllowed && hasUnhandledDue
        && queriedDate.equals(currentDate) && queriedZone.equals(currentZone)
        && currentDate.toEpochDay() > lastNotifiedEpochDay;
  }
}
