package com.dwlhm.finan.ui.summary;

import android.annotation.SuppressLint;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.CustomDatePickerView;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.util.date.DateRange;
import com.dwlhm.finan.util.date.PayrollCycleResolver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public final class SummaryDateRangeBottomSheet extends BottomSheetDialog {

  public interface OnRangeConfirmedListener {
    void onRangeConfirmed(LocalDate startDate, LocalDate endDate, int cutoffDay);
  }

  private static final String[] MONTH_NAMES = {
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
  };
  private static final String[] CYCLE_OPTIONS = {
    "Tanggal 1 (Awal Bulan)", "Tanggal 5", "Tanggal 10", "Tanggal 15",
    "Tanggal 20", "Tanggal 25", "Tanggal 28",
    "Hari Kerja Terakhir (Akhir Bulan)"
  };
  private static final int[] CYCLE_VALUES = {1, 5, 10, 15, 20, 25, 28, -1};
  private static final DateTimeFormatter PREVIEW_FMT =
      DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"));

  private final OnRangeConfirmedListener listener;
  private LocalDate pendingStart;
  private LocalDate pendingEnd;
  private int pendingCutoffDay;
  private boolean bulanMode = true;

  private int selectedYear;
  private Integer selectedMonth;

  private TextView modeBulan, modeKustom;
  private LinearLayout monthContainer, kustomContainer;
  private TextView yearLabel, rangePreview;
  private LinearLayout monthGrid;
  private TextView cycleValue;
  private CustomDatePickerView datePicker;
  private final int colorPrimary, colorSecondary, colorSelected;

  public SummaryDateRangeBottomSheet(
      @NonNull Context context,
      @NonNull LocalDate initialStart,
      @NonNull LocalDate initialEnd,
      int initialCutoffDay,
      @NonNull OnRangeConfirmedListener listener) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.listener = listener;
    this.pendingStart = initialStart;
    this.pendingEnd = initialEnd;
    this.pendingCutoffDay = initialCutoffDay;
    this.selectedYear = initialStart.getYear();
    this.selectedMonth = initialStart.getMonthValue();
    colorPrimary = ContextCompat.getColor(context, R.color.finan_primary);
    colorSecondary = ContextCompat.getColor(context, R.color.finan_text_secondary);
    colorSelected = ContextCompat.getColor(context, R.color.finan_chip_text_selected);
  }

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_summary_date_range_bottom_sheet);

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

    modeBulan = findViewById(R.id.summary_range_mode_bulan);
    modeKustom = findViewById(R.id.summary_range_mode_kustom);
    monthContainer = findViewById(R.id.summary_range_month_container);
    kustomContainer = findViewById(R.id.summary_range_kustom_container);
    yearLabel = findViewById(R.id.summary_range_year_label);
    TextView yearPrev = findViewById(R.id.summary_range_year_prev);
    TextView yearNext = findViewById(R.id.summary_range_year_next);
    rangePreview = findViewById(R.id.summary_range_preview);
    monthGrid = findViewById(R.id.summary_range_month_grid);
    cycleValue = findViewById(R.id.summary_range_cycle_value);
    datePicker = findViewById(R.id.summary_range_picker);
    DialogActionsView actions = findViewById(R.id.summary_range_actions);

    cycleValue.setText(getCycleLabel(pendingCutoffDay));

    long startMillis = pendingStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    long endMillis = pendingEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    datePicker.setRangeMode(true);
    datePicker.setRange(startMillis, endMillis);
    datePicker.setOnRangeSelectedListener((s, e) -> {
      pendingStart = Instant.ofEpochMilli(s).atZone(ZoneId.systemDefault()).toLocalDate();
      pendingEnd = Instant.ofEpochMilli(e).atZone(ZoneId.systemDefault()).toLocalDate();
    });

    modeBulan.setOnClickListener(v -> setMode(true));
    modeKustom.setOnClickListener(v -> setMode(false));

    if (yearPrev != null) yearPrev.setOnClickListener(v -> { selectedYear--; rebuildMonthGrid(); });
    if (yearNext != null) yearNext.setOnClickListener(v -> { selectedYear++; rebuildMonthGrid(); });

    cycleValue.setOnClickListener(v -> showCyclePicker());

    buildMonthGrid();
    setMode(true);

    if (actions != null) {
      actions.setOnPrimaryClickListener(v -> {
        listener.onRangeConfirmed(pendingStart, pendingEnd, pendingCutoffDay);
        dismiss();
      });
      actions.setOnCancelClickListener(v -> dismiss());
    }

    BottomSheetHelper.makeDraggable(this);
  }

  private void setMode(boolean bulan) {
    bulanMode = bulan;
    modeBulan.setBackgroundResource(bulan ? R.drawable.bg_toggle_active : R.drawable.bg_toggle_inactive);
    modeBulan.setTextColor(bulan ? 0xFFFFFFFF : 0xFF4A9E7F);
    modeKustom.setBackgroundResource(bulan ? R.drawable.bg_toggle_inactive : R.drawable.bg_toggle_active);
    modeKustom.setTextColor(bulan ? 0xFF4A9E7F : 0xFFFFFFFF);
    monthContainer.setVisibility(bulan ? View.VISIBLE : View.GONE);
    kustomContainer.setVisibility(bulan ? View.GONE : View.VISIBLE);
  }

  private void buildMonthGrid() {
    monthGrid.removeAllViews();
    updateYearLabel();
    for (int r = 0; r < 4; r++) {
      LinearLayout row = new LinearLayout(getContext());
      row.setOrientation(LinearLayout.HORIZONTAL);
      for (int c = 0; c < 3; c++) {
        int monthIdx = r * 3 + c;
        TextView cell = new TextView(getContext());
        cell.setText(MONTH_NAMES[monthIdx]);
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, dp(), 1f));
        cell.setTextSize(15f);
        cell.setClickable(true);
        cell.setFocusable(true);
        int m = monthIdx + 1;
        cell.setOnClickListener(v -> selectMonth(m));
        row.addView(cell);
      }
      monthGrid.addView(row);
    }
    updateMonthGridSelection();
    updateRangePreview();
  }

  private void rebuildMonthGrid() {
    updateYearLabel();
    updateMonthGridSelection();
    updateRangePreview();
  }

  private void selectMonth(int month) {
    selectedMonth = month;
    DateRange range = PayrollCycleResolver.forMonth(selectedYear, month, pendingCutoffDay);
    pendingStart = range.getStart();
    pendingEnd = range.getEnd();

    long startMillis = pendingStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    long endMillis = pendingEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    datePicker.setRange(startMillis, endMillis);

    updateMonthGridSelection();
    updateRangePreview();
  }

  private void updateYearLabel() {
    yearLabel.setText(String.valueOf(selectedYear));
  }

  private void updateMonthGridSelection() {
    LocalDate today = LocalDate.now();
    for (int i = 0; i < monthGrid.getChildCount(); i++) {
      LinearLayout row = (LinearLayout) monthGrid.getChildAt(i);
      for (int j = 0; j < row.getChildCount(); j++) {
        TextView cell = (TextView) row.getChildAt(j);
        int m = i * 3 + j + 1;
        boolean isSelected = selectedMonth != null && m == selectedMonth;
        boolean isCurrentMonth = m == today.getMonthValue() && selectedYear == today.getYear();
        if (isSelected) {
          cell.setBackgroundResource(R.drawable.bg_date_selected);
          cell.setTextColor(colorSelected);
        } else if (isCurrentMonth) {
          cell.setBackgroundResource(R.drawable.bg_date_today);
          cell.setTextColor(colorPrimary);
        } else {
          cell.setBackgroundResource(0);
          cell.setTextColor(colorSecondary);
        }
      }
    }
  }

  private void updateRangePreview() {
    if (selectedMonth != null) {
      rangePreview.setText(
          pendingStart.format(PREVIEW_FMT) + " → " + pendingEnd.format(PREVIEW_FMT));
    } else {
      rangePreview.setText("");
    }
  }

  private void showCyclePicker() {
    new MaterialAlertDialogBuilder(getContext())
        .setTitle("Pilih Siklus Gajian")
        .setItems(CYCLE_OPTIONS, (dialog, which) -> {
          pendingCutoffDay = CYCLE_VALUES[which];
          cycleValue.setText(getCycleLabel(pendingCutoffDay));

          if (bulanMode && selectedMonth != null) {
            selectMonth(selectedMonth);
          } else {
            DateRange range = PayrollCycleResolver.forDate(LocalDate.now(), pendingCutoffDay);
            pendingStart = range.getStart();
            pendingEnd = range.getEnd();
            long s = pendingStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long e = pendingEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            datePicker.setRange(s, e);
          }
        })
        .show();
  }

  private static String getCycleLabel(int cutoffDay) {
    if (cutoffDay == -1) {
      return "Hari Kerja Terakhir (Akhir Bulan)";
    } else if (cutoffDay == 1) {
      return "Tanggal 1 (Awal Bulan)";
    } else {
      return "Tanggal " + cutoffDay;
    }
  }

  private int dp() {
    return (int) (52 * getContext().getResources().getDisplayMetrics().density + 0.5f);
  }
}
