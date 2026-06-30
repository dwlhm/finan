package com.dwlhm.finan.ui.summary;

import android.content.Context;
import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.MonthlySummary;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class FinancialAdvisor {

  public enum AdviceType {
    NO_DATA,
    EARLY_PERIOD,
    ONE_THIRD,
    HALFWAY,
    OVERSPENDING,
    OVERSPENDING_CONSECUTIVE,
    HIGH_SPENDING,
    HEALTHY_SAVINGS,
    EXCELLENT_SAVINGS
  }

  public static final class AdviceDetails {
    public final AdviceType type;
    public final int percentage;

    public AdviceDetails(AdviceType type, int percentage) {
      this.type = type;
      this.percentage = percentage;
    }
  }

  public static final class Advice {
    public final String title;
    public final String message;
    public final int iconRes;
    public final int colorRes;
    public final int bgRes;

    public Advice(String title, String message, int iconRes, int colorRes, int bgRes) {
      this.title = title;
      this.message = message;
      this.iconRes = iconRes;
      this.colorRes = colorRes;
      this.bgRes = bgRes;
    }
  }

  public static AdviceDetails calculateAdviceDetails(MonthlySummary summary) {
    return calculateAdviceDetails(summary, null, null, 1.0);
  }

  public static AdviceDetails calculateAdviceDetails(
      MonthlySummary summary,
      MonthlySummary prevSummary,
      MonthlySummary prevPrevSummary) {
    return calculateAdviceDetails(summary, prevSummary, prevPrevSummary, 1.0);
  }

  public static AdviceDetails calculateAdviceDetails(
      MonthlySummary summary,
      MonthlySummary prevSummary,
      MonthlySummary prevPrevSummary,
      double dayProgress) {
    if (summary == null) {
      return new AdviceDetails(AdviceType.NO_DATA, 0);
    }

    long income = summary.getMonthIncomeMinor();
    long expense = summary.getMonthExpenseMinor();

    if (income == 0 && expense == 0) {
      return new AdviceDetails(AdviceType.NO_DATA, 0);
    }

    if (expense > income) {
      boolean prevDeficit = prevSummary != null
          && prevSummary.getMonthExpenseMinor() > prevSummary.getMonthIncomeMinor();
      boolean prevPrevDeficit = prevPrevSummary != null
          && prevPrevSummary.getMonthExpenseMinor() > prevPrevSummary.getMonthIncomeMinor();

      if (prevDeficit && prevPrevDeficit) {
        return new AdviceDetails(AdviceType.OVERSPENDING_CONSECUTIVE, 0);
      }
      return new AdviceDetails(AdviceType.OVERSPENDING, 0);
    }

    double effectiveProgress = Math.max(dayProgress, 0.05);
    double expensePercent = (expense * 100.0) / income;
    double normalizedPercent = Math.min(100.0, expensePercent / effectiveProgress);
    int percentage = (int) Math.round(normalizedPercent);

    int savings = Math.max(0, 100 - percentage);
    if (percentage > 80) {
      return new AdviceDetails(AdviceType.HIGH_SPENDING, percentage);
    } else if (dayProgress < 0.33) {
      return new AdviceDetails(AdviceType.EARLY_PERIOD, 0);
    } else if (dayProgress < 0.50) {
      return new AdviceDetails(AdviceType.ONE_THIRD, 0);
    } else if (dayProgress < 0.67) {
      return new AdviceDetails(AdviceType.HALFWAY, 0);
    } else if (percentage > 20) {
      return new AdviceDetails(AdviceType.HEALTHY_SAVINGS, savings);
    } else {
      return new AdviceDetails(AdviceType.EXCELLENT_SAVINGS, savings);
    }
  }

  public static Advice getAdvice(Context context, MonthlySummary summary) {
    return getAdvice(context, summary, null, null, null, null);
  }

  public static Advice getAdvice(
      Context context,
      MonthlySummary summary,
      MonthlySummary prevSummary,
      MonthlySummary prevPrevSummary) {
    return getAdvice(context, summary, prevSummary, prevPrevSummary, null, null);
  }

  public static Advice getAdvice(
      Context context,
      MonthlySummary summary,
      MonthlySummary prevSummary,
      MonthlySummary prevPrevSummary,
      LocalDate startDate,
      LocalDate endDate) {
    if (summary == null) {
      return null;
    }

    double dayProgress = 1.0;
    if (startDate != null && endDate != null) {
      long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
      LocalDate today = LocalDate.now();
      long elapsed = ChronoUnit.DAYS.between(startDate, today) + 1;
      if (elapsed < 0) elapsed = 0;
      if (elapsed > totalDays) elapsed = totalDays;
      dayProgress = totalDays > 0 ? (double) elapsed / totalDays : 1.0;
    }

    AdviceDetails details = calculateAdviceDetails(summary, prevSummary, prevPrevSummary, dayProgress);
    String baseMessage;
    int iconRes;
    int colorRes;
    int bgRes;
    String title;

    switch (details.type) {
      case OVERSPENDING_CONSECUTIVE:
        title = context.getString(R.string.advice_overspending_consecutive_title);
        baseMessage = context.getString(R.string.advice_overspending_consecutive_msg);
        iconRes = R.drawable.ic_error;
        colorRes = R.color.finan_error;
        bgRes = R.drawable.bg_panel_emphasis_error;
        break;
      case OVERSPENDING:
        title = context.getString(R.string.advice_overspending_title);
        baseMessage = context.getString(R.string.advice_overspending_msg);
        iconRes = R.drawable.ic_error;
        colorRes = R.color.finan_error;
        bgRes = R.drawable.bg_panel_emphasis_error;
        break;
      case HIGH_SPENDING:
        title = context.getString(R.string.advice_high_spending_title);
        baseMessage = context.getString(R.string.advice_high_spending_msg, details.percentage);
        iconRes = R.drawable.ic_error;
        colorRes = R.color.finan_warm_accent;
        bgRes = R.drawable.bg_panel_emphasis;
        break;
      case HEALTHY_SAVINGS:
        title = context.getString(R.string.advice_healthy_title);
        baseMessage = context.getString(R.string.advice_healthy_msg, details.percentage);
        iconRes = R.drawable.ic_check;
        colorRes = R.color.finan_income;
        bgRes = R.drawable.bg_panel_emphasis_success;
        break;
      case EXCELLENT_SAVINGS:
        title = context.getString(R.string.advice_excellent_title);
        baseMessage = context.getString(R.string.advice_excellent_msg, details.percentage);
        iconRes = R.drawable.ic_check;
        colorRes = R.color.finan_income;
        bgRes = R.drawable.bg_panel_emphasis_success;
        break;
      case EARLY_PERIOD:
        title = context.getString(R.string.advice_early_title);
        baseMessage = context.getString(R.string.advice_early_msg);
        iconRes = R.drawable.ic_schedule;
        colorRes = R.color.finan_text_secondary;
        bgRes = R.drawable.bg_card;
        break;
      case ONE_THIRD:
        title = context.getString(R.string.advice_onethird_title);
        baseMessage = context.getString(R.string.advice_onethird_msg);
        iconRes = R.drawable.ic_schedule;
        colorRes = R.color.finan_text_secondary;
        bgRes = R.drawable.bg_card;
        break;
      case HALFWAY:
        title = context.getString(R.string.advice_halfway_title);
        baseMessage = context.getString(R.string.advice_halfway_msg);
        iconRes = R.drawable.ic_schedule;
        colorRes = R.color.finan_text_secondary;
        bgRes = R.drawable.bg_card;
        break;
      case NO_DATA:
      default:
        title = context.getString(R.string.advice_no_data_title);
        baseMessage = context.getString(R.string.advice_no_data_msg);
        iconRes = R.drawable.ic_schedule;
        colorRes = R.color.finan_text_secondary;
        bgRes = R.drawable.bg_card;
        break;
    }

    String riseMessage = getHighestRiseCategoryMessage(context, summary, prevSummary);
    String fullMessage = baseMessage + (riseMessage.isEmpty() ? "" : riseMessage);

    return new Advice(title, fullMessage, iconRes, colorRes, bgRes);
  }

  public static String getHighestRiseCategoryMessage(Context context, MonthlySummary summary, MonthlySummary prevSummary) {
    if (summary == null || prevSummary == null) {
      return "";
    }

    java.util.Map<Long, Long> prevExpenses = new java.util.HashMap<>();
    for (com.dwlhm.finan.domain.model.CategoryTotal cat : prevSummary.getTopExpenseCategories()) {
      if (!cat.isIncome() && cat.getCategoryId() > 0) {
        prevExpenses.put(cat.getCategoryId(), cat.getTotalMinor());
      }
    }

    String highestRiseCategoryName = null;
    double highestRisePercent = 0.0;
    long highestRiseDiff = 0L;

    for (com.dwlhm.finan.domain.model.CategoryTotal cat : summary.getTopExpenseCategories()) {
      if (cat.isIncome() || cat.getCategoryId() <= 0) {
        continue;
      }
      long current = cat.getTotalMinor();
      Long prev = prevExpenses.get(cat.getCategoryId());
      if (prev != null && prev >= 50000L && current > prev && (current - prev) >= 25000L) {
        double percent = ((current - prev) * 100.0) / prev;
        if (percent > highestRisePercent) {
          highestRisePercent = percent;
          highestRiseCategoryName = cat.getCategoryName();
          highestRiseDiff = current - prev;
        }
      }
    }

    if (highestRiseCategoryName != null && highestRisePercent >= 1.0) {
      int roundedPercent = (int) Math.round(highestRisePercent);
      String diffFormatted = com.dwlhm.finan.util.money.MoneyFormatter.format(highestRiseDiff);
      return context.getString(R.string.advice_category_rise_template, highestRiseCategoryName, roundedPercent, diffFormatted);
    }

    return "";
  }

}
