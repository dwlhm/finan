package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.dwlhm.finan.R;

public final class ScreenHeaderView extends LinearLayout {

  private final ImageButton backButton;
  private final ImageButton actionButton;

  public ScreenHeaderView(Context context) {
    this(context, null);
  }

  public ScreenHeaderView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    setGravity(Gravity.CENTER_VERTICAL);
    setOrientation(HORIZONTAL);
    setPadding(dp(4), dp(8), dp(16), dp(8));

    CharSequence titleText = null;
    CharSequence actionDescription = null;
    int actionIcon = 0;
    if (attrs != null) {
      try (TypedArray typedArray =
          context.obtainStyledAttributes(attrs, R.styleable.ScreenHeaderView)) {
        titleText = typedArray.getText(R.styleable.ScreenHeaderView_titleText);
        actionIcon = typedArray.getResourceId(R.styleable.ScreenHeaderView_actionIcon, 0);
        actionDescription =
            typedArray.getText(R.styleable.ScreenHeaderView_actionContentDescription);
      }
    }

    backButton = createIconButton(context);
    backButton.setContentDescription(context.getString(R.string.common_back));
    backButton.setImageResource(R.drawable.ic_back);
    addView(backButton);

    TextView titleView = new TextView(context);
    titleView.setSingleLine(true);
    titleView.setEllipsize(TextUtils.TruncateAt.END);
    titleView.setTextAppearance(R.style.Finan_Text_Title);
    titleView.setText(titleText);
    LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
    addView(titleView, titleParams);

    actionButton = createIconButton(context);
    if (actionIcon == 0) {
      actionButton.setVisibility(GONE);
    } else {
      actionButton.setImageResource(actionIcon);
      actionButton.setContentDescription(actionDescription);
    }
    addView(actionButton);
  }

  public void setOnBackClickListener(@Nullable OnClickListener listener) {
    backButton.setOnClickListener(listener);
  }

  public void setOnActionClickListener(@Nullable OnClickListener listener) {
    actionButton.setOnClickListener(listener);
  }

  private ImageButton createIconButton(Context context) {
    ImageButton button = new ImageButton(context);
    LayoutParams params = new LayoutParams(dp(48), dp(48));
    button.setLayoutParams(params);
    button.setBackgroundResource(resolveSelectableBorderless(context));
    button.setScaleType(ImageButton.ScaleType.CENTER);
    return button;
  }

  private int resolveSelectableBorderless(Context context) {
    TypedValue value = new TypedValue();
    context
        .getTheme()
        .resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true);
    return value.resourceId;
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
  }
}
