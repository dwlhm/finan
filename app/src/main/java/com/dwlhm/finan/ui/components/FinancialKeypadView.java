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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;

public class FinancialKeypadView extends ViewGroup {

    private OnKeypadActionListener listener;

    private static final int ROWS = 4;
    private static final int COLS = 3;

    private final String[] keyLabels = {
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            "000", "0", "⌫"
    };

    private boolean showDecimal = true;
    private final View[] keyViews = new View[ROWS * COLS];
    private Paint borderPaint;

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

        borderPaint = new Paint();
        borderPaint.setColor(ContextCompat.getColor(context, R.color.finan_key_border));
        borderPaint.setStrokeWidth(dpToPx(1));

        int horizontalPadding = 0; // Removed horizontal padding to make it wider
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
                txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                txt.setTypeface(Typeface.DEFAULT_BOLD);
                txt.setTextColor(ContextCompat.getColor(context, R.color.finan_key_text));
                key = txt;
            }

            if (!"".equals(label)) {
                setupKeyBackground(key);
                key.setOnClickListener(v -> handleKeyClick(label));
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

    private void setupKeyBackground(View view) {
        Context context = getContext();
        StateListDrawable states = new StateListDrawable();

        int normalColor = android.graphics.Color.TRANSPARENT;
        int pressedColor = ContextCompat.getColor(context, R.color.finan_key_bg_pressed);

        GradientDrawable normalShape = new GradientDrawable();
        normalShape.setShape(GradientDrawable.RECTANGLE);
        normalShape.setCornerRadius(0);
        normalShape.setColor(normalColor);

        GradientDrawable pressedShape = new GradientDrawable();
        pressedShape.setShape(GradientDrawable.RECTANGLE);
        pressedShape.setCornerRadius(0);
        pressedShape.setColor(pressedColor);

        states.addState(new int[] { android.R.attr.state_pressed }, pressedShape);
        states.addState(new int[] {}, normalShape);

        view.setBackground(states);
    }

    public void setOnKeypadActionListener(OnKeypadActionListener listener) {
        this.listener = listener;
    }

    private void handleKeyClick(String label) {
        if (listener == null)
            return;

        switch (label) {
            case "⌫":
                listener.onBackspace();
                break;
            case "000":
            case "00":
                listener.onShortcut(label);
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

        int spacing = 0;
        int keyWidth = (availableWidth - (COLS - 1) * spacing) / COLS;
        int keyHeight = (int) (keyWidth * 0.65f);

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
        int spacing = 0;

        int availableWidth = getWidth() - paddingLeft - getPaddingRight();
        int keyWidth = (availableWidth - (COLS - 1) * spacing) / COLS;
        int keyHeight = (int) (keyWidth * 0.65f);

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

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (getChildCount() > 0) {
            int paddingLeft = getPaddingLeft();
            int paddingTop = getPaddingTop();
            int paddingRight = getPaddingRight();
            int paddingBottom = getPaddingBottom();

            View firstChild = getChildAt(0);
            int childWidth = firstChild.getWidth();
            int childHeight = firstChild.getHeight();

            int width = getWidth();
            int height = getHeight();

            for (int col = 1; col < COLS; col++) {
                int x = paddingLeft + col * childWidth;
                canvas.drawLine(x, paddingTop, x, height - paddingBottom, borderPaint);
            }

            for (int row = 1; row < ROWS; row++) {
                int y = paddingTop + row * childHeight;
                canvas.drawLine(paddingLeft, y, width - paddingRight, y, borderPaint);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
