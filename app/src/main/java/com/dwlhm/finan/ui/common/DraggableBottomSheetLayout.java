package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class DraggableBottomSheetLayout extends FrameLayout {

  private static final float DISMISS_THRESHOLD = 0.35f;

  private float initialRawY;
  private float initialTranslationY;
  private boolean isDragging;
  private final int touchSlop;
  private Runnable onDismissListener;

  public DraggableBottomSheetLayout(@NonNull Context context) {
    this(context, null);
  }

  public DraggableBottomSheetLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DraggableBottomSheetLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
  }

  public void setOnDismissListener(Runnable listener) {
    this.onDismissListener = listener;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        initialRawY = event.getRawY();
        initialTranslationY = getTranslationY();
        isDragging = false;
        return false;
      case MotionEvent.ACTION_MOVE:
        float dy = event.getRawY() - initialRawY;
        if (dy > touchSlop && !isDragging && !canChildScrollUp()) {
          isDragging = true;
          return true;
        }
        return false;
    }
    return super.onInterceptTouchEvent(event);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_MOVE:
        float dy = event.getRawY() - initialRawY;
        setTranslationY(initialTranslationY + Math.max(0, dy));
        return true;
      case MotionEvent.ACTION_UP:
        if (isDragging) {
          float translation = getTranslationY();
          if (translation > getHeight() * DISMISS_THRESHOLD) {
            animate()
                .translationY(getHeight())
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(onDismissListener)
                .start();
          } else {
            animate()
                .translationY(0)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
          }
          isDragging = false;
        }
        return true;
      case MotionEvent.ACTION_CANCEL:
        if (isDragging) {
          animate()
              .translationY(0)
              .setDuration(200)
              .start();
          isDragging = false;
        }
        return true;
    }
    return super.onTouchEvent(event);
  }

  private boolean canChildScrollUp() {
    if (getChildCount() == 0) return false;
    return getChildAt(0).canScrollVertically(-1);
  }
}
