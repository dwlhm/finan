package com.dwlhm.finan.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class PageResult<T, C> {

  private final List<T> items;
  private final boolean hasMore;
  private final C nextCursor;

  public PageResult(List<T> items, boolean hasMore, C nextCursor) {
    this.items = items == null ? Collections.emptyList() : items;
    this.hasMore = hasMore;
    this.nextCursor = nextCursor;
  }

  public List<T> getItems() {
    return items;
  }

  public boolean hasMore() {
    return hasMore;
  }

  public C getNextCursor() {
    return nextCursor;
  }

  /** Trims a limit+1 fetch into a page and derives the next cursor from the last kept item. */
  public static <T, C> PageResult<T, C> fromLimitPlusOne(
      List<T> fetched, int limit, Function<T, C> toCursor) {
    boolean hasMore = fetched.size() > limit;
    List<T> page =
        hasMore ? new ArrayList<>(fetched.subList(0, limit)) : new ArrayList<>(fetched);
    C cursor = page.isEmpty() ? null : toCursor.apply(page.get(page.size() - 1));
    return new PageResult<>(page, hasMore, cursor);
  }
}
