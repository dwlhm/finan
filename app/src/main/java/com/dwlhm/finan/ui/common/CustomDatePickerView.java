package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
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

  // ── Constants ─────────────────────────────────────────────────────────────────

  private static final String[] WEEKDAYS = { "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab" };
  private static final String[] MONTHS = {
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
  };

  // Drawable type tag used on each cell view
  private static final int TAG_NORMAL   = 0;
  private static final int TAG_RANGE_S  = 1; // range start
  private static final int TAG_RANGE_E  = 2; // range end
  private static final int TAG_RANGE_M  = 3; // range middle

  // ── Fields ────────────────────────────────────────────────────────────────────

  private final TextView headerText;
  private final LinearLayout grid, yearGrid;
  private final TextView navLeft, navRight;
  private final int colorPrimary, colorSecondary, colorDim, colorWeekend, colorSelected, colorRangeBg;
  private final int selBgRes;
  private int year, month, selectedDay = 1, selectedMonth, selectedYear, yearRangeStart;
  private int startDow, todayDay;
  private boolean showingYears;
  private OnDateSelectedListener listener;
  private OnRangeSelectedListener rangeListener;
  private boolean rangeMode;
  private Integer rangeStartDay, rangeStartMonth, rangeStartYear;
  private Integer rangeEndDay, rangeEndMonth, rangeEndYear;

  // ── Constructor ───────────────────────────────────────────────────────────────

  public CustomDatePickerView(Context context) {
    this(context, null);
  }

  public CustomDatePickerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);

    colorPrimary   = ContextCompat.getColor(context, R.color.finan_primary);
    colorSecondary = ContextCompat.getColor(context, R.color.finan_text_secondary);
    colorSelected  = ContextCompat.getColor(context, R.color.finan_chip_text_selected);
    colorDim       = (colorSecondary & 0x00FFFFFF) | 0x55000000;
    colorWeekend   = ContextCompat.getColor(context, R.color.finan_expense);
    colorRangeBg   = ContextCompat.getColor(context, R.color.finan_date_range_bg);
    selBgRes       = UiComponentStyles.selectableItemBackground(context);

    LocalDate now = LocalDate.now();
    year = now.getYear();
    month = now.getMonthValue();
    selectedMonth = month;
    selectedYear = year;
    yearRangeStart = (year / 20) * 20;

    // ── Header ────────────────────────────────────────────────────
    LinearLayout header = new LinearLayout(context);
    header.setOrientation(HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);

    navLeft  = makeNav("‹", selBgRes);
    navRight = makeNav("›", selBgRes);

    headerText = new TextView(context);
    headerText.setTextSize(16f);
    headerText.setTextColor(colorPrimary);
    headerText.setTypeface(headerText.getTypeface(), android.graphics.Typeface.BOLD);
    headerText.setGravity(Gravity.CENTER);
    headerText.setLayoutParams(new LayoutParams(0, dp(44), 1f));
    headerText.setClickable(true);
    headerText.setFocusable(true);
    headerText.setForeground(ContextCompat.getDrawable(context, selBgRes));
    headerText.setOnClickListener(v -> {
      showingYears = !showingYears;
      if (showingYears) yearRangeStart = (year / 20) * 20;
      render();
    });

    header.addView(navLeft);
    header.addView(headerText);
    header.addView(navRight);
    addView(header);

    // ── Divider ───────────────────────────────────────────────────
    View divider = new View(context);
    divider.setLayoutParams(new LayoutParams(-1, 1));
    divider.setBackgroundColor(ContextCompat.getColor(context, R.color.finan_divider));
    addView(divider);

    // ── Weekday row ───────────────────────────────────────────────
    LinearLayout weekRow = new LinearLayout(context);
    weekRow.setOrientation(HORIZONTAL);
    for (int i = 0; i < WEEKDAYS.length; i++) {
      TextView tv = new TextView(context);
      tv.setText(WEEKDAYS[i]);
      tv.setTextSize(11f);
      tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
      tv.setGravity(Gravity.CENTER);
      tv.setLayoutParams(new LayoutParams(0, dp(30), 1f));
      tv.setTextColor(i == 0 || i == 6 ? colorWeekend : colorSecondary);
      weekRow.addView(tv);
    }
    addView(weekRow);

    // ── Day grid ──────────────────────────────────────────────────
    grid = new LinearLayout(context);
    grid.setOrientation(VERTICAL);
    for (int r = 0; r < 6; r++) {
      LinearLayout row = new LinearLayout(context);
      row.setOrientation(HORIZONTAL);
      for (int c = 0; c < 7; c++) {
        DayCell cell = new DayCell(context);
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new LayoutParams(0, dp(44), 1f));
        cell.setTextSize(14f);
        cell.setClickable(true);
        cell.setFocusable(true);
        row.addView(cell);
      }
      grid.addView(row);
    }
    addView(grid);

    // ── "Hari Ini" button (right-aligned) ────────────────────────
    Button todayBtn = new Button(context, null, 0, R.style.Finan_Button_Nav);
    todayBtn.setText(R.string.date_picker_today);
    todayBtn.setOnClickListener(v -> {
      LocalDate today = LocalDate.now();
      year  = today.getYear();
      month = today.getMonthValue();
      selectedDay = today.getDayOfMonth();
      showingYears = false;
      render();
      if (rangeMode) {
        rangeStartDay   = today.getDayOfMonth();
        rangeStartMonth = today.getMonthValue();
        rangeStartYear  = today.getYear();
        rangeEndDay = null; rangeEndMonth = null; rangeEndYear = null;
      } else {
        fireSelected();
      }
    });
    LayoutParams todayParams = new LayoutParams(LayoutParams.WRAP_CONTENT, dp(36));
    todayParams.gravity = Gravity.END;
    todayParams.setMargins(0, dp(2), 0, 0);
    addView(todayBtn, todayParams);

    // ── Year grid ─────────────────────────────────────────────────
    yearGrid = new LinearLayout(context);
    yearGrid.setOrientation(VERTICAL);
    for (int r = 0; r < 5; r++) {
      LinearLayout row = new LinearLayout(context);
      row.setOrientation(HORIZONTAL);
      for (int c = 0; c < 4; c++) {
        TextView cell = new TextView(context);
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new LayoutParams(0, dp(52), 1f));
        cell.setTextSize(15f);
        cell.setClickable(true);
        cell.setFocusable(true);
        cell.setForeground(ContextCompat.getDrawable(context, selBgRes));
        row.addView(cell);
      }
      yearGrid.addView(row);
    }
    addView(yearGrid);
    yearGrid.setVisibility(GONE);

    render();
  }

  // ── Inner class: DayCell that draws range background ─────────────────────────

  /**
   * DayCell menggambar background range (half-rect untuk start/end, full rect
   * untuk middle) langsung di Canvas sebelum konten teks, sehingga tidak butuh
   * drawable XML terpisah dengan sintaks yang tidak didukung.
   */
  private class DayCell extends androidx.appcompat.widget.AppCompatTextView {
    private int rangeType = TAG_NORMAL; // TAG_NORMAL / TAG_RANGE_S / TAG_RANGE_E / TAG_RANGE_M
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    DayCell(Context ctx) {
      super(ctx);
    }

    void setRangeType(int type) {
      if (this.rangeType != type) {
        this.rangeType = type;
        invalidate();
      }
    }

    @Override
    protected void onDraw(Canvas canvas) {
      if (rangeType != TAG_NORMAL) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float radius = Math.min(w, h) / 2f * 0.85f;

        bgPaint.setColor(colorRangeBg & 0x00FFFFFF | 0x28000000); // 16% alpha
        // Proper alpha for light/dark themes:
        bgPaint.setColor(colorRangeBg);
        bgPaint.setAlpha(40); // ~16% alpha

        if (rangeType == TAG_RANGE_S) {
          // Right half rect
          canvas.drawRect(cx, cy - radius, w, cy + radius, bgPaint);
        } else if (rangeType == TAG_RANGE_E) {
          // Left half rect
          canvas.drawRect(0, cy - radius, cx, cy + radius, bgPaint);
        } else if (rangeType == TAG_RANGE_M) {
          // Full width rect
          canvas.drawRect(0, cy - radius, w, cy + radius, bgPaint);
        }
        bgPaint.setAlpha(255);
      }
      super.onDraw(canvas);
    }
  }

  // ── Nav helper ────────────────────────────────────────────────────────────────

  private TextView makeNav(String label, int selBg) {
    TextView tv = new TextView(getContext());
    tv.setText(label);
    tv.setTextSize(22f);
    tv.setTextColor(colorPrimary);
    tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
    tv.setGravity(Gravity.CENTER);
    tv.setMinimumWidth(dp(44));
    tv.setMinimumHeight(dp(44));
    tv.setClickable(true);
    tv.setFocusable(true);
    tv.setForeground(ContextCompat.getDrawable(getContext(), selBg));
    return tv;
  }

  // ── Cell accessors ────────────────────────────────────────────────────────────

  private DayCell cellAt(int pos) {
    return (DayCell) ((LinearLayout) grid.getChildAt(pos / 7)).getChildAt(pos % 7);
  }

  // ── Style application ─────────────────────────────────────────────────────────

  private void applyDayStyle(int day, boolean selected) {
    applyDayStyle(day, month, year, selected);
  }

  private void applyDayStyle(int day, int m, int y, boolean selected) {
    int pos = (m == month && y == year) ? startDow + day - 1 : -1;
    DayCell cell = findCell(day, m, y);
    if (cell == null) return;

    boolean isToday   = todayDay == day && m == month && y == year;
    boolean isWeekend = pos >= 0 && ((pos % 7) == 0 || (pos % 7) == 6);

    boolean inRange = false, isRangeStart = false, isRangeEnd = false, isSingleDay = false;

    if (rangeMode && rangeStartDay != null) {
      isRangeStart = day == rangeStartDay && m == rangeStartMonth && y == rangeStartYear;
      if (rangeEndDay != null) {
        isRangeEnd   = day == rangeEndDay   && m == rangeEndMonth   && y == rangeEndYear;
        isSingleDay  = isRangeStart && isRangeEnd;
        inRange      = !isRangeStart && !isRangeEnd
            && !beforeRangeStart(day, m, y) && !afterRangeEnd(day, m, y);
      } else {
        isSingleDay = isRangeStart;
        isRangeStart = false;
      }
    }

    // Foreground ripple: disable for range/selected cells, re-enable for normal
    if (selected || isRangeStart || isRangeEnd || inRange) {
      cell.setForeground(null);
    } else {
      cell.setForeground(ContextCompat.getDrawable(getContext(), selBgRes));
    }

    if (isSingleDay || selected) {
      cell.setRangeType(TAG_NORMAL);
      cell.setBackgroundResource(R.drawable.bg_date_selected);
      cell.setTextColor(colorSelected);
    } else if (isRangeStart) {
      cell.setRangeType(TAG_RANGE_S);
      cell.setBackgroundResource(R.drawable.bg_date_selected);
      cell.setTextColor(colorSelected);
    } else if (isRangeEnd) {
      cell.setRangeType(TAG_RANGE_E);
      cell.setBackgroundResource(R.drawable.bg_date_selected);
      cell.setTextColor(colorSelected);
    } else if (inRange) {
      cell.setRangeType(TAG_RANGE_M);
      cell.setBackground(null);
      cell.setTextColor(colorPrimary);
    } else if (isToday) {
      cell.setRangeType(TAG_NORMAL);
      cell.setBackgroundResource(R.drawable.bg_date_today);
      cell.setTextColor(colorPrimary);
    } else {
      cell.setRangeType(TAG_NORMAL);
      cell.setBackgroundResource(R.drawable.bg_date_cell);
      cell.setTextColor(isWeekend ? colorWeekend : colorSecondary);
    }
  }

  // ── findCell ──────────────────────────────────────────────────────────────────

  private DayCell findCell(int day, int m, int y) {
    if (m == month && y == year) {
      int pos = startDow + day - 1;
      if (pos >= 0 && pos < 42) return cellAt(pos);
      return null;
    }
    int pm = month == 1 ? 12 : month - 1;
    int py = month == 1 ? year - 1 : year;
    if (m == pm && y == py) {
      int dip = LocalDate.of(py, pm, 1).lengthOfMonth();
      int p = startDow - dip + day - 1;
      if (p >= 0 && p < startDow) return cellAt(p);
      return null;
    }
    int nm = month == 12 ? 1 : month + 1;
    int ny = month == 12 ? year + 1 : year;
    if (m == nm && y == ny) {
      int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
      int p = startDow + daysInMonth + day - 1;
      if (p >= startDow + daysInMonth && p < 42) return cellAt(p);
      return null;
    }
    return null;
  }

  // ── Render ────────────────────────────────────────────────────────────────────

  private void render() {
    if (showingYears) {
      navLeft.setVisibility(VISIBLE);
      navLeft.setOnClickListener(v -> { yearRangeStart -= 20; render(); });
      navRight.setVisibility(VISIBLE);
      navRight.setOnClickListener(v -> { yearRangeStart += 20; render(); });
      headerText.setText(yearRangeStart + " – " + (yearRangeStart + 19));
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
    navLeft.setOnClickListener(v -> {
      month = month == 1 ? 12 : month - 1;
      if (month == 12) year--;
      render();
    });
    navRight.setOnClickListener(v -> {
      month = month == 12 ? 1 : month + 1;
      if (month == 1) year++;
      render();
    });
    grid.setVisibility(VISIBLE);
    yearGrid.setVisibility(GONE);

    LocalDate first = LocalDate.of(year, month, 1);
    int daysInMonth = first.lengthOfMonth();
    startDow = first.getDayOfWeek().getValue() % 7; // 0=Sun

    int prevMonth = month == 1 ? 12 : month - 1;
    int prevYear  = month == 1 ? year - 1 : year;
    int daysInPrev = LocalDate.of(prevYear, prevMonth, 1).lengthOfMonth();

    LocalDate today = LocalDate.now();
    todayDay = (today.getYear() == year && today.getMonthValue() == month)
        ? today.getDayOfMonth() : -1;

    for (int p = 0; p < 42; p++) {
      DayCell cell = cellAt(p);
      cell.setVisibility(VISIBLE);
      cell.setBackground(null);
      cell.setRangeType(TAG_NORMAL);

      if (p < startDow) {
        // Overflow: previous month
        int day = daysInPrev - startDow + p + 1;
        int pm = prevMonth, py = prevYear, df = day;
        cell.setText(String.valueOf(day));
        cell.setForeground(null);
        if (rangeMode) {
          applyDayStyle(day, pm, py, false);
        } else {
          cell.setTextColor(colorDim);
          cell.setOnClickListener(v -> { month = pm; year = py; selectedDay = df; render(); fireSelected(); });
        }
      } else if (p < startDow + daysInMonth) {
        // Current month
        int day = p - startDow + 1;
        int df = day;
        cell.setText(String.valueOf(day));
        cell.setOnClickListener(v -> selectDate(df));
        applyDayStyle(day, day == selectedDay && selectedMonth == month && selectedYear == year);
      } else {
        // Overflow: next month
        int day = p - startDow - daysInMonth + 1;
        int nm = month == 12 ? 1 : month + 1;
        int ny = month == 12 ? year + 1 : year;
        int df = day;
        cell.setText(String.valueOf(day));
        cell.setForeground(null);
        if (rangeMode) {
          applyDayStyle(day, nm, ny, false);
        } else {
          cell.setTextColor(colorDim);
          cell.setOnClickListener(v -> { month = nm; year = ny; selectedDay = df; render(); fireSelected(); });
        }
      }
    }
  }

  // ── Selection ─────────────────────────────────────────────────────────────────

  private void selectDate(int day) {
    if (rangeMode) {
      if (rangeStartDay == null || rangeEndDay != null) {
        rangeStartDay   = day;
        rangeStartMonth = month;
        rangeStartYear  = year;
        rangeEndDay = null; rangeEndMonth = null; rangeEndYear = null;
        selectedDay = day;
        render();
      } else {
        LocalDate start = LocalDate.of(rangeStartYear, rangeStartMonth, rangeStartDay);
        LocalDate end   = LocalDate.of(year, month, day);
        if (end.isBefore(start)) {
          rangeEndDay   = rangeStartDay;   rangeEndMonth = rangeStartMonth;   rangeEndYear = rangeStartYear;
          rangeStartDay = day;             rangeStartMonth = month;           rangeStartYear = year;
        } else {
          rangeEndDay = day; rangeEndMonth = month; rangeEndYear = year;
        }
        selectedDay = rangeEndDay;
        render();
        fireRangeSelected();
      }
      return;
    }
    int old = selectedDay, oldM = selectedMonth, oldY = selectedYear;
    if (old == day && oldM == month && oldY == year) return;
    selectedDay = day; selectedMonth = month; selectedYear = year;
    applyDayStyle(old, oldM, oldY, false);
    applyDayStyle(day, true);
    fireSelected();
  }

  // ── Callbacks ─────────────────────────────────────────────────────────────────

  private void fireSelected() {
    if (listener != null) {
      listener.onDateSelected(
          LocalDate.of(year, month, selectedDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
  }

  private void fireRangeSelected() {
    if (rangeListener != null && rangeStartDay != null && rangeEndDay != null) {
      rangeListener.onRangeSelected(
          LocalDate.of(rangeStartYear, rangeStartMonth, rangeStartDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
          LocalDate.of(rangeEndYear, rangeEndMonth, rangeEndDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
  }

  // ── Range helpers ─────────────────────────────────────────────────────────────

  private boolean beforeRangeStart(int day, int m, int y) {
    if (rangeStartDay == null) return true;
    return LocalDate.of(y, m, day).isBefore(LocalDate.of(rangeStartYear, rangeStartMonth, rangeStartDay));
  }

  private boolean afterRangeEnd(int day, int m, int y) {
    if (rangeEndDay == null) return true;
    return LocalDate.of(y, m, day).isAfter(LocalDate.of(rangeEndYear, rangeEndMonth, rangeEndDay));
  }

  // ── Public API ────────────────────────────────────────────────────────────────

  public void setRangeMode(boolean rangeMode) {
    this.rangeMode = rangeMode;
    if (!rangeMode) {
      rangeStartDay = rangeStartMonth = rangeStartYear = null;
      rangeEndDay   = rangeEndMonth   = rangeEndYear   = null;
    }
  }

  public void setRange(long startMillis, long endMillis) {
    LocalDate start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDate end   = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate();
    rangeStartDay   = start.getDayOfMonth(); rangeStartMonth = start.getMonthValue(); rangeStartYear = start.getYear();
    rangeEndDay     = end.getDayOfMonth();   rangeEndMonth   = end.getMonthValue();   rangeEndYear   = end.getYear();
    year = start.getYear(); month = start.getMonthValue(); selectedDay = start.getDayOfMonth();
    render();
  }

  public void setOnRangeSelectedListener(OnRangeSelectedListener l) { this.rangeListener = l; }

  public void setDate(long millis) {
    LocalDate d = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    year = d.getYear(); month = d.getMonthValue();
    selectedDay = d.getDayOfMonth(); selectedMonth = d.getMonthValue(); selectedYear = d.getYear();
    render();
  }

  public void setOnDateSelectedListener(OnDateSelectedListener l) { this.listener = l; }

  private int dp(int v) {
    return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
  }
}
