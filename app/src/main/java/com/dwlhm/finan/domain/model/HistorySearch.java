package com.dwlhm.finan.domain.model;

import java.util.Collections;
import java.util.List;

public record HistorySearch(
    String text,
    Long amountMinor,
    List<Long> walletIds,
    List<Long> categoryIds,
    List<Long> merchantIds,
    List<Long> tagIds) {

  public HistorySearch {
    text = text == null ? "" : text.trim();
    walletIds = immutable(walletIds);
    categoryIds = immutable(categoryIds);
    merchantIds = immutable(merchantIds);
    tagIds = immutable(tagIds);
  }

  public static HistorySearch empty() {
    return new HistorySearch("", null, null, null, null, null);
  }

  public boolean isEmpty() {
    return text.isEmpty();
  }

  private static List<Long> immutable(List<Long> ids) {
    return ids == null || ids.isEmpty() ? Collections.emptyList() : List.copyOf(ids);
  }
}
