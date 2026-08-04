package com.dwlhm.finan.ui.common;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.dwlhm.finan.R;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateTimeBottomSheet extends BottomSheetDialog {

  public interface OnDateTimeSelectedListener {
    void onDateTimeSelected(long millis);
  }

  private static final DateTimeFormatter PREVIEW_FMT =
      DateTimeFormatter.ofPattern("d MMMM yyyy  •  HH:mm", Locale.forLanguageTag("id-ID"));

  private final OnDateTimeSelectedListener listener;
  private TextView preview;
  private long currentMillis;

  public DateTimeBottomSheet(
      @NonNull Context context,
      long initialMillis,
      @NonNull OnDateTimeSelectedListener listener) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.listener = listener;
    currentMillis = initialMillis;
  }

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_date_time_bottom_sheet);

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

    preview = findViewById(R.id.date_time_preview);
    CustomDatePickerView datePicker = findViewById(R.id.date_picker_custom);
    CustomTimePickerView timePicker = findViewById(R.id.time_picker_custom);
    DialogActionsView actions = findViewById(R.id.date_time_actions);

    updatePreview();
    if (datePicker != null) {
      datePicker.setDate(currentMillis);
      datePicker.setOnDateSelectedListener(millis -> {
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        LocalDateTime cur = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis), ZoneId.systemDefault());
        currentMillis = dt.toLocalDate().atTime(cur.toLocalTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        updatePreview();
      });
    }

    if (timePicker != null) {
      timePicker.setTime(
          LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis), ZoneId.systemDefault()).getHour(),
          LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis), ZoneId.systemDefault()).getMinute());
      timePicker.setOnTimeSelectedListener((h, m) -> {
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis), ZoneId.systemDefault());
        currentMillis = dt.toLocalDate().atTime(h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        updatePreview();
      });
    }
    if (actions != null) {
      actions.setOnPrimaryClickListener(v -> {
        listener.onDateTimeSelected(currentMillis);
        dismiss();
      });
      actions.setOnCancelClickListener(v -> dismiss());
    }

    BottomSheetHelper.makeDraggable(this);
  }

  private void updatePreview() {
    if (preview != null) {
      preview.setText(
          LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis), ZoneId.systemDefault()).format(PREVIEW_FMT));
    }
  }
}
