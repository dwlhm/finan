package com.dwlhm.finan.service.summary;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.SummaryDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.domain.model.CategoryTotal;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.domain.model.WalletBalance;
import com.dwlhm.finan.util.date.MonthRange;
import com.dwlhm.finan.util.date.TimeProvider;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class SummaryService {

  private final SummaryDao summaryDao;
  private final CategoryDao categoryDao;
  private final WalletDao walletDao;
  private final TimeProvider timeProvider;
  private final ZoneId zoneId;

  public SummaryService(
      SummaryDao summaryDao,
      CategoryDao categoryDao,
      WalletDao walletDao,
      TimeProvider timeProvider,
      ZoneId zoneId) {
    this.summaryDao = summaryDao;
    this.categoryDao = categoryDao;
    this.walletDao = walletDao;
    this.timeProvider = timeProvider;
    this.zoneId = zoneId;
  }

  public MonthlySummary loadCurrentMonth() {
    LocalDate today = InstantToLocalDate(timeProvider.currentTimeMillis(), zoneId);
    return loadMonth(today.getYear(), today.getMonthValue(), today);
  }

  public MonthlySummary loadRange(LocalDate startDate, LocalDate endDate) {
    return loadRange(startDate, endDate, null, null);
  }

  public MonthlySummary loadRange(
      LocalDate startDate, LocalDate endDate, Long walletId, Long categoryId) {
    LocalDate normalizedStart = startDate.isAfter(endDate) ? endDate : startDate;
    LocalDate normalizedEnd = startDate.isAfter(endDate) ? startDate : endDate;
    MonthRange startRange = MonthRange.forDay(normalizedStart, zoneId);
    MonthRange endRange = MonthRange.forDay(normalizedEnd, zoneId);
    long startInclusive = startRange.getStartInclusive();
    long endExclusive = endRange.getEndExclusive();

    List<SummaryDao.CashFlowAggregateRow> rows =
        summaryDao.getCashFlowCategoryTotals(startInclusive, endExclusive, walletId, categoryId);

    long totalIncome = 0;
    long totalExpense = 0;

    java.util.Map<CashFlowActivity, Long> activityInflows = new java.util.HashMap<>();
    java.util.Map<CashFlowActivity, Long> activityOutflows = new java.util.HashMap<>();
    java.util.Map<CashFlowActivity, List<CategoryTotal>> activityCategories = new java.util.HashMap<>();

    for (CashFlowActivity act : CashFlowActivity.values()) {
      activityInflows.put(act, 0L);
      activityOutflows.put(act, 0L);
      activityCategories.put(act, new ArrayList<>());
    }

    for (SummaryDao.CashFlowAggregateRow row : rows) {
      CashFlowActivity activity;
      try {
        activity = row.cashFlowActivity != null ? CashFlowActivity.valueOf(row.cashFlowActivity) : CashFlowActivity.UNCLASSIFIED;
      } catch (IllegalArgumentException e) {
        activity = CashFlowActivity.UNCLASSIFIED;
      }

      boolean isIncome = "INCOME".equals(row.type);
      long amt = row.totalMinor;

      if (isIncome) {
        totalIncome += amt;
        activityInflows.put(activity, activityInflows.get(activity) + amt);
      } else {
        totalExpense += amt;
        activityOutflows.put(activity, activityOutflows.get(activity) + amt);
      }

      Category category = row.categoryId > 0 ? categoryDao.findById(row.categoryId) : null;
      String name = category != null ? category.getName() : (row.categoryId > 0 ? "#" + row.categoryId : "Lainnya");
      activityCategories.get(activity).add(new CategoryTotal(row.categoryId, name, amt));
    }

    List<MonthlySummary.ActivitySummary> activitySummaries = new ArrayList<>();
    for (CashFlowActivity act : CashFlowActivity.values()) {
      long inflow = activityInflows.get(act);
      long outflow = activityOutflows.get(act);
      List<CategoryTotal> cats = activityCategories.get(act);

      if (inflow > 0 || outflow > 0) {
        activitySummaries.add(new MonthlySummary.ActivitySummary(act, inflow, outflow, cats));
      }
    }

    List<WalletBalance> balances = new ArrayList<>();
    for (Wallet wallet : walletDao.findAll()) {
      if (walletId != null && wallet.getId() != walletId) {
        continue;
      }
      balances.add(
          new WalletBalance(
              wallet.getId(),
              wallet.getName(),
              summaryDao.walletBalanceBefore(wallet.getId(), endExclusive)));
    }

    return new MonthlySummary(
        normalizedStart.getYear(),
        normalizedStart.getMonthValue(),
        totalExpense,
        totalIncome,
        activitySummaries,
        balances);
  }

  public MonthlySummary loadMonth(int year, int month, LocalDate today) {
    LocalDate start = LocalDate.of(year, month, 1);
    LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
    return loadRange(start, end);
  }

  private static LocalDate InstantToLocalDate(long epochMillis, ZoneId zoneId) {
    return java.time.Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate();
  }
}
