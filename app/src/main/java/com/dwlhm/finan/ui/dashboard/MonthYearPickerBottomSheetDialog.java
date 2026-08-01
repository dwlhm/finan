package com.dwlhm.finan.ui.dashboard;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;

import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.common.BottomSheetHelper;

import java.time.LocalDate;

public class MonthYearPickerBottomSheetDialog extends Dialog {

    public interface OnTimeRangeSelectedListener {
        void onTimeRangeSelected(DashboardViewModel.TimeRangeMode mode, int year, int month);
    }

    private NumberPicker pickerMonth;
    private NumberPicker pickerYear;
    private Button btnApply;
    private Button btnCancel;

    private final DashboardViewModel.TimeRangeMode initialMode;
    private final int initialYear;
    private final int initialMonth; // 1-12, or -1 for ALL
    private OnTimeRangeSelectedListener listener;

    private final String[] monthNames = new String[]{
            "Semua Bulan",
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    public MonthYearPickerBottomSheetDialog(@NonNull Context context, DashboardViewModel.TimeRangeMode mode, int year, int month) {
        super(context, R.style.Finan_BottomSheetDialog);
        this.initialMode = mode;
        this.initialYear = year;
        this.initialMonth = month;
    }

    public void setOnTimeRangeSelectedListener(OnTimeRangeSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_month_year_picker);

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

        BottomSheetHelper.makeDraggable(this);

        pickerMonth = findViewById(R.id.picker_month);
        pickerYear = findViewById(R.id.picker_year);
        btnApply = findViewById(R.id.btn_apply);
        btnCancel = findViewById(R.id.btn_cancel);

        setupPickers();

        btnCancel.setOnClickListener(v -> dismiss());
        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                int selectedYear = pickerYear.getValue();
                int selectedMonthIndex = pickerMonth.getValue(); // 0 is Semua Bulan, 1 is Jan, etc.
                
                if (selectedMonthIndex == 0) {
                    listener.onTimeRangeSelected(DashboardViewModel.TimeRangeMode.YEARLY, selectedYear, -1);
                } else {
                    listener.onTimeRangeSelected(DashboardViewModel.TimeRangeMode.MONTHLY, selectedYear, selectedMonthIndex);
                }
            }
            dismiss();
        });
    }

    private void setupPickers() {
        int currentYear = LocalDate.now().getYear();
        
        pickerYear.setMinValue(2000);
        pickerYear.setMaxValue(currentYear + 50);
        pickerYear.setValue(initialYear > 0 ? initialYear : currentYear);

        pickerMonth.setMinValue(0);
        pickerMonth.setMaxValue(12);
        pickerMonth.setDisplayedValues(monthNames);
        
        if (initialMode == DashboardViewModel.TimeRangeMode.YEARLY) {
            pickerMonth.setValue(0);
        } else {
            pickerMonth.setValue(initialMonth);
        }
    }
}
