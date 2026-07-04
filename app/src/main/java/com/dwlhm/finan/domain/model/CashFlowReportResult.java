package com.dwlhm.finan.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CashFlowReportResult {
  private final Map<String, List<CashFlowReport>> reportsByCurrency;
  private final List<CashFlowReport> allReports;

  public CashFlowReportResult(Map<String, List<CashFlowReport>> reportsByCurrency) {
    this.reportsByCurrency = Collections.unmodifiableMap(reportsByCurrency);
    List<CashFlowReport> all = new java.util.ArrayList<>();
    for (List<CashFlowReport> reports : reportsByCurrency.values()) {
      all.addAll(reports);
    }
    this.allReports = Collections.unmodifiableList(all);
  }

  public Map<String, List<CashFlowReport>> getReportsByCurrency() { return reportsByCurrency; }
  public List<CashFlowReport> getAllReports() { return allReports; }

  public boolean isSingleCurrency() { return reportsByCurrency.size() <= 1; }
}
