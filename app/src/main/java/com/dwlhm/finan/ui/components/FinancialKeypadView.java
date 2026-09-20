package com.dwlhm.finan.ui.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;

public class FinancialKeypadView extends ViewGroup {

    private static final int ALPHA_NOMINAL_KEY = 102; // 40% alpha (~102/255)
    private static final int ALPHA_OPERATOR_KEY = 64; // 25% alpha
    private static final int ALPHA_BACKSPACE_KEY = 64; // 25% alpha
    private static final int ALPHA_PRESSED_DELTA = 45; // +17.6% alpha tactile feedback
    private static final int STROKE_WIDTH_DP = 1;
    private static final int CORNER_RADIUS_DP = 18;
    private static final int STROKE_ALPHA_NORMAL = 65;
    private static final int STROKE_ALPHA_OPERATOR = 110;
    private static final int STROKE_ALPHA_PRESSED = 120;

    private OnKeypadActionListener listener;

    private static final int ROWS = 4;
    private static final int COLS = 4;

    private final String[] keyLabels = {
            "1", "2", "3", "+",
            "4", "5", "6", "-",
            "7", "8", "9", "÷",
            "000", "0", "⌫", "×"
    };

    private final boolean[] isOperator = {
            false, false, false, true,
            false, false, false, true,
            false, false, false, true,
            false, false, false, true
    };

    private final View[] keyViews = new View[ROWS * COLS];

    public FinancialKeypadView(Context context) {
        super(context);
        init(context);
    }

    public FinancialKeypadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FinancialKeypadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {
        setBackgroundColor(android.graphics.Color.TRANSPARENT);
        setFocusable(false);
        setFocusableInTouchMode(false);

        int horizontalPadding = 0;
        int verticalPadding = dpToPx(8);
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        int keyTextColor = ContextCompat.getColor(context, R.color.finan_text_dark_primary);

        for (int i = 0; i < keyLabels.length; i++) {
            final String label = keyLabels[i];
            View key;

            if ("⌫".equals(label)) {
                ImageView img = new ImageView(context);
                img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                img.setImageResource(R.drawable.ic_keypad_backspace);
                img.setColorFilter(keyTextColor);

                int iconPadding = dpToPx(16);
                img.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
                key = img;
            } else if ("".equals(label)) {
                key = new View(context);
            } else {
                TextView txt = new TextView(context);
                txt.setText(label);
                txt.setGravity(Gravity.CENTER);
                txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, isOperator[i] ? 24 : 22);
                txt.setTypeface(Typeface.DEFAULT_BOLD);
                txt.setTextColor(keyTextColor);
                key = txt;
            }

            if (!"".equals(label)) {
                setupKeyBackground(key, isOperator[i], label);
                key.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    handleKeyClick(label);
                });
                if ("⌫".equals(label)) {
                    key.setOnLongClickListener(v -> {
                        if (listener != null) listener.onClear();
                        return true;
                    });
                }
            }

            keyViews[i] = key;
            if (!"".equals(label)) {
                key.setFocusable(false);
                key.setFocusableInTouchMode(false);
                addView(key);
            }
        }
    }

    private void setupKeyBackground(View view, boolean isOperator) {
        setupKeyBackground(view, isOperator, "");
    }

    private void setupKeyBackground(View view, boolean isOperator, String label) {
        int normalAlpha;
        int r = 255, g = 255, b = 255;
        if ("⌫".equals(label)) {
            normalAlpha = ALPHA_BACKSPACE_KEY;
        } else if (isOperator) {
            normalAlpha = ALPHA_OPERATOR_KEY;
            r = 230;
            g = 248;
            b = 238;
        } else {
            normalAlpha = ALPHA_NOMINAL_KEY;
        }
        int pressedAlpha = Math.min(255, normalAlpha + ALPHA_PRESSED_DELTA);

        int normalColor = Color.argb(normalAlpha, r, g, b);
        int pressedColor = Color.argb(pressedAlpha, r, g, b);
        int normalStrokeAlpha = isOperator ? STROKE_ALPHA_OPERATOR : STROKE_ALPHA_NORMAL;
        int normalStrokeColor = Color.argb(normalStrokeAlpha, 255, 255, 255);
        int pressedStrokeColor = Color.argb(STROKE_ALPHA_PRESSED, 255, 255, 255);

        int strokeWidth = dpToPx(STROKE_WIDTH_DP);
        int cornerRadius = dpToPx(CORNER_RADIUS_DP);

        GradientDrawable normalShape = new GradientDrawable();
        normalShape.setShape(GradientDrawable.RECTANGLE);
        normalShape.setCornerRadius(cornerRadius);
        normalShape.setColor(normalColor);
        normalShape.setStroke(strokeWidth, normalStrokeColor);

        GradientDrawable pressedShape = new GradientDrawable();
        pressedShape.setShape(GradientDrawable.RECTANGLE);
        pressedShape.setCornerRadius(cornerRadius);
        pressedShape.setColor(pressedColor);
        pressedShape.setStroke(strokeWidth, pressedStrokeColor);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressedShape);
        states.addState(new int[]{}, normalShape);

        view.setBackground(states);
    }

    public void setOnKeypadActionListener(OnKeypadActionListener listener) {
        this.listener = listener;
    }

    private void handleKeyClick(String label) {
        if (listener == null) return;

        switch (label) {
            case "⌫":
                listener.onBackspace();
                break;
            case "000":
            case "00":
                listener.onShortcut(label);
                break;
            case "+":
            case "-":
            case "×":
                listener.onOperator(label);
                break;
            case "÷":
                listener.onOperator("/");
                break;
            default:
                try {
                    int digit = Integer.parseInt(label);
                    listener.onDigitEntered(digit);
                } catch (NumberFormatException ignored) {
                }
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = widthSize - getPaddingLeft() - getPaddingRight();

        int spacing = dpToPx(8);
        int keyWidth = (availableWidth - (COLS - 1) * spacing) / COLS;
        int keyHeight = (int) (keyWidth * 0.7f);

        int heightSize = getPaddingTop() + getPaddingBottom() + (ROWS * keyHeight) + ((ROWS - 1) * spacing);

        int childWidthSpec = MeasureSpec.makeMeasureSpec(keyWidth, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(keyHeight, MeasureSpec.EXACTLY);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(childWidthSpec, childHeightSpec);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int spacing = dpToPx(8);

        int availableWidth = getWidth() - paddingLeft - getPaddingRight();
        int keyWidth = (availableWidth - (COLS - 1) * spacing) / COLS;
        int keyHeight = (int) (keyWidth * 0.7f);

        int childIndex = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (childIndex >= getChildCount()) break;

                View child = getChildAt(childIndex);
                int childMeasuredWidth = child.getMeasuredWidth();
                int childMeasuredHeight = child.getMeasuredHeight();

                int left = paddingLeft + col * (keyWidth + spacing);
                int top = paddingTop + row * (keyHeight + spacing);

                child.layout(left, top, left + childMeasuredWidth, top + childMeasuredHeight);
                childIndex++;
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
