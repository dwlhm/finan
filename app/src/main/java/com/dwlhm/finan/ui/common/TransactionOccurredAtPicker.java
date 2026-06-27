package com.dwlhm.finan.ui.common;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TransactionOccurredAtPicker {

  private static final Locale LOCALE = Locale.forLanguageTag("id-ID");
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE);
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm", LOCALE);

  private final Context context;
  private final TextView dateView;
  private final TextView timeView;
  private final ZoneId zoneId;
  private long occurredAtMillis;

  public TransactionOccurredAtPicker(
      @NonNull Context context,
      @NonNull TextView dateView,
      @Nullable TextView timeView,
      long initialMillis) {
    this.context = context;
    this.dateView = dateView;
    this.timeView = timeView;
    this.zoneId = ZoneId.systemDefault();
    setOccurredAtMillis(initialMillis > 0L ? initialMillis : System.currentTimeMillis());
    dateView.setOnClickListener(v -> showDatePicker());
    if (timeView != null) {
        timeView.setOnClickListener(v -> showTimePicker());
    }
  }

  public long getOccurredAtMillis() {
    return occurredAtMillis;
  }

  public void resetToNow() {
    setOccurredAtMillis(System.currentTimeMillis());
  }

  public void setOccurredAtMillis(long millis) {
    occurredAtMillis = millis;
    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId);
    
    // Formatting "@Now" logic if it's close to current time, else format normally.
    // For simplicity, we just format the date.
    dateView.setText("@" + dateTime.format(DATE_FORMAT));
    
    if (timeView != null) {
        timeView.setText(dateTime.format(TIME_FORMAT));
    }
  }

  private void showDatePicker() {
    LocalDateTime current = LocalDateTime.ofInstant(Instant.ofEpochMilli(occurredAtMillis), zoneId);
    DatePickerDialog dialog =
        new DatePickerDialog(
            context,
            (picker, year, monthOfYear, dayOfMonth) -> {
              LocalDate selected = LocalDate.of(year, monthOfYear + 1, dayOfMonth);
              LocalDateTime updated = LocalDateTime.of(selected, current.toLocalTime());
              setOccurredAtMillis(updated.atZone(zoneId).toInstant().toEpochMilli());
            },
            current.getYear(),
            current.getMonthValue() - 1,
            current.getDayOfMonth());
    dialog.setTitle(R.string.transaction_date_picker_title);
    dialog.show();
  }

  private void showTimePicker() {
    LocalDateTime current = LocalDateTime.ofInstant(Instant.ofEpochMilli(occurredAtMillis), zoneId);
    TimePickerDialog dialog =
        new TimePickerDialog(
            context,
            (picker, hourOfDay, minute) -> {
              LocalDateTime updated =
                  LocalDateTime.of(current.toLocalDate(), LocalTime.of(hourOfDay, minute));
              setOccurredAtMillis(updated.atZone(zoneId).toInstant().toEpochMilli());
            },
            current.getHour(),
            current.getMinute(),
            true);
    dialog.setTitle(R.string.transaction_time_picker_title);
    dialog.show();
  }
}
