package com.dwlhm.finan.ui.dashboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.HistoryPageCursor;
import com.dwlhm.finan.domain.model.HistoryQuery;
import com.dwlhm.finan.domain.model.HistorySearch;
import com.dwlhm.finan.domain.model.HistoryTotals;
import com.dwlhm.finan.domain.model.InfiniteScrollConfig;
import com.dwlhm.finan.domain.model.PageResult;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.transaction.TransactionSearchResolver;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.EntityLookup;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollController;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollHandle;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollItemDecorations;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollRecyclerDataSink;
import com.dwlhm.finan.ui.components.DonutChartView;
import com.dwlhm.finan.ui.summary.FinancialAdvisor;
import com.dwlhm.finan.ui.transaction.StickyHeaderItemDecoration;
import com.dwlhm.finan.ui.transaction.TransactionDetailDialog;
import com.dwlhm.finan.ui.transaction.TransactionRecyclerAdapter;
import com.dwlhm.finan.util.date.DateRange;
import com.dwlhm.finan.util.date.PayrollCycleResolver;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;
import com.dwlhm.finan.domain.model.CashFlowReportResult;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.domain.model.CashFlowActivityTotal;
import com.dwlhm.finan.domain.model.CashFlowReport;
import com.dwlhm.finan.domain.model.CategoryTotal;
import com.dwlhm.finan.ui.common.UiComponentStyles;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressLint("SetTextI18n")
public class MonthlyDashboardFragment extends ScreenFragment {

    private static final String ARG_YEAR = "arg_year";
    private static final String ARG_MONTH = "arg_month";

    private DashboardViewModel sharedViewModel;
    private AppServices services;
    
    private int year;
    private int month;
    private int cutoffDay;
    private LocalDate startDate;
    private LocalDate endDate;
    
    private AppBarLayout appBarLayout;
    private TextView monthTitle;
    private DonutChartView donutChart;
    private View collapsedToolbar;
    private TextView collapsedBalanceText;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private LinearLayout emptyState;
    private TextView emptyTitle;
    private TextView emptyHint;

    private TransactionRecyclerAdapter transactionAdapter;
    private InfiniteScrollHandle scrollHandle;
    private StickyHeaderItemDecoration stickyDecoration;
    
    // Summary UI Elements
    private androidx.core.widget.NestedScrollView summaryScroll;
    private View weeklyChart;
    private LinearLayout chartWeeksContainer;
    private TextView weeklyChartTitle;
    private MaterialButtonToggleGroup weeklyToggleGroup;
    private View chartToggleIncome;
    private View chartToggleExpense;
    private LinearLayout chartLegend;
    private boolean showWeeklyChartIncome;
    
    private View categoryChart;
    private LinearLayout categoryBarsContainer;
    private MaterialButtonToggleGroup catToggleGroup;
    private View catChartToggleIncome;
    private View catChartToggleExpense;
    private boolean showCategoryChartIncome;
    
    private HistoryTotals cachedTotals;
    private CashFlowReportResult cachedReport;
    private MonthlySummary cachedSummary;
    private MonthlySummary cachedPrevSummary;
    private MonthlySummary cachedPrevPrevSummary;
    
    private HistoryQuery activeQuery = new HistoryQuery(null, null, null, null, null, false, HistorySearch.empty());
    
    private int reloadGeneration;
    private Map<Long, Category> categoriesById = Map.of();
    private Map<Long, Wallet> walletsById = Map.of();

    public static MonthlyDashboardFragment newInstance(int year, int month) {
        MonthlyDashboardFragment fragment = new MonthlyDashboardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_YEAR, year);
        args.putInt(ARG_MONTH, month);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_monthly_dashboard;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        services = ServicesProvider.get(requireContext());
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        
        if (getArguments() != null) {
            year = getArguments().getInt(ARG_YEAR);
            month = getArguments().getInt(ARG_MONTH);
        }
        
        int cutoffDay = requireContext().getSharedPreferences("finan_prefs", android.content.Context.MODE_PRIVATE)
                .getInt("cutoff_day", 1);

        DateRange range;
        this.cutoffDay = cutoffDay;

        if (month == -1) {
            range = PayrollCycleResolver.forYear(year, cutoffDay);
        } else {
            range = PayrollCycleResolver.forMonth(year, month, cutoffDay);
        }
        startDate = range.getStart();
        endDate = range.getEnd();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Always refresh data on resume so new transactions added in Capture show up
        loadData();
    }

    @Override
    protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Must use parent fragment to share ViewModel
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        
        appBarLayout = view.findViewById(R.id.app_bar_layout);
        monthTitle = view.findViewById(R.id.month_title);
        donutChart = view.findViewById(R.id.donut_chart);
        collapsedToolbar = view.findViewById(R.id.collapsed_toolbar);
        collapsedBalanceText = view.findViewById(R.id.collapsed_balance_text);
        tabLayout = view.findViewById(R.id.monthly_tabs);
        recyclerView = view.findViewById(R.id.monthly_recycler_view);
        emptyState = view.findViewById(R.id.monthly_empty);
        emptyTitle = view.findViewById(R.id.monthly_empty_title);
        emptyHint = view.findViewById(R.id.monthly_empty_hint);
        
        summaryScroll = view.findViewById(R.id.monthly_summary_scroll);
        weeklyChart = view.findViewById(R.id.monthly_weekly_chart);
        weeklyChartTitle = view.findViewById(R.id.monthly_weekly_chart_title);
        chartWeeksContainer = view.findViewById(R.id.monthly_chart_weeks);
        weeklyToggleGroup = view.findViewById(R.id.monthly_weekly_toggle_group);
        chartToggleIncome = view.findViewById(R.id.monthly_chart_toggle_income);
        chartToggleExpense = view.findViewById(R.id.monthly_chart_toggle_expense);
        chartLegend = view.findViewById(R.id.monthly_chart_legend);
        
        categoryChart = view.findViewById(R.id.monthly_category_chart);
        categoryBarsContainer = view.findViewById(R.id.monthly_category_bars);
        catToggleGroup = view.findViewById(R.id.monthly_cat_toggle_group);
        catChartToggleIncome = view.findViewById(R.id.monthly_cat_chart_toggle_income);
        catChartToggleExpense = view.findViewById(R.id.monthly_cat_chart_toggle_expense);

        if (weeklyToggleGroup != null) {
            weeklyToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                setWeeklyChartMode(checkedId == R.id.monthly_chart_toggle_income);
            });
        }
        if (catToggleGroup != null) {
            catToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                setCategoryChartMode(checkedId == R.id.monthly_cat_chart_toggle_income);
            });
        }
        updateChartToggle();
        updateCategoryChartToggle();
        layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        
        // Setup Transaction Adapter
        transactionAdapter = new TransactionRecyclerAdapter(requireContext());
        transactionAdapter.setOnTransactionClickListener((t, p) -> openTransactionDetail(p));
        stickyDecoration = new StickyHeaderItemDecoration(transactionAdapter);
        
        // Dynamic sticky header offset
        appBarLayout.addOnLayoutChangeListener((v, left, top, right, bottom, ol, ot, or, ob) -> {
            int height = bottom - top;
            if (height > 0) {
                // Approximate offset to account for tab height
                stickyDecoration.setStickyYOffset(0); 
            }
        });

        // Setup Infinite Scroll Handle manually
        int spacingPx = (int) (8 * getResources().getDisplayMetrics().density);
        InfiniteScrollConfig config = new InfiniteScrollConfig(InfiniteScrollConfig.DEFAULT_PAGE_SIZE, InfiniteScrollConfig.DEFAULT_LOAD_MORE_THRESHOLD, spacingPx);
        recyclerView.addItemDecoration(InfiniteScrollItemDecorations.bottomSpacing(config.getItemSpacingPx()));
        scrollHandle = new InfiniteScrollController<>(
                config,
                this::loadHistoryPage,
                new InfiniteScrollRecyclerDataSink<>(transactionAdapter),
                services.dbWorker,
                () -> {
                    if (scrollHandle != null) {
                        scrollHandle.onLastVisiblePositionChanged(layoutManager.findLastVisibleItemPosition());
                    }
                });
                
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && scrollHandle != null) {
                    scrollHandle.onLastVisiblePositionChanged(layoutManager.findLastVisibleItemPosition());
                }
            }
        });
        
        transactionAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (scrollHandle != null) scrollHandle.onLastVisiblePositionChanged(layoutManager.findLastVisibleItemPosition());
            }
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (scrollHandle != null) scrollHandle.onLastVisiblePositionChanged(layoutManager.findLastVisibleItemPosition());
            }
        });

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.forLanguageTag("id-ID"));

        if (month == -1) {
            monthTitle.setText(String.valueOf(year));
        } else {
            monthTitle.setText(LocalDate.of(year, month, 1).format(formatter));
        }
        
        monthTitle.setOnClickListener(v -> {
            DashboardViewModel.TimeRangeMode currentMode = (month == -1) ? 
                    DashboardViewModel.TimeRangeMode.YEARLY : DashboardViewModel.TimeRangeMode.MONTHLY;
                    
            MonthYearPickerBottomSheetDialog dialog = new MonthYearPickerBottomSheetDialog(
                    requireContext(), currentMode, year, month == -1 ? 1 : month);
            
            dialog.setOnTimeRangeSelectedListener((mode, selectedYear, selectedMonth) ->
                    sharedViewModel.setTimeRangeState(new DashboardViewModel.TimeRangeState(mode, selectedYear, selectedMonth)));
            dialog.show();
        });
        
        setupScrollListener();
        setupTabs();
        observeFilters();
        
        catChartToggleIncome.setOnClickListener(v -> setCategoryChartMode(true));
        catChartToggleExpense.setOnClickListener(v -> setCategoryChartMode(false));
        updateCategoryChartToggle();
        
        loadData();
        switchTab(tabLayout.getSelectedTabPosition());
    }

    private void setupScrollListener() {
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            int totalScrollRange = appBarLayout.getTotalScrollRange();
            float percentage = (float) Math.abs(verticalOffset) / (float) totalScrollRange;
            
            // Fade in the collapsed toolbar when scrolled past 70%
            if (percentage > 0.7f) {
                float alpha = (percentage - 0.7f) / 0.3f;
                collapsedToolbar.setAlpha(alpha);
            } else {
                collapsedToolbar.setAlpha(0f);
            }
        });
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observeFilters() {
        sharedViewModel.getSearchQuery().observe(getViewLifecycleOwner(), q -> { invalidateSummaryCache(); loadData(); });
        sharedViewModel.getWalletFilter().observe(getViewLifecycleOwner(), w -> { invalidateSummaryCache(); loadData(); });
        sharedViewModel.getCategoryFilter().observe(getViewLifecycleOwner(), c -> { invalidateSummaryCache(); loadData(); });
        sharedViewModel.getTransactionTypeFilter().observe(getViewLifecycleOwner(), t -> { invalidateSummaryCache(); loadData(); });
        sharedViewModel.getDisplayMode().observe(getViewLifecycleOwner(), mode -> updateDisplayModeUi());
    }

    private void invalidateSummaryCache() {
        cachedReport = null;
        cachedSummary = null;
        cachedPrevSummary = null;
        cachedPrevPrevSummary = null;
    }

    private void loadData() {
        int generation = ++reloadGeneration;
        Long startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long endExclusiveMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        String queryStr = sharedViewModel.getSearchQuery().getValue();
        Long walletId = sharedViewModel.getWalletFilter().getValue();
        Long categoryId = sharedViewModel.getCategoryFilter().getValue();
        String typeStr = sharedViewModel.getTransactionTypeFilter().getValue();
        
        boolean isSummaryActive = (tabLayout != null && tabLayout.getSelectedTabPosition() == 1);
        
        services.dbWorker.compute(
            () -> {
                List<Wallet> wallets = services.walletService.findAll();
                List<Category> categories = services.categoryDao.findAllOrdered();
                Map<Long, Wallet> wMap = EntityLookup.indexWallets(wallets);
                Map<Long, Category> cMap = EntityLookup.indexCategories(categories);
                TransactionSearchResolver resolver = new TransactionSearchResolver(wallets, categories);
                
                HistorySearch search = resolver.resolve(queryStr == null ? "" : queryStr);
                TransactionType type = null;
                if ("INCOME".equals(typeStr)) type = TransactionType.INCOME;
                else if ("EXPENSE".equals(typeStr)) type = TransactionType.EXPENSE;
                
                HistoryQuery query = new HistoryQuery(walletId, categoryId, type, startMillis, endExclusiveMillis, false, search);
                HistoryTotals totals = services.transactionGateway.findHistoryTotals(query);
                
                // Defer heavy summary queries if user is browsing the Transaksi tab
                CashFlowReportResult reportResult = null;
                MonthlySummary summary = null;
                MonthlySummary prevSummary = null;
                MonthlySummary prevPrevSummary = null;
                
                if (isSummaryActive) {
                    reportResult = services.cashFlowReportService.buildReport(startDate, endDate, cutoffDay, walletId);
                    summary = services.summaryService.loadRange(startDate, endDate, walletId, categoryId);
                    DateRange prevRange = PayrollCycleResolver.shiftMonths(startDate, cutoffDay, -1);
                    DateRange prevPrevRange = PayrollCycleResolver.shiftMonths(startDate, cutoffDay, -2);
                    prevSummary = services.summaryService.loadRange(prevRange.getStart(), prevRange.getEnd(), walletId, categoryId);
                    prevPrevSummary = services.summaryService.loadRange(prevPrevRange.getStart(), prevPrevRange.getEnd(), walletId, categoryId);
                }
                
                return new Object[]{wMap, cMap, query, totals, reportResult, summary, prevSummary, prevPrevSummary};
            },
            data -> {
                if (!isAdded() || generation != reloadGeneration || data == null) return;
                
                @SuppressWarnings("unchecked")
                Map<Long, Wallet> w = (Map<Long, Wallet>) data[0];
                walletsById = w;
                @SuppressWarnings("unchecked")
                Map<Long, Category> c = (Map<Long, Category>) data[1];
                categoriesById = c;
                activeQuery = (HistoryQuery) data[2];
                cachedTotals = (HistoryTotals) data[3];
                
                if (data[4] != null) cachedReport = (CashFlowReportResult) data[4];
                if (data[5] != null) cachedSummary = (MonthlySummary) data[5];
                if (data[6] != null) cachedPrevSummary = (MonthlySummary) data[6];
                if (data[7] != null) cachedPrevPrevSummary = (MonthlySummary) data[7];
                
                transactionAdapter.setEntityLookups(categoriesById, walletsById);
                updateDisplayModeUi();
                updateSummaryUi();
                
                if (tabLayout.getSelectedTabPosition() == 0) {
                    if (scrollHandle != null) scrollHandle.reload();
                } else {
                    bindSummaryTabUi();
                }
            }
        );
    }
    
    private void loadSummaryDataOnly() {
        if (cachedReport != null && cachedSummary != null) {
            bindSummaryTabUi();
            return;
        }
        int generation = reloadGeneration;
        Long walletId = sharedViewModel.getWalletFilter().getValue();
        Long categoryId = sharedViewModel.getCategoryFilter().getValue();

        services.dbWorker.compute(
            () -> {
                CashFlowReportResult reportResult = services.cashFlowReportService.buildReport(startDate, endDate, cutoffDay, walletId);
                MonthlySummary summary = services.summaryService.loadRange(startDate, endDate, walletId, categoryId);
                DateRange prevRange = PayrollCycleResolver.shiftMonths(startDate, cutoffDay, -1);
                DateRange prevPrevRange = PayrollCycleResolver.shiftMonths(startDate, cutoffDay, -2);
                MonthlySummary prevSummary = services.summaryService.loadRange(prevRange.getStart(), prevRange.getEnd(), walletId, categoryId);
                MonthlySummary prevPrevSummary = services.summaryService.loadRange(prevPrevRange.getStart(), prevPrevRange.getEnd(), walletId, categoryId);
                return new Object[]{reportResult, summary, prevSummary, prevPrevSummary};
            },
            data -> {
                if (!isAdded() || generation != reloadGeneration || data == null) return;
                cachedReport = (CashFlowReportResult) data[0];
                cachedSummary = (MonthlySummary) data[1];
                cachedPrevSummary = (MonthlySummary) data[2];
                cachedPrevPrevSummary = (MonthlySummary) data[3];
                bindSummaryTabUi();
            }
        );
    }
    
    private PageResult<Transaction, HistoryPageCursor> loadHistoryPage(@Nullable HistoryPageCursor cursor) {
        return services.transactionGateway.findHistoryPage(activeQuery, cursor, 30);
    }

    private void updateDisplayModeUi() {
        DashboardViewModel.DisplayMode mode = sharedViewModel.getDisplayMode().getValue();
        if (transactionAdapter != null) {
            long totalIncome = cachedTotals != null ? cachedTotals.getIncomeMinor() : 0L;
            transactionAdapter.setDisplayMode(mode, totalIncome);
        }
        updateSummaryUi();
    }
    
    private void updateSummaryUi() {
        if (cachedTotals == null) return;
        
        DashboardViewModel.DisplayMode mode = sharedViewModel.getDisplayMode().getValue();
        if (mode == DashboardViewModel.DisplayMode.MASKED) {
            collapsedBalanceText.setText(R.string.java_MonthlyDashboardFragment_rp);
            if (month == -1) {
                monthTitle.setText(String.valueOf(year));
            } else {
                monthTitle.setText(LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.forLanguageTag("id-ID"))));
            }
        } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
            collapsedBalanceText.setText(R.string.java_MonthlyDashboardFragment_100);
        } else {
            long netBalance = cachedTotals.getIncomeMinor() - cachedTotals.getExpenseMinor();
            collapsedBalanceText.setText(MoneyFormatter.format(netBalance));
        }
        
        List<DonutChartView.DonutItem> inflowItems = new ArrayList<>();
        List<DonutChartView.DonutItem> outflowItems = new ArrayList<>();
        
        if (cachedReport != null) {
            if (cachedTotals.getIncomeMinor() > 0) {
                 inflowItems.add(new DonutChartView.DonutItem("Pemasukan", cachedTotals.getIncomeMinor(), 0xFF4A9E7F));
            }
            if (cachedTotals.getExpenseMinor() > 0) {
                 outflowItems.add(new DonutChartView.DonutItem("Pengeluaran", cachedTotals.getExpenseMinor(), 0xFFE74C3C));
            }
        } else {
            if (cachedTotals.getIncomeMinor() > 0) {
                 inflowItems.add(new DonutChartView.DonutItem("Pemasukan", cachedTotals.getIncomeMinor(), 0xFF4A9E7F));
            }
            if (cachedTotals.getExpenseMinor() > 0) {
                 outflowItems.add(new DonutChartView.DonutItem("Pengeluaran", cachedTotals.getExpenseMinor(), 0xFFE74C3C));
            }
        }
        
        donutChart.setData(inflowItems, outflowItems);
        
        // Update new explicit text view for Saldo Bersih, Income, Expense
        TextView heroNetBalance = requireView().findViewById(R.id.monthly_hero_net_balance);
        if (heroNetBalance != null) {
            if (mode == DashboardViewModel.DisplayMode.MASKED) {
                heroNetBalance.setText(R.string.java_MonthlyDashboardFragment_rp);
            } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                heroNetBalance.setText(R.string.java_MonthlyDashboardFragment_100);
            } else {
                heroNetBalance.setText(MoneyFormatter.format(cachedTotals.getIncomeMinor() - cachedTotals.getExpenseMinor()));
            }
        }
        TextView heroIncome = requireView().findViewById(R.id.monthly_hero_income);
        if (heroIncome != null) {
            if (mode == DashboardViewModel.DisplayMode.MASKED) {
                heroIncome.setText(R.string.java_MonthlyDashboardFragment_rp);
            } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                heroIncome.setText(R.string.java_MonthlyDashboardFragment_100);
            } else {
                heroIncome.setText(MoneyFormatter.format(cachedTotals.getIncomeMinor()));
            }
        }
        TextView heroExpense = requireView().findViewById(R.id.monthly_hero_expense);
        if (heroExpense != null) {
            if (mode == DashboardViewModel.DisplayMode.MASKED) {
                heroExpense.setText(R.string.java_MonthlyDashboardFragment_rp);
            } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                if (cachedTotals.getIncomeMinor() > 0) {
                    float ratio = (cachedTotals.getExpenseMinor() * 100f) / cachedTotals.getIncomeMinor();
                    heroExpense.setText(String.format(java.util.Locale.getDefault(), "%.0f%%", ratio));
                } else {
                    heroExpense.setText("-");
                }
            } else {
                heroExpense.setText(MoneyFormatter.format(cachedTotals.getExpenseMinor()));
            }
        }
        
        FinancialAdvisor.Advice advice = FinancialAdvisor.getAdvice(requireContext(), cachedSummary, cachedPrevSummary, cachedPrevPrevSummary, startDate, endDate);
        
        android.widget.ImageView adviceBtn = requireView().findViewById(R.id.monthly_hero_advice_btn);
        android.widget.ImageView collapsedAdviceBtn = requireView().findViewById(R.id.collapsed_advice_btn);
        
        if (advice != null && cachedSummary != null) {
            if (adviceBtn != null) {
                adviceBtn.setVisibility(View.VISIBLE);
                adviceBtn.setOnClickListener(v ->
                        com.dwlhm.finan.ui.summary.FinancialAdviceDialog.show(requireContext(), advice, cachedSummary, sharedViewModel.getDisplayMode().getValue()));
            }
            if (collapsedAdviceBtn != null) {
                collapsedAdviceBtn.setVisibility(View.VISIBLE);
                collapsedAdviceBtn.setOnClickListener(v ->
                        com.dwlhm.finan.ui.summary.FinancialAdviceDialog.show(requireContext(), advice, cachedSummary, sharedViewModel.getDisplayMode().getValue()));
            }
        } else {
            if (adviceBtn != null) adviceBtn.setVisibility(View.GONE);
            if (collapsedAdviceBtn != null) collapsedAdviceBtn.setVisibility(View.GONE);
        }
        
        updateEmptyState(cachedTotals.getCount() == 0);
    }
    
    private void updateEmptyState(boolean empty) {
        if (tabLayout.getSelectedTabPosition() != 0) {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            return;
        }
        
        boolean searching = !activeQuery.search().isEmpty();
        emptyTitle.setText(searching ? R.string.history_search_empty : R.string.history_empty);
        emptyHint.setText(searching ? R.string.history_search_empty_hint : R.string.history_empty_hint);
        
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        summaryScroll.setVisibility(View.GONE);
    }

    private void switchTab(int position) {
        if (position == 0) {
            summaryScroll.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.removeItemDecoration(stickyDecoration);
            recyclerView.addItemDecoration(stickyDecoration);
            recyclerView.setAdapter(transactionAdapter);
            if (scrollHandle != null) scrollHandle.reload();
            updateSummaryUi();
        } else {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            summaryScroll.setVisibility(View.VISIBLE);
            recyclerView.removeItemDecoration(stickyDecoration);
            recyclerView.setAdapter(null);
            loadSummaryDataOnly();
        }
    }
    
    private void setWeeklyChartMode(boolean income) {
        if (showWeeklyChartIncome == income) return;
        showWeeklyChartIncome = income;
        updateChartToggle();
        if (cachedReport != null) {
            bindWeeklyChart(cachedReport);
        }
    }

    private void updateChartToggle() {
        if (weeklyToggleGroup == null) return;
        int targetId = showWeeklyChartIncome ? R.id.monthly_chart_toggle_income : R.id.monthly_chart_toggle_expense;
        if (weeklyToggleGroup.getCheckedButtonId() != targetId) {
            weeklyToggleGroup.check(targetId);
        }
    }
    
    private void setCategoryChartMode(boolean income) {
        if (showCategoryChartIncome == income) return;
        showCategoryChartIncome = income;
        updateCategoryChartToggle();
        if (cachedReport != null) {
            bindCategoryChart(cachedReport);
        }
    }

    private void updateCategoryChartToggle() {
        if (catToggleGroup == null) return;
        int targetId = showCategoryChartIncome ? R.id.monthly_cat_chart_toggle_income : R.id.monthly_cat_chart_toggle_expense;
        if (catToggleGroup.getCheckedButtonId() != targetId) {
            catToggleGroup.check(targetId);
        }
    }

    private void bindSummaryTabUi() {
        if (cachedReport == null || summaryScroll.getVisibility() != View.VISIBLE) return;
        
        bindWeeklyChart(cachedReport);
        bindCategoryChart(cachedReport);
    }
    
    private void bindCategoryChart(CashFlowReportResult reportResult) {
        categoryBarsContainer.removeAllViews();
        Map<Long, Long> amountByCatId = new HashMap<>();
        Map<Long, String> nameByCatId = new HashMap<>();
        
        long totalAmount = 0;
        long maxAmount = 0;
        
        for (CashFlowReport report : reportResult.getAllReports()) {
            for (CashFlowActivityTotal act : report.getActivityTotals()) {
                List<CategoryTotal> cats = showCategoryChartIncome ? act.getIncomeCategories() : act.getExpenseCategories();
                for (CategoryTotal cat : cats) {
                    Long current = amountByCatId.getOrDefault(cat.getCategoryId(), 0L);
                    long newVal = (current != null ? current : 0L) + cat.getTotalMinor();
                    amountByCatId.put(cat.getCategoryId(), newVal);
                    nameByCatId.put(cat.getCategoryId(), cat.getCategoryName());
                    
                    if (newVal > maxAmount) maxAmount = newVal;
                }
            }
        }
        
        for (long amount : amountByCatId.values()) {
            totalAmount += amount;
        }

        if (amountByCatId.isEmpty()) {
            categoryChart.setVisibility(View.GONE);
            return;
        }
        
        categoryChart.setVisibility(View.VISIBLE);

        List<Map.Entry<Long, Long>> sortedCats = new ArrayList<>(amountByCatId.entrySet());
        sortedCats.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        int i = 0;
        for (Map.Entry<Long, Long> entry : sortedCats) {
            String catName = nameByCatId.get(entry.getKey());
            long val = entry.getValue();
            if (val > 0) {
                categoryBarsContainer.addView(createCategoryRow(catName, val, maxAmount, totalAmount, i));
                i++;
            }
        }
    }
    
    private View createCategoryRow(String categoryName, long amount, long maxAmount, long totalAmount, int index) {
        android.content.Context context = requireContext();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, UiComponentStyles.dp(context, 5), 0, UiComponentStyles.dp(context, 5));

        TextView label = new TextView(context);
        label.setText(categoryName);
        label.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
        label.setTextSize(10f);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
            UiComponentStyles.dp(context, 72), LinearLayout.LayoutParams.WRAP_CONTENT);
        label.setLayoutParams(labelLp);
        label.setPadding(0, 0, UiComponentStyles.dp(context, 8), 0);
        row.addView(label);

        int barHeight = UiComponentStyles.dp(context, 28);
        int cornerRadius = UiComponentStyles.dp(context, 6);
        
        LinearLayout barContainer = new LinearLayout(context);
        barContainer.setOrientation(LinearLayout.HORIZONTAL);
        barContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        barContainer.setWeightSum((float) maxAmount);

        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(0, barHeight, (float) amount));
        bar.setMinimumHeight(barHeight);

        View segment = new View(context);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(DAY_COLORS[index % DAY_COLORS.length]);
        shape.setCornerRadius(cornerRadius);
        segment.setBackground(shape);
        segment.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        bar.addView(segment);
        
        barContainer.addView(bar);
        row.addView(barContainer);
        
        TextView amountLabel = new TextView(context);
        DashboardViewModel.DisplayMode mode = sharedViewModel.getDisplayMode().getValue();
        if (mode == DashboardViewModel.DisplayMode.MASKED) {
            amountLabel.setText(R.string.java_MonthlyDashboardFragment_rp);
        } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
            double percent = totalAmount > 0 ? (amount * 100.0 / totalAmount) : 0;
            amountLabel.setText(String.format(java.util.Locale.getDefault(), "%.0f%%", percent));
        } else {
            amountLabel.setText(MoneyFormatter.format(amount));
        }
        amountLabel.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
        amountLabel.setTextSize(10f);
        amountLabel.setTypeface(amountLabel.getTypeface(), Typeface.BOLD);
        amountLabel.setPadding(UiComponentStyles.dp(context, 8), 0, 0, 0);
        row.addView(amountLabel);

        return row;
    }
    
    private void bindWeeklyChart(CashFlowReportResult reportResult) {
        chartWeeksContainer.removeAllViews();
        long maxTotal = 0;
        int totalDataPoints = 0;

        if (month == -1) {
            weeklyChartTitle.setText(R.string.java_MonthlyDashboardFragment_aktivitas_mingguan);
            populateChartLegend(true);
            for (CashFlowReport report : reportResult.getAllReports()) {
                for (CashFlowReport.WeekSummary monthSummary : report.getWeekSummaries()) {
                    long monthTotal = showWeeklyChartIncome ? monthSummary.getWeekIncome() : monthSummary.getWeekExpense();
                    if (monthTotal > maxTotal) maxTotal = monthTotal;
                    totalDataPoints++; // count all months
                }
            }
        } else {
            weeklyChartTitle.setText(R.string.java_MonthlyDashboardFragment_aktivitas_harian);
            populateChartLegend(false);
            for (CashFlowReport report : reportResult.getAllReports()) {
                totalDataPoints += report.getWeekSummaries().size() * 7;
                for (CashFlowReport.WeekSummary week : report.getWeekSummaries()) {
                    long weekTotal = showWeeklyChartIncome ? week.getWeekIncome() : week.getWeekExpense();
                    if (weekTotal > maxTotal) maxTotal = weekTotal;
                }
            }
        }

        if (totalDataPoints == 0) {
            weeklyChart.setVisibility(View.GONE);
            return;
        }
        weeklyChart.setVisibility(View.VISIBLE);

        if (month == -1) {
            for (CashFlowReport report : reportResult.getAllReports()) {
                for (CashFlowReport.WeekSummary monthSummary : report.getWeekSummaries()) {
                    chartWeeksContainer.addView(createMonthRow(monthSummary, maxTotal));
                }
            }
        } else {
            for (CashFlowReport report : reportResult.getAllReports()) {
                for (CashFlowReport.WeekSummary week : report.getWeekSummaries()) {
                    long weekTotal = showWeeklyChartIncome ? week.getWeekIncome() : week.getWeekExpense();
                    if (weekTotal > 0) {
                        chartWeeksContainer.addView(createWeekRow(week, maxTotal));
                    }
                }
            }
        }
    }
    
    private static final int[] DAY_COLORS = {
        0xFFE74C3C, 0xFF2980B9, 0xFF27AE60, 0xFFF39C12, 0xFF8E44AD, 0xFF16A085, 0xFFE91E63
    };

    private static int dayOfWeekIndex(long dateMillis) {
        if (dateMillis <= 0) return 0;
        return java.time.Instant.ofEpochMilli(dateMillis)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            .getDayOfWeek().getValue() - 1;
    }

    private View createWeekRow(CashFlowReport.WeekSummary week, long maxWeekTotal) {
        android.content.Context context = requireContext();
        long weekTotal = showWeeklyChartIncome ? week.getWeekIncome() : week.getWeekExpense();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, UiComponentStyles.dp(context, 12));
        
        android.util.TypedValue outValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        row.setBackgroundResource(outValue.resourceId);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            long totalIncome = cachedTotals != null ? cachedTotals.getIncomeMinor() : 0L;
            services.dbWorker.compute(
                () -> {
                    String type = showWeeklyChartIncome ? "INCOME" : "EXPENSE";
                    java.util.Map<Long, java.util.List<com.dwlhm.finan.ui.summary.WeeklyDetailBottomSheetDialog.CategoryDetail>> categoriesPerDay = new java.util.HashMap<>();
                    for (CashFlowReport.DailyTotal day : week.getDays()) {
                        long startOfDay = day.getDateMillis();
                        long endOfDay = startOfDay + 86400000L;
                        java.util.List<com.dwlhm.finan.data.dao.SummaryDao.CategorySumRow> rows = 
                            services.summaryDao.categoryTotalsBetween(type, startOfDay, endOfDay, activeQuery.walletId(), 50);
                        java.util.List<com.dwlhm.finan.ui.summary.WeeklyDetailBottomSheetDialog.CategoryDetail> catDetails = new java.util.ArrayList<>();
                        for (com.dwlhm.finan.data.dao.SummaryDao.CategorySumRow catRow : rows) {
                            if (catRow.categoryId <= 0) continue;
                            com.dwlhm.finan.data.entity.Category cat = services.categoryDao.findById(catRow.categoryId);
                            String name = cat != null ? cat.getName() : ("#" + catRow.categoryId);
                            catDetails.add(new com.dwlhm.finan.ui.summary.WeeklyDetailBottomSheetDialog.CategoryDetail(name, catRow.totalMinor));
                        }
                        categoriesPerDay.put(startOfDay, catDetails);
                    }
                    return categoriesPerDay;
                },
                categoriesPerDay -> com.dwlhm.finan.ui.summary.WeeklyDetailBottomSheetDialog.show(
                        context,
                        week,
                        showWeeklyChartIncome,
                        sharedViewModel.getDisplayMode().getValue(),
                        totalIncome,
                        categoriesPerDay,
                        month == -1
                )
            );
        });

        TextView weekLabel = new TextView(context);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"));
        weekLabel.setText(week.getStartDate().format(fmt) + " - " + week.getEndDate().format(fmt));
        weekLabel.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
        weekLabel.setTextSize(10f);
        weekLabel.setTypeface(weekLabel.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
            UiComponentStyles.dp(context, 72), LinearLayout.LayoutParams.WRAP_CONTENT);
        weekLabel.setLayoutParams(labelLp);
        weekLabel.setPadding(0, 0, UiComponentStyles.dp(context, 8), 0);
        row.addView(weekLabel);

        int barHeight = UiComponentStyles.dp(context, 28);
        int cornerRadius = UiComponentStyles.dp(context, 6);
        
        LinearLayout barContainer = new LinearLayout(context);
        barContainer.setOrientation(LinearLayout.HORIZONTAL);
        barContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        barContainer.setWeightSum((float) maxWeekTotal);

        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(0, barHeight, (float) weekTotal));
        bar.setMinimumHeight(barHeight);

        List<CashFlowReport.DailyTotal> days = week.getDays();
        float weightSum = 0f;
        boolean hasData = false;
        for (CashFlowReport.DailyTotal day : days) {
            long val = showWeeklyChartIncome ? day.getIncomeMinor() : day.getExpenseMinor();
            if (val > 0) { weightSum += (float) val; hasData = true; }
        }

        if (hasData) {
            bar.setWeightSum(weightSum);
            int gapDp = UiComponentStyles.dp(context, 2);

            for (int i = 0; i < days.size(); i++) {
                long val = showWeeklyChartIncome ? days.get(i).getIncomeMinor() : days.get(i).getExpenseMinor();
                if (val <= 0) continue;

                View segment = new View(context);
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setColor(DAY_COLORS[dayOfWeekIndex(days.get(i).getDateMillis())]);
                shape.setCornerRadius(cornerRadius);
                segment.setBackground(shape);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, barHeight, (float) val);
                lp.rightMargin = gapDp;
                segment.setLayoutParams(lp);
                bar.addView(segment);
            }
        }
        barContainer.addView(bar);
        row.addView(barContainer);
        return row;
    }
    
    private View createMonthRow(CashFlowReport.WeekSummary monthSummary, long maxMonthTotal) {
        android.content.Context context = requireContext();
        long monthTotal = showWeeklyChartIncome ? monthSummary.getWeekIncome() : monthSummary.getWeekExpense();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, UiComponentStyles.dp(context, 12));
        
        android.util.TypedValue outValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        row.setBackgroundResource(outValue.resourceId);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            if (monthTotal <= 0) return; // Don't show bottom sheet if no data
            long totalIncome = cachedTotals != null ? cachedTotals.getIncomeMinor() : 0L;
            services.dbWorker.compute(
                () -> {
                    String type = showWeeklyChartIncome ? "INCOME" : "EXPENSE";
                    java.util.Map<Long, java.util.List<com.dwlhm.finan.ui.summary.MonthlyDetailBottomSheetDialog.CategoryDetail>> categoriesPerWeek = new java.util.HashMap<>();
                    for (CashFlowReport.DailyTotal w : monthSummary.getDays()) {
                        long startOfWeek = w.getDateMillis();
                        java.time.LocalDate weekStart = java.time.Instant.ofEpochMilli(startOfWeek).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                        java.time.LocalDate nextSunday = weekStart.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
                        java.time.LocalDate monthEnd = monthSummary.getEndDate();
                        java.time.LocalDate weekEnd = nextSunday.isBefore(monthEnd) ? nextSunday : monthEnd;
                        long endOfWeek = weekEnd.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                        
                        java.util.List<com.dwlhm.finan.data.dao.SummaryDao.CategorySumRow> rows = 
                            services.summaryDao.categoryTotalsBetween(type, startOfWeek, endOfWeek, activeQuery.walletId(), 50);
                        java.util.List<com.dwlhm.finan.ui.summary.MonthlyDetailBottomSheetDialog.CategoryDetail> catDetails = new java.util.ArrayList<>();
                        for (com.dwlhm.finan.data.dao.SummaryDao.CategorySumRow catRow : rows) {
                            if (catRow.categoryId <= 0) continue;
                            com.dwlhm.finan.data.entity.Category cat = services.categoryDao.findById(catRow.categoryId);
                            String name = cat != null ? cat.getName() : ("#" + catRow.categoryId);
                            catDetails.add(new com.dwlhm.finan.ui.summary.MonthlyDetailBottomSheetDialog.CategoryDetail(name, catRow.totalMinor));
                        }
                        categoriesPerWeek.put(startOfWeek, catDetails);
                    }
                    return categoriesPerWeek;
                },
                categoriesPerWeek -> com.dwlhm.finan.ui.summary.MonthlyDetailBottomSheetDialog.show(
                        context,
                        monthSummary,
                        showWeeklyChartIncome,
                        sharedViewModel.getDisplayMode().getValue(),
                        totalIncome,
                        categoriesPerWeek
                )
            );
        });

        TextView monthLabel = new TextView(context);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("id-ID"));
        // Use the middle of the month summary to avoid showing the previous month
        // when the financial cycle starts in the previous calendar month.
        java.time.LocalDate midDate = monthSummary.getStartDate()
            .plusDays(monthSummary.getStartDate().until(monthSummary.getEndDate()).getDays() / 2);
        monthLabel.setText(midDate.format(fmt));
        monthLabel.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
        monthLabel.setTextSize(10f);
        monthLabel.setTypeface(monthLabel.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
            UiComponentStyles.dp(context, 72), LinearLayout.LayoutParams.WRAP_CONTENT);
        monthLabel.setLayoutParams(labelLp);
        monthLabel.setPadding(0, 0, UiComponentStyles.dp(context, 8), 0);
        row.addView(monthLabel);

        int barHeight = UiComponentStyles.dp(context, 28);
        int cornerRadius = UiComponentStyles.dp(context, 6);
        
        LinearLayout barContainer = new LinearLayout(context);
        barContainer.setOrientation(LinearLayout.HORIZONTAL);
        barContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        barContainer.setWeightSum(maxMonthTotal > 0 ? (float) maxMonthTotal : 1f);

        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(0, barHeight, (float) monthTotal));
        if (monthTotal > 0) bar.setMinimumHeight(barHeight);

        List<CashFlowReport.DailyTotal> weeks = monthSummary.getDays();
        float weightSum = 0f;
        boolean hasData = false;
        for (CashFlowReport.DailyTotal week : weeks) {
            long val = showWeeklyChartIncome ? week.getIncomeMinor() : week.getExpenseMinor();
            if (val > 0) { weightSum += (float) val; hasData = true; }
        }

        if (hasData) {
            bar.setWeightSum(weightSum);
            int gapDp = UiComponentStyles.dp(context, 2);

            for (int i = 0; i < weeks.size(); i++) {
                long val = showWeeklyChartIncome ? weeks.get(i).getIncomeMinor() : weeks.get(i).getExpenseMinor();
                if (val <= 0) continue;

                View segment = new View(context);
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setColor(DAY_COLORS[i % DAY_COLORS.length]);
                shape.setCornerRadius(cornerRadius);
                segment.setBackground(shape);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, barHeight, (float) val);
                lp.rightMargin = gapDp;
                segment.setLayoutParams(lp);
                bar.addView(segment);
            }
        }
        barContainer.addView(bar);
        row.addView(barContainer);
        return row;
    }
    
    private void openTransactionDetail(int position) {
        BottomSheetHelper.show(new TransactionDetailDialog(
                requireContext(),
                services,
                transactionAdapter.getTransactionAt(position),
                this::loadData));
    }
    
    // Summary Helper Methods
    
    private void populateChartLegend(boolean isYearly) {
        chartLegend.removeAllViews();
        android.content.Context context = requireContext();
        String[] labels = isYearly ? new String[]{"1", "2", "3", "4", "5"} : new String[]{"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
        int count = isYearly ? 5 : 7;
        int dotSize = UiComponentStyles.dp(context, 6);

        for (int i = 0; i < count; i++) {
            GradientDrawable dotShape = new GradientDrawable();
            dotShape.setShape(GradientDrawable.OVAL);
            dotShape.setColor(DAY_COLORS[i % DAY_COLORS.length]);

            View dot = new View(context);
            dot.setLayoutParams(new LinearLayout.LayoutParams(dotSize, dotSize));
            dot.setBackground(dotShape);

            TextView label = new TextView(context);
            label.setText(labels[i]);
            label.setTextSize(9f);
            label.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
            label.setPadding(UiComponentStyles.dp(context, 3), 0, 0, 0);

            LinearLayout item = new LinearLayout(context);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER);
            item.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            item.addView(dot);
            item.addView(label);
            chartLegend.addView(item);
        }
    }
    
    private String format(long amountMinor) {
        return MoneyFormatter.format(amountMinor);
    }

    @Override
    public void onDestroyView() {
        reloadGeneration++;
        if (scrollHandle != null) {
            scrollHandle.dispose();
            scrollHandle = null;
        }
        super.onDestroyView();
    }
}
