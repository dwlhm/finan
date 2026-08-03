package com.dwlhm.finan.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Helper untuk picker tanggal & waktu pada form transaksi.
 * Tap pada dateView atau timeView akan membuka DateTimeBottomSheet
 * (menggunakan custom calendar + custom time picker).
 */
@SuppressLint("SetTextI18n")
public final class TransactionOccurredAtPicker {

  private static final Locale LOCALE = Locale.forLanguageTag("id-ID");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", LOCALE);
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", LOCALE);

  private final Context context;
  private final TextView dateView;
  @Nullable private final TextView timeView;
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

    // Tap tanggal → buka DateTimeBottomSheet
    dateView.setOnClickListener(v -> showDateTimePicker());
    // Tap waktu → juga buka DateTimeBottomSheet (edit tanggal+waktu sekaligus)
    if (timeView != null) {
      timeView.setOnClickListener(v -> showDateTimePicker());
    }
  }

  public long getOccurredAtMillis() {
    return occurredAtMillis;
  }

  public void setOccurredAtMillis(long millis) {
    occurredAtMillis = millis;
    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId);

    LocalDate date = dateTime.toLocalDate();
    LocalDate today = LocalDate.now(zoneId);

    if (date.equals(today)) {
      dateView.setText(R.string.java_TransactionOccurredAtPicker_hari_ini);
    } else if (date.equals(today.minusDays(1))) {
      dateView.setText(R.string.java_TransactionOccurredAtPicker_kemarin);
    } else if (date.equals(today.plusDays(1))) {
      dateView.setText(R.string.java_TransactionOccurredAtPicker_besok);
    } else {
      dateView.setText("@" + dateTime.format(DATE_FORMAT));
    }

    if (timeView != null) {
      timeView.setText(dateTime.format(TIME_FORMAT));
    }
  }

  private void showDateTimePicker() {
    new DateTimeBottomSheet(context, occurredAtMillis, this::setOccurredAtMillis).show();
  }
}
