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
import com.google.android.material.button.MaterialButtonToggleGroup;
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
    private MaterialButtonToggleGroup modeToggleGroup;
    private View modeNominal;
    private View modePercentage;
    private View modeMasked;
    
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
        modeToggleGroup = view.findViewById(R.id.dashboard_mode_toggle_group);
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
    public void onStart() {
        super.onStart();
        if (searchInput != null) {
            searchInput.clearFocus();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (searchInput != null) {
            searchInput.clearFocus();
        }
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
        viewPager.setClipToPadding(false);
        viewPager.setClipChildren(false);
        viewPager.setOffscreenPageLimit(2);

        View innerRecycler = viewPager.getChildAt(0);
        if (innerRecycler instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) innerRecycler).setClipChildren(false);
            ((android.view.ViewGroup) innerRecycler).setClipToPadding(false);
            innerRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        int pageMargin = com.dwlhm.finan.ui.common.UiComponentStyles.dp(requireContext(), 10);
        androidx.viewpager2.widget.CompositePageTransformer compositeTransformer = new androidx.viewpager2.widget.CompositePageTransformer();
        compositeTransformer.addTransformer(new androidx.viewpager2.widget.MarginPageTransformer(pageMargin));
        compositeTransformer.addTransformer((page, position) -> {
            float MIN_SCALE = 0.88f;
            float MIN_ALPHA = 0.75f;
            float absPos = Math.abs(position);
            if (absPos >= 1f) {
                page.setScaleX(MIN_SCALE);
                page.setScaleY(MIN_SCALE);
                page.setAlpha(MIN_ALPHA);
            } else {
                float scaleFactor = MIN_SCALE + (1f - MIN_SCALE) * (1f - absPos);
                float alphaFactor = MIN_ALPHA + (1f - MIN_ALPHA) * (1f - absPos);
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);
                page.setAlpha(alphaFactor);
            }
        });
        viewPager.setPageTransformer(compositeTransformer);

        viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int offset = position - DashboardPagerAdapter.START_POSITION;
                int cutoffDay = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
                        .getInt("cutoff_day", 1);
                LocalDate todayMaxDate = PayrollCycleResolver.baseMonthForToday(LocalDate.now(), cutoffDay);
                DashboardViewModel.TimeRangeState currentState = viewModel.getTimeRangeState().getValue();
                DashboardViewModel.TimeRangeMode mode = currentState != null ? currentState.mode : DashboardViewModel.TimeRangeMode.MONTHLY;

                if (mode == DashboardViewModel.TimeRangeMode.YEARLY) {
                    LocalDate targetYear = todayMaxDate.plusYears(offset);
                    if (currentState == null || currentState.year != targetYear.getYear()) {
                        viewModel.setTimeRangeState(new DashboardViewModel.TimeRangeState(mode, targetYear.getYear(), -1));
                    }
                } else {
                    LocalDate targetMonth = todayMaxDate.plusMonths(offset);
                    if (currentState == null || currentState.year != targetMonth.getYear() || currentState.month != targetMonth.getMonthValue()) {
                        viewModel.setTimeRangeState(new DashboardViewModel.TimeRangeState(mode, targetMonth.getYear(), targetMonth.getMonthValue()));
                    }
                }
            }
        });

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
        
        int cutoffDay = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
                .getInt("cutoff_day", 1);
        LocalDate todayMaxDate = PayrollCycleResolver.baseMonthForToday(LocalDate.now(), cutoffDay);

        if (viewPager.getAdapter() == null) {
            DashboardPagerAdapter pagerAdapter = new DashboardPagerAdapter(this, todayMaxDate, state.mode);
            viewPager.setAdapter(pagerAdapter);
            viewPager.setOffscreenPageLimit(2);
        }
        
        int targetPosition = DashboardPagerAdapter.START_POSITION;
        if (state.mode == DashboardViewModel.TimeRangeMode.YEARLY) {
            int yearOffset = state.year - todayMaxDate.getYear();
            targetPosition = DashboardPagerAdapter.START_POSITION + yearOffset;
        } else {
            int monthOffset = (state.year - todayMaxDate.getYear()) * 12 + (state.month - todayMaxDate.getMonthValue());
            targetPosition = DashboardPagerAdapter.START_POSITION + monthOffset;
        }
        
        if (targetPosition > DashboardPagerAdapter.START_POSITION) {
            targetPosition = DashboardPagerAdapter.START_POSITION;
        } else if (targetPosition < 0) {
            targetPosition = 0;
        }
        
        if (viewPager.getCurrentItem() != targetPosition) {
            viewPager.setCurrentItem(targetPosition, false);
        }
    }
    private void setupGlobalControls() {
        modeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.dashboard_mode_nominal) {
                setDisplayMode(DashboardViewModel.DisplayMode.NOMINAL);
            } else if (checkedId == R.id.dashboard_mode_percentage) {
                setDisplayMode(DashboardViewModel.DisplayMode.PERCENTAGE);
            } else if (checkedId == R.id.dashboard_mode_masked) {
                setDisplayMode(DashboardViewModel.DisplayMode.MASKED);
            }
        });
        
        searchInput.setOnClickListener(v -> openSearchPage(searchInput.getText() != null ? searchInput.getText().toString().trim() : ""));
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                openSearchPage(searchInput.getText() != null ? searchInput.getText().toString().trim() : "");
            }
        });

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
                openSearchPage(query);
                return true;
            }
            return false;
        });
        
        searchClear.setOnClickListener(v -> searchInput.setText(""));
    }

    private void openSearchPage(String query) {
        if (searchInput != null) {
            searchInput.clearFocus();
            searchInput.setText(""); // clear so it doesn't re-trigger when returning
        }
        android.content.Intent intent = new android.content.Intent(requireContext(), com.dwlhm.finan.ui.search.SearchTransactionActivity.class);
        intent.putExtra("EXTRA_QUERY", query);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().overridePendingTransition(R.anim.screen_enter, R.anim.screen_exit);
        }
    }

    private void observeViewModel() {
        viewModel.getDisplayMode().observe(getViewLifecycleOwner(), this::updateDisplayModeUi);
        
        viewModel.getTimeRangeState().observe(getViewLifecycleOwner(), this::updateViewPagerWithTimeRange);
    }

    private void updateDisplayModeUi(DashboardViewModel.DisplayMode mode) {
        int targetId = R.id.dashboard_mode_nominal;
        if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
            targetId = R.id.dashboard_mode_percentage;
        } else if (mode == DashboardViewModel.DisplayMode.MASKED) {
            targetId = R.id.dashboard_mode_masked;
        }
        if (modeToggleGroup != null && modeToggleGroup.getCheckedButtonId() != targetId) {
            modeToggleGroup.check(targetId);
        }
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
