package com.dwlhm.finan.ui.common.infinitescroll;

import androidx.annotation.Nullable;

import java.util.List;

/** Applies paged data to the visible list without knowing how items are rendered. */
public interface InfiniteScrollDataSink<T> {

  void replaceItems(@Nullable List<T> items);

  void appendItems(List<T> items);

  void setLoadingFooterVisible(boolean visible);

  int getContentItemCount();

  int getDisplayedItemCount();

  boolean isLoadingFooterVisible();
}
