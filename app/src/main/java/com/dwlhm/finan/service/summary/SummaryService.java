package com.dwlhm.finan.service.summary;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.SummaryDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.CategoryTotal;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.domain.model.TransactionType;
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
    LocalDate today =
        InstantToLocalDate(timeProvider.currentTimeMillis(), zoneId);
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

    long expense =
        summaryDao.sumByTypeBetween(
            TransactionType.EXPENSE.name(), startInclusive, endExclusive, walletId, categoryId);
    long income =
        summaryDao.sumByTypeBetween(
            TransactionType.INCOME.name(), startInclusive, endExclusive, walletId, categoryId);

    List<CategoryTotal> topCategories = new ArrayList<>();
    for (SummaryDao.CategorySumRow row :
        summaryDao.expenseByCategory(startInclusive, endExclusive, walletId, categoryId)) {
      Category category = categoryDao.findById(row.categoryId);
      String name = category != null ? category.getName() : "#" + row.categoryId;
      topCategories.add(new CategoryTotal(row.categoryId, name, row.totalMinor));
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
        expense,
        income,
        expense,
        income,
        topCategories,
        balances);
  }

  public MonthlySummary loadMonth(int year, int month, LocalDate today) {
    MonthRange monthRange = MonthRange.forMonth(year, month, zoneId);
    MonthRange dayRange = MonthRange.forDay(today, zoneId);

    long monthExpense =
        summaryDao.sumByTypeBetween(
            TransactionType.EXPENSE.name(), monthRange.getStartInclusive(), monthRange.getEndExclusive());
    long monthIncome =
        summaryDao.sumByTypeBetween(
            TransactionType.INCOME.name(), monthRange.getStartInclusive(), monthRange.getEndExclusive());
    long todayExpense =
        summaryDao.sumByTypeBetween(
            TransactionType.EXPENSE.name(), dayRange.getStartInclusive(), dayRange.getEndExclusive());
    long todayIncome =
        summaryDao.sumByTypeBetween(
            TransactionType.INCOME.name(), dayRange.getStartInclusive(), dayRange.getEndExclusive());

    List<CategoryTotal> topCategories = new ArrayList<>();
    for (SummaryDao.CategorySumRow row :
        summaryDao.expenseByCategory(monthRange.getStartInclusive(), monthRange.getEndExclusive())) {
      Category category = categoryDao.findById(row.categoryId);
      String name = category != null ? category.getName() : "#" + row.categoryId;
      topCategories.add(new CategoryTotal(row.categoryId, name, row.totalMinor));
    }

    List<WalletBalance> balances = new ArrayList<>();
    for (Wallet wallet : walletDao.findAll()) {
      balances.add(
          new WalletBalance(
              wallet.getId(),
              wallet.getName(),
              summaryDao.walletBalanceBefore(wallet.getId(), dayRange.getEndExclusive())));
    }

    return new MonthlySummary(
        year,
        month,
        monthExpense,
        monthIncome,
        todayExpense,
        todayIncome,
        topCategories,
        balances);
  }

  private static LocalDate InstantToLocalDate(long epochMillis, ZoneId zoneId) {
    return java.time.Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate();
  }
}
