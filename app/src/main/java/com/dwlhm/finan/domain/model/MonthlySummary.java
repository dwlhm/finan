package com.dwlhm.finan.domain.model;

import java.util.Collections;
import java.util.List;

public final class MonthlySummary {

  private final int year;
  private final int month;
  private final long totalExpenseMinor;
  private final long totalIncomeMinor;
  private final long netFlowMinor;
  private final List<ActivitySummary> activitySummaries;
  private final List<WalletBalance> walletBalances;

  public MonthlySummary(
      int year,
      int month,
      long totalExpenseMinor,
      long totalIncomeMinor,
      List<ActivitySummary> activitySummaries,
      List<WalletBalance> walletBalances) {
    this.year = year;
    this.month = month;
    this.totalExpenseMinor = totalExpenseMinor;
    this.totalIncomeMinor = totalIncomeMinor;
    this.netFlowMinor = totalIncomeMinor - totalExpenseMinor;
    this.activitySummaries = Collections.unmodifiableList(activitySummaries);
    this.walletBalances = Collections.unmodifiableList(walletBalances);
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public long getMonthExpenseMinor() {
    return totalExpenseMinor;
  }

  public long getMonthIncomeMinor() {
    return totalIncomeMinor;
  }

  public long getTodayExpenseMinor() {
    return totalExpenseMinor;
  }

  public long getTodayIncomeMinor() {
    return totalIncomeMinor;
  }

  public long getNetFlowMinor() {
    return netFlowMinor;
  }

  public List<ActivitySummary> getActivitySummaries() {
    return activitySummaries;
  }

  public List<WalletBalance> getWalletBalances() {
    return walletBalances;
  }

  public List<CategoryTotal> getTopExpenseCategories() {
    java.util.List<CategoryTotal> combined = new java.util.ArrayList<>();
    for (ActivitySummary act : activitySummaries) {
      combined.addAll(act.getTopCategories());
    }
    return combined;
  }

  public static final class ActivitySummary {
    private final CashFlowActivity activity;
    private final long inflowMinor;
    private final long outflowMinor;
    private final long netFlowMinor;
    private final List<CategoryTotal> topCategories;

    public ActivitySummary(
        CashFlowActivity activity,
        long inflowMinor,
        long outflowMinor,
        List<CategoryTotal> topCategories) {
      this.activity = activity;
      this.inflowMinor = inflowMinor;
      this.outflowMinor = outflowMinor;
      this.netFlowMinor = inflowMinor - outflowMinor;
      this.topCategories = Collections.unmodifiableList(topCategories);
    }

    public CashFlowActivity getActivity() {
      return activity;
    }

    public long getInflowMinor() {
      return inflowMinor;
    }

    public long getOutflowMinor() {
      return outflowMinor;
    }

    public long getNetFlowMinor() {
      return netFlowMinor;
    }

    public List<CategoryTotal> getTopCategories() {
      return topCategories;
    }
  }
}
