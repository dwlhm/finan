package com.dwlhm.finan.ui.common.infinitescroll;

import androidx.annotation.Nullable;

import java.util.List;

public final class InfiniteScrollRecyclerDataSink<T> implements InfiniteScrollDataSink<T> {

  private final InfiniteScrollRecyclerAdapter<T, ?> adapter;

  public InfiniteScrollRecyclerDataSink(InfiniteScrollRecyclerAdapter<T, ?> adapter) {
    this.adapter = adapter;
  }

  @Override
  public void replaceItems(@Nullable List<T> items) {
    adapter.replaceItems(items);
  }

  @Override
  public void appendItems(List<T> items) {
    adapter.appendItems(items);
  }

  @Override
  public void setLoadingFooterVisible(boolean visible) {
    adapter.setLoadingFooterVisible(visible);
  }

  @Override
  public int getContentItemCount() {
    return adapter.getContentItemCount();
  }

  @Override
  public int getDisplayedItemCount() {
    return adapter.getItemCount();
  }

  @Override
  public boolean isLoadingFooterVisible() {
    return adapter.isLoadingFooterVisible();
  }
}
