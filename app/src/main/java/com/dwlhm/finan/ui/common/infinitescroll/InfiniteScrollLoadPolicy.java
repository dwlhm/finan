package com.dwlhm.finan.ui.common.infinitescroll;

/** Pure rules for when infinite scroll should fetch the next page. */
public final class InfiniteScrollLoadPolicy {

  public static final int NO_VISIBLE_POSITION = -1;

  private InfiniteScrollLoadPolicy() {}

  public static boolean canLoadMore(
      boolean loadingInitial, boolean loadingMore, boolean hasMore) {
    return !loadingInitial && !loadingMore && hasMore;
  }

  public static boolean shouldPrefetch(
      int lastVisiblePosition,
      int displayedItemCount,
      boolean loadingFooterVisible,
      int loadMoreThreshold) {
    if (lastVisiblePosition == NO_VISIBLE_POSITION || displayedItemCount == 0) {
      return false;
    }
    int lastContentIndex =
        loadingFooterVisible ? displayedItemCount - 2 : displayedItemCount - 1;
    return lastVisiblePosition >= lastContentIndex - loadMoreThreshold;
  }
}
