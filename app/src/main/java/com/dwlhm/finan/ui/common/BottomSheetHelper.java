package com.dwlhm.finan.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
public final class BottomSheetHelper {

  private static final String PREFS_NAME = "finan_prefs";
  private static final String MASKED_MODE_KEY = "settings_wallet_masked_mode";

  public static void show(BottomSheetDialog dialog) {
    dialog.show();
    FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
    if (bottomSheet != null) {
      bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
      BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
      behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      behavior.setSkipCollapsed(true);

      ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (v, insets) -> {
        int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
        v.setPadding(
            v.getPaddingLeft(),
            v.getPaddingTop(),
            v.getPaddingRight(),
            bottomInset);
        return insets;
      });
    }
  }

  public static void show(Dialog dialog) {
    if (dialog instanceof BottomSheetDialog) {
      show((BottomSheetDialog) dialog);
      return;
    }
    dialog.show();
    Window window = dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      WindowManager.LayoutParams params = window.getAttributes();
      params.width = WindowManager.LayoutParams.MATCH_PARENT;
      params.height = WindowManager.LayoutParams.WRAP_CONTENT;
      params.gravity = Gravity.BOTTOM;
      window.setAttributes(params);
      window.setWindowAnimations(android.R.style.Animation_InputMethod);
    }
    makeDraggable(dialog);
  }

  public static void makeDraggable(Dialog dialog) {
    if (dialog instanceof BottomSheetDialog) {
      return;
    }
    Window window = dialog.getWindow();
    if (window == null) return;

    ViewGroup contentFrame = window.getDecorView().findViewById(android.R.id.content);
    if (contentFrame == null || contentFrame.getChildCount() == 0) return;

    View originalContent = contentFrame.getChildAt(0);
    contentFrame.removeView(originalContent);

    DraggableBottomSheetLayout wrapper = new DraggableBottomSheetLayout(dialog.getContext());
    wrapper.setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    wrapper.addView(originalContent);
    wrapper.setOnDismissListener(dialog::dismiss);
    contentFrame.addView(wrapper);
  }

  public static boolean isMasked(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    return prefs.getBoolean(MASKED_MODE_KEY, false);
  }
}
