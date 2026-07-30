package com.dwlhm.finan.ui.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
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

    private OnKeypadActionListener listener;

    private static final int ROWS = 4;
    private static final int COLS = 4;

    private final String[] keyLabels = {
            "1", "2", "3", "+",
            "4", "5", "6", "-",
            "7", "8", "9", "×",
            "000", "0", "⌫", "÷"
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
        setBackgroundColor(ContextCompat.getColor(context, R.color.finan_keypad_bg));
        setFocusable(false);
        setFocusableInTouchMode(false);

        int horizontalPadding = 0;
        int verticalPadding = dpToPx(16);
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        for (int i = 0; i < keyLabels.length; i++) {
            final String label = keyLabels[i];
            View key;

            if ("⌫".equals(label)) {
                ImageView img = new ImageView(context);
                img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                img.setImageResource(R.drawable.ic_keypad_backspace);
                img.setColorFilter(ContextCompat.getColor(context, R.color.finan_key_text));

                int iconPadding = dpToPx(16);
                img.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
                key = img;
            } else if ("".equals(label)) {
                key = new View(context);
            } else {
                TextView txt = new TextView(context);
                txt.setText(label);
                txt.setGravity(Gravity.CENTER);
                txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, isOperator[i] ? 20 : 22);
                txt.setTypeface(Typeface.DEFAULT_BOLD);
                txt.setTextColor(ContextCompat.getColor(context,
                        isOperator[i] ? R.color.finan_primary : R.color.finan_key_text));
                key = txt;
            }

            if (!"".equals(label)) {
                setupKeyBackground(key, isOperator[i]);
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
        Context context = getContext();
        StateListDrawable states = new StateListDrawable();

        // 10% opacity primary for operator background
        int operatorBgColor = ContextCompat.getColor(context, R.color.finan_primary);
        operatorBgColor = (operatorBgColor & 0x00FFFFFF) | 0x1A000000;
        
        // 5% opacity black/surface for number background
        int numberBgColor = ContextCompat.getColor(context, R.color.finan_text_primary);
        numberBgColor = (numberBgColor & 0x00FFFFFF) | 0x0A000000;

        int normalColor = isOperator ? operatorBgColor : numberBgColor;
        int pressedColor = ContextCompat.getColor(context, R.color.finan_key_bg_pressed);

        int cornerRadius = dpToPx(12);

        GradientDrawable normalShape = new GradientDrawable();
        normalShape.setShape(GradientDrawable.RECTANGLE);
        normalShape.setCornerRadius(cornerRadius);
        normalShape.setColor(normalColor);

        GradientDrawable pressedShape = new GradientDrawable();
        pressedShape.setShape(GradientDrawable.RECTANGLE);
        pressedShape.setCornerRadius(cornerRadius);
        pressedShape.setColor(pressedColor);

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
