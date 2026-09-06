package com.dwlhm.finan.domain.model;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ForwardCashFlowSummary {

  private final long currentActualBalanceMinor;
  private final long scheduledInflowMinor;
  private final long scheduledOutflowMinor;
  private final long remainingAfterPlansMinor;
  private final long projectedEndBalanceMinor;
  @NonNull private final List<UpcomingObligation> upcomingObligations;
  private final int horizonDays;
  @NonNull private final LocalDate horizonDate;

  public ForwardCashFlowSummary(
      long currentActualBalanceMinor,
      long scheduledInflowMinor,
      long scheduledOutflowMinor,
      long remainingAfterPlansMinor,
      long projectedEndBalanceMinor,
      @NonNull List<UpcomingObligation> upcomingObligations,
      int horizonDays,
      @NonNull LocalDate horizonDate) {
    this.currentActualBalanceMinor = currentActualBalanceMinor;
    this.scheduledInflowMinor = scheduledInflowMinor;
    this.scheduledOutflowMinor = scheduledOutflowMinor;
    this.remainingAfterPlansMinor = remainingAfterPlansMinor;
    this.projectedEndBalanceMinor = projectedEndBalanceMinor;
    this.upcomingObligations = Collections.unmodifiableList(new ArrayList<>(upcomingObligations));
    this.horizonDays = horizonDays;
    this.horizonDate = horizonDate;
  }

  public long getCurrentActualBalanceMinor() {
    return currentActualBalanceMinor;
  }

  public long getScheduledInflowMinor() {
    return scheduledInflowMinor;
  }

  public long getScheduledOutflowMinor() {
    return scheduledOutflowMinor;
  }

  public long getRemainingAfterPlansMinor() {
    return remainingAfterPlansMinor;
  }

  public long getProjectedEndBalanceMinor() {
    return projectedEndBalanceMinor;
  }

  @NonNull
  public List<UpcomingObligation> getUpcomingObligations() {
    return upcomingObligations;
  }

  public int getHorizonDays() {
    return horizonDays;
  }

  @NonNull
  public LocalDate getHorizonDate() {
    return horizonDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ForwardCashFlowSummary that = (ForwardCashFlowSummary) o;
    return currentActualBalanceMinor == that.currentActualBalanceMinor
        && scheduledInflowMinor == that.scheduledInflowMinor
        && scheduledOutflowMinor == that.scheduledOutflowMinor
        && remainingAfterPlansMinor == that.remainingAfterPlansMinor
        && projectedEndBalanceMinor == that.projectedEndBalanceMinor
        && horizonDays == that.horizonDays
        && Objects.equals(upcomingObligations, that.upcomingObligations)
        && Objects.equals(horizonDate, that.horizonDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        currentActualBalanceMinor,
        scheduledInflowMinor,
        scheduledOutflowMinor,
        remainingAfterPlansMinor,
        projectedEndBalanceMinor,
        upcomingObligations,
        horizonDays,
        horizonDate);
  }
}
