package com.dwlhm.finan.ui.dashboard;

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
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.dwlhm.finan.domain.model.CashFlowReportResult;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.domain.model.CashFlowActivityTotal;
import com.dwlhm.finan.domain.model.CashFlowReport;
import com.dwlhm.finan.domain.model.CategoryTotal;
import com.dwlhm.finan.domain.model.WalletBalance;
import com.dwlhm.finan.ui.common.UiComponentStyles;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.util.TypedValue;
import java.util.ArrayList;

import java.util.HashMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthlyDashboardFragment extends ScreenFragment {

    private static final String ARG_YEAR = "arg_year";
    private static final String ARG_MONTH = "arg_month";

    private DashboardViewModel sharedViewModel;
    private AppServices services;
    
    private int year;
    private int month;
    private LocalDate startDate;
    private LocalDate endDate;
    
    private AppBarLayout appBarLayout;
    private TextView monthTitle;
    private DonutChartView donutChart;
    private View collapsedToolbar;
    private TextView collapsedBalanceText;
    private View collapsedAdviceBtn;
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
    private LinearLayout weeklyChart;
    private LinearLayout chartWeeksContainer;
    private TextView weeklyChartTitle;
    private TextView chartToggleIncome;
    private TextView chartToggleExpense;
    private LinearLayout chartLegend;
    private boolean showWeeklyChartIncome;
    
    private LinearLayout categoryChart;
    private LinearLayout categoryBarsContainer;
    private TextView catChartToggleIncome;
    private TextView catChartToggleExpense;
    private boolean showCategoryChartIncome;
    
    private HistoryTotals cachedTotals;
    private CashFlowReportResult cachedReport;
    private MonthlySummary cachedSummary;
    private MonthlySummary cachedPrevSummary;
    private MonthlySummary cachedPrevPrevSummary;
    private long cachedUnclassifiedCount;
    private long cachedUnclassifiedAmount;
    
    private HistoryQuery activeQuery = new HistoryQuery(null, null, null, null, null, false, HistorySearch.empty());
    
    private int reloadGeneration;
    private Map<Long, Category> categoriesById = Map.of();
    private Map<Long, Wallet> walletsById = Map.of();
    private TransactionSearchResolver searchResolver;

    public static MonthlyDashboardFragment newInstance(int year, int month) {
        MonthlyDashboardFragment fragment = new MonthlyDashboardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_YEAR, year);
        args.putInt(ARG_MONTH, month);
        fragment.setArguments(args);
        return fragment;
    }

    private static LocalDate lastBusinessDayOfMonth(LocalDate date) {
        LocalDate lastDay = date.withDayOfMonth(date.lengthOfMonth());
        if (lastDay.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
            return lastDay.minusDays(1);
        } else if (lastDay.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            return lastDay.minusDays(2);
        }
        return lastDay;
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
        
        if (month == -1) {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        } else {
            int cutoffDay = requireContext().getSharedPreferences("finan_prefs", android.content.Context.MODE_PRIVATE)
                    .getInt("cutoff_day", 1);
            
            LocalDate anchor = LocalDate.of(year, month, 1);
            if (cutoffDay == 1) {
                startDate = anchor;
            } else if (cutoffDay == -1) {
                startDate = lastBusinessDayOfMonth(anchor.minusMonths(1));
            } else {
                if (cutoffDay > 15) {
                    startDate = anchor.minusMonths(1).withDayOfMonth(cutoffDay);
                } else {
                    startDate = anchor.withDayOfMonth(cutoffDay);
                }
            }
            endDate = startDate.plusMonths(1).minusDays(1);
        }
    }

    private boolean isDataLoaded = false;

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
        collapsedAdviceBtn = view.findViewById(R.id.collapsed_advice_btn);
        tabLayout = view.findViewById(R.id.monthly_tabs);
        recyclerView = view.findViewById(R.id.monthly_recycler_view);
        emptyState = view.findViewById(R.id.monthly_empty);
        emptyTitle = view.findViewById(R.id.monthly_empty_title);
        emptyHint = view.findViewById(R.id.monthly_empty_hint);
        
        summaryScroll = view.findViewById(R.id.monthly_summary_scroll);
        weeklyChart = view.findViewById(R.id.monthly_weekly_chart);
        weeklyChartTitle = view.findViewById(R.id.monthly_weekly_chart_title);
        chartWeeksContainer = view.findViewById(R.id.monthly_chart_weeks);
        chartToggleIncome = view.findViewById(R.id.monthly_chart_toggle_income);
        chartToggleExpense = view.findViewById(R.id.monthly_chart_toggle_expense);
        chartLegend = view.findViewById(R.id.monthly_chart_legend);
        
        categoryChart = view.findViewById(R.id.monthly_category_chart);
        categoryBarsContainer = view.findViewById(R.id.monthly_category_bars);
        catChartToggleIncome = view.findViewById(R.id.monthly_cat_chart_toggle_income);
        catChartToggleExpense = view.findViewById(R.id.monthly_cat_chart_toggle_expense);

        chartToggleIncome.setOnClickListener(v -> setWeeklyChartMode(true));
        chartToggleExpense.setOnClickListener(v -> setWeeklyChartMode(false));
        updateChartToggle();
        populateChartLegend(month == -1);

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
            monthTitle.setText(startDate.format(formatter));
        }
        
        monthTitle.setOnClickListener(v -> {
            DashboardViewModel.TimeRangeMode currentMode = (month == -1) ? 
                    DashboardViewModel.TimeRangeMode.YEARLY : DashboardViewModel.TimeRangeMode.MONTHLY;
                    
            MonthYearPickerBottomSheetDialog dialog = new MonthYearPickerBottomSheetDialog(
                    requireContext(), currentMode, year, month == -1 ? 1 : month);
            
            dialog.setOnTimeRangeSelectedListener((mode, selectedYear, selectedMonth) -> {
                sharedViewModel.setTimeRangeState(new DashboardViewModel.TimeRangeState(mode, selectedYear, selectedMonth));
            });
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
        sharedViewModel.getSearchQuery().observe(getViewLifecycleOwner(), q -> loadData());
        sharedViewModel.getWalletFilter().observe(getViewLifecycleOwner(), w -> loadData());
        sharedViewModel.getCategoryFilter().observe(getViewLifecycleOwner(), c -> loadData());
        sharedViewModel.getTransactionTypeFilter().observe(getViewLifecycleOwner(), t -> loadData());
        sharedViewModel.getDisplayMode().observe(getViewLifecycleOwner(), mode -> updateDisplayModeUi());
    }

    private void loadData() {
        int generation = ++reloadGeneration;
        Long startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long endExclusiveMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        String queryStr = sharedViewModel.getSearchQuery().getValue();
        Long walletId = sharedViewModel.getWalletFilter().getValue();
        Long categoryId = sharedViewModel.getCategoryFilter().getValue();
        String typeStr = sharedViewModel.getTransactionTypeFilter().getValue();
        
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
                
                // Fetch Summary Data
                CashFlowReportResult reportResult = services.cashFlowReportService.buildReport(startDate, endDate, walletId);
                MonthlySummary summary = services.summaryService.loadRange(startDate, endDate, walletId, categoryId);
                MonthlySummary prevSummary = services.summaryService.loadRange(startDate.minusMonths(1), endDate.minusMonths(1), walletId, categoryId);
                MonthlySummary prevPrevSummary = services.summaryService.loadRange(startDate.minusMonths(2), endDate.minusMonths(2), walletId, categoryId);
                
                long unclassifiedCount = 0L;
                long unclassifiedAmount = 0L;
                List<Category> allCats = cMap.isEmpty() ? services.categoryDao.findAllOrdered() : new ArrayList<>(cMap.values());
                for (Category cat : allCats) {
                    if (CashFlowActivity.UNCLASSIFIED.name().equals(cat.getCashFlowActivity())) unclassifiedCount++;
                }
                for (CashFlowReport report : reportResult.getAllReports()) {
                    for (CashFlowActivityTotal act : report.getActivityTotals()) {
                        if (act.getActivity() == CashFlowActivity.UNCLASSIFIED) {
                            unclassifiedAmount += act.getIncomeMinor() + act.getExpenseMinor();
                        }
                    }
                }
                
                return new Object[]{wMap, cMap, resolver, query, totals, reportResult, summary, prevSummary, prevPrevSummary, unclassifiedCount, unclassifiedAmount};
            },
            data -> {
                if (!isAdded() || generation != reloadGeneration || data == null) return;
                
                @SuppressWarnings("unchecked")
                Map<Long, Wallet> w = (Map<Long, Wallet>) data[0];
                walletsById = w;
                @SuppressWarnings("unchecked")
                Map<Long, Category> c = (Map<Long, Category>) data[1];
                categoriesById = c;
                searchResolver = (TransactionSearchResolver) data[2];
                activeQuery = (HistoryQuery) data[3];
                cachedTotals = (HistoryTotals) data[4];
                cachedReport = (CashFlowReportResult) data[5];
                cachedSummary = (MonthlySummary) data[6];
                cachedPrevSummary = (MonthlySummary) data[7];
                cachedPrevPrevSummary = (MonthlySummary) data[8];
                cachedUnclassifiedCount = (long) data[9];
                cachedUnclassifiedAmount = (long) data[10];
                
                transactionAdapter.setEntityLookups(categoriesById, walletsById);
                updateDisplayModeUi();
                bindSummaryTabUi();
                
                if (tabLayout.getSelectedTabPosition() == 0) {
                    if (scrollHandle != null) scrollHandle.reload();
                }
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
            collapsedBalanceText.setText("Rp ***");
            if (month == -1) {
                monthTitle.setText(String.valueOf(year));
            } else {
                monthTitle.setText(startDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.forLanguageTag("id-ID"))));
            }
        } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
            collapsedBalanceText.setText("100%");
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
                heroNetBalance.setText("Rp ***");
            } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                heroNetBalance.setText("100%");
            } else {
                heroNetBalance.setText(MoneyFormatter.format(cachedTotals.getIncomeMinor() - cachedTotals.getExpenseMinor()));
            }
        }
        TextView heroIncome = requireView().findViewById(R.id.monthly_hero_income);
        if (heroIncome != null) {
            if (mode == DashboardViewModel.DisplayMode.MASKED) {
                heroIncome.setText("Rp ***");
            } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                heroIncome.setText("100%");
            } else {
                heroIncome.setText(MoneyFormatter.format(cachedTotals.getIncomeMinor()));
            }
        }
        TextView heroExpense = requireView().findViewById(R.id.monthly_hero_expense);
        if (heroExpense != null) {
            if (mode == DashboardViewModel.DisplayMode.MASKED) {
                heroExpense.setText("Rp ***");
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
                adviceBtn.setOnClickListener(v -> {
                    com.dwlhm.finan.ui.summary.FinancialAdviceDialog.show(requireContext(), advice, cachedSummary, sharedViewModel.getDisplayMode().getValue());
                });
            }
            if (collapsedAdviceBtn != null) {
                collapsedAdviceBtn.setVisibility(View.VISIBLE);
                collapsedAdviceBtn.setOnClickListener(v -> {
                    com.dwlhm.finan.ui.summary.FinancialAdviceDialog.show(requireContext(), advice, cachedSummary, sharedViewModel.getDisplayMode().getValue());
                });
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
            updateSummaryUi(); // to trigger empty state logic
        } else {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            summaryScroll.setVisibility(View.VISIBLE);
            recyclerView.removeItemDecoration(stickyDecoration);
            recyclerView.setAdapter(null);
            bindSummaryTabUi();
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
        if (chartToggleIncome == null || chartToggleExpense == null) return;
        if (showWeeklyChartIncome) {
            chartToggleIncome.setBackgroundResource(R.drawable.bg_toggle_active);
            chartToggleIncome.setTextColor(0xFFFFFFFF);
            chartToggleExpense.setBackground(null);
            chartToggleExpense.setTextColor(0xFF4A9E7F);
        } else {
            chartToggleExpense.setBackgroundResource(R.drawable.bg_toggle_active);
            chartToggleExpense.setTextColor(0xFFFFFFFF);
            chartToggleIncome.setBackground(null);
            chartToggleIncome.setTextColor(0xFF4A9E7F);
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
        if (catChartToggleIncome == null || catChartToggleExpense == null) return;
        if (showCategoryChartIncome) {
            catChartToggleIncome.setBackgroundResource(R.drawable.bg_toggle_active);
            catChartToggleIncome.setTextColor(0xFFFFFFFF);
            catChartToggleExpense.setBackground(null);
            catChartToggleExpense.setTextColor(0xFF4A9E7F);
        } else {
            catChartToggleExpense.setBackgroundResource(R.drawable.bg_toggle_active);
            catChartToggleExpense.setTextColor(0xFFFFFFFF);
            catChartToggleIncome.setBackground(null);
            catChartToggleIncome.setTextColor(0xFF4A9E7F);
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
                    long current = amountByCatId.getOrDefault(cat.getCategoryId(), 0L);
                    long newVal = current + cat.getTotalMinor();
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
        bar.setMinimumWidth(barHeight);

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
            amountLabel.setText("Rp ***");
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
        long totalAmount = 0;
        int totalDataPoints = 0;
        
        if (month == -1) {
            weeklyChartTitle.setText("Aktivitas Mingguan");
            populateChartLegend(true);
            for (CashFlowReport report : reportResult.getAllReports()) {
                for (CashFlowReport.WeekSummary monthSummary : report.getWeekSummaries()) {
                    long monthTotal = showWeeklyChartIncome ? monthSummary.getWeekIncome() : monthSummary.getWeekExpense();
                    totalAmount += monthTotal;
                    if (monthTotal > maxTotal) maxTotal = monthTotal;
                    totalDataPoints++; // count all months
                }
            }
        } else {
            weeklyChartTitle.setText("Aktivitas Harian");
            populateChartLegend(false);
            for (CashFlowReport report : reportResult.getAllReports()) {
                totalDataPoints += report.getWeekSummaries().size() * 7;
                for (CashFlowReport.WeekSummary week : report.getWeekSummaries()) {
                    long weekTotal = showWeeklyChartIncome ? week.getWeekIncome() : week.getWeekExpense();
                    totalAmount += weekTotal;
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
                    chartWeeksContainer.addView(createMonthRow(monthSummary, maxTotal, totalAmount));
                }
            }
        } else {
            for (CashFlowReport report : reportResult.getAllReports()) {
                for (CashFlowReport.WeekSummary week : report.getWeekSummaries()) {
                    long weekTotal = showWeeklyChartIncome ? week.getWeekIncome() : week.getWeekExpense();
                    if (weekTotal > 0) {
                        chartWeeksContainer.addView(createWeekRow(week, maxTotal, totalAmount));
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

    private View createWeekRow(CashFlowReport.WeekSummary week, long maxWeekTotal, long totalAmount) {
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
                categoriesPerDay -> {
                    com.dwlhm.finan.ui.summary.WeeklyDetailBottomSheetDialog.show(
                        context, 
                        week, 
                        showWeeklyChartIncome, 
                        sharedViewModel.getDisplayMode().getValue(), 
                        totalIncome,
                        categoriesPerDay,
                        month == -1
                    );
                }
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
        bar.setMinimumWidth(barHeight);

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
    
    private View createMonthRow(CashFlowReport.WeekSummary monthSummary, long maxMonthTotal, long totalAmount) {
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
                categoriesPerWeek -> {
                    com.dwlhm.finan.ui.summary.MonthlyDetailBottomSheetDialog.show(
                        context, 
                        monthSummary, 
                        showWeeklyChartIncome, 
                        sharedViewModel.getDisplayMode().getValue(), 
                        totalIncome,
                        categoriesPerWeek
                    );
                }
            );
        });

        TextView monthLabel = new TextView(context);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("id-ID"));
        monthLabel.setText(monthSummary.getStartDate().format(fmt));
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
        if (monthTotal > 0) bar.setMinimumWidth(barHeight);

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
                () -> loadData()));
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
    
    private String fmtPct(long amount, long total, boolean signed) {
        if (total <= 0) return "0%";
        double pct = (amount * 100.0) / total;
        return (signed && amount >= 0 ? "+" : "") + String.format(Locale.getDefault(), "%.1f%%", pct);
    }
    
    private String format(long amountMinor) {
        return MoneyFormatter.format(amountMinor);
    }

    private String formatCompact(long amountMinor) {
        long amount = amountMinor / 100;
        if (Math.abs(amount) >= 1_000_000_000) {
            return String.format(Locale.getDefault(), "%.1fM", amount / 1_000_000_000.0);
        } else if (Math.abs(amount) >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.1fJt", amount / 1_000_000.0);
        } else if (Math.abs(amount) >= 1_000) {
            return String.format(Locale.getDefault(), "%.1fRb", amount / 1_000.0);
        }
        return String.valueOf(amount);
    }
    
    private String getActivityTitle(CashFlowActivity activity) {
        switch (activity) {
            case OPERATING: return getString(R.string.cashflow_operating);
            case INVESTING: return getString(R.string.cashflow_investing);
            case FINANCING: return getString(R.string.cashflow_financing);
            case UNCLASSIFIED: default: return getString(R.string.cashflow_unclassified);
        }
    }

    private View createNetBalanceRow(android.content.Context context, long amount, boolean masked) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, UiComponentStyles.dp(context, 8), 0, UiComponentStyles.dp(context, 8));

        TextView labelView = new TextView(context);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        labelView.setText("Total Saldo Bersih");
        labelView.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
        labelView.setTextSize(14f);
        labelView.setTypeface(labelView.getTypeface(), android.graphics.Typeface.BOLD);
        row.addView(labelView);

        TextView amountView = new TextView(context);
        amountView.setText(masked ? "Rp ***" : MoneyFormatter.format(amount));
        amountView.setTextColor(ContextCompat.getColor(context, amount >= 0 ? R.color.finan_primary : R.color.finan_expense));
        amountView.setTextSize(14f);
        amountView.setTypeface(amountView.getTypeface(), android.graphics.Typeface.BOLD);
        row.addView(amountView);

        return row;
    }

    private View createLegendHeader(android.content.Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        tv.setPadding(0, UiComponentStyles.dp(context, 8), 0, UiComponentStyles.dp(context, 4));
        return tv;
    }
    
    private View createLegendRow(android.content.Context context, String label, long amount, int color, double percentage) {
        boolean showPercentageMode = sharedViewModel.getDisplayMode().getValue() == DashboardViewModel.DisplayMode.PERCENTAGE;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, UiComponentStyles.dp(context, 4), 0, UiComponentStyles.dp(context, 4));

        View dot = new View(context);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(UiComponentStyles.dp(context, 8), UiComponentStyles.dp(context, 8));
        dotParams.setMarginEnd(UiComponentStyles.dp(context, 8));
        dot.setLayoutParams(dotParams);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        dot.setBackground(shape);

        TextView labelView = new TextView(context);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelParams);
        if (showPercentageMode) {
            labelView.setText(label);
        } else {
            labelView.setText(String.format(Locale.getDefault(), "%s (%.0f%%)", label, percentage));
        }
        labelView.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        labelView.setMaxLines(1);

        TextView amountView = new TextView(context);
        amountView.setText(showPercentageMode ? String.format(Locale.getDefault(), "%.0f%%", percentage) : format(amount));
        amountView.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
        amountView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        amountView.setTypeface(amountView.getTypeface(), Typeface.BOLD);

        row.addView(dot);
        row.addView(labelView);
        row.addView(amountView);
        return row;
    }
    
    private int getCategoryColor(android.content.Context context, long categoryId) {
        int[] palette = {
            ContextCompat.getColor(context, R.color.finan_primary),
            ContextCompat.getColor(context, R.color.finan_income),
            ContextCompat.getColor(context, R.color.finan_expense),
            ContextCompat.getColor(context, R.color.finan_warm_accent),
            ContextCompat.getColor(context, R.color.finan_accent),
            0xFF4A90E2, 0xFF8B6EB8, 0xFF4F6D7A
        };
        int index = (int) (categoryId % palette.length);
        if (index < 0) index += palette.length;
        return palette[index];
    }

    private View createDivider(android.content.Context context) {
        View divider = new View(context);
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.finan_divider));
        divider.setLayoutParams(
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, UiComponentStyles.dp(context, 1)));
        return divider;
    }

    // Inner Adapters
    
    private class CashFlowAdapter extends RecyclerView.Adapter<CashFlowAdapter.ViewHolder> {
        private java.util.List<CashFlowActivityTotal> items = new ArrayList<>();

        @android.annotation.SuppressLint("NotifyDataSetChanged")
        public void setItems(java.util.List<CashFlowActivityTotal> newItems) {
            this.items = newItems;
            //noinspection NotifyDataSetChanged
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_cash_flow_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CashFlowActivityTotal actTotal = items.get(position);
            holder.bind(actTotal);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView netText;
            TextView inflowText;
            TextView outflowText;
            DonutChartView donutChart;
            LinearLayout legendList;
            View chartContainer;
            View divider;

            ViewHolder(View card) {
                super(card);
                title = card.findViewById(R.id.item_cashflow_title);
                netText = card.findViewById(R.id.item_cashflow_net);
                inflowText = card.findViewById(R.id.item_cashflow_inflow);
                outflowText = card.findViewById(R.id.item_cashflow_outflow);
                donutChart = card.findViewById(R.id.item_cashflow_donut_chart);
                legendList = card.findViewById(R.id.item_cashflow_chart_legend);
                chartContainer = card.findViewById(R.id.item_cashflow_chart_container);
                divider = card.findViewById(R.id.item_cashflow_divider);
            }

            void bind(CashFlowActivityTotal actTotal) {
                android.content.Context context = itemView.getContext();
                boolean showPercentageMode = sharedViewModel.getDisplayMode().getValue() == DashboardViewModel.DisplayMode.PERCENTAGE;
                title.setText(getActivityTitle(actTotal.getActivity()));
                long net = actTotal.getNetMinor();
                long inflow = actTotal.getIncomeMinor();
                long outflow = actTotal.getExpenseMinor();

                if (showPercentageMode) {
                    long base = inflow > 0 ? inflow : (outflow > 0 ? outflow : 1);
                    netText.setText(fmtPct(net, base, true));
                    inflowText.setText(getString(R.string.cashflow_inflow_label) + ": " + fmtPct(inflow, base, false));
                    outflowText.setText(getString(R.string.cashflow_outflow_label) + ": " + fmtPct(outflow, base, false));
                } else {
                    netText.setText((net >= 0 ? "+" : "") + format(net));
                    inflowText.setText(getString(R.string.cashflow_inflow_label) + ": " + format(inflow));
                    outflowText.setText(getString(R.string.cashflow_outflow_label) + ": " + format(outflow));
                }
                netText.setTextColor(ContextCompat.getColor(context, net >= 0 ? R.color.finan_income : R.color.finan_expense));

                java.util.List<CategoryTotal> categories = new ArrayList<>();
                categories.addAll(actTotal.getIncomeCategories());
                categories.addAll(actTotal.getExpenseCategories());
                if (categories.isEmpty()) {
                    chartContainer.setVisibility(View.GONE);
                    divider.setVisibility(View.GONE);
                } else {
                    chartContainer.setVisibility(View.VISIBLE);
                    divider.setVisibility(View.VISIBLE);

                    java.util.List<CategoryTotal> inflowCategories = new ArrayList<>();
                    java.util.List<CategoryTotal> outflowCategories = new ArrayList<>();
                    for (CategoryTotal cat : categories) {
                        if (cat.isIncome()) inflowCategories.add(cat);
                        else outflowCategories.add(cat);
                    }

                    long legendBase = inflow > 0 ? inflow : (outflow > 0 ? outflow : 1);
                    if (inflow > 0) {
                        long remaining = inflow - outflow;
                        donutChart.setCenterText("Sisa", String.format(Locale.getDefault(), "%.0f%%", Math.max(0.0, remaining * 100.0 / inflow)));
                    } else if (outflow > 0) {
                        donutChart.setCenterText("Keluar", showPercentageMode ? "100%" : formatCompact(outflow));
                    }

                    java.util.List<DonutChartView.DonutItem> inflowDonutItems = new ArrayList<>();
                    java.util.List<DonutChartView.DonutItem> outflowDonutItems = new ArrayList<>();
                    legendList.removeAllViews();

                    if (!inflowCategories.isEmpty()) {
                        legendList.addView(createLegendHeader(context, "PENDAPATAN"));
                        for (CategoryTotal cat : inflowCategories) {
                            int color = getCategoryColor(context, cat.getCategoryId());
                            inflowDonutItems.add(new DonutChartView.DonutItem(cat.getCategoryName(), cat.getTotalMinor(), color));
                            double percentage = cat.getTotalMinor() * 100.0 / legendBase;
                            legendList.addView(createLegendRow(context, cat.getCategoryName(), cat.getTotalMinor(), color, percentage));
                        }
                    }

                    if (!outflowCategories.isEmpty()) {
                        legendList.addView(createLegendHeader(context, "PENGELUARAN"));
                        for (CategoryTotal cat : outflowCategories) {
                            int color = getCategoryColor(context, cat.getCategoryId());
                            outflowDonutItems.add(new DonutChartView.DonutItem(cat.getCategoryName(), cat.getTotalMinor(), color));
                            double percentage = cat.getTotalMinor() * 100.0 / legendBase;
                            legendList.addView(createLegendRow(context, cat.getCategoryName(), cat.getTotalMinor(), color, percentage));
                        }
                    }
                    donutChart.setData(inflowDonutItems, outflowDonutItems);
                }
            }
        }
    }

    private class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.ViewHolder> {
        private java.util.List<WalletBalance> items = new ArrayList<>();
        private long totalBalance = 0;

        @android.annotation.SuppressLint("NotifyDataSetChanged")
        public void setItems(java.util.List<WalletBalance> newItems, long totalBalance) {
            this.items = newItems;
            this.totalBalance = totalBalance;
            //noinspection NotifyDataSetChanged
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.content.Context context = parent.getContext();
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            View divider = createDivider(context);
            root.addView(divider);

            LinearLayout row = new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, UiComponentStyles.dp(context, 10), 0, UiComponentStyles.dp(context, 10));

            TextView name = new TextView(context);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setMaxLines(1);
            name.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
            name.setTextSize(15f);
            row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView balance = new TextView(context);
            balance.setMaxLines(1);
            balance.setTextSize(15f);
            balance.setTypeface(balance.getTypeface(), Typeface.BOLD);
            row.addView(balance, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            root.addView(row);
            return new ViewHolder(root, divider, name, balance);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WalletBalance wallet = items.get(position);
            boolean showPercentageMode = sharedViewModel.getDisplayMode().getValue() == DashboardViewModel.DisplayMode.PERCENTAGE;
            holder.divider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            holder.name.setText(wallet.getWalletName());
            if (showPercentageMode && totalBalance > 0) {
                holder.balance.setText(fmtPct(wallet.getBalanceMinor(), totalBalance, false));
            } else {
                holder.balance.setText(format(wallet.getBalanceMinor()));
            }
            holder.balance.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                wallet.getBalanceMinor() < 0 ? R.color.finan_expense : R.color.finan_primary));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View divider;
            TextView name;
            TextView balance;
            ViewHolder(View itemView, View divider, TextView name, TextView balance) {
                super(itemView);
                this.divider = divider;
                this.name = name;
                this.balance = balance;
            }
        }
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
