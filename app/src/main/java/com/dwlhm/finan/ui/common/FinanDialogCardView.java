package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.dwlhm.finan.R;

public final class FinanDialogCardView extends LinearLayout {

  public FinanDialogCardView(Context context) {
    this(context, null);
  }

  public FinanDialogCardView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    setBackgroundResource(R.drawable.bg_card);
    setOrientation(VERTICAL);
    setPadding(dp(20), dp(18), dp(20), dp(18));

    CharSequence titleText = null;
    if (attrs != null) {
      TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FinanDialogCardView);
      titleText = typedArray.getText(R.styleable.FinanDialogCardView_titleText);
      typedArray.recycle();
    }

    if (titleText != null && titleText.length() > 0) {
      TextView titleView = new TextView(context);
      titleView.setTextAppearance(context, R.style.Finan_Text_Title);
      titleView.setText(titleText);
      addView(titleView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
  }
}
