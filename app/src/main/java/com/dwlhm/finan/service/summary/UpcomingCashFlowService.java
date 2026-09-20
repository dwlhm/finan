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
import com.dwlhm.finan.domain.model.Horizon;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  public HorizonWindow resolveHorizon(
      @NonNull Horizon horizon, @NonNull LocalDate today, @NonNull ZoneId zone) {
    if (zone == null) {
      throw new IllegalArgumentException("zone must not be null");
    }
    switch (horizon) {
      case SEVEN:
        return new HorizonWindow(today, today.plusDays(6));
      case THIRTY:
        return new HorizonWindow(today, today.plusDays(29));
      case MONTH_END:
      default:
        return new HorizonWindow(today, YearMonth.from(today).atEndOfMonth());
    }
  }

  public static final class HorizonWindow {
    @NonNull private final LocalDate fromDate;
    @NonNull private final LocalDate toDate;

    public HorizonWindow(@NonNull LocalDate fromDate, @NonNull LocalDate toDate) {
      this.fromDate = fromDate;
      this.toDate = toDate;
    }

    @NonNull
    public LocalDate getFromDate() {
      return fromDate;
    }

    @NonNull
    public LocalDate getToDate() {
      return toDate;
    }
  }

  @NonNull
  public ForwardCashFlowSummary calculateForwardSummary(
      @NonNull LocalDate fromDate,
      @NonNull LocalDate toDate,
      @Nullable Long walletFilterId) {

    if (fromDate == null || toDate == null || fromDate.isAfter(toDate)
        || fromDate.getYear() < 1900 || toDate.getYear() > 9999
        || ChronoUnit.DAYS.between(fromDate, toDate) > 3660) {
      throw new IllegalArgumentException("Invalid forecast horizon (maximum ten years)");
    }
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

    Map<Long, Wallet> walletById = new HashMap<>();
    for (Wallet wallet : allWallets) {
      walletById.put(wallet.getId(), wallet);
    }
    Map<Long, Category> categoryById = new HashMap<>();
    for (Category category : categoryDao.findAllOrdered()) {
      categoryById.put(category.getId(), category);
    }

    List<TransactionTemplate> scheduledTemplates = transactionTemplateDao.findScheduledTemplates();
    List<UpcomingObligation> upcomingObligations = new ArrayList<>();

    long scheduledInflowMinor = 0L;
    long scheduledOutflowMinor = 0L;

    for (TransactionTemplate template : scheduledTemplates) {
      boolean transfer = template.getType() == TransactionType.TRANSFER_OUT;
      boolean destinationSelected = transfer && walletFilterId != null
          && walletFilterId.equals(template.getDestinationWalletId());
      if (walletFilterId != null && template.getWalletId() != null
          && !template.getWalletId().equals(walletFilterId) && !destinationSelected) continue;

      List<LocalDate> candidateDates = findOccurrences(template, fromDate, toDate);
      List<String> candidateDateStrings = new ArrayList<>(candidateDates.size());
      for (LocalDate candidateDate : candidateDates) {
        candidateDateStrings.add(candidateDate.toString());
      }
      Set<String> handled =
          candidateDateStrings.isEmpty()
              ? Collections.emptySet()
              : transactionTemplateDao.handledOccurrences(
                  template.getId(), candidateDateStrings);
      boolean hasHistory = transactionTemplateDao.hasOccurrenceHistory(template.getId());
      for (LocalDate candidateDate : candidateDates) {
        if (handled.contains(candidateDate.toString())
            || (!hasHistory && isLegacyHandled(template, candidateDate))) continue;
        long dueEpochMillis = candidateDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
        int daysRemaining = (int) ChronoUnit.DAYS.between(today, candidateDate);

        String walletName = null;
        if (template.getWalletId() != null) {
          Wallet w = walletById.get(template.getWalletId());
          if (w != null) {
            walletName = w.getName();
          }
        }

        String categoryName = null;
        if (template.getCategoryId() != null) {
          Category c = categoryById.get(template.getCategoryId());
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

        if (template.getType() == TransactionType.INCOME || destinationSelected) {
          scheduledInflowMinor += template.getAmountMinor();
        } else if (template.getType() == TransactionType.EXPENSE || (template.getType() == TransactionType.TRANSFER_OUT && walletFilterId != null)) {
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

  private boolean isLegacyHandled(TransactionTemplate template, LocalDate candidate) {
    if (template.getLastRecordedAt() <= 0) return false;
    LocalDate recorded = Instant.ofEpochMilli(template.getLastRecordedAt()).atZone(zoneId).toLocalDate();
    switch (template.getFrequency()) {
      case DAILY: return candidate.equals(recorded);
      case WEEKLY:
        return candidate.with(java.time.DayOfWeek.MONDAY)
            .equals(recorded.with(java.time.DayOfWeek.MONDAY));
      case YEARLY: return candidate.getYear() == recorded.getYear();
      default: return YearMonth.from(candidate).equals(YearMonth.from(recorded));
    }
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
        int month = template.getDueMonth();
        if (month < 1 || month > 12) continue;
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
