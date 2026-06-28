package com.dwlhm.finan.ui.summary;

import android.content.Context;
import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.MonthlySummary;

public final class FinancialAdvisor {

  public enum AdviceType {
    NO_DATA,
    OVERSPENDING,
    HIGH_SPENDING,
    HEALTHY_SAVINGS
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
    if (summary == null) {
      return new AdviceDetails(AdviceType.NO_DATA, 0);
    }

    long income = summary.getMonthIncomeMinor();
    long expense = summary.getMonthExpenseMinor();

    if (income == 0 && expense == 0) {
      return new AdviceDetails(AdviceType.NO_DATA, 0);
    }

    if (expense > income) {
      return new AdviceDetails(AdviceType.OVERSPENDING, 0);
    }

    double expensePercent = (expense * 100.0) / income;
    if (expensePercent > 80.0) {
      return new AdviceDetails(AdviceType.HIGH_SPENDING, (int) Math.round(expensePercent));
    }

    double savingsPercent = 100.0 - expensePercent;
    return new AdviceDetails(AdviceType.HEALTHY_SAVINGS, (int) Math.round(savingsPercent));
  }

  public static Advice getAdvice(Context context, MonthlySummary summary) {
    if (summary == null) {
      return null;
    }

    AdviceDetails details = calculateAdviceDetails(summary);
    switch (details.type) {
      case OVERSPENDING:
        return new Advice(
            context.getString(R.string.advice_overspending_title),
            context.getString(R.string.advice_overspending_msg),
            R.drawable.ic_error,
            R.color.finan_error,
            R.drawable.bg_panel_emphasis_error
        );
      case HIGH_SPENDING:
        return new Advice(
            context.getString(R.string.advice_high_spending_title),
            context.getString(R.string.advice_high_spending_msg, details.percentage),
            R.drawable.ic_error,
            R.color.finan_warm_accent,
            R.drawable.bg_panel_emphasis
        );
      case HEALTHY_SAVINGS:
        return new Advice(
            context.getString(R.string.advice_healthy_title),
            context.getString(R.string.advice_healthy_msg, details.percentage),
            R.drawable.ic_check,
            R.color.finan_income,
            R.drawable.bg_panel_emphasis_success
        );
      case NO_DATA:
      default:
        return new Advice(
            context.getString(R.string.advice_no_data_title),
            context.getString(R.string.advice_no_data_msg),
            R.drawable.ic_schedule,
            R.color.finan_text_secondary,
            R.drawable.bg_card
        );
    }
  }
}
