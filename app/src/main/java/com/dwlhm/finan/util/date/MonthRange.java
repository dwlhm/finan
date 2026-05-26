package com.dwlhm.finan.util.date;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class MonthRange {

  private final long startInclusive;
  private final long endExclusive;

  public MonthRange(long startInclusive, long endExclusive) {
    this.startInclusive = startInclusive;
    this.endExclusive = endExclusive;
  }

  public long getStartInclusive() {
    return startInclusive;
  }

  public long getEndExclusive() {
    return endExclusive;
  }

  public static MonthRange forMonth(int year, int month, ZoneId zoneId) {
    ZonedDateTime start = LocalDate.of(year, month, 1).atStartOfDay(zoneId);
    ZonedDateTime end = start.plusMonths(1);
    return new MonthRange(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli());
  }

  public static MonthRange forDay(LocalDate day, ZoneId zoneId) {
    ZonedDateTime start = day.atStartOfDay(zoneId);
    ZonedDateTime end = start.plusDays(1);
    return new MonthRange(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli());
  }

  public static LocalDate today(ZoneId zoneId) {
    return Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zoneId).toLocalDate();
  }
}
