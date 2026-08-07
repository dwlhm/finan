package com.dwlhm.finan.ui.common;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class FilterDialog {

  public interface ApplyListener {
    void onApply(@NonNull List<Long> selectedIds);
  }

  public interface ResetListener {
    void onReset();
  }

  public static final class Option {
    private final Long id;
    private final String label;

    public Option(@Nullable Long id, @NonNull String label) {
      this.id = id;
      this.label = label;
    }

    @Nullable
    public Long getId() {
      return id;
    }

    @NonNull
    public String getLabel() {
      return label;
    }
  }

  public static final class Group {
    private final String label;
    private final String pickerTitle;
    private final List<Option> options;
    private final Long selectedId;

    public Group(
        @NonNull String label,
        @NonNull String pickerTitle,
        @NonNull List<Option> options,
        @Nullable Long selectedId) {
      this.label = label;
      this.pickerTitle = pickerTitle;
      this.options = Collections.unmodifiableList(new ArrayList<>(options));
      this.selectedId = selectedId;
    }
  }

  private FilterDialog() {}

  public static void show(
      @NonNull Context context,
      @NonNull String title,
      @NonNull String applyText,
      @NonNull String resetText,
      @NonNull List<Group> groups,
      @NonNull ApplyListener applyListener,
      @NonNull ResetListener resetListener) {
    BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Finan_BottomSheetDialog);

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundResource(R.drawable.bg_bottom_sheet);
    int horizontalPadding = UiComponentStyles.dp(context, 20);
    int topPadding = UiComponentStyles.dp(context, 12);
    int bottomPadding = UiComponentStyles.dp(context, 20);
    root.setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding);

    View handle = new View(context);
    LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(
        UiComponentStyles.dp(context, 40), UiComponentStyles.dp(context, 4));
    handleLp.gravity = Gravity.CENTER_HORIZONTAL;
    handleLp.bottomMargin = UiComponentStyles.dp(context, 16);
    handle.setLayoutParams(handleLp);
    handle.setBackgroundResource(R.drawable.bg_bottom_sheet_handle);
    root.addView(handle);

    if (!TextUtils.isEmpty(title)) {
      TextView titleView = new TextView(context);
      titleView.setText(title);
      titleView.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
      titleView.setTextSize(18f);
      titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
      root.addView(titleView);
    }

    ArrayList<Long> pendingIds = new ArrayList<>();
    ArrayList<TextView> valueViews = new ArrayList<>();

    LinearLayout content = new LinearLayout(context);
    content.setOrientation(LinearLayout.VERTICAL);

    for (int i = 0; i < groups.size(); i++) {
      Group group = groups.get(i);
      pendingIds.add(group.selectedId);
      TextView valueView = createPickerValue(context, optionLabel(group, group.selectedId));
      final int groupIndex = i;
      valueView.setOnClickListener(
          v ->
              showChoiceDialog(
                  context,
                  group,
                  pendingIds.get(groupIndex),
                  selectedId -> {
                    pendingIds.set(groupIndex, selectedId);
                    valueViews.get(groupIndex).setText(optionLabel(group, selectedId));
                  }));
      valueViews.add(valueView);
      content.addView(createPickerRow(context, group.label, valueView));
    }
    root.addView(content);

    TextView resetButton = new TextView(context);
    resetButton.setText(resetText);
    resetButton.setTextColor(ContextCompat.getColor(context, R.color.finan_warm_accent));
    resetButton.setTextSize(13f);
    resetButton.setTypeface(resetButton.getTypeface(), Typeface.BOLD);
    resetButton.setClickable(true);
    resetButton.setFocusable(true);
    int p = UiComponentStyles.dp(context, 4);
    resetButton.setPadding(p, p, p, p);
    LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    resetLp.topMargin = UiComponentStyles.dp(context, 16);
    resetButton.setLayoutParams(resetLp);
    resetButton.setOnClickListener(v -> {
      resetListener.onReset();
      dialog.dismiss();
    });
    root.addView(resetButton);

    DialogActionsView actions = new DialogActionsView(context);
    actions.setPrimaryText(applyText);
    LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    actionsLp.topMargin = UiComponentStyles.dp(context, 12);
    actions.setLayoutParams(actionsLp);
    actions.setOnPrimaryClickListener(v -> {
      applyListener.onApply(pendingIds);
      dialog.dismiss();
    });
    actions.setOnCancelClickListener(v -> dialog.dismiss());
    root.addView(actions);

    dialog.setContentView(root);
    BottomSheetHelper.show(dialog);
  }

  private static void showChoiceDialog(
      Context context, Group group, Long currentId, ChoiceListener listener) {
    String[] labels = new String[group.options.size()];
    int checkedIndex = 0;
    for (int i = 0; i < group.options.size(); i++) {
      Option option = group.options.get(i);
      labels[i] = option.getLabel();
      if (sameId(currentId, option.getId())) {
        checkedIndex = i;
      }
    }

    BottomSheetHelper.showOptionPicker(
        context,
        group.pickerTitle,
        labels,
        checkedIndex,
        (which, option) -> listener.onChoice(group.options.get(which).getId()));
  }

  private static LinearLayout createPickerRow(
      Context context, String labelText, TextView valueView) {
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams rowParams =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = UiComponentStyles.dp(context, 10);
    row.setLayoutParams(rowParams);

    TextView label = new TextView(context);
    label.setText(labelText);
    label.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
    label.setTextSize(12f);
    label.setTypeface(label.getTypeface(), Typeface.BOLD);
    row.addView(label);

    LinearLayout.LayoutParams valueParams =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    valueParams.topMargin = UiComponentStyles.dp(context, 6);
    row.addView(valueView, valueParams);
    return row;
  }

  private static TextView createPickerValue(Context context, String text) {
    TextView value = new TextView(context);
    value.setBackgroundResource(R.drawable.bg_control_surface);
    value.setClickable(true);
    value.setEllipsize(TextUtils.TruncateAt.END);
    value.setFocusable(true);
    value.setForeground(
        ContextCompat.getDrawable(context, UiComponentStyles.selectableItemBackground(context)));
    value.setGravity(Gravity.CENTER_VERTICAL);
    value.setMaxLines(1);
    value.setMinHeight(UiComponentStyles.dp(context, 46));
    value.setPadding(
        UiComponentStyles.dp(context, 12),
        0,
        UiComponentStyles.dp(context, 12),
        0);
    value.setText(text);
    value.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
    value.setTextSize(15f);
    return value;
  }

  private static String optionLabel(Group group, Long selectedId) {
    for (Option option : group.options) {
      if (sameId(selectedId, option.getId())) {
        return option.getLabel();
      }
    }
    return group.options.isEmpty() ? "" : group.options.get(0).getLabel();
  }

  private static boolean sameId(Long left, Long right) {
    if (left == null || right == null) {
      return left == right;
    }
    return left.equals(right);
  }

  private interface ChoiceListener {
    void onChoice(Long id);
  }
}
