package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class CustomDatePickerView extends LinearLayout {

  public interface OnDateSelectedListener {
    void onDateSelected(long millis);
  }

  public interface OnRangeSelectedListener {
    void onRangeSelected(long startMillis, long endMillis);
  }

  private static final String[] WEEKDAYS = { "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab" };
  private static final String[] MONTHS = {
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
  };

  private final TextView headerText;
  private final LinearLayout grid, yearGrid;
  private final TextView navLeft, navRight;
  private final int colorPrimary, colorSecondary, colorDim, colorWeekend, colorSelected;
  private final int colorRangeBg;
  private int year, month, selectedDay = 1, yearRangeStart;
  private int startDow, todayDay;
  private boolean showingYears;
  private OnDateSelectedListener listener;
  private OnRangeSelectedListener rangeListener;
  private boolean rangeMode;
  private Integer rangeStartDay, rangeStartMonth, rangeStartYear;
  private Integer rangeEndDay, rangeEndMonth, rangeEndYear;

  public CustomDatePickerView(Context context) {
    this(context, null);
  }

  public CustomDatePickerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);

    colorPrimary = ContextCompat.getColor(context, R.color.finan_primary);
    colorSecondary = ContextCompat.getColor(context, R.color.finan_text_secondary);
    colorSelected = ContextCompat.getColor(context, R.color.finan_chip_text_selected);
    colorDim = (colorSecondary & 0x00FFFFFF) | (0x80000000);
    colorWeekend = ContextCompat.getColor(context, R.color.finan_expense);
    colorRangeBg = (colorPrimary & 0x00FFFFFF) | (0x18000000);
    int selBg = UiComponentStyles.selectableItemBackground(context);

    LocalDate now = LocalDate.now();
    year = now.getYear();
    month = now.getMonthValue();
    yearRangeStart = (year / 20) * 20;

    LinearLayout header = new LinearLayout(context);
    header.setOrientation(HORIZONTAL);
    header.setGravity(Gravity.CENTER);

    headerText = new TextView(context);
    headerText.setTextSize(17f);
    headerText.setTextColor(colorPrimary);
    headerText.setTypeface(headerText.getTypeface(), android.graphics.Typeface.BOLD);
    headerText.setGravity(Gravity.CENTER);
    headerText.setLayoutParams(new LayoutParams(0, dp(48), 1f));
    headerText.setClickable(true);
    headerText.setFocusable(true);
    headerText.setForeground(ContextCompat.getDrawable(context, selBg));
    headerText.setOnClickListener(v -> {
      if (showingYears) {
        showingYears = false;
        render();
      } else {
        showingYears = true;
        yearRangeStart = (year / 20) * 20;
        render();
      }
    });

    navLeft = makeNav("\u2039", selBg);
    navRight = makeNav("\u203A", selBg);
    header.addView(navLeft);
    header.addView(headerText);
    header.addView(navRight);
    addView(header);

    View divider = new View(context);
    divider.setLayoutParams(new LayoutParams(-1, 1));
    divider.setBackgroundColor(ContextCompat.getColor(context, R.color.finan_divider));
    addView(divider);

    LinearLayout weekRow = new LinearLayout(context);
    weekRow.setOrientation(HORIZONTAL);
    for (int i = 0; i < WEEKDAYS.length; i++) {
      TextView tv = new TextView(context);
      tv.setText(WEEKDAYS[i]);
      tv.setTextSize(12f);
      tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
      tv.setGravity(Gravity.CENTER);
      tv.setLayoutParams(new LayoutParams(0, dp(36), 1f));
      tv.setTextColor(i == 0 || i == 6 ? colorWeekend : colorSecondary);
      weekRow.addView(tv);
    }
    addView(weekRow);

    grid = new LinearLayout(context);
    grid.setOrientation(VERTICAL);
    for (int r = 0; r < 6; r++) {
      LinearLayout row = new LinearLayout(context);
      row.setOrientation(HORIZONTAL);
      for (int c = 0; c < 7; c++) {
        TextView cell = new TextView(context);
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new LayoutParams(0, dp(48), 1f));
        cell.setTextSize(15f);
        cell.setClickable(true);
        cell.setFocusable(true);
        cell.setForeground(ContextCompat.getDrawable(context, selBg));
        row.addView(cell);
      }
      grid.addView(row);
    }
    addView(grid);

    Button todayBtn = new Button(context, null, 0, R.style.Finan_Button_Nav);
    todayBtn.setText(R.string.date_picker_today);
    todayBtn.setOnClickListener(v -> {
      LocalDate today = LocalDate.now();
      year = today.getYear();
      month = today.getMonthValue();
      selectedDay = today.getDayOfMonth();
      showingYears = false;
      render();
      if (rangeMode) {
        rangeStartDay = today.getDayOfMonth();
        rangeStartMonth = today.getMonthValue();
        rangeStartYear = today.getYear();
        rangeEndDay = null;
        rangeEndMonth = null;
        rangeEndYear = null;
      } else {
        fireSelected();
      }
    });
    LayoutParams todayParams = new LayoutParams(LayoutParams.WRAP_CONTENT, dp(40));
    todayParams.setMargins(0, dp(4), 0, 0);
    addView(todayBtn, todayParams);

    yearGrid = new LinearLayout(context);
    yearGrid.setOrientation(VERTICAL);
    for (int r = 0; r < 5; r++) {
      LinearLayout row = new LinearLayout(context);
      row.setOrientation(HORIZONTAL);
      for (int c = 0; c < 4; c++) {
        TextView cell = new TextView(context);
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new LayoutParams(0, dp(56), 1f));
        cell.setTextSize(16f);
        cell.setClickable(true);
        cell.setFocusable(true);
        cell.setForeground(ContextCompat.getDrawable(context, selBg));
        row.addView(cell);
      }
      yearGrid.addView(row);
    }
    addView(yearGrid);
    yearGrid.setVisibility(GONE);

    render();
  }

  private TextView makeNav(String label, int selBg) {
    TextView tv = new TextView(getContext());
    tv.setText(label);
    tv.setTextSize(24f);
    tv.setTextColor(colorPrimary);
    tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
    tv.setGravity(Gravity.CENTER);
    tv.setMinimumWidth(dp(48));
    tv.setMinimumHeight(dp(48));
    tv.setClickable(true);
    tv.setFocusable(true);
    tv.setForeground(ContextCompat.getDrawable(getContext(), selBg));
    return tv;
  }

  private TextView cellAt(int pos) {
    return (TextView) ((LinearLayout) grid.getChildAt(pos / 7)).getChildAt(pos % 7);
  }

  private void applyDayStyle(int day, boolean selected) {
    applyDayStyle(day, month, year, selected);
  }

  private void applyDayStyle(int day, int m, int y, boolean selected) {
    int pos = startDow + day - 1;
    if (m != month || y != year) {
      pos = -1;
    }
    TextView cell = findCell(day, m, y);
    if (cell == null) return;
    boolean isToday = todayDay == day && m == month && y == year;
    boolean isWeekend = pos >= 0 && ((pos % 7) == 0 || (pos % 7) == 6);
    boolean inRange = false;
    boolean isRangeStart = false;
    boolean isRangeEnd = false;
    if (rangeMode && rangeStartDay != null && rangeEndDay != null) {
      isRangeStart = day == rangeStartDay && m == rangeStartMonth && y == rangeStartYear;
      isRangeEnd = day == rangeEndDay && m == rangeEndMonth && y == rangeEndYear;
      inRange = !isRangeStart && !isRangeEnd
          && !beforeRangeStart(day, m, y) && !afterRangeEnd(day, m, y);
    }
    if (selected) {
      cell.setBackgroundResource(R.drawable.bg_date_selected);
      cell.setTextColor(colorSelected);
    } else if (isRangeStart || isRangeEnd) {
      cell.setBackgroundResource(R.drawable.bg_date_selected);
      cell.setTextColor(colorSelected);
    } else if (inRange) {
      cell.setBackgroundColor(colorRangeBg);
      cell.setTextColor(colorPrimary);
    } else if (isToday) {
      cell.setBackgroundResource(R.drawable.bg_date_today);
      cell.setTextColor(colorPrimary);
    } else {
      cell.setBackgroundResource(R.drawable.bg_date_cell);
      cell.setTextColor(isWeekend ? colorWeekend : colorSecondary);
    }
    cell.invalidate();
  }

  private TextView findCell(int day, int m, int y) {
    if (m == month && y == year) {
      int pos = startDow + day - 1;
      if (pos >= 0 && pos < 42) return cellAt(pos);
      return null;
    }
    for (int p = 0; p < 42; p++) {
      TextView cell = cellAt(p);
      boolean isPrev = p < startDow;
      boolean isNext = p >= startDow + LocalDate.of(year, month, 1).lengthOfMonth();
      if (isPrev) {
        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;
        int daysInPrev = LocalDate.of(prevYear, prevMonth, 1).lengthOfMonth();
        int cellDay = daysInPrev - startDow + p + 1;
        if (cellDay == day && prevMonth == m && prevYear == y) return cell;
      } else if (isNext) {
        int nextMonth = month == 12 ? 1 : month + 1;
        int nextYear = month == 12 ? year + 1 : year;
        int cellDay = p - startDow - LocalDate.of(year, month, 1).lengthOfMonth() + 1;
        if (cellDay == day && nextMonth == m && nextYear == y) return cell;
      }
    }
    return null;
  }

  private void render() {
    if (showingYears) {
      navLeft.setVisibility(VISIBLE);
      navLeft.setOnClickListener(v -> { yearRangeStart -= 20; render(); });
      navRight.setVisibility(VISIBLE);
      navRight.setOnClickListener(v -> { yearRangeStart += 20; render(); });
      headerText.setText(yearRangeStart + " \u2013 " + (yearRangeStart + 19));
      grid.setVisibility(GONE);
      yearGrid.setVisibility(VISIBLE);
      LocalDate today = LocalDate.now();
      for (int r = 0; r < 5; r++) {
        LinearLayout row = (LinearLayout) yearGrid.getChildAt(r);
        for (int c = 0; c < 4; c++) {
          TextView cell = (TextView) row.getChildAt(c);
          int y = yearRangeStart + r * 4 + c;
          cell.setText(String.valueOf(y));
          boolean sel = y == year;
          cell.setBackgroundResource(sel ? R.drawable.bg_date_selected : 0);
          cell.setTextColor(sel ? colorSelected : y == today.getYear() ? colorPrimary : colorSecondary);
          final int yf = y;
          cell.setOnClickListener(v -> { year = yf; showingYears = false; render(); });
        }
      }
      return;
    }

    navLeft.setVisibility(VISIBLE);
    navRight.setVisibility(VISIBLE);
    headerText.setText(MONTHS[month - 1] + " " + year);
    navLeft.setOnClickListener(v -> { month = month == 1 ? 12 : month - 1; if (month == 12) year--; render(); });
    navRight.setOnClickListener(v -> { month = month == 12 ? 1 : month + 1; if (month == 1) year++; render(); });
    grid.setVisibility(VISIBLE);
    yearGrid.setVisibility(GONE);

    LocalDate first = LocalDate.of(year, month, 1);
    int daysInMonth = first.lengthOfMonth();
    startDow = first.getDayOfWeek().getValue() % 7;

    int prevMonth = month == 1 ? 12 : month - 1;
    int prevYear = month == 1 ? year - 1 : year;
    int daysInPrev = LocalDate.of(prevYear, prevMonth, 1).lengthOfMonth();

    LocalDate today = LocalDate.now();
    todayDay = (today.getYear() == year && today.getMonthValue() == month) ? today.getDayOfMonth() : -1;

    for (int p = 0; p < 42; p++) {
      TextView cell = cellAt(p);
      cell.setVisibility(VISIBLE);
      cell.setBackgroundResource(0);

      int col = p % 7;
      if (p < startDow) {
        int day = daysInPrev - startDow + p + 1;
        cell.setText(String.valueOf(day));
        int pm = prevMonth, py = prevYear, df = day;
        if (rangeMode && rangeStartDay != null && rangeEndDay == null) {
          cell.setTextColor(colorDim);
          cell.setOnClickListener(v -> { month = pm; year = py; selectedDay = df; render(); fireSelected(); });
        } else {
          cell.setTextColor(colorDim);
          cell.setOnClickListener(v -> { month = pm; year = py; selectedDay = df; render(); fireSelected(); });
        }
        if (rangeMode) {
          applyDayStyle(day, pm, py, false);
        }
      } else if (p < startDow + daysInMonth) {
        int day = p - startDow + 1;
        cell.setText(String.valueOf(day));
        int df = day;
        cell.setOnClickListener(v -> selectDate(df));
        applyDayStyle(day, day == selectedDay);
      } else {
        int day = p - startDow - daysInMonth + 1;
        cell.setText(String.valueOf(day));
        int nm = month == 12 ? 1 : month + 1;
        int ny = month == 12 ? year + 1 : year;
        int df = day;
        if (rangeMode && rangeStartDay != null && rangeEndDay == null) {
          cell.setTextColor(colorDim);
          cell.setOnClickListener(v -> { month = nm; year = ny; selectedDay = df; render(); fireSelected(); });
        } else {
          cell.setTextColor(colorDim);
          cell.setOnClickListener(v -> { month = nm; year = ny; selectedDay = df; render(); fireSelected(); });
        }
        if (rangeMode) {
          applyDayStyle(day, nm, ny, false);
        }
      }
    }
  }

  private void selectDate(int day) {
    if (rangeMode) {
      if (rangeStartDay == null || rangeEndDay != null) {
        rangeStartDay = day;
        rangeStartMonth = month;
        rangeStartYear = year;
        rangeEndDay = null;
        rangeEndMonth = null;
        rangeEndYear = null;
        selectedDay = day;
        render();
      } else {
        int endDay = day;
        int endMonth = month;
        int endYear = year;
        LocalDate start = LocalDate.of(rangeStartYear, rangeStartMonth, rangeStartDay);
        LocalDate end = LocalDate.of(endYear, endMonth, endDay);
        if (end.isBefore(start)) {
          rangeStartDay = endDay;
          rangeStartMonth = endMonth;
          rangeStartYear = endYear;
          rangeEndDay = start.getDayOfMonth();
          rangeEndMonth = start.getMonthValue();
          rangeEndYear = start.getYear();
        } else {
          rangeEndDay = endDay;
          rangeEndMonth = endMonth;
          rangeEndYear = endYear;
        }
        selectedDay = endDay;
        render();
        fireRangeSelected();
      }
      return;
    }
    int old = selectedDay;
    if (old == day) return;
    selectedDay = day;
    applyDayStyle(old, false);
    applyDayStyle(day, true);
    fireSelected();
  }

  private void fireSelected() {
    if (listener != null) {
      listener.onDateSelected(
          LocalDate.of(year, month, selectedDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
  }

  private void fireRangeSelected() {
    if (rangeListener != null && rangeStartDay != null && rangeEndDay != null) {
      LocalDate start = LocalDate.of(rangeStartYear, rangeStartMonth, rangeStartDay);
      LocalDate end = LocalDate.of(rangeEndYear, rangeEndMonth, rangeEndDay);
      rangeListener.onRangeSelected(
          start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
          end.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
  }

  private boolean beforeRangeStart(int day, int m, int y) {
    if (rangeStartDay == null) return true;
    LocalDate d = LocalDate.of(y, m, day);
    LocalDate rs = LocalDate.of(rangeStartYear, rangeStartMonth, rangeStartDay);
    return d.isBefore(rs);
  }

  private boolean afterRangeEnd(int day, int m, int y) {
    if (rangeEndDay == null) return true;
    LocalDate d = LocalDate.of(y, m, day);
    LocalDate re = LocalDate.of(rangeEndYear, rangeEndMonth, rangeEndDay);
    return d.isAfter(re);
  }

  public void setRangeMode(boolean rangeMode) {
    this.rangeMode = rangeMode;
    if (!rangeMode) {
      rangeStartDay = rangeStartMonth = rangeStartYear = null;
      rangeEndDay = rangeEndMonth = rangeEndYear = null;
    }
  }

  public void setRange(long startMillis, long endMillis) {
    LocalDate start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDate end = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate();
    rangeStartDay = start.getDayOfMonth();
    rangeStartMonth = start.getMonthValue();
    rangeStartYear = start.getYear();
    rangeEndDay = end.getDayOfMonth();
    rangeEndMonth = end.getMonthValue();
    rangeEndYear = end.getYear();
    year = start.getYear();
    month = start.getMonthValue();
    selectedDay = start.getDayOfMonth();
    render();
  }

  public void setOnRangeSelectedListener(OnRangeSelectedListener l) {
    this.rangeListener = l;
  }

  public void setDate(long millis) {
    LocalDate d = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    year = d.getYear();
    month = d.getMonthValue();
    selectedDay = d.getDayOfMonth();
    render();
  }

  public void setOnDateSelectedListener(OnDateSelectedListener l) {
    this.listener = l;
  }

  private int dp(int v) {
    return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
  }
}
