package com.dwlhm.finan.util.date;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Inclusive date range for payroll cycles.
 *
 * <p>Start and end are both inclusive calendar days. This matches the mental model used
 * throughout the UI, where a range is shown as "29 Jun 2026 → 28 Jul 2026".
 */
public final class DateRange {

  private final LocalDate start;
  private final LocalDate end;

  public DateRange(LocalDate start, LocalDate end) {
    this.start = Objects.requireNonNull(start, "start");
    this.end = Objects.requireNonNull(end, "end");
  }

  public LocalDate getStart() {
    return start;
  }

  public LocalDate getEnd() {
    return end;
  }

  public boolean contains(LocalDate date) {
    return !date.isBefore(start) && !date.isAfter(end);
  }

  public long days() {
    return start.until(end).getDays() + 1L;
  }

  public DateRange shiftMonths(int months) {
    return new DateRange(start.plusMonths(months), end.plusMonths(months));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DateRange)) return false;
    DateRange other = (DateRange) o;
    return start.equals(other.start) && end.equals(other.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return start + " → " + end;
  }
}
