package com.dwlhm.finan.ui.common.infinitescroll;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public final class InfiniteScrollItemDecorations {

  private InfiniteScrollItemDecorations() {}

  public static RecyclerView.ItemDecoration bottomSpacing(int spacingPx) {
    return new RecyclerView.ItemDecoration() {
      @Override
      public void getItemOffsets(
          @NonNull Rect outRect,
          @NonNull View view,
          @NonNull RecyclerView parent,
          @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        RecyclerView.Adapter<?> listAdapter = parent.getAdapter();
        if (position != RecyclerView.NO_POSITION
            && listAdapter != null
            && position < listAdapter.getItemCount() - 1) {
          outRect.bottom = spacingPx;
        }
      }
    };
  }
}
