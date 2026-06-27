package com.dwlhm.finan.ui.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dwlhm.finan.R;

public class FinanToast {

    private static final int DURATION_MS = 4000;
    private static final int ANIMATION_DURATION_MS = 300;
    
    private final Activity activity;
    private final View toastView;
    private final Handler handler;
    private final Runnable dismissRunnable;
    
    private float initialTouchX;
    private float initialTouchY;
    private boolean isDismissing = false;

    private FinanToast(Activity activity, String message, String actionText, Runnable onAction) {
        this.activity = activity;
        this.handler = new Handler(Looper.getMainLooper());
        this.dismissRunnable = this::dismiss;

        LayoutInflater inflater = LayoutInflater.from(activity);
        toastView = inflater.inflate(R.layout.view_finan_toast, null);

        TextView messageView = toastView.findViewById(R.id.finan_toast_message);
        Button actionButton = toastView.findViewById(R.id.finan_toast_action);

        messageView.setText(message);

        if (actionText != null && !actionText.isEmpty() && onAction != null) {
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setText(actionText);
            actionButton.setOnClickListener(v -> {
                onAction.run();
                dismiss();
            });
        } else {
            actionButton.setVisibility(View.GONE);
        }

        setupTouchListener();
    }

    public static FinanToast show(Activity activity, String message, String actionText, Runnable onAction) {
        FinanToast toast = new FinanToast(activity, message, actionText, onAction);
        toast.show();
        return toast;
    }

    public static FinanToast show(Activity activity, String message) {
        return show(activity, message, null, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener() {
        toastView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    handler.removeCallbacks(dismissRunnable); // Pause auto-dismiss
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - initialTouchX;
                    float deltaY = event.getRawY() - initialTouchY;
                    
                    // Allow dragging down, left, or right
                    if (deltaY > 0 || Math.abs(deltaX) > 0) {
                        toastView.setTranslationX(deltaX);
                        if (deltaY > 0) {
                            toastView.setTranslationY(deltaY);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float finalDeltaX = event.getRawX() - initialTouchX;
                    float finalDeltaY = event.getRawY() - initialTouchY;
                    
                    // Threshold to dismiss
                    float dismissThreshold = v.getWidth() / 4f;
                    float verticalThreshold = v.getHeight() / 2f;

                    if (Math.abs(finalDeltaX) > dismissThreshold || finalDeltaY > verticalThreshold) {
                        dismissWithSwipe(finalDeltaX, finalDeltaY);
                    } else {
                        // Snap back
                        toastView.animate()
                                .translationX(0)
                                .translationY(0)
                                .setDuration(200)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                        // Resume auto-dismiss
                        handler.postDelayed(dismissRunnable, DURATION_MS);
                    }
                    // Prevent click events if dragged
                    if (Math.abs(finalDeltaX) > 10 || Math.abs(finalDeltaY) > 10) {
                        return true;
                    }
                    // Otherwise let standard click processing happen (though we consume the event, we can let child views handle clicks natively if we dispatch properly, 
                    // but for a layout with buttons, intercepting touches could block the button.
                    // Wait, returning true consumes the event and buttons won't get clicked.
                    break;
            }
            return false; // let the button process its own clicks if not dragging
        });
    }

    private void show() {
        ViewGroup root = activity.findViewById(android.R.id.content);
        
        // Remove existing toast if any
        View existing = root.findViewWithTag("finan_toast");
        if (existing != null) {
            root.removeView(existing);
        }
        
        toastView.setTag("finan_toast");

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.BOTTOM;
        
        root.addView(toastView, params);

        // Entry animation
        toastView.setAlpha(0f);
        toastView.setTranslationY(100f);
        toastView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ANIMATION_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        handler.postDelayed(dismissRunnable, DURATION_MS);
    }

    public void dismiss() {
        if (isDismissing) return;
        isDismissing = true;
        
        handler.removeCallbacks(dismissRunnable);

        toastView.animate()
                .alpha(0f)
                .translationY(toastView.getHeight() > 0 ? toastView.getHeight() : 100f)
                .setDuration(ANIMATION_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ViewGroup root = (ViewGroup) toastView.getParent();
                        if (root != null) {
                            root.removeView(toastView);
                        }
                    }
                })
                .start();
    }

    private void dismissWithSwipe(float deltaX, float deltaY) {
        if (isDismissing) return;
        isDismissing = true;
        handler.removeCallbacks(dismissRunnable);

        float endTranslationX = deltaX;
        float endTranslationY = deltaY;
        
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            endTranslationX = deltaX > 0 ? toastView.getWidth() : -toastView.getWidth();
        } else {
            endTranslationY = toastView.getHeight();
        }

        toastView.animate()
                .alpha(0f)
                .translationX(endTranslationX)
                .translationY(endTranslationY)
                .setDuration(ANIMATION_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ViewGroup root = (ViewGroup) toastView.getParent();
                        if (root != null) {
                            root.removeView(toastView);
                        }
                    }
                })
                .start();
    }
}
