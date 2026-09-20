package com.dwlhm.finan.service.reminder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.Test;

public final class ReminderPolicyTest {
  private final LocalDate today = LocalDate.of(2031, 4, 18);
  private final ZoneId zone = ZoneId.of("Asia/Jakarta");

  @Test public void unhandledDueOnFirstEnabledDayNotifies() {
    assertTrue(allowed(true, true, today, zone, Long.MIN_VALUE, true));
  }

  @Test public void disabledDeniedAndEmptyNeverNotify() {
    assertFalse(allowed(false, true, today, zone, Long.MIN_VALUE, true));
    assertFalse(allowed(true, false, today, zone, Long.MIN_VALUE, true));
    assertFalse(allowed(true, true, today, zone, Long.MIN_VALUE, false));
  }

  @Test public void dateOrZoneChangeDuringQueryDiscardsResult() {
    assertFalse(allowed(true, true, today.plusDays(1), zone, Long.MIN_VALUE, true));
    assertFalse(allowed(true, true, today, ZoneId.of("Europe/Paris"), Long.MIN_VALUE, true));
  }

  @Test public void sameDayAndClockRollbackDoNotRepeatButNextDayDoes() {
    assertFalse(allowed(true, true, today, zone, today.toEpochDay(), true));
    assertFalse(allowed(true, true, today, zone, today.plusDays(3).toEpochDay(), true));
    assertTrue(allowed(true, true, today, zone, today.minusDays(1).toEpochDay(), true));
  }

  private boolean allowed(boolean enabled, boolean permission, LocalDate currentDate,
      ZoneId currentZone, long lastDay, boolean due) {
    return ReminderPolicy.shouldNotify(enabled, permission, today, zone,
        currentDate, currentZone, lastDay, due);
  }
}
