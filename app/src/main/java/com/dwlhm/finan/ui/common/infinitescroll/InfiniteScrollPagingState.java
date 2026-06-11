package com.dwlhm.finan.ui.common.infinitescroll;

import androidx.annotation.Nullable;

/** Immutable paging flags for a single infinite-scroll session. */
public final class InfiniteScrollPagingState<C> {

  private final int generation;
  @Nullable private final C nextCursor;
  private final boolean hasMore;
  private final boolean loadingInitial;
  private final boolean loadingMore;

  private InfiniteScrollPagingState(
      int generation,
      @Nullable C nextCursor,
      boolean hasMore,
      boolean loadingInitial,
      boolean loadingMore) {
    this.generation = generation;
    this.nextCursor = nextCursor;
    this.hasMore = hasMore;
    this.loadingInitial = loadingInitial;
    this.loadingMore = loadingMore;
  }

  public static <C> InfiniteScrollPagingState<C> initial() {
    return new InfiniteScrollPagingState<>(0, null, true, false, false);
  }

  public InfiniteScrollPagingState<C> invalidate() {
    return new InfiniteScrollPagingState<>(
        generation + 1, nextCursor, hasMore, loadingInitial, loadingMore);
  }

  public InfiniteScrollPagingState<C> beginReload() {
    return new InfiniteScrollPagingState<>(generation + 1, null, true, true, false);
  }

  public InfiniteScrollPagingState<C> afterInitialPage(
      @Nullable C cursor, boolean pageHasMore) {
    return new InfiniteScrollPagingState<>(generation, cursor, pageHasMore, false, false);
  }

  public InfiniteScrollPagingState<C> beginLoadMore() {
    return new InfiniteScrollPagingState<>(generation, nextCursor, hasMore, false, true);
  }

  public InfiniteScrollPagingState<C> afterLoadMore(@Nullable C cursor, boolean pageHasMore) {
    return new InfiniteScrollPagingState<>(generation, cursor, pageHasMore, false, false);
  }

  public InfiniteScrollPagingState<C> withHasMore(boolean value) {
    return new InfiniteScrollPagingState<>(generation, nextCursor, value, loadingInitial, loadingMore);
  }

  public InfiniteScrollPagingState<C> clearLoadMore() {
    return new InfiniteScrollPagingState<>(generation, nextCursor, hasMore, loadingInitial, false);
  }

  public int getGeneration() {
    return generation;
  }

  @Nullable
  public C getNextCursor() {
    return nextCursor;
  }

  public boolean hasMore() {
    return hasMore;
  }

  public boolean isLoadingInitial() {
    return loadingInitial;
  }

  public boolean isLoadingMore() {
    return loadingMore;
  }

  public boolean isStale(int expectedGeneration) {
    return generation != expectedGeneration;
  }
}
