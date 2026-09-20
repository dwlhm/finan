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

  @SuppressLint("ClickableViewAccessibility")
  public static void bindSpringScale(View view) {
    view.setOnTouchListener(
        (pressedView, event) -> {
          switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
              pressedView
                  .animate()
                  .scaleX(0.88f)
                  .scaleY(0.88f)
                  .setDuration(120)
                  .setInterpolator(new android.view.animation.DecelerateInterpolator())
                  .start();
              break;
            case MotionEvent.ACTION_UP:
              pressedView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
              // fallthrough
            case MotionEvent.ACTION_CANCEL:
              pressedView
                  .animate()
                  .scaleX(1.0f)
                  .scaleY(1.0f)
                  .setDuration(300)
                  .setInterpolator(new android.view.animation.OvershootInterpolator(3.0f))
                  .start();
              break;
            default:
              break;
          }
          return false;
        });
  }
}
