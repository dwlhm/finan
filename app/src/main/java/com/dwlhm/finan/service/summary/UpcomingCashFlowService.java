package com.dwlhm.finan.service.summary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.SummaryDao;
import com.dwlhm.finan.data.dao.TransactionTemplateDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.ForwardCashFlowSummary;
import com.dwlhm.finan.domain.model.RecurringFrequency;
import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.domain.model.UpcomingObligation;
import com.dwlhm.finan.util.date.TimeProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class UpcomingCashFlowService {

  private final TransactionTemplateDao transactionTemplateDao;
  private final WalletDao walletDao;
  private final CategoryDao categoryDao;
  private final SummaryDao summaryDao;
  private final TimeProvider timeProvider;
  private final ZoneId zoneId;

  public UpcomingCashFlowService(
      TransactionTemplateDao transactionTemplateDao,
      WalletDao walletDao,
      CategoryDao categoryDao,
      SummaryDao summaryDao,
      TimeProvider timeProvider,
      ZoneId zoneId) {
    this.transactionTemplateDao = transactionTemplateDao;
    this.walletDao = walletDao;
    this.categoryDao = categoryDao;
    this.summaryDao = summaryDao;
    this.timeProvider = timeProvider;
    this.zoneId = zoneId;
  }

  @NonNull
  public ForwardCashFlowSummary calculateForwardSummary(
      @NonNull LocalDate fromDate,
      @NonNull LocalDate toDate,
      @Nullable Long walletFilterId) {

    LocalDate today = Instant.ofEpochMilli(timeProvider.currentTimeMillis()).atZone(zoneId).toLocalDate();
    long asOfMillis = timeProvider.currentTimeMillis();

    long currentActualBalanceMinor = 0L;
    List<Wallet> allWallets = walletDao.findAll();
    for (Wallet wallet : allWallets) {
      if (walletFilterId != null && wallet.getId() != walletFilterId) {
        continue;
      }
      currentActualBalanceMinor += summaryDao.walletBalanceBefore(wallet.getId(), asOfMillis);
    }

    long cycleStartMillis = fromDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
    long cycleEndMillis = toDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli();

    List<TransactionTemplate> scheduledTemplates = transactionTemplateDao.findScheduledTemplates();
    List<UpcomingObligation> upcomingObligations = new ArrayList<>();

    long scheduledInflowMinor = 0L;
    long scheduledOutflowMinor = 0L;

    for (TransactionTemplate template : scheduledTemplates) {
      if (walletFilterId != null && template.getWalletId() != null && !template.getWalletId().equals(walletFilterId)) {
        continue;
      }

      if (template.getLastRecordedAt() >= cycleStartMillis && template.getLastRecordedAt() < cycleEndMillis) {
        continue;
      }

      List<LocalDate> candidateDates = findOccurrences(template, fromDate, toDate);
      for (LocalDate candidateDate : candidateDates) {
        long dueEpochMillis = candidateDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
        int daysRemaining = (int) ChronoUnit.DAYS.between(today, candidateDate);

        String walletName = null;
        if (template.getWalletId() != null) {
          Wallet w = walletDao.findById(template.getWalletId());
          if (w != null) {
            walletName = w.getName();
          }
        }

        String categoryName = null;
        if (template.getCategoryId() != null) {
          Category c = categoryDao.findById(template.getCategoryId());
          if (c != null) {
            categoryName = c.getName();
          }
        }

        UpcomingObligation obligation =
            new UpcomingObligation(
                template.getId(),
                template.getName(),
                template.getType(),
                template.getAmountMinor(),
                template.getWalletId(),
                walletName,
                template.getCategoryId(),
                categoryName,
                template.getIcon(),
                dueEpochMillis,
                template.getDueDay(),
                daysRemaining,
                template.getFrequency());

        upcomingObligations.add(obligation);

        if (template.getType() == TransactionType.INCOME) {
          scheduledInflowMinor += template.getAmountMinor();
        } else if (template.getType() == TransactionType.EXPENSE || template.getType() == TransactionType.TRANSFER_OUT) {
          scheduledOutflowMinor += template.getAmountMinor();
        }
      }
    }

    upcomingObligations.sort((a, b) -> {
      int cmp = Long.compare(a.getDueEpochMillis(), b.getDueEpochMillis());
      if (cmp != 0) return cmp;
      return Long.compare(a.getTemplateId(), b.getTemplateId());
    });

    int horizonDays = (int) ChronoUnit.DAYS.between(today, toDate);
    if (horizonDays < 0) horizonDays = 0;

    long remainingAfterPlansMinor = currentActualBalanceMinor - scheduledOutflowMinor;
    long projectedEndBalanceMinor = currentActualBalanceMinor + scheduledInflowMinor - scheduledOutflowMinor;

    return new ForwardCashFlowSummary(
        currentActualBalanceMinor,
        scheduledInflowMinor,
        scheduledOutflowMinor,
        remainingAfterPlansMinor,
        projectedEndBalanceMinor,
        upcomingObligations,
        horizonDays,
        toDate);
  }

  private List<LocalDate> findOccurrences(
      TransactionTemplate template, LocalDate fromDate, LocalDate toDate) {
    List<LocalDate> dates = new ArrayList<>();
    RecurringFrequency frequency = template.getFrequency();
    if (frequency == null || frequency == RecurringFrequency.NONE || frequency == RecurringFrequency.MONTHLY) {
      YearMonth startYm = YearMonth.from(fromDate);
      YearMonth endYm = YearMonth.from(toDate);
      YearMonth currentYm = startYm;
      while (!currentYm.isAfter(endYm)) {
        int day = template.getDueDay();
        if (day <= 0) day = 1;
        int effectiveDay = Math.min(day, currentYm.lengthOfMonth());
        LocalDate candidate = currentYm.atDay(effectiveDay);
        if (!candidate.isBefore(fromDate) && !candidate.isAfter(toDate)) {
          dates.add(candidate);
        }
        currentYm = currentYm.plusMonths(1);
      }
    } else if (frequency == RecurringFrequency.WEEKLY) {
      int targetDayOfWeek = template.getDueDay();
      if (targetDayOfWeek < 1 || targetDayOfWeek > 7) targetDayOfWeek = 1;
      LocalDate current = fromDate;
      while (!current.isAfter(toDate)) {
        if (current.getDayOfWeek().getValue() == targetDayOfWeek) {
          dates.add(current);
        }
        current = current.plusDays(1);
      }
    } else if (frequency == RecurringFrequency.DAILY) {
      LocalDate current = fromDate;
      while (!current.isAfter(toDate)) {
        dates.add(current);
        current = current.plusDays(1);
      }
    } else if (frequency == RecurringFrequency.YEARLY) {
      int startYear = fromDate.getYear();
      int endYear = toDate.getYear();
      for (int yr = startYear; yr <= endYear; yr++) {
        int month = fromDate.getMonthValue();
        int day = template.getDueDay() > 0 ? template.getDueDay() : 1;
        int maxDays = YearMonth.of(yr, month).lengthOfMonth();
        LocalDate candidate = LocalDate.of(yr, month, Math.min(day, maxDays));
        if (!candidate.isBefore(fromDate) && !candidate.isAfter(toDate)) {
          dates.add(candidate);
        }
      }
    }
    return dates;
  }
}
