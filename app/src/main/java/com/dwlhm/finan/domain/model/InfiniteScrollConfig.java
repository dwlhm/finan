package com.dwlhm.finan.domain.model;

/** Immutable infinite-scroll list configuration. */
public final class InfiniteScrollConfig {

  public static final int DEFAULT_PAGE_SIZE = 30;
  public static final int DEFAULT_LOAD_MORE_THRESHOLD = 5;

  private final int pageSize;
  private final int loadMoreThreshold;
  private final int itemSpacingPx;

  public InfiniteScrollConfig(int pageSize, int loadMoreThreshold, int itemSpacingPx) {
    this.pageSize = Math.max(1, pageSize);
    this.loadMoreThreshold = Math.max(0, loadMoreThreshold);
    this.itemSpacingPx = Math.max(0, itemSpacingPx);
  }

  public static InfiniteScrollConfig defaults() {
    return new InfiniteScrollConfig(
        DEFAULT_PAGE_SIZE, DEFAULT_LOAD_MORE_THRESHOLD, 0);
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getLoadMoreThreshold() {
    return loadMoreThreshold;
  }

  public int getItemSpacingPx() {
    return itemSpacingPx;
  }
}
