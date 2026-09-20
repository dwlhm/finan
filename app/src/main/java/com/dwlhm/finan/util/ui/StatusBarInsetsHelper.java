package com.dwlhm.finan.util.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class StatusBarInsetsHelper {

  private StatusBarInsetsHelper() {}

  public static void applyTopPadding(@NonNull View headerView, int extraSpacingDp) {
    if (headerView == null) return;
    int extraSpacingPx = (int) (extraSpacingDp * headerView.getContext().getResources().getDisplayMetrics().density + 0.5f);

    ViewCompat.setOnApplyWindowInsetsListener(headerView, (v, windowInsets) -> {
      int statusBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
      int fallback = getFallbackStatusBarHeight(v.getContext());
      int effectiveHeight = Math.max(statusBarHeight, fallback);
      v.setPadding(v.getPaddingLeft(), effectiveHeight + extraSpacingPx, v.getPaddingRight(), v.getPaddingBottom());
      return windowInsets;
    });

    int initialFallback = getFallbackStatusBarHeight(headerView.getContext());
    if (initialFallback > 0) {
      headerView.setPadding(headerView.getPaddingLeft(), initialFallback + extraSpacingPx, headerView.getPaddingRight(), headerView.getPaddingBottom());
    }
    ViewCompat.requestApplyInsets(headerView);
  }

  @SuppressLint({"InternalInsetResource", "DiscouragedApi"})
  private static int getFallbackStatusBarHeight(Context context) {
    if (context == null) return 0;
    int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    return resId > 0 ? context.getResources().getDimensionPixelSize(resId) : (int) (24 * context.getResources().getDisplayMetrics().density);
  }
}
