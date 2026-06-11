package com.dwlhm.finan.ui.common.infinitescroll;

/** View-facing contract for an attached infinite-scroll session. */
public interface InfiniteScrollHandle {

  void reload();

  void dispose();

  boolean isEmpty();

  void onLastVisiblePositionChanged(int lastVisiblePosition);
}
