package com.dwlhm.finan.ui.common.infinitescroll;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;

final class InfiniteScrollConfig {

  static final int DEFAULT_PAGE_SIZE = 30;
  static final int DEFAULT_LOAD_MORE_THRESHOLD = 5;

  final int pageSize;
  final int loadMoreThreshold;
  final int itemSpacingPx;
  @LayoutRes final int loadingFooterLayoutRes;

  InfiniteScrollConfig(
      int pageSize, int loadMoreThreshold, int itemSpacingPx, @LayoutRes int loadingFooterLayoutRes) {
    this.pageSize = Math.max(1, pageSize);
    this.loadMoreThreshold = Math.max(0, loadMoreThreshold);
    this.itemSpacingPx = Math.max(0, itemSpacingPx);
    this.loadingFooterLayoutRes = loadingFooterLayoutRes;
  }

  static InfiniteScrollConfig from(Context context, @Nullable AttributeSet attrs) {
    int pageSize = DEFAULT_PAGE_SIZE;
    int threshold = DEFAULT_LOAD_MORE_THRESHOLD;
    int spacingPx = 0;
    @LayoutRes int loadingLayout = R.layout.item_infinite_scroll_loading;
    if (attrs != null) {
      TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.InfiniteScrollListView);
      pageSize = a.getInt(R.styleable.InfiniteScrollListView_infiniteScrollPageSize, pageSize);
      threshold =
          a.getInt(R.styleable.InfiniteScrollListView_infiniteScrollLoadThreshold, threshold);
      spacingPx =
          a.getDimensionPixelSize(R.styleable.InfiniteScrollListView_infiniteScrollItemSpacing, 0);
      int customLoading =
          a.getResourceId(R.styleable.InfiniteScrollListView_infiniteScrollLoadingLayout, 0);
      if (customLoading != 0) {
        loadingLayout = customLoading;
      }
      a.recycle();
    }
    return new InfiniteScrollConfig(pageSize, threshold, spacingPx, loadingLayout);
  }
}
