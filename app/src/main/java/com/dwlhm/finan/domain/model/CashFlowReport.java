package com.dwlhm.finan.domain.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public final class CashFlowReport {

  public static final class DailyTotal {
    private final long dateMillis;
    private final long incomeMinor;
    private final long expenseMinor;

    public DailyTotal(long dateMillis, long incomeMinor, long expenseMinor) {
      this.dateMillis = dateMillis;
      this.incomeMinor = incomeMinor;
      this.expenseMinor = expenseMinor;
    }

    public long getDateMillis() { return dateMillis; }
    public long getIncomeMinor() { return incomeMinor; }
    public long getExpenseMinor() { return expenseMinor; }
  }

  public static final class WeekSummary {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<DailyTotal> days;

    public WeekSummary(LocalDate startDate, LocalDate endDate, List<DailyTotal> days) {
      this.startDate = startDate;
      this.endDate = endDate;
      this.days = Collections.unmodifiableList(days);
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public List<DailyTotal> getDays() { return days; }

    public long getWeekIncome() {
      long total = 0L;
      for (DailyTotal d : days) total += d.incomeMinor;
      return total;
    }

    public long getWeekExpense() {
      long total = 0L;
      for (DailyTotal d : days) total += d.expenseMinor;
      return total;
    }
  }
  private final LocalDate startDate;
  private final LocalDate endDate;
  private final String currencyCode;
  private final Long walletId;
  private final long openingBalanceMinor;
  private final long incomeMinor;
  private final long expenseMinor;
  private final long transferInMinor;
  private final long transferOutMinor;
  private final long adjustmentIncreaseMinor;
  private final long adjustmentDecreaseMinor;
  private final long closingBalanceMinor;
  private final List<CashFlowActivityTotal> activityTotals;
  private final List<CategoryTotal> incomeCategories;
  private final List<CategoryTotal> expenseCategories;
  private final long netCashFlowMinor;
  private final long netTransferMinor;
  private final long netAdjustmentMinor;
  private final long balanceChangeMinor;
  private final List<WeekSummary> weekSummaries;

  public CashFlowReport(
      LocalDate startDate,
      LocalDate endDate,
      String currencyCode,
      Long walletId,
      long openingBalanceMinor,
      long incomeMinor,
      long expenseMinor,
      long transferInMinor,
      long transferOutMinor,
      long adjustmentIncreaseMinor,
      long adjustmentDecreaseMinor,
      long closingBalanceMinor,
      List<CashFlowActivityTotal> activityTotals,
      List<CategoryTotal> incomeCategories,
      List<CategoryTotal> expenseCategories,
      List<WeekSummary> weekSummaries) {
    this.startDate = startDate;
    this.endDate = endDate;
    this.currencyCode = currencyCode;
    this.walletId = walletId;
    this.openingBalanceMinor = openingBalanceMinor;
    this.incomeMinor = incomeMinor;
    this.expenseMinor = expenseMinor;
    this.transferInMinor = transferInMinor;
    this.transferOutMinor = transferOutMinor;
    this.adjustmentIncreaseMinor = adjustmentIncreaseMinor;
    this.adjustmentDecreaseMinor = adjustmentDecreaseMinor;
    this.closingBalanceMinor = closingBalanceMinor;
    this.activityTotals = Collections.unmodifiableList(activityTotals);
    this.incomeCategories = Collections.unmodifiableList(incomeCategories);
    this.expenseCategories = Collections.unmodifiableList(expenseCategories);
    this.weekSummaries = weekSummaries != null ? Collections.unmodifiableList(weekSummaries) : List.of();
    this.netCashFlowMinor = incomeMinor - expenseMinor;
    this.netTransferMinor = transferInMinor - transferOutMinor;
    this.netAdjustmentMinor = adjustmentIncreaseMinor - adjustmentDecreaseMinor;
    this.balanceChangeMinor = netCashFlowMinor + netTransferMinor + netAdjustmentMinor;
  }

  public LocalDate getStartDate() { return startDate; }
  public LocalDate getEndDate() { return endDate; }
  public String getCurrencyCode() { return currencyCode; }
  public Long getWalletId() { return walletId; }
  public long getOpeningBalanceMinor() { return openingBalanceMinor; }
  public long getIncomeMinor() { return incomeMinor; }
  public long getExpenseMinor() { return expenseMinor; }
  public long getTransferInMinor() { return transferInMinor; }
  public long getTransferOutMinor() { return transferOutMinor; }
  public long getAdjustmentIncreaseMinor() { return adjustmentIncreaseMinor; }
  public long getAdjustmentDecreaseMinor() { return adjustmentDecreaseMinor; }
  public long getClosingBalanceMinor() { return closingBalanceMinor; }
  public List<CashFlowActivityTotal> getActivityTotals() { return activityTotals; }
  public List<CategoryTotal> getIncomeCategories() { return incomeCategories; }
  public List<CategoryTotal> getExpenseCategories() { return expenseCategories; }
  public long getNetCashFlowMinor() { return netCashFlowMinor; }
  public long getNetTransferMinor() { return netTransferMinor; }
  public long getNetAdjustmentMinor() { return netAdjustmentMinor; }
  public long getBalanceChangeMinor() { return balanceChangeMinor; }

  public List<WeekSummary> getWeekSummaries() { return weekSummaries; }

  public String getHeadlineLabel() {
    if (netCashFlowMinor > 0) return "Pemasukan lebih besar";
    if (netCashFlowMinor < 0) return "Pengeluaran lebih besar";
    return "Pemasukan dan pengeluaran seimbang";
  }
}
