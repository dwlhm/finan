package com.dwlhm.finan.domain.model;

public final class HistoryTotals {

  private final int count;
  private final long incomeMinor;
  private final long expenseMinor;

  public HistoryTotals(int count, long incomeMinor, long expenseMinor) {
    this.count = count;
    this.incomeMinor = incomeMinor;
    this.expenseMinor = expenseMinor;
  }

  public int getCount() {
    return count;
  }

  public long getIncomeMinor() {
    return incomeMinor;
  }

  public long getExpenseMinor() {
    return expenseMinor;
  }
}
