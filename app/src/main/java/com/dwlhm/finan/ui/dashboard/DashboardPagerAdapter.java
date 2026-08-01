package com.dwlhm.finan.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.time.LocalDate;

public class DashboardPagerAdapter extends FragmentStateAdapter {

    // Start with a large arbitrary number for pseudo-infinite scrolling
    public static final int START_POSITION = 5000;
    public static final int MAX_PAGES = 10000;
    
    private final LocalDate initialDate;
    private final DashboardViewModel.TimeRangeMode mode;

    public DashboardPagerAdapter(@NonNull Fragment fragment, LocalDate initialDate, DashboardViewModel.TimeRangeMode mode) {
        super(fragment);
        this.initialDate = initialDate;
        this.mode = mode;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int offset = position - START_POSITION;
        if (mode == DashboardViewModel.TimeRangeMode.YEARLY) {
            LocalDate yearDate = initialDate.plusYears(offset);
            return MonthlyDashboardFragment.newInstance(yearDate.getYear(), -1);
        } else {
            LocalDate monthDate = initialDate.plusMonths(offset);
            return MonthlyDashboardFragment.newInstance(monthDate.getYear(), monthDate.getMonthValue());
        }
    }

    @Override
    public int getItemCount() {
        return MAX_PAGES;
    }
}
