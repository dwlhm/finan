package com.dwlhm.finan.util.date;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;

public class PayrollCycleResolverTest {

  @Test
  public void forMonth_cutoff1_usesCalendarMonth() {
    DateRange range = PayrollCycleResolver.forMonth(2026, 7, 1);
    assertEquals(LocalDate.of(2026, 7, 1), range.getStart());
    assertEquals(LocalDate.of(2026, 7, 31), range.getEnd());
  }

  @Test
  public void forMonth_cutoff5_sameMonthAnchor() {
    DateRange range = PayrollCycleResolver.forMonth(2026, 7, 5);
    assertEquals(LocalDate.of(2026, 7, 5), range.getStart());
    assertEquals(LocalDate.of(2026, 8, 4), range.getEnd());
  }

  @Test
  public void forMonth_cutoff15_sameMonthAnchor() {
    DateRange range = PayrollCycleResolver.forMonth(2026, 7, 15);
    assertEquals(LocalDate.of(2026, 7, 15), range.getStart());
    assertEquals(LocalDate.of(2026, 8, 14), range.getEnd());
  }

  @Test
  public void forMonth_cutoff29_previousMonthAnchor() {
    // July financial month: 29 June - 28 July
    DateRange range = PayrollCycleResolver.forMonth(2026, 7, 29);
    assertEquals(LocalDate.of(2026, 6, 29), range.getStart());
    assertEquals(LocalDate.of(2026, 7, 28), range.getEnd());
  }

  @Test
  public void forMonth_cutoff31_clampsToEndOfPreviousMonth() {
    // 31 June is invalid, so June has 30 days; July cycle starts 30 June
    DateRange range = PayrollCycleResolver.forMonth(2026, 7, 31);
    assertEquals(LocalDate.of(2026, 6, 30), range.getStart());
    assertEquals(LocalDate.of(2026, 7, 29), range.getEnd());
  }

  @Test
  public void forDate_onCutoffDay_belongsToNextCycle() {
    // 29 July with cutoff 29 belongs to August
    DateRange range = PayrollCycleResolver.forDate(LocalDate.of(2026, 7, 29), 29);
    assertEquals(LocalDate.of(2026, 7, 29), range.getStart());
    assertEquals(LocalDate.of(2026, 8, 28), range.getEnd());
  }

  @Test
  public void forDate_dayBeforeCutoff_belongsToCurrentLabelledMonth() {
    // 28 July with cutoff 29 belongs to July
    DateRange range = PayrollCycleResolver.forDate(LocalDate.of(2026, 7, 28), 29);
    assertEquals(LocalDate.of(2026, 6, 29), range.getStart());
    assertEquals(LocalDate.of(2026, 7, 28), range.getEnd());
  }

  @Test
  public void forYear_cutoff29_spansTwelveCycles() {
    DateRange range = PayrollCycleResolver.forYear(2026, 29);
    assertEquals(LocalDate.of(2025, 12, 29), range.getStart());
    assertEquals(LocalDate.of(2026, 12, 28), range.getEnd());
  }

  @Test
  public void forYear_cutoff1_spansCalendarYear() {
    DateRange range = PayrollCycleResolver.forYear(2026, 1);
    assertEquals(LocalDate.of(2026, 1, 1), range.getStart());
    assertEquals(LocalDate.of(2026, 12, 31), range.getEnd());
  }

  @Test
  public void baseMonthForToday_cutoff29_onCutoffDay_returnsNextMonth() {
    LocalDate base = PayrollCycleResolver.baseMonthForToday(LocalDate.of(2026, 7, 29), 29);
    assertEquals(LocalDate.of(2026, 8, 1), base);
  }

  @Test
  public void baseMonthForToday_cutoff29_beforeCutoff_returnsCurrentMonth() {
    LocalDate base = PayrollCycleResolver.baseMonthForToday(LocalDate.of(2026, 7, 28), 29);
    assertEquals(LocalDate.of(2026, 7, 1), base);
  }

  @Test
  public void shiftMonths_movesToAdjacentCycle() {
    DateRange prev = PayrollCycleResolver.shiftMonths(LocalDate.of(2026, 7, 28), 29, -1);
    assertEquals(LocalDate.of(2026, 5, 29), prev.getStart());
    assertEquals(LocalDate.of(2026, 6, 28), prev.getEnd());
  }

  @Test
  public void shiftMonths_lastBusinessDay_fromAugustToSeptember() {
    // August financial month: 31 July -> 30 August
    // Shifting +1 from August start should land in September, not October.
    DateRange next = PayrollCycleResolver.shiftMonths(LocalDate.of(2026, 7, 31), -1, 1);
    assertEquals(LocalDate.of(2026, 8, 31), next.getStart());
    assertEquals(LocalDate.of(2026, 9, 29), next.getEnd());
  }

  @Test
  public void shiftMonths_lastBusinessDay_fromAugustToJuly() {
    DateRange prev = PayrollCycleResolver.shiftMonths(LocalDate.of(2026, 7, 31), -1, -1);
    assertEquals(LocalDate.of(2026, 6, 30), prev.getStart());
    assertEquals(LocalDate.of(2026, 7, 29), prev.getEnd());
  }
}
