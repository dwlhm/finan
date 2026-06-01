package com.dwlhm.finan.ui.common.infinitescroll;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.PageResult;

/**
 * Cursor-based infinite scroll list. Usage: {@code list.setup(adapter, loader); list.reload();}
 */
public final class InfiniteScrollListView extends FrameLayout {

  private final RecyclerView recyclerView;
  private final InfiniteScrollConfig config;
  private final LinearLayoutManager layoutManager;

  private InfiniteScrollRecyclerAdapter<?, ?> adapter;
  private PageLoader<?, ?> loader;
  private Object nextCursor;
  private boolean hasMore = true;
  private boolean loadingInitial;
  private boolean loadingMore;
  private int loadGeneration;

  public InfiniteScrollListView(@NonNull Context context) {
    this(context, null);
  }

  public InfiniteScrollListView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public InfiniteScrollListView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    LayoutInflater.from(context).inflate(R.layout.view_infinite_scroll_list, this, true);
    recyclerView = findViewById(R.id.infinite_scroll_recycler);
    layoutManager = new LinearLayoutManager(context);
    recyclerView.setLayoutManager(layoutManager);
    config = InfiniteScrollConfig.from(context, attrs);
  }

  public <T, C> void setup(
      @NonNull InfiniteScrollRecyclerAdapter<T, ?> adapter, @NonNull PageLoader<T, C> loader) {
    teardown();
    this.adapter = adapter;
    this.loader = loader;
    recyclerView.setAdapter(adapter);
    applyItemSpacing();
    recyclerView.addOnScrollListener(scrollListener);
    adapter.registerAdapterDataObserver(adapterObserver);
  }

  public void reload() {
    if (loader == null || adapter == null) {
      return;
    }
    int generation = ++loadGeneration;
    nextCursor = null;
    hasMore = true;
    loadingInitial = true;
    loadingMore = false;
    adapter.setLoadingFooterVisible(false);
    adapter.replaceItems(null);
    PageResult<?, ?> page = loadPage(null);
    if (generation != loadGeneration) {
      return;
    }
    loadingInitial = false;
    adapter.replaceItems(page.getItems());
    nextCursor = page.getNextCursor();
    hasMore = page.hasMore();
    maybeLoadMore();
  }

  public boolean isEmpty() {
    return adapter != null && adapter.getContentItemCount() == 0 && !loadingInitial;
  }

  public int getPageSize() {
    return config.pageSize;
  }

  @NonNull
  public RecyclerView getRecyclerView() {
    return recyclerView;
  }

  @Override
  protected void onDetachedFromWindow() {
    teardown();
    super.onDetachedFromWindow();
  }

  private void teardown() {
    recyclerView.removeOnScrollListener(scrollListener);
    if (adapter != null) {
      adapter.unregisterAdapterDataObserver(adapterObserver);
    }
    adapter = null;
    loader = null;
  }

  @SuppressWarnings("unchecked")
  private <C> PageResult<?, C> loadPage(C cursor) {
    return ((PageLoader<Object, C>) loader).loadPage(cursor);
  }

  private void maybeLoadMore() {
    if (loadingInitial || loadingMore || !hasMore) {
      return;
    }
    int lastVisible = layoutManager.findLastVisibleItemPosition();
    if (lastVisible == RecyclerView.NO_POSITION || adapter.getItemCount() == 0) {
      return;
    }
    int lastContent = loadingMore ? adapter.getItemCount() - 2 : adapter.getItemCount() - 1;
    if (lastVisible < lastContent - config.loadMoreThreshold) {
      return;
    }
    loadMore();
  }

  private void loadMore() {
    if (loadingInitial || loadingMore || !hasMore) {
      return;
    }
    loadingMore = true;
    adapter.setLoadingFooterVisible(true);
    int generation = loadGeneration;
    PageResult<?, ?> page = loadPage(nextCursor);
    loadingMore = false;
    adapter.setLoadingFooterVisible(false);
    if (generation != loadGeneration) {
      return;
    }
    if (page.getItems().isEmpty()) {
      hasMore = false;
      return;
    }
    adapter.appendItems(page.getItems());
    nextCursor = page.getNextCursor();
    hasMore = page.hasMore();
    maybeLoadMore();
  }

  private void applyItemSpacing() {
    while (recyclerView.getItemDecorationCount() > 0) {
      recyclerView.removeItemDecorationAt(0);
    }
    if (config.itemSpacingPx <= 0) {
      return;
    }
    int spacing = config.itemSpacingPx;
    recyclerView.addItemDecoration(
        new RecyclerView.ItemDecoration() {
          @Override
          public void getItemOffsets(
              @NonNull Rect outRect,
              @NonNull View view,
              @NonNull RecyclerView parent,
              @NonNull RecyclerView.State state) {
            int pos = parent.getChildAdapterPosition(view);
            RecyclerView.Adapter<?> listAdapter = parent.getAdapter();
            if (pos != RecyclerView.NO_POSITION
                && listAdapter != null
                && pos < listAdapter.getItemCount() - 1) {
              outRect.bottom = spacing;
            }
          }
        });
  }

  private final RecyclerView.OnScrollListener scrollListener =
      new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
          if (dy > 0) {
            maybeLoadMore();
          }
        }
      };

  private final RecyclerView.AdapterDataObserver adapterObserver =
      new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
          maybeLoadMore();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
          maybeLoadMore();
        }
      };
}
