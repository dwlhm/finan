package com.dwlhm.finan.ui.common;

import android.content.Context;
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
  private TextView tabDate;
  private TextView tabTime;
  private View datePresetsContainer;
  private View timePresetsContainer;
  private View datePickerContainer;
  private View timePickerContainer;
  private CustomDatePickerView datePicker;
  private CustomTimePickerView timePicker;
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
    tabDate = findViewById(R.id.tab_date);
    tabTime = findViewById(R.id.tab_time);
    datePresetsContainer = findViewById(R.id.date_presets_container);
    timePresetsContainer = findViewById(R.id.time_presets_container);
    datePickerContainer = findViewById(R.id.date_picker_container);
    timePickerContainer = findViewById(R.id.time_picker_container);
    datePicker = findViewById(R.id.date_picker_custom);
    timePicker = findViewById(R.id.time_picker_custom);
    actions = findViewById(R.id.date_time_actions);

    updatePreview();

    // Setup Tab Switching & Preview Clicks
    if (tabDate != null) {
      tabDate.setOnClickListener(v -> switchTab(true));
    }
    if (tabTime != null) {
      tabTime.setOnClickListener(v -> switchTab(false));
    }
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
    TextView[] timeChips = new TextView[] { chipNow, chipMorning, chipNoon, chipEvening };

    // Setup Date Picker
    if (datePicker != null) {
      datePicker.setDate(currentMillis);
      datePicker.setOnDateSelectedListener(millis -> {
        LocalDate selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
        updateDateInCurrentMillis(selectedDate);
        highlightPresetChip(null, dateChips);
      });
    }

    // Setup Time Picker
    if (timePicker != null) {
      LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis), ZoneId.systemDefault());
      timePicker.setTime(ldt.getHour(), ldt.getMinute());
      timePicker.setOnTimeSelectedListener((h, m) -> updateTimeInCurrentMillis(h, m));
    }

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
        if (timePicker != null) {
          timePicker.setTime(now.getHour(), now.getMinute());
        } else {
          updateTimeInCurrentMillis(now.getHour(), now.getMinute());
        }
      });
    }

    if (chipMorning != null) {
      chipMorning.setOnClickListener(v -> {
        highlightPresetChip(chipMorning, timeChips);
        if (timePicker != null) {
          timePicker.setTime(8, 0);
        } else {
          updateTimeInCurrentMillis(8, 0);
        }
      });
    }

    if (chipNoon != null) {
      chipNoon.setOnClickListener(v -> {
        highlightPresetChip(chipNoon, timeChips);
        if (timePicker != null) {
          timePicker.setTime(12, 0);
        } else {
          updateTimeInCurrentMillis(12, 0);
        }
      });
    }

    if (chipEvening != null) {
      chipEvening.setOnClickListener(v -> {
        highlightPresetChip(chipEvening, timeChips);
        if (timePicker != null) {
          timePicker.setTime(19, 0);
        } else {
          updateTimeInCurrentMillis(19, 0);
        }
      });
    }

    // Setup Minute Adjust Chips
    View chipMinus15 = findViewById(R.id.chip_minus_15);
    if (chipMinus15 != null) {
      chipMinus15.setOnClickListener(v -> {
        highlightPresetChip(null, timeChips);
        if (timePicker != null) {
          timePicker.addMinutes(-15);
        }
      });
    }

    View chipMinus5 = findViewById(R.id.chip_minus_5);
    if (chipMinus5 != null) {
      chipMinus5.setOnClickListener(v -> {
        highlightPresetChip(null, timeChips);
        if (timePicker != null) {
          timePicker.addMinutes(-5);
        }
      });
    }

    View chipPlus5 = findViewById(R.id.chip_plus_5);
    if (chipPlus5 != null) {
      chipPlus5.setOnClickListener(v -> {
        highlightPresetChip(null, timeChips);
        if (timePicker != null) {
          timePicker.addMinutes(5);
        }
      });
    }

    View chipPlus15 = findViewById(R.id.chip_plus_15);
    if (chipPlus15 != null) {
      chipPlus15.setOnClickListener(v -> {
        highlightPresetChip(null, timeChips);
        if (timePicker != null) {
          timePicker.addMinutes(15);
        }
      });
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
  private void switchTab(boolean showDateTab) {
    isDateTabActive = showDateTab;
    int primaryColor = ContextCompat.getColor(getContext(), R.color.finan_primary);
    int secondaryColor = ContextCompat.getColor(getContext(), R.color.finan_text_secondary);
    if (showDateTab) {
      if (tabDate != null) {
        tabDate.setBackgroundResource(R.drawable.bg_chip_selected);
        tabDate.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_chip_text_selected));
      }
      if (tabTime != null) {
        tabTime.setBackground(null);
        tabTime.setTextColor(secondaryColor);
      }
      if (previewDate != null) {
        previewDate.setTextColor(primaryColor);
      }
      if (previewTime != null) {
        previewTime.setTextColor(secondaryColor);
      }
      if (datePickerContainer != null) datePickerContainer.setVisibility(View.VISIBLE);
      if (timePickerContainer != null) timePickerContainer.setVisibility(View.GONE);
      if (datePresetsContainer != null) datePresetsContainer.setVisibility(View.VISIBLE);
      if (timePresetsContainer != null) timePresetsContainer.setVisibility(View.GONE);
    } else {
      if (tabTime != null) {
        tabTime.setBackgroundResource(R.drawable.bg_chip_selected);
        tabTime.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_chip_text_selected));
      }
      if (tabDate != null) {
        tabDate.setBackground(null);
        tabDate.setTextColor(secondaryColor);
      }
      if (previewTime != null) {
        previewTime.setTextColor(primaryColor);
      }
      if (previewDate != null) {
        previewDate.setTextColor(secondaryColor);
      }
      if (timePickerContainer != null) timePickerContainer.setVisibility(View.VISIBLE);
      if (datePickerContainer != null) datePickerContainer.setVisibility(View.GONE);
      if (timePresetsContainer != null) timePresetsContainer.setVisibility(View.VISIBLE);
      if (datePresetsContainer != null) datePresetsContainer.setVisibility(View.GONE);
    }
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
}
