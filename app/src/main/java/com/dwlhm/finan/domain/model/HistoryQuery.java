package com.dwlhm.finan.domain.model;

public record HistoryQuery(
    Long walletId,
    Long categoryId,
    TransactionType type,
    Long startInclusiveMillis,
    Long endExclusiveMillis,
    boolean oldestFirst,
    HistorySearch search) {

  public HistoryQuery {
    search = search == null ? HistorySearch.empty() : search;
  }
}
