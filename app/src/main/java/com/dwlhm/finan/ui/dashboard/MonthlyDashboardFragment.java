package com.dwlhm.finan.ui.dashboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
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
import com.dwlhm.finan.service.balance.MonthlyBalanceCalculator;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.EntityLookup;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ScreenNavigator;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollController;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollHandle;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollItemDecorations;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollRecyclerDataSink;
import com.dwlhm.finan.ui.summary.FinancialAdvisor;
import com.dwlhm.finan.ui.transaction.TransactionDetailDialog;
import com.dwlhm.finan.ui.transaction.TransactionRecyclerAdapter;
import com.dwlhm.finan.util.date.DateRange;
import com.dwlhm.finan.util.date.PayrollCycleResolver;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.google.android.material.appbar.AppBarLayout;
import com.dwlhm.finan.domain.model.CashFlowReportResult;
import com.dwlhm.finan.domain.model.ForwardCashFlowSummary;
import com.dwlhm.finan.domain.model.MonthlySummary;
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
    private View monthPickerContainer;
    private TextView monthTitle;
    private TextView periodRangeLabel;
    private View periodPreviousButton;
    private View periodNextButton;
    private View collapsedToolbar;
    private TextView collapsedBalanceText;
    private ImageView collapsedAdviceBtn;
    private RecyclerView recyclerView;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private LinearLayoutManager layoutManager;

    // Hero Statement UI Elements
    private TextView heroNetBalance;
    private ImageView heroAdviceBtn;
    private TextView heroIncome;
    private TextView heroExpense;
    private View heroRatioBar;
    private View heroRatioIncome;
    private View heroRatioExpense;
    private View heroRunwayPill;
    private TextView heroRunwayLabel;
    private TextView heroRunwayAmount;
    private View heroAnalyticsBtn;

    private TextView monthlyTransactionsTitle;
    private TransactionRecyclerAdapter transactionAdapter;
    private MonthlySummaryHeaderAdapter summaryHeaderAdapter;
    private View summaryHeaderView;
    private ConcatAdapter unifiedAdapter;
    private InfiniteScrollHandle scrollHandle;
    
    // Summary UI Elements
    private LinearLayout summaryEmptyState;
    
    private HistoryTotals cachedTotals;
    private CashFlowReportResult cachedReport;
    private MonthlySummary cachedSummary;
    private MonthlySummary cachedPrevSummary;
    private MonthlySummary cachedPrevPrevSummary;
    private ForwardCashFlowSummary cachedForwardSummary;
    private MonthlyBalanceCalculator.Result cachedBalance;
    
    private HistoryQuery activeQuery = new HistoryQuery(null, null, null, null, null, false, HistorySearch.empty());
    
    private int reloadGeneration;
    private int lastLoadedVersion = -1;
    private int lastLoadedFiltersHash = 0;
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
        Integer version = sharedViewModel.getDataVersion().getValue();
        int currentVersion = version != null ? version : 0;
        int filtersHash = currentFiltersHash();
        if (currentVersion != lastLoadedVersion
                || filtersHash != lastLoadedFiltersHash
                || transactionAdapter == null
                || transactionAdapter.getItemCount() == 0) {
            loadData();
        }
    }

    private int currentFiltersHash() {
        return java.util.Objects.hash(
                sharedViewModel.getSearchQuery().getValue(),
                sharedViewModel.getWalletFilter().getValue(),
                sharedViewModel.getCategoryFilter().getValue(),
                sharedViewModel.getTransactionTypeFilter().getValue());
    }

    @Override
    protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Must use parent fragment to share ViewModel
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        
        appBarLayout = view.findViewById(R.id.app_bar_layout);
        monthPickerContainer = view.findViewById(R.id.month_picker_container);
        monthTitle = view.findViewById(R.id.month_title);
        periodRangeLabel = view.findViewById(R.id.monthly_period_range);
        periodPreviousButton = view.findViewById(R.id.monthly_period_previous);
        periodNextButton = view.findViewById(R.id.monthly_period_next);
        collapsedToolbar = view.findViewById(R.id.collapsed_toolbar);
        collapsedBalanceText = view.findViewById(R.id.collapsed_balance_text);
        collapsedAdviceBtn = view.findViewById(R.id.collapsed_advice_btn);

        // Bind Hero statement elements
        heroNetBalance = view.findViewById(R.id.monthly_hero_net_balance);
        heroAdviceBtn = view.findViewById(R.id.monthly_hero_advice_btn);
        heroIncome = view.findViewById(R.id.monthly_hero_income);
        heroExpense = view.findViewById(R.id.monthly_hero_expense);
        heroRatioBar = view.findViewById(R.id.monthly_hero_ratio_bar);
        heroRatioIncome = view.findViewById(R.id.monthly_hero_ratio_income);
        heroRatioExpense = view.findViewById(R.id.monthly_hero_ratio_expense);
        heroRunwayPill = view.findViewById(R.id.monthly_hero_runway_pill);
        heroRunwayLabel = view.findViewById(R.id.monthly_hero_runway_label);
        heroRunwayAmount = view.findViewById(R.id.monthly_hero_runway_amount);

        if (heroRunwayPill != null) {
            heroRunwayPill.setOnClickListener(v -> {
                if (cachedForwardSummary != null) {
                    UpcomingDetailBottomSheet.show(
                            requireContext(),
                            services,
                            cachedForwardSummary,
                            sharedViewModel.getDisplayMode().getValue(),
                            this::loadData
                    );
                }
            });
        }

        heroAnalyticsBtn = view.findViewById(R.id.monthly_hero_analytics_btn);
        if (heroAnalyticsBtn != null) {
            heroAnalyticsBtn.setOnClickListener(v -> openAnalyticsBottomSheet());
        }

        recyclerView = view.findViewById(R.id.monthly_recycler_view);
        swipeRefresh = view.findViewById(R.id.monthly_swipe_refresh);
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                lastLoadedVersion = -1;
                loadData();
            });
        }
        layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        summaryHeaderView = getLayoutInflater().inflate(
                R.layout.item_monthly_summary, recyclerView, false);
        summaryEmptyState = summaryHeaderView.findViewById(R.id.monthly_summary_empty);
        monthlyTransactionsTitle = summaryHeaderView.findViewById(R.id.monthly_transactions_title);
        View sectionRefreshBtn = summaryHeaderView.findViewById(R.id.monthly_section_refresh_btn);
        sectionRefreshBtn.setOnClickListener(v -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
            lastLoadedVersion = -1;
            loadData();
        });
        
        transactionAdapter = new TransactionRecyclerAdapter(requireContext());
        transactionAdapter.setOnTransactionClickListener((t, p) -> openTransactionDetail(p));
        summaryHeaderAdapter = new MonthlySummaryHeaderAdapter(summaryHeaderView);
        unifiedAdapter = new ConcatAdapter(summaryHeaderAdapter, transactionAdapter);
        recyclerView.setAdapter(unifiedAdapter);

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
                        scrollHandle.onLastVisiblePositionChanged(getLastTransactionPosition());
                    }
                });
                
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && scrollHandle != null) {
                    scrollHandle.onLastVisiblePositionChanged(getLastTransactionPosition());
                }
            }
        });
        
        transactionAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (scrollHandle != null) scrollHandle.onLastVisiblePositionChanged(getLastTransactionPosition());
            }
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (scrollHandle != null) scrollHandle.onLastVisiblePositionChanged(getLastTransactionPosition());
            }
        });

        bindPeriodHeader();

        setupScrollListener();
        observeFilters();

        loadData();
    }

    private void openAnalyticsBottomSheet() {
        if (cachedReport != null && cachedTotals != null) {
            CashFlowAnalyticsBottomSheet.show(
                    requireContext(),
                    services,
                    cachedReport,
                    cachedTotals,
                    sharedViewModel.getDisplayMode().getValue(),
                    year,
                    month,
                    activeQuery.walletId());
        }
    }

    private void bindPeriodHeader() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"));

        if (month == -1) {
            monthTitle.setText(String.valueOf(year));
        } else {
            monthTitle.setText(LocalDate.of(year, month, 1).format(formatter));
        }

        View.OnClickListener pickerClickListener = v -> {
            DashboardViewModel.TimeRangeMode currentMode = (month == -1)
                    ? DashboardViewModel.TimeRangeMode.YEARLY
                    : DashboardViewModel.TimeRangeMode.MONTHLY;

            MonthYearPickerBottomSheetDialog dialog = new MonthYearPickerBottomSheetDialog(
                    requireContext(), currentMode, year, month == -1 ? 1 : month);

            dialog.setOnTimeRangeSelectedListener((mode, selectedYear, selectedMonth) ->
                    sharedViewModel.setTimeRangeState(new DashboardViewModel.TimeRangeState(
                            mode, selectedYear, selectedMonth)));
            dialog.show();
        };

        if (monthPickerContainer != null) {
            monthPickerContainer.setOnClickListener(pickerClickListener);
        }
        if (monthTitle != null) {
            monthTitle.setOnClickListener(pickerClickListener);
        }

        if (periodPreviousButton != null) {
            periodPreviousButton.setOnClickListener(v -> nudgePeriod(-1));
        }
        if (periodNextButton != null) {
            periodNextButton.setOnClickListener(v -> nudgePeriod(1));
        }
        updatePeriodNavigationUi();
    }

    private void nudgePeriod(int direction) {
        if (direction == 0 || sharedViewModel == null) return;

        DashboardViewModel.TimeRangeState currentState = sharedViewModel.getTimeRangeState().getValue();
        DashboardViewModel.TimeRangeMode mode = currentState != null
                ? currentState.mode
                : (month == -1 ? DashboardViewModel.TimeRangeMode.YEARLY
                : DashboardViewModel.TimeRangeMode.MONTHLY);
        int currentYear = currentState != null ? currentState.year : year;
        int currentMonth = currentState != null ? currentState.month : month;
        int cutoff = requireContext().getSharedPreferences(
                "finan_prefs", android.content.Context.MODE_PRIVATE).getInt("cutoff_day", 1);
        LocalDate latestMonth = PayrollCycleResolver.baseMonthForToday(LocalDate.now(), cutoff);

        if (mode == DashboardViewModel.TimeRangeMode.YEARLY) {
            int targetYear = currentYear + direction;
            if (targetYear > latestMonth.getYear()) return;
            sharedViewModel.setTimeRangeState(new DashboardViewModel.TimeRangeState(
                    mode, targetYear, -1));
            return;
        }

        if (currentMonth < 1 || currentMonth > 12) return;
        LocalDate targetMonth = LocalDate.of(currentYear, currentMonth, 1).plusMonths(direction);
        if (targetMonth.isAfter(latestMonth)) return;
        sharedViewModel.setTimeRangeState(new DashboardViewModel.TimeRangeState(
                mode, targetMonth.getYear(), targetMonth.getMonthValue()));
    }

    private void updatePeriodNavigationUi() {
        if (periodRangeLabel == null || periodPreviousButton == null || periodNextButton == null) return;

        DateTimeFormatter rangeFormatter = DateTimeFormatter.ofPattern(
                "d MMM yyyy", Locale.forLanguageTag("id-ID"));
        periodRangeLabel.setText(getString(
                R.string.monthly_period_range_format,
                startDate.format(rangeFormatter),
                endDate.format(rangeFormatter)));

        int cutoff = requireContext().getSharedPreferences(
                "finan_prefs", android.content.Context.MODE_PRIVATE).getInt("cutoff_day", 1);
        LocalDate latestMonth = PayrollCycleResolver.baseMonthForToday(LocalDate.now(), cutoff);
        boolean isCurrentOrFuture = month == -1
                ? year >= latestMonth.getYear()
                : !LocalDate.of(year, month, 1).isBefore(latestMonth);
        periodPreviousButton.setEnabled(true);
        periodNextButton.setEnabled(!isCurrentOrFuture);
        periodNextButton.setAlpha(isCurrentOrFuture ? 0.45f : 1f);
    }

    private boolean hasActiveFilters() {
        String query = sharedViewModel.getSearchQuery().getValue();
        return (query != null && !query.trim().isEmpty())
                || sharedViewModel.getWalletFilter().getValue() != null
                || sharedViewModel.getCategoryFilter().getValue() != null
                || sharedViewModel.getTransactionTypeFilter().getValue() != null;
    }

    private void setSummaryEmptyState(boolean empty) {
        if (summaryEmptyState == null || summaryHeaderView == null) return;
        TextView title = summaryEmptyState.findViewById(R.id.monthly_summary_empty_title);
        TextView hint = summaryEmptyState.findViewById(R.id.monthly_summary_empty_hint);
        View action = summaryEmptyState.findViewById(R.id.monthly_summary_empty_action);
        boolean filtered = hasActiveFilters();

        if (!empty) {
            summaryEmptyState.setVisibility(View.GONE);
            if (monthlyTransactionsTitle != null) monthlyTransactionsTitle.setVisibility(View.VISIBLE);
            return;
        }

        if (title != null) {
            title.setText(filtered
                    ? R.string.monthly_summary_filtered_title
                    : R.string.monthly_summary_empty_title);
        }
        if (hint != null) {
            hint.setText(filtered
                    ? R.string.monthly_summary_filtered_hint
                    : R.string.monthly_summary_empty_hint);
        }
        if (action != null) {
            action.setVisibility(filtered ? View.GONE : View.VISIBLE);
            action.setOnClickListener(v -> openCapture());
        }

        summaryEmptyState.setVisibility(View.VISIBLE);
        if (monthlyTransactionsTitle != null) monthlyTransactionsTitle.setVisibility(View.GONE);
    }

    private void openCapture() {
        if (getActivity() instanceof ScreenNavigator) {
            ((ScreenNavigator) getActivity()).openCapture();
        }
    }

    private void setupScrollListener() {
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (swipeRefresh != null) swipeRefresh.setEnabled(verticalOffset == 0);
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

    private int getLastTransactionPosition() {
        if (layoutManager == null || summaryHeaderAdapter == null) {
            return RecyclerView.NO_POSITION;
        }
        int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
        if (lastVisiblePosition == RecyclerView.NO_POSITION) {
            return RecyclerView.NO_POSITION;
        }
        return lastVisiblePosition - summaryHeaderAdapter.getItemCount();
    }

    private void observeFilters() {
        sharedViewModel.getSearchQuery().observe(getViewLifecycleOwner(), q -> { invalidateSummaryCache(); loadData(); });
        sharedViewModel.getWalletFilter().observe(getViewLifecycleOwner(), w -> { invalidateSummaryCache(); loadData(); });
        sharedViewModel.getCategoryFilter().observe(getViewLifecycleOwner(), c -> { invalidateSummaryCache(); loadData(); });
        sharedViewModel.getTransactionTypeFilter().observe(getViewLifecycleOwner(), t -> { invalidateSummaryCache(); loadData(); });
        sharedViewModel.getDisplayMode().observe(getViewLifecycleOwner(), mode -> updateDisplayModeUi());
        sharedViewModel.getDataVersion().observe(getViewLifecycleOwner(), version -> {
            if (isResumed()) loadData();
        });
    }

    private void invalidateSummaryCache() {
        cachedReport = null;
        cachedSummary = null;
        cachedPrevSummary = null;
        cachedPrevPrevSummary = null;
        cachedForwardSummary = null;
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
                
                CashFlowReportResult reportResult = services.cashFlowReportService.buildReport(startDate, endDate, cutoffDay, walletId);
                MonthlySummary summary = services.summaryService.loadRange(startDate, endDate, walletId, categoryId);
                DateRange prevRange = PayrollCycleResolver.shiftMonths(startDate, cutoffDay, -1);
                DateRange prevPrevRange = PayrollCycleResolver.shiftMonths(startDate, cutoffDay, -2);
                MonthlySummary prevSummary = services.summaryService.loadRange(prevRange.getStart(), prevRange.getEnd(), walletId, categoryId);
                MonthlySummary prevPrevSummary = services.summaryService.loadRange(prevPrevRange.getStart(), prevPrevRange.getEnd(), walletId, categoryId);
                MonthlyBalanceCalculator.Result balance = services.monthlyBalanceCalculator.from(summary, prevSummary);
                ForwardCashFlowSummary forwardSummary = services.upcomingCashFlowService.calculateForwardSummary(LocalDate.now(), LocalDate.now().plusDays(29), walletId);

                return new Object[]{wMap, cMap, query, totals, reportResult, summary, prevSummary, prevPrevSummary, forwardSummary, balance};
            },
            data -> {
                if (!isAdded() || generation != reloadGeneration) return;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (data == null) return;

                Integer version = sharedViewModel.getDataVersion().getValue();
                lastLoadedVersion = version != null ? version : 0;
                lastLoadedFiltersHash = currentFiltersHash();
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
                if (data[8] != null) cachedForwardSummary = (ForwardCashFlowSummary) data[8];
                cachedBalance = (MonthlyBalanceCalculator.Result) data[9];
                
                transactionAdapter.setEntityLookups(categoriesById, walletsById);
                updateDisplayModeUi();
                if (scrollHandle != null) scrollHandle.reload();
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
            long netBalance = cachedBalance != null
                    ? cachedBalance.getNetMinor()
                    : cachedTotals.getIncomeMinor() - cachedTotals.getExpenseMinor();
            collapsedBalanceText.setText(MoneyFormatter.format(netBalance));
        }

        long incomeMinor = cachedTotals.getIncomeMinor();
        long expenseMinor = cachedTotals.getExpenseMinor();
        long netBalance = cachedBalance != null ? cachedBalance.getNetMinor() : incomeMinor - expenseMinor;

        // Update Hero Net Balance
        if (heroNetBalance != null) {
            if (mode == DashboardViewModel.DisplayMode.MASKED) {
                heroNetBalance.setText(R.string.java_MonthlyDashboardFragment_rp);
                heroNetBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.finan_summary_on_hero));
            } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                heroNetBalance.setText(R.string.java_MonthlyDashboardFragment_100);
                heroNetBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.finan_summary_on_hero));
            } else {
                heroNetBalance.setText(MoneyFormatter.format(netBalance));
                if (netBalance >= 0) {
                    heroNetBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.finan_summary_net_income));
                } else {
                    heroNetBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.finan_summary_net_expense));
                }
            }
        }

        // Update Hero Income
        if (heroIncome != null) {
            if (mode == DashboardViewModel.DisplayMode.MASKED) {
                heroIncome.setText(R.string.java_MonthlyDashboardFragment_rp);
            } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                heroIncome.setText(R.string.java_MonthlyDashboardFragment_100);
            } else {
                heroIncome.setText(MoneyFormatter.format(incomeMinor));
            }
        }

        // Update Hero Expense
        if (heroExpense != null) {
            if (mode == DashboardViewModel.DisplayMode.MASKED) {
                heroExpense.setText(R.string.java_MonthlyDashboardFragment_rp);
            } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                if (incomeMinor > 0) {
                    float ratio = (expenseMinor * 100f) / incomeMinor;
                    heroExpense.setText(String.format(java.util.Locale.getDefault(), "%.0f%%", ratio));
                } else {
                    heroExpense.setText("-");
                }
            } else {
                heroExpense.setText(MoneyFormatter.format(expenseMinor));
            }
        }

        // Update Horizontal Proportion Ratio Bar
        long totalFlow = incomeMinor + expenseMinor;
        if (heroRatioIncome != null && heroRatioExpense != null) {
            if (totalFlow <= 0) {
                heroRatioIncome.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 50f));
                heroRatioExpense.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 50f));
            } else {
                float incRatio = (float) incomeMinor / (float) totalFlow * 100f;
                float expRatio = (float) expenseMinor / (float) totalFlow * 100f;
                if (incomeMinor > 0 && incRatio < 2f) incRatio = 2f;
                if (expenseMinor > 0 && expRatio < 2f) expRatio = 2f;
                float sum = incRatio + expRatio;
                heroRatioIncome.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (incRatio / sum) * 100f));
                heroRatioExpense.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (expRatio / sum) * 100f));
            }
        }

        // Update Hero Runway Subtle Pill
        if (heroRunwayPill != null) {
            if (cachedForwardSummary == null) {
                heroRunwayPill.setVisibility(View.GONE);
            } else {
                heroRunwayPill.setVisibility(View.VISIBLE);
                if (heroRunwayAmount != null) {
                    if (mode == DashboardViewModel.DisplayMode.MASKED) {
                        heroRunwayAmount.setText("••••••");
                        heroRunwayAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.finan_summary_on_hero));
                    } else {
                        long remaining = cachedForwardSummary.getRemainingAfterPlansMinor();
                        heroRunwayAmount.setText(MoneyFormatter.format(remaining));
                        if (remaining < 0) {
                            heroRunwayAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.finan_summary_net_expense));
                        } else {
                            heroRunwayAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.finan_summary_on_hero));
                        }
                    }
                }
            }
        }

        FinancialAdvisor.Advice advice = FinancialAdvisor.getAdvice(requireContext(), cachedSummary, cachedPrevSummary, cachedPrevPrevSummary, startDate, endDate);

        if (advice != null && cachedSummary != null) {
            if (heroAdviceBtn != null) {
                heroAdviceBtn.setVisibility(View.VISIBLE);
                heroAdviceBtn.setOnClickListener(v ->
                        com.dwlhm.finan.ui.summary.FinancialAdviceDialog.show(requireContext(), advice, cachedSummary, sharedViewModel.getDisplayMode().getValue()));
            }
            if (collapsedAdviceBtn != null) {
                collapsedAdviceBtn.setVisibility(View.VISIBLE);
                collapsedAdviceBtn.setOnClickListener(v ->
                        com.dwlhm.finan.ui.summary.FinancialAdviceDialog.show(requireContext(), advice, cachedSummary, sharedViewModel.getDisplayMode().getValue()));
            }
        } else {
            if (heroAdviceBtn != null) heroAdviceBtn.setVisibility(View.GONE);
            if (collapsedAdviceBtn != null) collapsedAdviceBtn.setVisibility(View.GONE);
        }

        if (heroAnalyticsBtn != null) {
            boolean hasTransactions = cachedTotals.getCount() > 0 && cachedReport != null;
            heroAnalyticsBtn.setVisibility(hasTransactions ? View.VISIBLE : View.GONE);
        }

        bindSummaryHeader();
    }

    private void bindSummaryHeader() {
        if (summaryHeaderView == null || cachedTotals == null) return;

        boolean empty = cachedTotals.getCount() == 0;
        setSummaryEmptyState(empty);
    }
    
    private void openTransactionDetail(int position) {
        BottomSheetHelper.show(new TransactionDetailDialog(
                requireContext(),
                services,
                transactionAdapter.getTransactionAt(position),
                this::loadData));
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
