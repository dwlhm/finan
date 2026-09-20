package com.dwlhm.finan.ui.summary;

import static org.junit.Assert.assertEquals;
import com.dwlhm.finan.domain.model.MonthlySummary;
import java.time.YearMonth;
import java.util.Collections;
import org.junit.Test;

public final class FinancialAdvisorTest {
@Test
  public void testNullSummary() {
    FinancialAdvisor.AdviceDetails details = FinancialAdvisor.calculateAdviceDetails(null);
    assertEquals(FinancialAdvisor.AdviceType.NO_DATA, details.type);
    assertEquals(0, details.percentage);
  }

  @Test
  public void testNoData() {
    MonthlySummary summary = new MonthlySummary(2026, 6, 0L, 0L, Collections.emptyList(), Collections.emptyList());
    FinancialAdvisor.AdviceDetails details = FinancialAdvisor.calculateAdviceDetails(summary);
    assertEquals(FinancialAdvisor.AdviceType.NO_DATA, details.type);
    assertEquals(0, details.percentage);
  }

  @Test
  public void testOverspending() {
    MonthlySummary summary = new MonthlySummary(2026, 6, 1000L, 500L, Collections.emptyList(), Collections.emptyList());
    FinancialAdvisor.AdviceDetails details = FinancialAdvisor.calculateAdviceDetails(summary);
    assertEquals(FinancialAdvisor.AdviceType.OVERSPENDING, details.type);
    assertEquals(0, details.percentage);
  }

  @Test
  public void testHighSpending() {
    // 90% spending
    MonthlySummary summary = new MonthlySummary(2026, 6, 900L, 1000L, Collections.emptyList(), Collections.emptyList());
    FinancialAdvisor.AdviceDetails details = FinancialAdvisor.calculateAdviceDetails(summary);
    assertEquals(FinancialAdvisor.AdviceType.HIGH_SPENDING, details.type);
    assertEquals(90, details.percentage);
  }

  @Test
  public void testHealthySavings() {
    // 40% spending -> 60% savings
    MonthlySummary summary = new MonthlySummary(2026, 6, 400L, 1000L, Collections.emptyList(), Collections.emptyList());
    FinancialAdvisor.AdviceDetails details = FinancialAdvisor.calculateAdviceDetails(summary);
    assertEquals(FinancialAdvisor.AdviceType.HEALTHY_SAVINGS, details.type);
    assertEquals(60, details.percentage);
  }

  @Test
  public void testExcellentSavings() {
    // 15% spending -> 85% savings
    MonthlySummary summary = new MonthlySummary(2026, 6, 150L, 1000L, Collections.emptyList(), Collections.emptyList());
    FinancialAdvisor.AdviceDetails details = FinancialAdvisor.calculateAdviceDetails(summary);
    assertEquals(FinancialAdvisor.AdviceType.EXCELLENT_SAVINGS, details.type);
    assertEquals(85, details.percentage);
  }

  @Test
  public void testConsecutiveDeficit() {
    // Current month: Deficit
    MonthlySummary summary = new MonthlySummary(2026, 6, 1200L, 1000L, Collections.emptyList(), Collections.emptyList());
    // Prev month: Deficit
    MonthlySummary prev = new MonthlySummary(2026, 5, 1100L, 1000L, Collections.emptyList(), Collections.emptyList());
    // Prev Prev month: Deficit
    MonthlySummary prevPrev = new MonthlySummary(2026, 4, 1300L, 1000L, Collections.emptyList(), Collections.emptyList());

    FinancialAdvisor.AdviceDetails details = FinancialAdvisor.calculateAdviceDetails(summary, prev, prevPrev);
    assertEquals(FinancialAdvisor.AdviceType.OVERSPENDING_CONSECUTIVE, details.type);
  }

  @Test
  public void testNonConsecutiveDeficit() {
    // Current month: Deficit
    MonthlySummary summary = new MonthlySummary(2026, 6, 1200L, 1000L, Collections.emptyList(), Collections.emptyList());
    // Prev month: Deficit
    MonthlySummary prev = new MonthlySummary(2026, 5, 1100L, 1000L, Collections.emptyList(), Collections.emptyList());
    // Prev Prev month: Healthy
    MonthlySummary prevPrev = new MonthlySummary(2026, 4, 800L, 1000L, Collections.emptyList(), Collections.emptyList());

    FinancialAdvisor.AdviceDetails details = FinancialAdvisor.calculateAdviceDetails(summary, prev, prevPrev);
    assertEquals(FinancialAdvisor.AdviceType.OVERSPENDING, details.type);
  }

  @Test
  public void testCategoryComparisonNulls() {
    assertEquals("", FinancialAdvisor.getHighestRiseCategoryMessage(null, null, null));
  }

  @Test public void halfwayHighSpendingUsesActualIncomeRatio() {
    assertAdvice(2700, 3000, 0.5, FinancialAdvisor.AdviceType.HIGH_SPENDING, 90);
  }

  @Test public void earlyHighSpendingUsesTheSameActualRatio() {
    assertAdvice(2700, 3000, 0.1, FinancialAdvisor.AdviceType.HIGH_SPENDING, 90);
  }

  @Test public void zeroIncomeDistinguishesEmptyFromDeficit() {
    assertAdvice(0, 0, 0.5, FinancialAdvisor.AdviceType.NO_DATA, 0);
    assertAdvice(1, 0, 0.5, FinancialAdvisor.AdviceType.OVERSPENDING, 0);
  }

  @Test public void endOfPeriodSavingsUseUnscaledActualRatio() {
    assertAdvice(1200, 3000, 1.0, FinancialAdvisor.AdviceType.HEALTHY_SAVINGS, 60);
  }

  private void assertAdvice(long expense, long income, double dayProgress,
      FinancialAdvisor.AdviceType type, int percentage) {
    YearMonth period = YearMonth.now();
    MonthlySummary summary = new MonthlySummary(period.getYear(), period.getMonthValue(), expense,
        income, Collections.emptyList(), Collections.emptyList());
    FinancialAdvisor.AdviceDetails advice =
        FinancialAdvisor.calculateAdviceDetails(summary, null, null, dayProgress);
    assertEquals(type, advice.type);
    assertEquals(percentage, advice.percentage);
  }
}
