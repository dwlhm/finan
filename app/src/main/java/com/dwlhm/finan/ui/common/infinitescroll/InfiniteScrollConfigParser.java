package com.dwlhm.finan.ui.common.infinitescroll;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.InfiniteScrollConfig;

/** Parses {@link InfiniteScrollConfig} from {@link InfiniteScrollListView} XML attributes. */
public final class InfiniteScrollConfigParser {

  private InfiniteScrollConfigParser() {}

  @NonNull
  public static InfiniteScrollConfig parse(@NonNull Context context, @Nullable AttributeSet attrs) {
    if (attrs == null) {
      return InfiniteScrollConfig.defaults();
    }
    int pageSize = InfiniteScrollConfig.DEFAULT_PAGE_SIZE;
    int threshold = InfiniteScrollConfig.DEFAULT_LOAD_MORE_THRESHOLD;
    int spacingPx = 0;
    TypedArray styled = context.obtainStyledAttributes(attrs, R.styleable.InfiniteScrollListView);
    try {
      pageSize = styled.getInt(R.styleable.InfiniteScrollListView_infiniteScrollPageSize, pageSize);
      threshold =
          styled.getInt(R.styleable.InfiniteScrollListView_infiniteScrollLoadThreshold, threshold);
      spacingPx =
          styled.getDimensionPixelSize(
              R.styleable.InfiniteScrollListView_infiniteScrollItemSpacing, 0);
    } finally {
      styled.recycle();
    }
    return new InfiniteScrollConfig(pageSize, threshold, spacingPx);
  }
}
