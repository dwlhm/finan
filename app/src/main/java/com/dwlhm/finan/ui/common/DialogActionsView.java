package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.dwlhm.finan.R;

public final class DialogActionsView extends LinearLayout {

  private final Button cancelButton;
  private final Button primaryButton;

  public DialogActionsView(Context context) {
    this(context, null);
  }

  public DialogActionsView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    setOrientation(HORIZONTAL);

    CharSequence primaryText = null;
    CharSequence secondaryText = context.getString(android.R.string.cancel);
    if (attrs != null) {
      try (TypedArray typedArray =
          context.obtainStyledAttributes(attrs, R.styleable.DialogActionsView)) {
        primaryText = typedArray.getText(R.styleable.DialogActionsView_primaryText);
        CharSequence configuredSecondaryText =
            typedArray.getText(R.styleable.DialogActionsView_secondaryText);
        if (configuredSecondaryText != null) {
          secondaryText = configuredSecondaryText;
        }
      }
    }

    cancelButton = new Button(context, null, 0, R.style.Finan_Button_Nav);
    cancelButton.setMinWidth(dp(96));
    cancelButton.setPadding(dp(16), 0, dp(16), 0);
    cancelButton.setText(secondaryText);
    addView(cancelButton, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

    primaryButton = new Button(context, null, 0, R.style.Finan_Button_Primary);
    primaryButton.setMinWidth(0);
    primaryButton.setPadding(dp(24), 0, dp(24), 0);
    primaryButton.setText(primaryText);
    LayoutParams primaryParams =
        new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    primaryParams.setMarginStart(dp(8));
    addView(primaryButton, primaryParams);
  }

  public void setOnCancelClickListener(@Nullable View.OnClickListener listener) {
    cancelButton.setOnClickListener(listener);
  }

  public void setOnPrimaryClickListener(@Nullable View.OnClickListener listener) {
    primaryButton.setOnClickListener(listener);
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
  }
}
