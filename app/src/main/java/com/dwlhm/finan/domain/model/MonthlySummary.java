package com.dwlhm.finan.domain.model;

import java.util.Collections;
import java.util.List;

public final class MonthlySummary {

  private final int year;
  private final int month;
  private final long monthExpenseMinor;
  private final long monthIncomeMinor;
  private final long todayExpenseMinor;
  private final long todayIncomeMinor;
  private final List<CategoryTotal> topExpenseCategories;
  private final List<WalletBalance> walletBalances;

  public MonthlySummary(
      int year,
      int month,
      long monthExpenseMinor,
      long monthIncomeMinor,
      long todayExpenseMinor,
      long todayIncomeMinor,
      List<CategoryTotal> topExpenseCategories,
      List<WalletBalance> walletBalances) {
    this.year = year;
    this.month = month;
    this.monthExpenseMinor = monthExpenseMinor;
    this.monthIncomeMinor = monthIncomeMinor;
    this.todayExpenseMinor = todayExpenseMinor;
    this.todayIncomeMinor = todayIncomeMinor;
    this.topExpenseCategories =
        Collections.unmodifiableList(topExpenseCategories);
    this.walletBalances = Collections.unmodifiableList(walletBalances);
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public long getMonthExpenseMinor() {
    return monthExpenseMinor;
  }

  public long getMonthIncomeMinor() {
    return monthIncomeMinor;
  }

  public long getTodayExpenseMinor() {
    return todayExpenseMinor;
  }

  public long getTodayIncomeMinor() {
    return todayIncomeMinor;
  }

  public List<CategoryTotal> getTopExpenseCategories() {
    return topExpenseCategories;
  }

  public List<WalletBalance> getWalletBalances() {
    return walletBalances;
  }
}
