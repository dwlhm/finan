package com.dwlhm.finan.ui.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.google.android.material.card.MaterialCardView;

public class FloatingBottomNavView extends MaterialCardView {

  public interface OnItemSelectedListener {
    boolean onItemSelected(@IdRes int itemId);
  }

  private View glassBackdrop;
  private View itemCapture;
  private ImageView iconCapture;
  private TextView labelCapture;

  private View itemDashboard;
  private ImageView iconDashboard;
  private TextView labelDashboard;

  private View itemSettings;
  private ImageView iconSettings;
  private TextView labelSettings;

  private int selectedItemId = R.id.nav_capture;
  private OnItemSelectedListener listener;

  public FloatingBottomNavView(@NonNull Context context) {
    super(context);
    init(context);
  }

  public FloatingBottomNavView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public FloatingBottomNavView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    LayoutInflater.from(context).inflate(R.layout.view_floating_bottom_nav, this, true);

    setRadius(dpToPx(context, 34));
    setCardElevation(dpToPx(context, 12));
    setPreventCornerOverlap(false);
    setCardBackgroundColor(Color.TRANSPARENT);

    glassBackdrop = findViewById(R.id.nav_glass_backdrop);

    // Apply real-time frosted glass backdrop blur ONLY to Layer 1 on Android 12+ (API 31+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && glassBackdrop != null) {
      try {
        glassBackdrop.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP));
      } catch (Exception ignored) {
      }
    }

    itemCapture = findViewById(R.id.nav_item_capture);
    iconCapture = findViewById(R.id.nav_icon_capture);
    labelCapture = findViewById(R.id.nav_label_capture);

    itemDashboard = findViewById(R.id.nav_item_dashboard);
    iconDashboard = findViewById(R.id.nav_icon_dashboard);
    labelDashboard = findViewById(R.id.nav_label_dashboard);

    itemSettings = findViewById(R.id.nav_item_settings);
    iconSettings = findViewById(R.id.nav_icon_settings);
    labelSettings = findViewById(R.id.nav_label_settings);

    itemCapture.setOnClickListener(v -> selectItem(R.id.nav_capture, true));
    itemDashboard.setOnClickListener(v -> selectItem(R.id.nav_dashboard, true));
    itemSettings.setOnClickListener(v -> selectItem(R.id.nav_settings, true));

    updateUi(false);
  }

  public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
    this.listener = listener;
  }

  public int getSelectedItemId() {
    return selectedItemId;
  }

  public void setSelectedItemId(@IdRes int itemId) {
    selectItem(itemId, false);
  }

  private void selectItem(@IdRes int itemId, boolean fromUser) {
    if (fromUser && listener != null && selectedItemId != itemId) {
      boolean handled = listener.onItemSelected(itemId);
      if (!handled) {
        return;
      }
    }

    if (selectedItemId != itemId) {
      selectedItemId = itemId;
      updateUi(true);
    }
  }

  private void updateUi(boolean animate) {
    if (animate) {
      TransitionManager.beginDelayedTransition(
          this, new AutoTransition().setDuration(180));
    }

    boolean isNight = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
        == android.content.res.Configuration.UI_MODE_NIGHT_YES;

    // Translucent Liquid Glass colors
    int cardGlassBg = isNight ? 0xB31E1E1E : 0xCCFFFFFF; // 70% - 80% translucent backdrop
    int strokeGlassRim = isNight ? 0x40FFFFFF : 0x40000000; // Glossy edge reflection rim
    int primaryColor = ContextCompat.getColor(getContext(), R.color.finan_primary);
    int secondaryColor = ContextCompat.getColor(getContext(), R.color.finan_text_secondary);
    int activePillGlassBg = isNight ? 0xE0264A4A : 0xE6E0F2F1; // Glossy active pill tint

    if (glassBackdrop != null) {
      glassBackdrop.setBackgroundColor(cardGlassBg);
    }
    setStrokeColor(strokeGlassRim);
    setStrokeWidth((int) dpToPx(getContext(), 1.5f));

    updateItemState(itemCapture, iconCapture, labelCapture, selectedItemId == R.id.nav_capture, primaryColor, secondaryColor, activePillGlassBg);
    updateItemState(itemDashboard, iconDashboard, labelDashboard, selectedItemId == R.id.nav_dashboard, primaryColor, secondaryColor, activePillGlassBg);
    updateItemState(itemSettings, iconSettings, labelSettings, selectedItemId == R.id.nav_settings, primaryColor, secondaryColor, activePillGlassBg);
  }

  private void updateItemState(
      View item,
      ImageView icon,
      TextView label,
      boolean active,
      int primaryColor,
      int secondaryColor,
      int activeBg) {

    if (active) {
      item.setBackground(createPillDrawable(activeBg));
      icon.setColorFilter(primaryColor);
      label.setTextColor(primaryColor);
      label.setVisibility(View.VISIBLE);
    } else {
      item.setBackground(null);
      icon.setColorFilter(secondaryColor);
      label.setVisibility(View.GONE);
    }
  }

  private GradientDrawable createPillDrawable(int color) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setShape(GradientDrawable.RECTANGLE);
    drawable.setCornerRadius(dpToPx(getContext(), 24));
    drawable.setColor(color);
    return drawable;
  }

  private static float dpToPx(Context context, float dp) {
    return dp * context.getResources().getDisplayMetrics().density;
  }
}
