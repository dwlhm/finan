package com.dwlhm.finan.service.balance;

import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.service.summary.SummaryService;

import java.time.LocalDate;

public final class MonthlyBalanceCalculator {

  private final SummaryService summaryService;

  public MonthlyBalanceCalculator(SummaryService summaryService) {
    this.summaryService = summaryService;
  }

  public Result calculate(
      LocalDate start,
      LocalDate end,
      LocalDate prevStart,
      LocalDate prevEnd,
      Long walletId,
      Long categoryId) {
    MonthlySummary current = summaryService.loadRange(start, end, walletId, categoryId);
    MonthlySummary prev = summaryService.loadRange(prevStart, prevEnd, walletId, categoryId);
    return from(current, prev);
  }

  public Result from(MonthlySummary current, MonthlySummary prev) {
    long incomeMinor = current.getMonthIncomeMinor();
    long expenseMinor = current.getMonthExpenseMinor();
    long netMinor = incomeMinor - expenseMinor;
    long prevNetMinor = prev == null ? 0L : prev.getMonthIncomeMinor() - prev.getMonthExpenseMinor();
    int trendPercent = 0;
    if (prevNetMinor != 0L) {
      trendPercent = (int) Math.round(((double) (netMinor - prevNetMinor) / Math.abs(prevNetMinor)) * 100);
    } else if (netMinor > 0) {
      trendPercent = 100;
    } else if (netMinor < 0) {
      trendPercent = -100;
    }
    return new Result(incomeMinor, expenseMinor, netMinor, prevNetMinor, trendPercent);
  }

  public static final class Result {
    private final long incomeMinor;
    private final long expenseMinor;
    private final long netMinor;
    private final long prevNetMinor;
    private final int trendPercent;

    Result(long incomeMinor, long expenseMinor, long netMinor, long prevNetMinor, int trendPercent) {
      this.incomeMinor = incomeMinor;
      this.expenseMinor = expenseMinor;
      this.netMinor = netMinor;
      this.prevNetMinor = prevNetMinor;
      this.trendPercent = trendPercent;
    }

    public long getIncomeMinor() {
      return incomeMinor;
    }

    public long getExpenseMinor() {
      return expenseMinor;
    }

    public long getNetMinor() {
      return netMinor;
    }

    public long getPrevNetMinor() {
      return prevNetMinor;
    }

    public int getTrendPercent() {
      return trendPercent;
    }
  }
}
