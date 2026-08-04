package com.dwlhm.finan.ui.history;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.CustomDatePickerView;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.util.date.DateRange;
import com.dwlhm.finan.util.date.PayrollCycleResolver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class HistoryDateRangeBottomSheet extends BottomSheetDialog {

  public interface OnRangeConfirmedListener {
    void onRangeConfirmed(@Nullable LocalDate startDate, @Nullable LocalDate endDate);
  }

  private final OnRangeConfirmedListener listener;
  private long pendingStartMillis;
  private long pendingEndMillis;
  private boolean hasPendingRange;

  public HistoryDateRangeBottomSheet(
      @NonNull Context context,
      @Nullable LocalDate initialStart,
      @Nullable LocalDate initialEnd,
      @NonNull OnRangeConfirmedListener listener) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.listener = listener;
    if (initialStart != null && initialEnd != null) {
      this.pendingStartMillis = initialStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      this.pendingEndMillis = initialEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      this.hasPendingRange = true;
    } else {
      int cutoffDay = context.getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
          .getInt("cutoff_day", 1);
      DateRange range = PayrollCycleResolver.forDate(LocalDate.now(), cutoffDay);
      this.pendingStartMillis = range.getStart().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      this.pendingEndMillis = range.getEnd().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      this.hasPendingRange = false;
    }
  }

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_history_date_range_bottom_sheet);

    Window window = getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      WindowManager.LayoutParams params = window.getAttributes();
      params.width = WindowManager.LayoutParams.MATCH_PARENT;
      params.height = WindowManager.LayoutParams.WRAP_CONTENT;
      params.gravity = android.view.Gravity.BOTTOM;
      window.setAttributes(params);
      window.setWindowAnimations(android.R.style.Animation_InputMethod);
    }

    CustomDatePickerView datePicker = findViewById(R.id.history_date_range_picker);
    TextView resetButton = findViewById(R.id.history_date_range_reset);
    DialogActionsView actions = findViewById(R.id.history_date_range_actions);
    if (datePicker != null) {
      datePicker.setRangeMode(true);
      if (hasPendingRange) {
        datePicker.setRange(pendingStartMillis, pendingEndMillis);
      } else {
        int cutoffDay = getContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
            .getInt("cutoff_day", 1);
        DateRange range = PayrollCycleResolver.forDate(LocalDate.now(), cutoffDay);
        long start = range.getStart().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end = range.getEnd().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        datePicker.setRange(start, end);
      }
      datePicker.setOnRangeSelectedListener((start, end) -> {
        pendingStartMillis = start;
        pendingEndMillis = end;
        hasPendingRange = true;
      });
    }
    if (resetButton != null) {
      resetButton.setOnClickListener(v -> {
        listener.onRangeConfirmed(null, null);
        dismiss();
      });
    }

    if (actions != null) {
      actions.setOnPrimaryClickListener(v -> {
        if (hasPendingRange) {
          LocalDate start = Instant.ofEpochMilli(pendingStartMillis).atZone(ZoneId.systemDefault()).toLocalDate();
          LocalDate end = Instant.ofEpochMilli(pendingEndMillis).atZone(ZoneId.systemDefault()).toLocalDate();
          listener.onRangeConfirmed(start, end);
        } else {
          listener.onRangeConfirmed(null, null);
        }
        dismiss();
      });
      actions.setOnCancelClickListener(v -> dismiss());
    }

    BottomSheetHelper.makeDraggable(this);
  }
}
