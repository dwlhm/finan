package com.dwlhm.finan.domain.model;

import java.util.Collections;
import java.util.List;

public final class CashFlowActivityTotal {
  private final CashFlowActivity activity;
  private final long incomeMinor;
  private final long expenseMinor;
  private final long netMinor;
  private final List<CategoryTotal> incomeCategories;
  private final List<CategoryTotal> expenseCategories;

  public CashFlowActivityTotal(
      CashFlowActivity activity,
      long incomeMinor,
      long expenseMinor,
      List<CategoryTotal> incomeCategories,
      List<CategoryTotal> expenseCategories) {
    this.activity = activity;
    this.incomeMinor = incomeMinor;
    this.expenseMinor = expenseMinor;
    this.netMinor = incomeMinor - expenseMinor;
    this.incomeCategories = Collections.unmodifiableList(incomeCategories);
    this.expenseCategories = Collections.unmodifiableList(expenseCategories);
  }

  public CashFlowActivity getActivity() { return activity; }
  public long getIncomeMinor() { return incomeMinor; }
  public long getExpenseMinor() { return expenseMinor; }
  public long getNetMinor() { return netMinor; }
  public List<CategoryTotal> getIncomeCategories() { return incomeCategories; }
  public List<CategoryTotal> getExpenseCategories() { return expenseCategories; }
}
