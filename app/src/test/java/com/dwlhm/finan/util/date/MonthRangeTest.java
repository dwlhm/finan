package com.dwlhm.finan.util.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

public class MonthRangeTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");

  @Test
  public void forMonth_coversFullCalendarMonth() {
    MonthRange range = MonthRange.forMonth(2026, 5, ZONE);
    LocalDate first = LocalDate.of(2026, 5, 1);
    LocalDate last = LocalDate.of(2026, 6, 1);
    long start = first.atStartOfDay(ZONE).toInstant().toEpochMilli();
    long end = last.atStartOfDay(ZONE).toInstant().toEpochMilli();
    assertEquals(start, range.getStartInclusive());
    assertEquals(end, range.getEndExclusive());
  }

  @Test
  public void forDay_isTwentyFourHours() {
    LocalDate day = LocalDate.of(2026, 5, 26);
    MonthRange range = MonthRange.forDay(day, ZONE);
    assertTrue(range.getEndExclusive() > range.getStartInclusive());
    assertEquals(86_400_000L, range.getEndExclusive() - range.getStartInclusive());
  }
}
