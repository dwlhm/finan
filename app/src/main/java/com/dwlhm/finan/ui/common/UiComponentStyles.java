package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;

public final class UiComponentStyles {

  private UiComponentStyles() {}

  public static void prepareChip(@NonNull Button chip) {
    chip.setAllCaps(false);
    chip.setGravity(Gravity.CENTER);
    chip.setSingleLine(true);
    chip.setEllipsize(TextUtils.TruncateAt.END);
    chip.setMinWidth(dp(chip.getContext(), 88));
    chip.setMinHeight(dp(chip.getContext(), 40));
    chip.setPadding(dp(chip.getContext(), 14), 0, dp(chip.getContext(), 14), 0);
    chip.setTextSize(13f);
  }

  public static void setChipSelected(
      @NonNull Context context,
      @NonNull Button chip,
      boolean selected,
      @DrawableRes int selectedBackgroundRes) {
    chip.setBackgroundResource(selected ? selectedBackgroundRes : R.drawable.bg_chip);
    chip.setTextColor(
        ContextCompat.getColor(
            context, selected ? R.color.finan_chip_text_selected : R.color.finan_chip_text));
  }

  public static void setSelectButtonSelected(
      @NonNull Context context,
      @NonNull Button button,
      boolean selected,
      @DrawableRes int selectedBackgroundRes) {
    button.setBackgroundResource(selected ? selectedBackgroundRes : R.drawable.bg_control_surface);
    button.setTextColor(
        ContextCompat.getColor(
            context, selected ? R.color.finan_chip_text_selected : R.color.finan_primary));
  }

  public static int dp(@NonNull Context context, int value) {
    return Math.round(value * context.getResources().getDisplayMetrics().density);
  }
}
