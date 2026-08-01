package com.dwlhm.finan.service.summary;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.SummaryDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.domain.model.CashFlowActivityTotal;
import com.dwlhm.finan.domain.model.CashFlowReport;
import com.dwlhm.finan.domain.model.CashFlowReportResult;
import com.dwlhm.finan.domain.model.CategoryTotal;
import com.dwlhm.finan.util.date.TimeProvider;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CashFlowReportService {

  private final SummaryDao summaryDao;
  private final CategoryDao categoryDao;
  private final WalletDao walletDao;
  private final TimeProvider timeProvider;
  private final ZoneId zoneId;

  public CashFlowReportService(
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

  public CashFlowReportResult buildReport(LocalDate startDate, LocalDate endDate, Long walletId) {
    LocalDate normalizedStart = startDate.isAfter(endDate) ? endDate : startDate;
    LocalDate normalizedEnd = startDate.isAfter(endDate) ? startDate : endDate;

    long startInclusive = toEpochDayStart(normalizedStart);
    long endExclusive = toEpochDayEnd(normalizedEnd);

    List<Wallet> wallets = walletDao.findAll();
    Map<String, List<Wallet>> walletsByCurrency = groupByCurrency(wallets, walletId);

    Map<String, List<CashFlowReport>> reportsByCurrency = new LinkedHashMap<>();
    for (Map.Entry<String, List<Wallet>> entry : walletsByCurrency.entrySet()) {
      String currency = entry.getKey();
      List<Wallet> currencyWallets = entry.getValue();
      List<CashFlowReport> currencyReports = new ArrayList<>();

      if (walletId != null) {
        currencyReports.add(buildSingleReport(normalizedStart, normalizedEnd,
            startInclusive, endExclusive, currency, walletId));
      } else if (currencyWallets.size() == 1) {
        currencyReports.add(buildSingleReport(normalizedStart, normalizedEnd,
            startInclusive, endExclusive, currency, currencyWallets.get(0).getId()));
      } else {
        currencyReports.add(buildCombinedReport(normalizedStart, normalizedEnd,
            startInclusive, endExclusive, currency, currencyWallets));
      }

      reportsByCurrency.put(currency, currencyReports);
    }

    return new CashFlowReportResult(reportsByCurrency);
  }

  private CashFlowReport buildSingleReport(
      LocalDate startDate, LocalDate endDate,
      long startInclusive, long endExclusive,
      String currencyCode, long walletId) {
    long openingBalance = summaryDao.walletBalanceBefore(walletId, startInclusive);
    long closingBalance = summaryDao.walletBalanceBefore(walletId, endExclusive);
    SummaryDao.CashFlowTotalsRow totals =
        summaryDao.cashFlowTotalsBetween(startInclusive, endExclusive, walletId);
    List<SummaryDao.CashFlowAggregateRow> activityRows =
        summaryDao.activityTotalsBetween(startInclusive, endExclusive, walletId);
    List<CategoryTotal> incomeCategories = loadTopCategories("INCOME", startInclusive, endExclusive, walletId, 5);
    List<CategoryTotal> expenseCategories = loadTopCategories("EXPENSE", startInclusive, endExclusive, walletId, 5);
    long zoneOffsetMillis = zoneId.getRules().getOffset(java.time.Instant.now()).getTotalSeconds() * 1000L;
    List<CashFlowReport.WeekSummary> weeks = buildWeekSummaries(startDate, endDate,
        summaryDao.dailyTotalsBetween(startInclusive, endExclusive, walletId, zoneOffsetMillis));

    return verifyInvariant(new CashFlowReport(
        startDate, endDate, currencyCode, walletId,
        openingBalance,
        totals.incomeMinor, totals.expenseMinor,
        totals.transferInMinor, totals.transferOutMinor,
        totals.adjIncreaseMinor, totals.adjDecreaseMinor,
        closingBalance,
        buildActivityTotals(activityRows, walletId),
        incomeCategories, expenseCategories, weeks));
  }

  private CashFlowReport buildCombinedReport(
      LocalDate startDate, LocalDate endDate,
      long startInclusive, long endExclusive,
      String currencyCode, List<Wallet> wallets) {
    long openingBalance = 0L;
    long closingBalance = 0L;
    long income = 0L, expense = 0L;
    long transferIn = 0L, transferOut = 0L;
    long adjInc = 0L, adjDec = 0L;
    List<SummaryDao.CashFlowAggregateRow> allActivityRows = new ArrayList<>();

    for (Wallet w : wallets) {
      openingBalance += summaryDao.walletBalanceBefore(w.getId(), startInclusive);
      closingBalance += summaryDao.walletBalanceBefore(w.getId(), endExclusive);
      SummaryDao.CashFlowTotalsRow t =
          summaryDao.cashFlowTotalsBetween(startInclusive, endExclusive, w.getId());
      income += t.incomeMinor;
      expense += t.expenseMinor;
      transferIn += t.transferInMinor;
      transferOut += t.transferOutMinor;
      adjInc += t.adjIncreaseMinor;
      adjDec += t.adjDecreaseMinor;
      allActivityRows.addAll(
          summaryDao.activityTotalsBetween(startInclusive, endExclusive, w.getId()));
    }

    List<CategoryTotal> incomeCategories = new ArrayList<>();
    for (Wallet w : wallets) {
      mergeCategoryTotals(incomeCategories,
          loadTopCategories("INCOME", startInclusive, endExclusive, w.getId(), 5));
    }
    incomeCategories.sort((a, b) -> Long.compare(b.getTotalMinor(), a.getTotalMinor()));
    if (incomeCategories.size() > 5) incomeCategories = incomeCategories.subList(0, 5);

    List<CategoryTotal> expenseCategories = new ArrayList<>();
    for (Wallet w : wallets) {
      mergeCategoryTotals(expenseCategories,
          loadTopCategories("EXPENSE", startInclusive, endExclusive, w.getId(), 5));
    }
    expenseCategories.sort((a, b) -> Long.compare(b.getTotalMinor(), a.getTotalMinor()));
    if (expenseCategories.size() > 5) expenseCategories = expenseCategories.subList(0, 5);

    long zoneOffsetMillis = zoneId.getRules().getOffset(java.time.Instant.now()).getTotalSeconds() * 1000L;
    List<CashFlowReport.WeekSummary> weeks = buildWeekSummaries(startDate, endDate,
        summaryDao.dailyTotalsBetween(startInclusive, endExclusive, null, zoneOffsetMillis));

    return verifyInvariant(new CashFlowReport(
        startDate, endDate, currencyCode, null,
        openingBalance,
        income, expense,
        transferIn, transferOut,
        adjInc, adjDec,
        closingBalance,
        buildActivityTotals(allActivityRows, null),
        incomeCategories, expenseCategories, weeks));
  }

  private List<CashFlowActivityTotal> buildActivityTotals(
      List<SummaryDao.CashFlowAggregateRow> activityRows, Long walletId) {
    Map<CashFlowActivity, Long> incomeByActivity = new java.util.HashMap<>();
    Map<CashFlowActivity, Long> expenseByActivity = new java.util.HashMap<>();
    Map<CashFlowActivity, List<CategoryTotal>> incomeCatsByActivity = new java.util.HashMap<>();
    Map<CashFlowActivity, List<CategoryTotal>> expenseCatsByActivity = new java.util.HashMap<>();

    for (CashFlowActivity act : CashFlowActivity.values()) {
      incomeByActivity.put(act, 0L);
      expenseByActivity.put(act, 0L);
      incomeCatsByActivity.put(act, new ArrayList<>());
      expenseCatsByActivity.put(act, new ArrayList<>());
    }

    Map<Long, String> categoryNames = new java.util.HashMap<>();
    Map<Long, String> categoryActivities = new java.util.HashMap<>();

    for (SummaryDao.CashFlowAggregateRow row : activityRows) {
      CashFlowActivity activity;
      try {
        activity = row.cashFlowActivity != null
            ? CashFlowActivity.valueOf(row.cashFlowActivity)
            : CashFlowActivity.UNCLASSIFIED;
      } catch (IllegalArgumentException e) {
        activity = CashFlowActivity.UNCLASSIFIED;
      }

      long amt = row.totalMinor;
      if ("INCOME".equals(row.type)) {
        incomeByActivity.put(activity, incomeByActivity.get(activity) + amt);
      } else {
        expenseByActivity.put(activity, expenseByActivity.get(activity) + amt);
      }

      if (row.categoryId > 0) {
        String name = categoryNames.get(row.categoryId);
        if (name == null) {
          Category cat = categoryDao.findById(row.categoryId);
          name = cat != null ? cat.getName() : ("#" + row.categoryId);
          categoryNames.put(row.categoryId, name);
        }
        CategoryTotal ct = new CategoryTotal(row.categoryId, name, amt, "INCOME".equals(row.type));
        if ("INCOME".equals(row.type)) {
          incomeCatsByActivity.get(activity).add(ct);
        } else {
          expenseCatsByActivity.get(activity).add(ct);
        }
      }
    }

    List<CashFlowActivityTotal> result = new ArrayList<>();
    for (CashFlowActivity act : CashFlowActivity.values()) {
      long inc = incomeByActivity.get(act);
      long exp = expenseByActivity.get(act);
      if (inc > 0 || exp > 0) {
        List<CategoryTotal> incCats = incomeCatsByActivity.get(act);
        incCats.sort((a, b) -> Long.compare(b.getTotalMinor(), a.getTotalMinor()));
        List<CategoryTotal> expCats = expenseCatsByActivity.get(act);
        expCats.sort((a, b) -> Long.compare(b.getTotalMinor(), a.getTotalMinor()));
        result.add(new CashFlowActivityTotal(act, inc, exp, incCats, expCats));
      }
    }
    return result;
  }

  private List<CategoryTotal> loadTopCategories(
      String type, long startInclusive, long endExclusive, Long walletId, int limit) {
    List<SummaryDao.CategorySumRow> rows =
        summaryDao.categoryTotalsBetween(type, startInclusive, endExclusive, walletId, limit);
    List<CategoryTotal> result = new ArrayList<>();
    for (SummaryDao.CategorySumRow row : rows) {
      if (row.categoryId <= 0) continue;
      Category cat = categoryDao.findById(row.categoryId);
      String name = cat != null ? cat.getName() : ("#" + row.categoryId);
      result.add(new CategoryTotal(row.categoryId, name, row.totalMinor, "INCOME".equals(type)));
    }
    return result;
  }

  private List<CashFlowReport.WeekSummary> buildWeekSummaries(
      LocalDate periodStart, LocalDate periodEnd,
      List<SummaryDao.DailyTotalRow> dailyRows) {
      java.util.Map<Long, SummaryDao.DailyTotalRow> byDay = new java.util.LinkedHashMap<>();
      for (SummaryDao.DailyTotalRow row : dailyRows) {
        byDay.put(row.dateMillis, row);
      }
  
      List<CashFlowReport.WeekSummary> weeks = new ArrayList<>();
      List<CashFlowReport.DailyTotal> currentDays = new ArrayList<>();
      
      boolean isYearly = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd) > 300;
      
      if (isYearly) {
          // Aggregate by month, but return 12 WeekSummaries
          for (int month = 1; month <= 12; month++) {
              LocalDate monthStart = LocalDate.of(periodStart.getYear(), month, 1);
              LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
              List<CashFlowReport.DailyTotal> weekTotals = new ArrayList<>();
              
              LocalDate weekStart = null;
              long weekIncome = 0L;
              long weekExpense = 0L;
              for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
                  long dayMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli();
                  SummaryDao.DailyTotalRow row = byDay.get(dayMillis);
                  if (row != null) {
                      weekIncome += row.incomeMinor;
                      weekExpense += row.expenseMinor;
                  }
                  if (weekStart == null) weekStart = date;
                  if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY || date.equals(monthEnd)) {
                      weekTotals.add(new CashFlowReport.DailyTotal(weekStart.atStartOfDay(zoneId).toInstant().toEpochMilli(), weekIncome, weekExpense));
                      weekIncome = 0;
                      weekExpense = 0;
                      weekStart = null;
                  }
              }
              weeks.add(new CashFlowReport.WeekSummary(monthStart, monthEnd, weekTotals));
          }
      } else {
          LocalDate weekStart = null;
          for (LocalDate date = periodStart; !date.isAfter(periodEnd); date = date.plusDays(1)) {
            long dayMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli();
            SummaryDao.DailyTotalRow row = byDay.get(dayMillis);
            long income = row != null ? row.incomeMinor : 0L;
            long expense = row != null ? row.expenseMinor : 0L;
      
            if (weekStart == null) weekStart = date;
            currentDays.add(new CashFlowReport.DailyTotal(dayMillis, income, expense));
      
            if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY || date.equals(periodEnd)) {
              weeks.add(new CashFlowReport.WeekSummary(weekStart, date, new ArrayList<>(currentDays)));
              currentDays.clear();
              weekStart = null;
            }
          }
      }
  
      return weeks;
    }

  private static void mergeCategoryTotals(List<CategoryTotal> target, List<CategoryTotal> source) {
    for (CategoryTotal st : source) {
      boolean found = false;
      for (int i = 0; i < target.size(); i++) {
        if (target.get(i).getCategoryId() == st.getCategoryId()) {
          target.set(i, new CategoryTotal(st.getCategoryId(), st.getCategoryName(),
              target.get(i).getTotalMinor() + st.getTotalMinor(), st.isIncome()));
          found = true;
          break;
        }
      }
      if (!found) {
        target.add(st);
      }
    }
  }

  private static Map<String, List<Wallet>> groupByCurrency(List<Wallet> wallets, Long walletId) {
    Map<String, List<Wallet>> grouped = new LinkedHashMap<>();
    for (Wallet w : wallets) {
      if (walletId != null && w.getId() != walletId) continue;
      String code = w.getCurrencyCode() != null ? w.getCurrencyCode() : "IDR";
      grouped.computeIfAbsent(code, k -> new ArrayList<>()).add(w);
    }
    return grouped;
  }

  private CashFlowReport verifyInvariant(CashFlowReport report) {
    long expectedChange =
        report.getNetCashFlowMinor() + report.getNetTransferMinor() + report.getNetAdjustmentMinor();
    long actualChange = report.getClosingBalanceMinor() - report.getOpeningBalanceMinor();
    if (expectedChange != actualChange) {
      android.util.Log.e("CashFlowReport",
          "Invariant failed: closing - opening (" + actualChange
          + ") != netCashFlow + netTransfer + netAdjustment (" + expectedChange
          + ") for wallet=" + report.getWalletId()
          + " currency=" + report.getCurrencyCode());
    }
    if (report.getWalletId() == null) {
      long netTransfer = report.getNetTransferMinor();
      if (netTransfer != 0) {
        android.util.Log.w("CashFlowReport",
            "Combined report has non-zero net transfer: " + netTransfer
            + " for currency=" + report.getCurrencyCode());
      }
    }
    return report;
  }

  private long toEpochDayStart(LocalDate date) {
    return date.atStartOfDay(zoneId).toInstant().toEpochMilli();
  }

  private long toEpochDayEnd(LocalDate date) {
    return date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
  }
}
