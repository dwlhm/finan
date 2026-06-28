package com.dwlhm.finan.ui.summary;

import org.junit.Test;
import com.dwlhm.finan.domain.model.MonthlySummary;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
}
