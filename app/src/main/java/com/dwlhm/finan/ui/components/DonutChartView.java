package com.dwlhm.finan.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class DonutChartView extends View {

    public static final class DonutItem {
        private final String label;
        private final long value;
        private final int color;

        public DonutItem(String label, long value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public long getValue() {
            return value;
        }

        public int getColor() {
            return color;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF outerRect = new RectF();
    private final RectF innerRect = new RectF();
    
    private final List<DonutItem> inflowItems = new ArrayList<>();
    private final List<DonutItem> outflowItems = new ArrayList<>();
    private long totalInflow = 0;
    private long totalOutflow = 0;
    private float animationProgress = 0f;
    private ValueAnimator animator;

    private String centerLine1 = "";
    private String centerLine2 = "";

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND); // Premium visual style with rounded ends
    }

    public void setData(List<DonutItem> inflows, List<DonutItem> outflows) {
        this.inflowItems.clear();
        this.inflowItems.addAll(inflows);
        this.outflowItems.clear();
        this.outflowItems.addAll(outflows);
        
        this.totalInflow = 0;
        for (DonutItem item : inflows) {
            this.totalInflow += item.getValue();
        }
        
        this.totalOutflow = 0;
        for (DonutItem item : outflows) {
            this.totalOutflow += item.getValue();
        }
        
        startAnimation();
    }

    public void setCenterText(String line1, String line2) {
        this.centerLine1 = line1 != null ? line1 : "";
        this.centerLine2 = line2 != null ? line2 : "";
        invalidate();
    }

    public void startAnimation() {
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(800);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float height = getHeight() - getPaddingTop() - getPaddingBottom();
        float size = Math.min(width, height);
        if (size <= 0) return;

        float strokeWidth = size * 0.10f; // clean stroke thickness
        float spacing = size * 0.04f; // gap between concentric rings

        float centerX = getPaddingLeft() + width / 2f;
        float centerY = getPaddingTop() + height / 2f;

        // Outer Rect
        outerRect.set(
            centerX - size / 2f + strokeWidth / 2f,
            centerY - size / 2f + strokeWidth / 2f,
            centerX + size / 2f - strokeWidth / 2f,
            centerY + size / 2f - strokeWidth / 2f
        );

        // Inner Rect
        float innerOffset = strokeWidth + spacing;
        innerRect.set(
            outerRect.left + innerOffset,
            outerRect.top + innerOffset,
            outerRect.right - innerOffset,
            outerRect.bottom - innerOffset
        );

        paint.setStrokeWidth(strokeWidth);

        // 1. Draw Outer Ring Inflow
        // 1a. Draw background track (light-gray)
        paint.setColor(0xFFE9EEF2);
        canvas.drawArc(outerRect, -90f, 360f, false, paint);

        // 1b. Draw Inflow slices
        if (totalInflow > 0) {
            float startAngle = -90f;
            float gapAngle = inflowItems.size() > 1 ? 6f : 0f; // spacing between rounded cap slices
            for (DonutItem item : inflowItems) {
                float sweepAngle = (item.getValue() / (float) totalInflow) * 360f;
                paint.setColor(item.getColor());
                if (sweepAngle > gapAngle) {
                    float drawSweep = (sweepAngle - gapAngle) * animationProgress;
                    canvas.drawArc(outerRect, startAngle + gapAngle / 2f, drawSweep, false, paint);
                }
                startAngle += sweepAngle;
            }
        }

        // 2. Draw Inner Ring Outflow relative to Inflow
        // 2a. Draw background track (light-gray)
        paint.setColor(0xFFE9EEF2);
        canvas.drawArc(innerRect, -90f, 360f, false, paint);

        // 2b. Draw Outflow slices
        if (totalOutflow > 0) {
            float totalSweep = 360f;
            if (totalInflow > 0) {
                totalSweep = ((float) totalOutflow / (float) totalInflow) * 360f;
                if (totalSweep > 360f) {
                    totalSweep = 360f;
                }
            }

            float startAngle = -90f;
            float gapAngle = outflowItems.size() > 1 ? 6f : 0f;
            for (DonutItem item : outflowItems) {
                float sweepAngle = (item.getValue() / (float) totalOutflow) * totalSweep;
                paint.setColor(item.getColor());
                if (sweepAngle > gapAngle) {
                    float drawSweep = (sweepAngle - gapAngle) * animationProgress;
                    canvas.drawArc(innerRect, startAngle + gapAngle / 2f, drawSweep, false, paint);
                }
                startAngle += sweepAngle;
            }
        }

        // 3. Draw Center Text
        if (!centerLine1.isEmpty()) {
            textPaint.setTextSize(size * 0.08f);
            textPaint.setColor(0xFF5C6B73); // finan_text_secondary (darker gray for high contrast)
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            canvas.drawText(centerLine1, centerX, centerY - size * 0.02f, textPaint);
        }

        if (!centerLine2.isEmpty()) {
            textPaint.setTextSize(size * 0.12f);
            textPaint.setColor(0xFF2D6A6A); // finan_primary (bold theme teal)
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(centerLine2, centerX, centerY + size * 0.09f, textPaint);
        }
    }
}
