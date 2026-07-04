package com.dwlhm.finan.ui.export;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.DialogActionsView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ExportDateRangeBottomSheet extends Dialog {

  public interface OnRangeSelectedListener {
    void onRangeSelected(@Nullable Long startDate, @Nullable Long endDate);
  }

  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"));

  private final OnRangeSelectedListener listener;
  private TextView startValue;
  private TextView endValue;
  @Nullable private Long startDate;
  @Nullable private Long endDate;

  public ExportDateRangeBottomSheet(
      @NonNull Context context,
      @Nullable Long initialStart,
      @Nullable Long initialEnd,
      @NonNull OnRangeSelectedListener listener) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.listener = listener;
    this.startDate = initialStart;
    this.endDate = initialEnd;
  }

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_export_date_range_bottom_sheet);

    Window window = getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      WindowManager.LayoutParams params = window.getAttributes();
      params.width = WindowManager.LayoutParams.MATCH_PARENT;
      params.height = WindowManager.LayoutParams.WRAP_CONTENT;
      params.gravity = Gravity.BOTTOM;
      window.setAttributes(params);
      window.setWindowAnimations(android.R.style.Animation_InputMethod);
    }

    startValue = findViewById(R.id.export_date_start_value);
    endValue = findViewById(R.id.export_date_end_value);
    TextView reset = findViewById(R.id.export_date_reset);
    DialogActionsView actions = findViewById(R.id.export_date_actions);

    refreshDisplay();

    startValue.setOnClickListener(v -> showDatePicker(true));
    endValue.setOnClickListener(v -> showDatePicker(false));
    reset.setOnClickListener(v -> {
      startDate = null;
      endDate = null;
      refreshDisplay();
    });

    actions.setOnPrimaryClickListener(v -> {
      listener.onRangeSelected(startDate, endDate);
      dismiss();
    });
    actions.setOnCancelClickListener(v -> dismiss());

    BottomSheetHelper.makeDraggable(this);
  }

  private void showDatePicker(boolean isStart) {
    Context ctx = getContext();
    long millis = isStart
        ? (startDate != null ? startDate : System.currentTimeMillis())
        : (endDate != null ? endDate - 86400000L : System.currentTimeMillis());
    LocalDate date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();

    DatePickerDialog dialog = new DatePickerDialog(ctx, (view, year, month, dayOfMonth) -> {
      long selected = LocalDate.of(year, month + 1, dayOfMonth)
          .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      if (isStart) {
        startDate = selected;
      } else {
        endDate = selected + 86400000L;
      }
      refreshDisplay();
    }, date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());

    if (isStart && endDate != null) {
      dialog.getDatePicker().setMaxDate(endDate - 1);
    } else if (!isStart && startDate != null) {
      dialog.getDatePicker().setMinDate(startDate);
    }

    dialog.show();
  }

  private void refreshDisplay() {
    startValue.setText(startDate != null
        ? Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate().format(FMT)
        : getContext().getString(R.string.export_date_all_time));
    endValue.setText(endDate != null
        ? Instant.ofEpochMilli(endDate - 86400000L).atZone(ZoneId.systemDefault()).toLocalDate().format(FMT)
        : getContext().getString(R.string.export_date_all_time));
  }
}
