package com.dwlhm.finan.util.date;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Resolves financial months based on a payroll cycle.
 *
 * <p>Finan does not use calendar months. A "month" is the period between two pay days.
 * For example, if the user is paid on the 29th, the July financial month runs from
 * 29 June (inclusive) to 28 July (inclusive). Any transaction on 29 July or later
 * belongs to August.
 *
 * <p>Cut-off rules:
 * <ul>
 *   <li>{@code 1}  - calendar month (1st to last day of month).</li>
 *   <li>{@code -1} - last business day of the previous month to the day before the
 *                    last business day of the current month.</li>
 *   <li>{@code 2..15} - same-month cutoff (e.g. the 5th means 5th to 4th next month).</li>
 *   <li>{@code 16..31} - previous-month cutoff (e.g. the 29th means 29th previous month
 *                        to 28th current month). This keeps the label intuitive: "July"
 *                        contains the bulk of July days even when the cycle starts in June.</li>
 * </ul>
 */
public final class PayrollCycleResolver {

  private PayrollCycleResolver() {
  }

  /**
   * Returns the financial-month range that contains the given date.
   */
  public static DateRange forDate(LocalDate date, int cutoffDay) {
    LocalDate start = cycleStartForDate(date, cutoffDay);
    return new DateRange(start, endForStart(start, cutoffDay));
  }

  /**
   * Returns the financial-month range for the labelled calendar month.
   * The label is interpreted as: "the month whose calendar name should be used
   * for this cycle". For a 29th cutoff, July 2026 is the cycle 29 Jun 2026 - 28 Jul 2026.
   */
  public static DateRange forMonth(int year, int month, int cutoffDay) {
    LocalDate start = cycleStartForMonth(year, month, cutoffDay);
    return new DateRange(start, endForStart(start, cutoffDay));
  }

  /**
   * Returns the financial-year range for the labelled calendar year.
   * A financial year is the 12 consecutive cycles starting with the cycle for
   * January of that label year.
   */
  public static DateRange forYear(int year, int cutoffDay) {
    LocalDate yearStart = cycleStartForMonth(year, 1, cutoffDay);
    LocalDate nextYearStart = yearStart.plusMonths(12);
    return new DateRange(yearStart, nextYearStart.minusDays(1));
  }

  /**
   * Returns the labelled base month for the dashboard pager.
   * For a late-month or last-business-day cutoff the label is one calendar month
   * ahead of the cycle start, so the user sees the month that most of the cycle
   * days belong to.
   */
  public static LocalDate baseMonthForToday(LocalDate today, int cutoffDay) {
    LocalDate cycleStart = cycleStartForDate(today, cutoffDay);
    LocalDate labelMonth = (cutoffDay == 1) ? cycleStart : cycleStart.plusMonths(1);
    return LocalDate.of(labelMonth.getYear(), labelMonth.getMonthValue(), 1);
  }

  /**
   * Returns the start of the cycle that contains the given date.
   */
  public static LocalDate cycleStartForDate(LocalDate date, int cutoffDay) {
    if (cutoffDay == 1) {
      return date.withDayOfMonth(1);
    }
    if (cutoffDay == -1) {
      LocalDate thisMonthCutoff = lastBusinessDayOfMonth(date);
      if (date.isBefore(thisMonthCutoff)) {
        return lastBusinessDayOfMonth(date.minusMonths(1));
      }
      return thisMonthCutoff;
    }
    LocalDate candidate = date.withDayOfMonth(cutoffDay);
    if (date.isBefore(candidate)) {
      candidate = candidate.minusMonths(1);
    }
    return candidate;
  }

  /**
   * Returns the start of the cycle labelled by the given calendar month/year.
   */
  public static LocalDate cycleStartForMonth(int year, int month, int cutoffDay) {
    LocalDate anchor = LocalDate.of(year, month, 1);
    if (cutoffDay == 1) {
      return anchor;
    }
    if (cutoffDay == -1) {
      return lastBusinessDayOfMonth(anchor.minusMonths(1));
    }
    if (cutoffDay > 15) {
      LocalDate previousMonth = anchor.minusMonths(1);
      int safeDay = Math.min(cutoffDay, previousMonth.lengthOfMonth());
      return previousMonth.withDayOfMonth(safeDay);
    }
    return anchor.withDayOfMonth(cutoffDay);
  }

  /**
   * Returns the inclusive end of a cycle given its start.
   */
  public static LocalDate endForStart(LocalDate start, int cutoffDay) {
    return start.plusMonths(1).minusDays(1);
  }

  /**
   * Shifts the cycle containing {@code anyDateInCycle} by a whole number of months.
   * This recomputes the target cycle from the payroll rules, so it remains correct
   * for floating cutoffs such as the last business day of the month.
   */
  public static DateRange shiftMonths(LocalDate anyDateInCycle, int cutoffDay, int months) {
    LocalDate shiftedDate = anyDateInCycle.plusMonths(months);
    return forDate(shiftedDate, cutoffDay);
  }

  /**
   * Returns the last business day of the month for the given date.
   */
  public static LocalDate lastBusinessDayOfMonth(LocalDate date) {
    LocalDate lastDay = date.withDayOfMonth(date.lengthOfMonth());
    DayOfWeek dow = lastDay.getDayOfWeek();
    if (dow == DayOfWeek.SATURDAY) {
      return lastDay.minusDays(1);
    }
    if (dow == DayOfWeek.SUNDAY) {
      return lastDay.minusDays(2);
    }
    return lastDay;
  }
}
