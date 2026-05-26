package com.dwlhm.finan.ui.common;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.dwlhm.finan.R;

public final class CollapsibleController {

  private final View toggle;
  private final View panel;
  private final ImageView indicator;
  @DrawableRes private final int expandedIconRes;
  @DrawableRes private final int collapsedIconRes;

  public static CollapsibleController bind(
      @NonNull View root,
      int toggleId,
      int panelId,
      int indicatorId) {
    return bind(
        root,
        toggleId,
        panelId,
        indicatorId,
        R.drawable.ic_expand_less,
        R.drawable.ic_expand_more);
  }

  public static CollapsibleController bind(
      @NonNull View root,
      int toggleId,
      int panelId,
      int indicatorId,
      @DrawableRes int expandedIconRes,
      @DrawableRes int collapsedIconRes) {
    return new CollapsibleController(
        root.findViewById(toggleId),
        root.findViewById(panelId),
        root.findViewById(indicatorId),
        expandedIconRes,
        collapsedIconRes);
  }

  private CollapsibleController(
      @NonNull View toggle,
      @NonNull View panel,
      @NonNull ImageView indicator,
      @DrawableRes int expandedIconRes,
      @DrawableRes int collapsedIconRes) {
    this.toggle = toggle;
    this.panel = panel;
    this.indicator = indicator;
    this.expandedIconRes = expandedIconRes;
    this.collapsedIconRes = collapsedIconRes;
    this.toggle.setOnClickListener(v -> setExpanded(!isExpanded()));
    render();
  }

  public boolean isExpanded() {
    return panel.getVisibility() == View.VISIBLE;
  }

  public void setExpanded(boolean expanded) {
    panel.setVisibility(expanded ? View.VISIBLE : View.GONE);
    render();
  }

  private void render() {
    boolean expanded = isExpanded();
    toggle.setSelected(expanded);
    indicator.setImageResource(expanded ? expandedIconRes : collapsedIconRes);
  }
}
