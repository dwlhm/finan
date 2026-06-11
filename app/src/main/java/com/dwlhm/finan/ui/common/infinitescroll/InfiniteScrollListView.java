package com.dwlhm.finan.ui.common.infinitescroll;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.InfiniteScrollConfig;

/**
 * Cursor-based infinite scroll list. Usage:
 *
 * <pre>{@code
 * list.setup(adapter, loader, executor);
 * list.reload();
 * }</pre>
 */
public final class InfiniteScrollListView extends FrameLayout {

  private final RecyclerView recyclerView;
  private final int pageSize;
  private final int loadMoreThreshold;
  private final int itemSpacingPx;
  private final LinearLayoutManager layoutManager;

  @Nullable private InfiniteScrollHandle scrollHandle;
  @Nullable private InfiniteScrollRecyclerAdapter<?, ?> boundAdapter;

  private final RecyclerView.OnScrollListener scrollListener =
      new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
          if (dy > 0) {
            notifyLastVisiblePositionChanged();
          }
        }
      };

  private final RecyclerView.AdapterDataObserver adapterObserver =
      new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
          notifyLastVisiblePositionChanged();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
          notifyLastVisiblePositionChanged();
        }
      };

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
    InfiniteScrollConfig config = resolveConfig(context, attrs, isInEditMode());
    pageSize = config.getPageSize();
    loadMoreThreshold = config.getLoadMoreThreshold();
    itemSpacingPx = config.getItemSpacingPx();
  }

  @NonNull
  private static InfiniteScrollConfig resolveConfig(
      @NonNull Context context, @Nullable AttributeSet attrs, boolean inEditMode) {
    if (inEditMode) {
      return InfiniteScrollConfig.defaults();
    }
    try {
      return InfiniteScrollConfigParser.parse(context, attrs);
    } catch (RuntimeException ignored) {
      return InfiniteScrollConfig.defaults();
    }
  }

  @NonNull
  private InfiniteScrollConfig scrollConfig() {
    return new InfiniteScrollConfig(pageSize, loadMoreThreshold, itemSpacingPx);
  }

  public <T, C> void setup(
      @NonNull InfiniteScrollRecyclerAdapter<T, ?> adapter,
      @NonNull PageLoader<T, C> loader,
      @NonNull PageLoadExecutor executor) {
    teardown();
    boundAdapter = adapter;
    recyclerView.setAdapter(adapter);
    applyItemSpacing();
    scrollHandle =
        new InfiniteScrollController<>(
            scrollConfig(),
            loader,
            new InfiniteScrollRecyclerDataSink<>(adapter),
            executor,
            this::notifyLastVisiblePositionChanged);
    recyclerView.addOnScrollListener(scrollListener);
    adapter.registerAdapterDataObserver(adapterObserver);
  }

  public void reload() {
    if (scrollHandle == null) {
      return;
    }
    scrollHandle.reload();
  }

  public boolean isEmpty() {
    return scrollHandle != null && scrollHandle.isEmpty();
  }

  public int getPageSize() {
    return pageSize;
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
    if (scrollHandle != null) {
      scrollHandle.dispose();
      scrollHandle = null;
    }
    recyclerView.removeOnScrollListener(scrollListener);
    if (boundAdapter != null) {
      boundAdapter.unregisterAdapterDataObserver(adapterObserver);
      boundAdapter = null;
    }
  }

  private void notifyLastVisiblePositionChanged() {
    if (scrollHandle == null) {
      return;
    }
    scrollHandle.onLastVisiblePositionChanged(layoutManager.findLastVisibleItemPosition());
  }

  private void applyItemSpacing() {
    while (recyclerView.getItemDecorationCount() > 0) {
      recyclerView.removeItemDecorationAt(0);
    }
    if (itemSpacingPx <= 0) {
      return;
    }
    recyclerView.addItemDecoration(InfiniteScrollItemDecorations.bottomSpacing(itemSpacingPx));
  }
}
