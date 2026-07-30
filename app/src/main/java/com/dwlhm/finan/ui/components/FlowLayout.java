package com.dwlhm.finan.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class FlowLayout extends ViewGroup {
    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int currentWidth = 0;
        int currentHeight = 0;
        int currentLineWidth = 0;
        int currentLineHeight = 0;

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            measureChild(child, widthMeasureSpec, heightMeasureSpec);

            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

            if (widthMode != MeasureSpec.UNSPECIFIED && currentLineWidth + childWidth > widthSize) {
                currentWidth = Math.max(currentWidth, currentLineWidth);
                currentHeight += currentLineHeight;
                currentLineWidth = childWidth;
                currentLineHeight = childHeight;
            } else {
                currentLineWidth += childWidth;
                currentLineHeight = Math.max(currentLineHeight, childHeight);
            }

            if (i == count - 1) {
                currentWidth = Math.max(currentWidth, currentLineWidth);
                currentHeight += currentLineHeight;
            }
        }

        int measuredWidth = widthMode == MeasureSpec.EXACTLY ? widthSize + getPaddingLeft() + getPaddingRight() : currentWidth + getPaddingLeft() + getPaddingRight();
        int measuredHeight = heightMode == MeasureSpec.EXACTLY ? heightSize + getPaddingTop() + getPaddingBottom() : currentHeight + getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int xPos = getPaddingLeft();
        int yPos = getPaddingTop();
        int currentLineHeight = 0;

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (xPos + childWidth + lp.leftMargin + lp.rightMargin > width - getPaddingRight()) {
                xPos = getPaddingLeft();
                yPos += currentLineHeight;
                currentLineHeight = 0;
            }

            int topOffset = lp.topMargin;
            
            if (childWidth > 0 && childHeight > 0) {
                // If gravity is bottom (this requires a custom attr, but we'll approximate it by assuming all items align bottom of line)
                // A true FlowLayout would do this better, but we can just align items vertically centered or top for now.
                // Or let's align bottom by using the layout height
            }

            child.layout(xPos + lp.leftMargin, yPos + topOffset, xPos + lp.leftMargin + childWidth, yPos + topOffset + childHeight);

            xPos += childWidth + lp.leftMargin + lp.rightMargin;
            currentLineHeight = Math.max(currentLineHeight, childHeight + lp.topMargin + lp.bottomMargin);
        }
        
        // Relayout aligned to baseline of each line
        xPos = getPaddingLeft();
        yPos = getPaddingTop();
        currentLineHeight = 0;
        
        // Second pass: actually align them to the center of the line
        int lineStart = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (xPos + childWidth + lp.leftMargin + lp.rightMargin > width - getPaddingRight() && i > lineStart) {
                // Find max height for this line
                int maxLineHeight = 0;
                for (int j = lineStart; j < i; j++) {
                    View c = getChildAt(j);
                    if (c.getVisibility() == GONE) continue;
                    MarginLayoutParams clp = (MarginLayoutParams) c.getLayoutParams();
                    maxLineHeight = Math.max(maxLineHeight, c.getMeasuredHeight() + clp.topMargin + clp.bottomMargin);
                }

                // Relayout previous line
                int currX = getPaddingLeft();
                for (int j = lineStart; j < i; j++) {
                    View c = getChildAt(j);
                    if (c.getVisibility() == GONE) continue;
                    MarginLayoutParams clp = (MarginLayoutParams) c.getLayoutParams();
                    int cW = c.getMeasuredWidth();
                    int cH = c.getMeasuredHeight();
                    int topOffset = (maxLineHeight - (cH + clp.topMargin + clp.bottomMargin)) / 2;
                    int cTop = yPos + clp.topMargin + topOffset;
                    c.layout(currX + clp.leftMargin, cTop, currX + clp.leftMargin + cW, cTop + cH);
                    currX += cW + clp.leftMargin + clp.rightMargin;
                }
                
                xPos = getPaddingLeft();
                yPos += currentLineHeight;
                currentLineHeight = 0;
                lineStart = i;
            }

            xPos += childWidth + lp.leftMargin + lp.rightMargin;
            currentLineHeight = Math.max(currentLineHeight, childHeight + lp.topMargin + lp.bottomMargin);
        }
        
        // Last line
        int maxLineHeight = 0;
        for (int j = lineStart; j < count; j++) {
            View c = getChildAt(j);
            if (c.getVisibility() == GONE) continue;
            MarginLayoutParams clp = (MarginLayoutParams) c.getLayoutParams();
            maxLineHeight = Math.max(maxLineHeight, c.getMeasuredHeight() + clp.topMargin + clp.bottomMargin);
        }

        int currX = getPaddingLeft();
        for (int j = lineStart; j < count; j++) {
            View c = getChildAt(j);
            if (c.getVisibility() == GONE) continue;
            MarginLayoutParams clp = (MarginLayoutParams) c.getLayoutParams();
            int cW = c.getMeasuredWidth();
            int cH = c.getMeasuredHeight();
            int topOffset = (maxLineHeight - (cH + clp.topMargin + clp.bottomMargin)) / 2;
            int cTop = yPos + clp.topMargin + topOffset;
            c.layout(currX + clp.leftMargin, cTop, currX + clp.leftMargin + cW, cTop + cH);
            currX += cW + clp.leftMargin + clp.rightMargin;
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }
}
