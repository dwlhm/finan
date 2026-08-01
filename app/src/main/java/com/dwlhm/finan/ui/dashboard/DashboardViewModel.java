package com.dwlhm.finan.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DashboardViewModel extends ViewModel {

    public enum DisplayMode {
        NOMINAL,
        PERCENTAGE,
        MASKED
    }

    private final MutableLiveData<DisplayMode> displayMode = new MutableLiveData<>(DisplayMode.NOMINAL);
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Long> walletFilter = new MutableLiveData<>(null);
    private final MutableLiveData<Long> categoryFilter = new MutableLiveData<>(null);
    private final MutableLiveData<String> transactionTypeFilter = new MutableLiveData<>(null);

    public LiveData<DisplayMode> getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(DisplayMode mode) {
        if (displayMode.getValue() != mode) {
            displayMode.setValue(mode);
        }
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String query) {
        if (query != null && !query.equals(searchQuery.getValue())) {
            searchQuery.setValue(query);
        }
    }

    public LiveData<Long> getWalletFilter() {
        return walletFilter;
    }

    public void setWalletFilter(Long walletId) {
        walletFilter.setValue(walletId);
    }

    public LiveData<Long> getCategoryFilter() {
        return categoryFilter;
    }

    public void setCategoryFilter(Long categoryId) {
        categoryFilter.setValue(categoryId);
    }

    public LiveData<String> getTransactionTypeFilter() {
        return transactionTypeFilter;
    }

    public void setTransactionTypeFilter(String type) {
        transactionTypeFilter.setValue(type);
    }

    public enum TimeRangeMode {
        MONTHLY,
        YEARLY
    }

    public static class TimeRangeState {
        public final TimeRangeMode mode;
        public final int year;
        public final int month; // 1-12, or -1 if YEARLY

        public TimeRangeState(TimeRangeMode mode, int year, int month) {
            this.mode = mode;
            this.year = year;
            this.month = month;
        }
    }

    private final MutableLiveData<TimeRangeState> timeRangeState = new MutableLiveData<>(null);

    public LiveData<TimeRangeState> getTimeRangeState() {
        return timeRangeState;
    }

    public void setTimeRangeState(TimeRangeState state) {
        timeRangeState.setValue(state);
    }
}
