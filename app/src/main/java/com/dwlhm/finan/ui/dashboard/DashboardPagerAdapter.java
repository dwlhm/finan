package com.dwlhm.finan.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.time.LocalDate;

public class DashboardPagerAdapter extends FragmentStateAdapter {

    // Start with a large arbitrary number for pseudo-infinite scrolling
    public static final int START_POSITION = 5000;
    public static final int MAX_PAGES = 10000;
    
    private final LocalDate todayMaxDate;
    private final DashboardViewModel.TimeRangeMode mode;

    public DashboardPagerAdapter(@NonNull Fragment fragment, LocalDate todayMaxDate, DashboardViewModel.TimeRangeMode mode) {
        super(fragment);
        this.todayMaxDate = todayMaxDate;
        this.mode = mode;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int offset = position - START_POSITION;
        if (mode == DashboardViewModel.TimeRangeMode.YEARLY) {
            LocalDate yearDate = todayMaxDate.plusYears(offset);
            return MonthlyDashboardFragment.newInstance(yearDate.getYear(), -1);
        } else {
            LocalDate monthDate = todayMaxDate.plusMonths(offset);
            return MonthlyDashboardFragment.newInstance(monthDate.getYear(), monthDate.getMonthValue());
        }
    }

    @Override
    public int getItemCount() {
        // Cap adapter at current month/year (START_POSITION), preventing future scrolling
        return START_POSITION + 1;
    }
}
