package com.dwlhm.finan.ui.dashboard;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.DebouncedTextWatcher;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.history.HistoryFilterBottomSheet;

import java.time.LocalDate;
import java.util.List;

public class DashboardFragment extends ScreenFragment {

    private static final String PREF_DISPLAY_MODE = "dashboard_display_mode";

    private DashboardViewModel viewModel;
    private AppServices services;
    
    private ViewPager2 viewPager;
    private DashboardPagerAdapter pagerAdapter;
    
    private TextView modeNominal;
    private TextView modePercentage;
    private TextView modeMasked;
    
    private EditText searchInput;
    private ImageButton searchClear;
    private ImageButton filterButton;
    
    private DebouncedTextWatcher searchWatcher;

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_dashboard;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        services = ServicesProvider.get(requireContext());
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        
        // Restore display mode from prefs
        int savedModeOrdinal = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
                .getInt(PREF_DISPLAY_MODE, DashboardViewModel.DisplayMode.NOMINAL.ordinal());
        try {
            viewModel.setDisplayMode(DashboardViewModel.DisplayMode.values()[savedModeOrdinal]);
        } catch (Exception e) {
            viewModel.setDisplayMode(DashboardViewModel.DisplayMode.NOMINAL);
        }
    }

    @Override
    protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewPager = view.findViewById(R.id.dashboard_view_pager);
        modeNominal = view.findViewById(R.id.dashboard_mode_nominal);
        modePercentage = view.findViewById(R.id.dashboard_mode_percentage);
        modeMasked = view.findViewById(R.id.dashboard_mode_masked);
        searchInput = view.findViewById(R.id.dashboard_search_input);
        searchClear = view.findViewById(R.id.dashboard_search_clear);
        filterButton = view.findViewById(R.id.dashboard_filter_button);
        filterButton = view.findViewById(R.id.dashboard_filter_button);
        
        setupViewPager();
        setupGlobalControls();
        observeViewModel();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadGlobalInboxPrompt();
    }
    
    private void loadGlobalInboxPrompt() {
        services.dbWorker.compute(
            () -> {
                List<Category> allCats = services.categoryDao.findAllOrdered();
                long unclassifiedCount = 0;
                for (Category cat : allCats) {
                    if (com.dwlhm.finan.domain.model.CashFlowActivity.UNCLASSIFIED.name().equals(cat.getCashFlowActivity())) {
                        unclassifiedCount++;
                    }
                }
                return unclassifiedCount;
            },
            count -> {
                if (!isAdded() || count == null) return;
                
                View inboxPrompt = requireView().findViewById(R.id.dashboard_inbox_prompt);
                if (inboxPrompt == null) return;
                
                if (count > 0) {
                    inboxPrompt.setVisibility(View.VISIBLE);
                    TextView title = inboxPrompt.findViewById(R.id.dashboard_inbox_title);
                    TextView msg = inboxPrompt.findViewById(R.id.dashboard_inbox_message);
                    
                    title.setText(getString(R.string.cashflow_unclassified));
                    msg.setText(count + " kategori belum dikelompokkan");
                    
                    inboxPrompt.setOnClickListener(v -> {
                        ((com.dwlhm.finan.ui.common.ScreenNavigator) requireActivity()).openCategoriesFiltered("UNCLASSIFIED");
                    });
                } else {
                    inboxPrompt.setVisibility(View.GONE);
                }
            }
        );
    }

    private void setupViewPager() {
        // Initial setup will rely on TimeRangeState. If null, we default it.
        DashboardViewModel.TimeRangeState currentState = viewModel.getTimeRangeState().getValue();
        if (currentState == null) {
            LocalDate today = LocalDate.now();
            int cutoffDay = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
                    .getInt("cutoff_day", 1);
                    
            LocalDate baseDate = today;
            if (cutoffDay > 1) {
                if (cutoffDay > 15) {
                    if (today.getDayOfMonth() >= cutoffDay) {
                        baseDate = today.plusMonths(1);
                    }
                } else {
                    if (today.getDayOfMonth() < cutoffDay) {
                        baseDate = today.minusMonths(1);
                    }
                }
            } else if (cutoffDay == -1) {
                LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
                if (lastDay.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
                    lastDay = lastDay.minusDays(1);
                } else if (lastDay.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                    lastDay = lastDay.minusDays(2);
                }
                
                if (!today.isBefore(lastDay)) {
                    baseDate = today.plusMonths(1);
                }
            }

            viewModel.setTimeRangeState(new DashboardViewModel.TimeRangeState(
                    DashboardViewModel.TimeRangeMode.MONTHLY,
                    baseDate.getYear(),
                    baseDate.getMonthValue()
            ));
        }
    }

    private void updateViewPagerWithTimeRange(DashboardViewModel.TimeRangeState state) {
        if (state == null) return;
        
        LocalDate baseDate;
        if (state.mode == DashboardViewModel.TimeRangeMode.YEARLY) {
            baseDate = LocalDate.of(state.year, 1, 1);
        } else {
            baseDate = LocalDate.of(state.year, state.month, 1);
        }
        
        pagerAdapter = new DashboardPagerAdapter(this, baseDate, state.mode);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setCurrentItem(DashboardPagerAdapter.START_POSITION, false);
    }

    private void setupGlobalControls() {
        modeNominal.setOnClickListener(v -> setDisplayMode(DashboardViewModel.DisplayMode.NOMINAL));
        modePercentage.setOnClickListener(v -> setDisplayMode(DashboardViewModel.DisplayMode.PERCENTAGE));
        modeMasked.setOnClickListener(v -> setDisplayMode(DashboardViewModel.DisplayMode.MASKED));
        
        searchWatcher = new DebouncedTextWatcher(300L, value -> {
            viewModel.setSearchQuery(value.trim());
        });
        searchInput.addTextChangedListener(searchWatcher);
        
        searchClear.setOnClickListener(v -> {
            searchInput.setText("");
            viewModel.setSearchQuery("");
        });
        
        filterButton.setOnClickListener(v -> openFilterDialog());
        

    }

    private void observeViewModel() {
        viewModel.getDisplayMode().observe(getViewLifecycleOwner(), this::updateDisplayModeUi);
        
        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            searchClear.setVisibility(query == null || query.isEmpty() ? View.GONE : View.VISIBLE);
        });
        
        viewModel.getTimeRangeState().observe(getViewLifecycleOwner(), state -> {
            updateViewPagerWithTimeRange(state);
        });
        
        viewModel.getWalletFilter().observe(getViewLifecycleOwner(), w -> updateFilterButtonUi());
        viewModel.getCategoryFilter().observe(getViewLifecycleOwner(), c -> updateFilterButtonUi());
    }

    private void updateDisplayModeUi(DashboardViewModel.DisplayMode mode) {
        modeNominal.setBackground(null);
        modeNominal.setTextColor(0xFF4A9E7F);
        modePercentage.setBackground(null);
        modePercentage.setTextColor(0xFF4A9E7F);
        modeMasked.setBackground(null);
        modeMasked.setTextColor(0xFF4A9E7F);
        
        TextView activeView = modeNominal;
        if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) activeView = modePercentage;
        else if (mode == DashboardViewModel.DisplayMode.MASKED) activeView = modeMasked;
        
        activeView.setBackgroundResource(R.drawable.bg_toggle_active);
        activeView.setTextColor(0xFFFFFFFF);
    }

    private void setDisplayMode(DashboardViewModel.DisplayMode mode) {
        viewModel.setDisplayMode(mode);
        requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt(PREF_DISPLAY_MODE, mode.ordinal())
                .apply();
    }

    private void updateFilterButtonUi() {
        boolean active = viewModel.getWalletFilter().getValue() != null 
                || viewModel.getCategoryFilter().getValue() != null
                || viewModel.getTransactionTypeFilter().getValue() != null;
        
        filterButton.setAlpha(active ? 1f : 0.82f);
        filterButton.setColorFilter(ContextCompat.getColor(requireContext(), 
                active ? R.color.finan_warm_accent : R.color.finan_primary));
    }

    private void openFilterDialog() {
        services.dbWorker.compute(
            () -> {
                List<Wallet> wallets = services.walletService.findAll();
                List<Category> categories = services.categoryDao.findAllOrdered();
                return new Object[]{wallets, categories};
            },
            data -> {
                if (!isAdded() || data == null) return;
                List<Wallet> wallets = (List<Wallet>) data[0];
                List<Category> categories = (List<Category>) data[1];
                
                Long selectedWallet = viewModel.getWalletFilter().getValue();
                Long selectedCategory = viewModel.getCategoryFilter().getValue();
                String typeStr = viewModel.getTransactionTypeFilter().getValue();
                Long selectedType = null;
                if ("INCOME".equals(typeStr)) selectedType = 2L;
                else if ("EXPENSE".equals(typeStr)) selectedType = 1L;
                
                HistoryFilterBottomSheet sheet = new HistoryFilterBottomSheet(
                        requireContext(),
                        wallets,
                        categories,
                        selectedWallet,
                        selectedCategory,
                        selectedType,
                        null, // sortId not universally supported here yet, or we can add it to ViewModel
                        (w, c, t, s) -> {
                            viewModel.setWalletFilter(w);
                            viewModel.setCategoryFilter(c);
                            if (t != null) {
                                viewModel.setTransactionTypeFilter(t == 2L ? "INCOME" : "EXPENSE");
                            } else {
                                viewModel.setTransactionTypeFilter(null);
                            }
                        },
                        () -> {
                            viewModel.setWalletFilter(null);
                            viewModel.setCategoryFilter(null);
                            viewModel.setTransactionTypeFilter(null);
                        });
                sheet.show();
            }
        );
    }

    @Override
    public void onDestroyView() {
        if (searchWatcher != null) searchWatcher.cancel();
        super.onDestroyView();
    }
}
