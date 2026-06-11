package com.dwlhm.finan.util.ui;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

public final class ViewPressAnimator {

  private ViewPressAnimator() {}

  @SuppressLint("ClickableViewAccessibility")
  public static void bindScale(View view) {
    view.setOnTouchListener(
        (pressedView, event) -> {
          switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
              pressedView.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).start();
              break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
              pressedView.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
              break;
            default:
              break;
          }
          return false;
        });
  }
}
