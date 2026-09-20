package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.dwlhm.finan.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateTimeBottomSheet extends BottomSheetDialog {

  public interface OnDateTimeSelectedListener {
    void onDateTimeSelected(long millis);
  }

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"));
  private static final DateTimeFormatter TIME_FMT =
      DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID"));

  private final OnDateTimeSelectedListener listener;
  private TextView previewDate;
  private TextView previewTime;
  private View datePresetsContainer;
  private View timePresetsContainer;
  private View datePickerContainer;
  private View timePickerContainer;
  private CustomDatePickerView datePicker;
  private Circular24HourClockView clockPickerView;
  private TextView timeModeHour;
  private TextView timeModeMinute;
  private TextView[] timeChips;
  private DialogActionsView actions;

  private long currentMillis;
  private boolean isDateTabActive = true;

  public DateTimeBottomSheet(
      @NonNull Context context,
      long initialMillis,
      @NonNull OnDateTimeSelectedListener listener) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.listener = listener;
    this.currentMillis = initialMillis;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
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

    previewDate = findViewById(R.id.preview_date);
    previewTime = findViewById(R.id.preview_time);
    datePresetsContainer = findViewById(R.id.date_presets_container);
    timePresetsContainer = findViewById(R.id.time_presets_container);
    datePickerContainer = findViewById(R.id.date_picker_container);
    timePickerContainer = findViewById(R.id.time_picker_container);
    datePicker = findViewById(R.id.date_picker_custom);
    clockPickerView = findViewById(R.id.clock_picker_view);
    timeModeHour = findViewById(R.id.time_mode_hour);
    timeModeMinute = findViewById(R.id.time_mode_minute);
    actions = findViewById(R.id.date_time_actions);

    updatePreview();

    // Setup Preview Clicks
    if (previewDate != null) {
      previewDate.setOnClickListener(v -> switchTab(true));
    }
    if (previewTime != null) {
      previewTime.setOnClickListener(v -> switchTab(false));
    }

    // Setup Date Picker
    // Preset Views
    TextView chipToday = findViewById(R.id.chip_today);
    TextView chipYesterday = findViewById(R.id.chip_yesterday);
    TextView chipTomorrow = findViewById(R.id.chip_tomorrow);

    TextView chipNow = findViewById(R.id.chip_now);
    TextView chipMorning = findViewById(R.id.chip_morning);
    TextView chipNoon = findViewById(R.id.chip_noon);
    TextView chipEvening = findViewById(R.id.chip_evening);

    TextView[] dateChips = new TextView[] { chipToday, chipYesterday, chipTomorrow };
    timeChips = new TextView[] { chipNow, chipMorning, chipNoon, chipEvening };

    // Setup Date Picker
    if (datePicker != null) {
      datePicker.setTodayButtonVisible(false);
      datePicker.setDate(currentMillis);
      datePicker.setOnDateSelectedListener(millis -> {
        LocalDate selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
        updateDateInCurrentMillis(selectedDate);
        highlightPresetChip(null, dateChips);
      });
    }

    // Setup Clock Picker View
    if (clockPickerView != null) {
      LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis), ZoneId.systemDefault());
      clockPickerView.setTime(ldt.getHour(), ldt.getMinute());
      clockPickerView.setOnTimeChangedListener((h, m) -> {
        updateTimeInCurrentMillis(h, m);
        highlightPresetChip(null, timeChips);
      });
      clockPickerView.setOnModeChangedListener(mode -> updateTimeModeUi(mode));
    }

    if (timeModeHour != null) {
      timeModeHour.setOnClickListener(v -> {
        if (clockPickerView != null) {
          clockPickerView.setMode(Circular24HourClockView.Mode.HOUR);
        }
        updateTimeModeUi(Circular24HourClockView.Mode.HOUR);
      });
    }

    if (timeModeMinute != null) {
      timeModeMinute.setOnClickListener(v -> {
        if (clockPickerView != null) {
          clockPickerView.setMode(Circular24HourClockView.Mode.MINUTE);
        }
        updateTimeModeUi(Circular24HourClockView.Mode.MINUTE);
      });
    }

    updateTimeModeUi(Circular24HourClockView.Mode.HOUR);

    // Setup Date Preset Chips
    if (chipToday != null) {
      chipToday.setOnClickListener(v -> {
        LocalDate today = LocalDate.now();
        highlightPresetChip(chipToday, dateChips);
        if (datePicker != null) {
          datePicker.selectDate(today);
        } else {
          updateDateInCurrentMillis(today);
        }
      });
    }

    if (chipYesterday != null) {
      chipYesterday.setOnClickListener(v -> {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        highlightPresetChip(chipYesterday, dateChips);
        if (datePicker != null) {
          datePicker.selectDate(yesterday);
        } else {
          updateDateInCurrentMillis(yesterday);
        }
      });
    }

    if (chipTomorrow != null) {
      chipTomorrow.setOnClickListener(v -> {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        highlightPresetChip(chipTomorrow, dateChips);
        if (datePicker != null) {
          datePicker.selectDate(tomorrow);
        } else {
          updateDateInCurrentMillis(tomorrow);
        }
      });
    }

    // Setup Time Preset Chips
    if (chipNow != null) {
      chipNow.setOnClickListener(v -> {
        LocalTime now = LocalTime.now();
        highlightPresetChip(chipNow, timeChips);
        setPickerTime(now.getHour(), now.getMinute());
      });
    }

    if (chipMorning != null) {
      chipMorning.setOnClickListener(v -> {
        highlightPresetChip(chipMorning, timeChips);
        setPickerTime(8, 0);
      });
    }

    if (chipNoon != null) {
      chipNoon.setOnClickListener(v -> {
        highlightPresetChip(chipNoon, timeChips);
        setPickerTime(12, 0);
      });
    }

    if (chipEvening != null) {
      chipEvening.setOnClickListener(v -> {
        highlightPresetChip(chipEvening, timeChips);
        setPickerTime(19, 0);
      });
    }

    // Setup Minute Adjust Chips
    View chipMinus15 = findViewById(R.id.chip_minus_15);
    if (chipMinus15 != null) {
      chipMinus15.setOnClickListener(v -> addMinutes(-15));
    }

    View chipMinus5 = findViewById(R.id.chip_minus_5);
    if (chipMinus5 != null) {
      chipMinus5.setOnClickListener(v -> addMinutes(-5));
    }

    View chipPlus5 = findViewById(R.id.chip_plus_5);
    if (chipPlus5 != null) {
      chipPlus5.setOnClickListener(v -> addMinutes(5));
    }

    View chipPlus15 = findViewById(R.id.chip_plus_15);
    if (chipPlus15 != null) {
      chipPlus15.setOnClickListener(v -> addMinutes(15));
    }

    // Setup Dialog Actions
    if (actions != null) {
      actions.setOnPrimaryClickListener(v -> {
        listener.onDateTimeSelected(currentMillis);
        dismiss();
      });
      actions.setOnCancelClickListener(v -> dismiss());
    }

    switchTab(true);
    BottomSheetHelper.makeDraggable(this);
  }

  private void setPickerTime(int hour, int minute) {
    if (clockPickerView != null) {
      clockPickerView.setTime(hour, minute);
    }
    updateTimeInCurrentMillis(hour, minute);
  }

  private void addMinutes(int delta) {
    if (clockPickerView == null) return;
    clockPickerView.addMinutes(delta);
    updateTimeInCurrentMillis(clockPickerView.getHour(), clockPickerView.getMinute());
    highlightPresetChip(null, timeChips);
  }

  private void applySubtleElevation(View view, boolean active) {
    if (view == null) return;
    ViewCompat.setElevation(view, active ? dp(2f) : 0f);
  }

  private void updateTimeModeUi(Circular24HourClockView.Mode mode) {
    int primaryColor = ContextCompat.getColor(getContext(), R.color.finan_text_primary);
    int secondaryColor = ContextCompat.getColor(getContext(), R.color.finan_text_secondary);
    if (mode == Circular24HourClockView.Mode.HOUR) {
      if (timeModeHour != null) {
        timeModeHour.setBackgroundResource(R.drawable.bg_tab_active_pill);
        timeModeHour.setTextColor(primaryColor);
      }
      if (timeModeMinute != null) {
        timeModeMinute.setBackground(null);
        timeModeMinute.setTextColor(secondaryColor);
      }
    } else {
      if (timeModeMinute != null) {
        timeModeMinute.setBackgroundResource(R.drawable.bg_tab_active_pill);
        timeModeMinute.setTextColor(primaryColor);
      }
      if (timeModeHour != null) {
        timeModeHour.setBackground(null);
        timeModeHour.setTextColor(secondaryColor);
      }
    }
    applySubtleElevation(timeModeHour, mode == Circular24HourClockView.Mode.HOUR);
    applySubtleElevation(timeModeMinute, mode == Circular24HourClockView.Mode.MINUTE);
  }

  private void switchTab(boolean showDateTab) {
    isDateTabActive = showDateTab;
    int primaryColor = ContextCompat.getColor(getContext(), R.color.finan_text_primary);
    int secondaryColor = ContextCompat.getColor(getContext(), R.color.finan_text_secondary);
    ColorStateList activeTint = ColorStateList.valueOf(primaryColor);
    ColorStateList inactiveTint = ColorStateList.valueOf(secondaryColor);

    if (showDateTab) {
      if (previewDate != null) {
        previewDate.setBackgroundResource(R.drawable.bg_tab_active_pill);
        previewDate.setTextColor(primaryColor);
        previewDate.setCompoundDrawableTintList(activeTint);
      }
      if (previewTime != null) {
        previewTime.setBackground(null);
        previewTime.setTextColor(secondaryColor);
        previewTime.setCompoundDrawableTintList(inactiveTint);
      }
      if (datePickerContainer != null) datePickerContainer.setVisibility(View.VISIBLE);
      if (timePickerContainer != null) timePickerContainer.setVisibility(View.GONE);
      if (datePresetsContainer != null) datePresetsContainer.setVisibility(View.VISIBLE);
      if (timePresetsContainer != null) timePresetsContainer.setVisibility(View.GONE);
    } else {
      if (previewTime != null) {
        previewTime.setBackgroundResource(R.drawable.bg_tab_active_pill);
        previewTime.setTextColor(primaryColor);
        previewTime.setCompoundDrawableTintList(activeTint);
      }
      if (previewDate != null) {
        previewDate.setBackground(null);
        previewDate.setTextColor(secondaryColor);
        previewDate.setCompoundDrawableTintList(inactiveTint);
      }
      if (timePickerContainer != null) timePickerContainer.setVisibility(View.VISIBLE);
      if (datePickerContainer != null) datePickerContainer.setVisibility(View.GONE);
      if (timePresetsContainer != null) timePresetsContainer.setVisibility(View.VISIBLE);
      if (datePresetsContainer != null) datePresetsContainer.setVisibility(View.GONE);
    }
    applySubtleElevation(previewDate, showDateTab);
    applySubtleElevation(previewTime, !showDateTab);
  }

  private void updateDateInCurrentMillis(LocalDate newDate) {
    LocalTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis), ZoneId.systemDefault()).toLocalTime();
    currentMillis = newDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    updatePreview();
  }

  private void updateTimeInCurrentMillis(int hour, int minute) {
    LocalDate date = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis), ZoneId.systemDefault()).toLocalDate();
    currentMillis = date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    updatePreview();
  }

  private void updatePreview() {
    LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis), ZoneId.systemDefault());
    if (previewDate != null) {
      previewDate.setText(ldt.format(DATE_FMT));
    }
    if (previewTime != null) {
      previewTime.setText(ldt.format(TIME_FMT));
    }
  }

  private void highlightPresetChip(TextView active, TextView[] group) {
    if (group == null) return;
    for (TextView tv : group) {
      if (tv == null) continue;
      if (tv == active) {
        tv.setBackgroundResource(R.drawable.bg_chip_selected);
        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_chip_text_selected));
      } else {
        tv.setBackgroundResource(R.drawable.bg_chip);
        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_text_primary));
      }
    }
  }

  private float dp(float val) {
    return val * getContext().getResources().getDisplayMetrics().density;
  }
}
