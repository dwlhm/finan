package com.dwlhm.finan.ui.dashboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.DebouncedTextWatcher;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.util.date.PayrollCycleResolver;

import java.time.LocalDate;
import java.util.List;

@SuppressLint("SetTextI18n")
public class DashboardFragment extends ScreenFragment {

    private static final String PREF_DISPLAY_MODE = "dashboard_display_mode";

    private DashboardViewModel viewModel;
    private AppServices services;
    
    private ViewPager2 viewPager;
    
    private TextView modeNominal;
    private TextView modePercentage;
    private TextView modeMasked;
    
    private EditText searchInput;
    private ImageButton searchClear;
    
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
                    msg.setText(count + " " + getString(R.string.java_DashboardFragment_kategori_belum_dikelompokkan));
                    
                    inboxPrompt.setOnClickListener(v -> ((com.dwlhm.finan.ui.common.ScreenNavigator) requireActivity()).openCategoriesFiltered("UNCLASSIFIED"));
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
            int cutoffDay = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
                    .getInt("cutoff_day", 1);
            LocalDate baseDate = PayrollCycleResolver.baseMonthForToday(LocalDate.now(), cutoffDay);

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
        
        DashboardPagerAdapter pagerAdapter = new DashboardPagerAdapter(this, baseDate, state.mode);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setCurrentItem(DashboardPagerAdapter.START_POSITION, false);
    }

    private void setupGlobalControls() {
        modeNominal.setOnClickListener(v -> setDisplayMode(DashboardViewModel.DisplayMode.NOMINAL));
        modePercentage.setOnClickListener(v -> setDisplayMode(DashboardViewModel.DisplayMode.PERCENTAGE));
        modeMasked.setOnClickListener(v -> setDisplayMode(DashboardViewModel.DisplayMode.MASKED));
        
        searchWatcher = new DebouncedTextWatcher(500L, value -> {
            String query = value.trim();
            if (query.length() > 3) {
                openSearchPage(query);
            }
        });
        searchInput.addTextChangedListener(searchWatcher);
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                searchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    openSearchPage(query);
                }
                return true;
            }
            return false;
        });
        
        searchClear.setOnClickListener(v -> searchInput.setText(""));
    }

    private void openSearchPage(String query) {
        searchInput.setText(""); // clear so it doesn't re-trigger when returning
        android.content.Intent intent = new android.content.Intent(requireContext(), com.dwlhm.finan.ui.search.SearchTransactionActivity.class);
        intent.putExtra("EXTRA_QUERY", query);
        startActivity(intent);
    }

    private void observeViewModel() {
        viewModel.getDisplayMode().observe(getViewLifecycleOwner(), this::updateDisplayModeUi);
        
        viewModel.getTimeRangeState().observe(getViewLifecycleOwner(), this::updateViewPagerWithTimeRange);
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

    @Override
    public void onDestroyView() {
        if (searchWatcher != null) searchWatcher.cancel();
        super.onDestroyView();
    }
}
