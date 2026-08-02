package com.dwlhm.finan.ui.export;

import android.annotation.SuppressLint;
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
import com.dwlhm.finan.ui.common.CustomDatePickerView;
import com.dwlhm.finan.ui.common.DialogActionsView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public final class ExportDateRangeBottomSheet extends Dialog {

  public interface OnRangeSelectedListener {
    void onRangeSelected(@Nullable Long startDate, @Nullable Long endDate);
  }

  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"));

  private final OnRangeSelectedListener listener;
  private CustomDatePickerView datePicker;
  private TextView rangePreview;
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

    rangePreview = findViewById(R.id.export_date_range_preview);
    datePicker = findViewById(R.id.export_date_picker);
    TextView reset = findViewById(R.id.export_date_reset);
    DialogActionsView actions = findViewById(R.id.export_date_actions);

    datePicker.setRangeMode(true);

    // Jika ada initial range, tampilkan di calendar
    if (startDate != null && endDate != null) {
      datePicker.setRange(startDate, endDate - 86400000L);
    } else {
      // Default ke bulan ini
      LocalDate now = LocalDate.now();
      long s = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      long e = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      datePicker.setRange(s, e);
    }

    datePicker.setOnRangeSelectedListener((s, e) -> {
      startDate = s;
      endDate = e + 86400000L; // end is exclusive (next day)
      refreshPreview();
    });

    refreshPreview();

    reset.setOnClickListener(v -> {
      startDate = null;
      endDate = null;
      refreshPreview();
    });

    actions.setOnPrimaryClickListener(v -> {
      listener.onRangeSelected(startDate, endDate);
      dismiss();
    });
    actions.setOnCancelClickListener(v -> dismiss());

    BottomSheetHelper.makeDraggable(this);
  }

  private void refreshPreview() {
    if (startDate != null && endDate != null) {
      String startStr = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate().format(FMT);
      String endStr   = Instant.ofEpochMilli(endDate - 86400000L).atZone(ZoneId.systemDefault()).toLocalDate().format(FMT);
      rangePreview.setText(startStr + "  →  " + endStr);
    } else {
      rangePreview.setText(getContext().getString(R.string.export_date_all_time));
    }
  }
}
