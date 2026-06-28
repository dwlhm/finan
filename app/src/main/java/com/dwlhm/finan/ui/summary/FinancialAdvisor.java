package com.dwlhm.finan.ui.summary;

import android.content.Context;
import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.MonthlySummary;

public final class FinancialAdvisor {

  public enum AdviceType {
    NO_DATA,
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
    return calculateAdviceDetails(summary, null, null);
  }

  public static AdviceDetails calculateAdviceDetails(
      MonthlySummary summary,
      MonthlySummary prevSummary,
      MonthlySummary prevPrevSummary) {
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

    double expensePercent = (expense * 100.0) / income;
    int percentage = (int) Math.round(expensePercent);

    if (percentage > 80) {
      return new AdviceDetails(AdviceType.HIGH_SPENDING, percentage);
    } else if (percentage > 20) {
      return new AdviceDetails(AdviceType.HEALTHY_SAVINGS, 100 - percentage);
    } else {
      return new AdviceDetails(AdviceType.EXCELLENT_SAVINGS, 100 - percentage);
    }
  }

  public static Advice getAdvice(Context context, MonthlySummary summary) {
    return getAdvice(context, summary, null, null);
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

  public static Advice getAdvice(
      Context context,
      MonthlySummary summary,
      MonthlySummary prevSummary,
      MonthlySummary prevPrevSummary) {
    if (summary == null) {
      return null;
    }

    AdviceDetails details = calculateAdviceDetails(summary, prevSummary, prevPrevSummary);
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
}
