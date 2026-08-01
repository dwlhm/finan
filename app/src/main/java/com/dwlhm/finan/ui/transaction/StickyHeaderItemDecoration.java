package com.dwlhm.finan.ui.transaction;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StickyHeaderItemDecoration extends RecyclerView.ItemDecoration {

    private final StickyHeaderInterface mListener;
    private int mStickyHeaderHeight;
    private int mStickyYOffset;

    public StickyHeaderItemDecoration(@NonNull StickyHeaderInterface listener) {
        mListener = listener;
    }

    public void setStickyYOffset(int offset) {
        mStickyYOffset = offset;
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        View topChild = parent.getChildAt(0);
        if (topChild == null) {
            return;
        }

        int topChildPosition = parent.getChildAdapterPosition(topChild);
        if (topChildPosition == RecyclerView.NO_POSITION) {
            return;
        }

        int headerPos = mListener.getHeaderPositionForItem(topChildPosition);
        if (headerPos == RecyclerView.NO_POSITION) return;
        View currentHeader = getHeaderViewForItem(headerPos, parent);
        fixLayoutSize(parent, currentHeader);
        int contactPoint = mStickyYOffset + currentHeader.getBottom();
        View childInContact = getChildInContact(parent, contactPoint, headerPos);

        if (childInContact != null && mListener.isHeader(parent.getChildAdapterPosition(childInContact))) {
            moveHeader(c, currentHeader, childInContact, parent);
            return;
        }

        drawHeader(c, currentHeader, parent);
    }

    private View getHeaderViewForItem(int headerPosition, RecyclerView parent) {
        int layoutResId = mListener.getHeaderLayout(headerPosition);
        View header = android.view.LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        mListener.bindHeaderData(header, headerPosition);
        return header;
    }

    private void drawHeader(Canvas c, View header, RecyclerView parent) {
        c.save();
        // Determine the natural position of the current header
        View firstView = parent.getChildAt(0);
        int topPos = parent.getChildAdapterPosition(firstView);
        int naturalTop = firstView.getTop();
        if (mListener.isHeader(topPos)) {
            // If the first view is a header itself, its natural top is where it is
        } else {
            // Otherwise it's sticking
            naturalTop = mStickyYOffset;
        }
        
        c.translate(parent.getPaddingLeft(), Math.max(mStickyYOffset, naturalTop));
        header.draw(c);
        c.restore();
    }

    private void moveHeader(Canvas c, View currentHeader, View nextHeader, RecyclerView parent) {
        c.save();
        int targetY = nextHeader.getTop() - currentHeader.getHeight();
        c.translate(parent.getPaddingLeft(), Math.min(mStickyYOffset, targetY));
        currentHeader.draw(c);
        c.restore();
    }

    private View getChildInContact(RecyclerView parent, int contactPoint, int currentHeaderPos) {
        View childInContact = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int childPos = parent.getChildAdapterPosition(child);
            
            if (childPos == currentHeaderPos) {
                continue; // Do not push yourself
            }

            int heightTolerance = 0;
            boolean isChildHeader = mListener.isHeader(childPos);
            if (isChildHeader) {
                heightTolerance = mStickyHeaderHeight - child.getHeight();
            }
            
            int childBottomPosition;
            if (child.getTop() > 0) {
                childBottomPosition = child.getBottom() + heightTolerance;
            } else {
                childBottomPosition = child.getBottom();
            }

            if (childBottomPosition > contactPoint) {
                if (child.getTop() <= contactPoint) {
                    childInContact = child;
                    break;
                }
            }
        }
        return childInContact;
    }

    private void fixLayoutSize(ViewGroup parent, View view) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);

        int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec, parent.getPaddingLeft() + parent.getPaddingRight(), view.getLayoutParams() != null ? view.getLayoutParams().width : ViewGroup.LayoutParams.MATCH_PARENT);
        int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec, parent.getPaddingTop() + parent.getPaddingBottom(), view.getLayoutParams() != null ? view.getLayoutParams().height : ViewGroup.LayoutParams.WRAP_CONTENT);

        view.measure(childWidthSpec, childHeightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), mStickyHeaderHeight = view.getMeasuredHeight());
    }

    public interface StickyHeaderInterface {
        int getHeaderPositionForItem(int itemPosition);
        int getHeaderLayout(int headerPosition);
        void bindHeaderData(View header, int headerPosition);
        boolean isHeader(int itemPosition);
    }
}
