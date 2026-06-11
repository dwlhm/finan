package com.dwlhm.finan.ui.common.infinitescroll;

import androidx.annotation.Nullable;

import com.dwlhm.finan.domain.model.InfiniteScrollConfig;
import com.dwlhm.finan.domain.model.PageResult;

public final class InfiniteScrollController<T, C> implements InfiniteScrollHandle {

  private final InfiniteScrollConfig config;
  private final PageLoader<T, C> loader;
  private final InfiniteScrollDataSink<T> dataSink;
  private final PageLoadExecutor executor;
  private final Runnable schedulePrefetchCheck;

  private InfiniteScrollPagingState<C> pagingState = InfiniteScrollPagingState.initial();

  public InfiniteScrollController(
      InfiniteScrollConfig config,
      PageLoader<T, C> loader,
      InfiniteScrollDataSink<T> dataSink,
      PageLoadExecutor executor,
      Runnable schedulePrefetchCheck) {
    this.config = config;
    this.loader = loader;
    this.dataSink = dataSink;
    this.executor = executor;
    this.schedulePrefetchCheck = schedulePrefetchCheck;
  }

  @Override
  public void reload() {
    pagingState = pagingState.beginReload();
    dataSink.setLoadingFooterVisible(false);
    dataSink.replaceItems(null);
    int generation = pagingState.getGeneration();
    executor.compute(() -> loader.loadPage(null), page -> applyInitialPage(generation, page));
  }

  @Override
  public void dispose() {
    pagingState = pagingState.invalidate();
  }

  @Override
  public boolean isEmpty() {
    return !pagingState.isLoadingInitial() && dataSink.getContentItemCount() == 0;
  }

  @Override
  public void onLastVisiblePositionChanged(int lastVisiblePosition) {
    if (!InfiniteScrollLoadPolicy.canLoadMore(
        pagingState.isLoadingInitial(), pagingState.isLoadingMore(), pagingState.hasMore())) {
      return;
    }
    if (!InfiniteScrollLoadPolicy.shouldPrefetch(
        lastVisiblePosition,
        dataSink.getDisplayedItemCount(),
        dataSink.isLoadingFooterVisible(),
        config.getLoadMoreThreshold())) {
      return;
    }
    loadMore();
  }

  private void loadMore() {
    if (!InfiniteScrollLoadPolicy.canLoadMore(
        pagingState.isLoadingInitial(), pagingState.isLoadingMore(), pagingState.hasMore())) {
      return;
    }
    pagingState = pagingState.beginLoadMore();
    dataSink.setLoadingFooterVisible(true);
    int generation = pagingState.getGeneration();
    C cursor = pagingState.getNextCursor();
    executor.compute(() -> loader.loadPage(cursor), page -> applyMorePage(generation, page));
  }

  private void applyInitialPage(int generation, @Nullable PageResult<T, C> page) {
    if (pagingState.isStale(generation) || page == null) {
      return;
    }
    pagingState = pagingState.afterInitialPage(page.getNextCursor(), page.hasMore());
    dataSink.replaceItems(page.getItems());
    schedulePrefetchCheck.run();
  }

  private void applyMorePage(int generation, @Nullable PageResult<T, C> page) {
    pagingState = pagingState.clearLoadMore();
    dataSink.setLoadingFooterVisible(false);
    if (pagingState.isStale(generation) || page == null) {
      return;
    }
    if (page.getItems().isEmpty()) {
      pagingState = pagingState.withHasMore(false);
      return;
    }
    pagingState = pagingState.afterLoadMore(page.getNextCursor(), page.hasMore());
    dataSink.appendItems(page.getItems());
    schedulePrefetchCheck.run();
  }
}
