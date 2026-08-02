package com.dwlhm.finan.ui.summary;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.domain.model.CashFlowActivityTotal;
import com.dwlhm.finan.domain.model.CashFlowReport;
import com.dwlhm.finan.domain.model.CashFlowReportResult;
import com.dwlhm.finan.domain.model.CategoryTotal;
import com.dwlhm.finan.domain.model.HistoryQuery;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.domain.model.WalletBalance;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.EntityLookup;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ScreenNavigator;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.UiComponentStyles;
import com.dwlhm.finan.util.money.MoneyFormatter;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.ViewFlipper;
import android.transition.TransitionManager;
import com.dwlhm.finan.ui.components.DonutChartView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressLint("SetTextI18n")
public final class SummaryFragment extends ScreenFragment {

  private static final String START_DATE_STATE_KEY = "summary_start_date";
  private static final String END_DATE_STATE_KEY = "summary_end_date";
  private static final String WALLET_FILTER_STATE_KEY = "summary_wallet_filter";
  private static final String CATEGORY_FILTER_STATE_KEY = "summary_category_filter";
  private static final long FILTER_NONE_ID = -1L;
  private static final int CATEGORY_PROGRESS_MAX = 1000;

  private AppServices services;
  private int loadGeneration;
  private Map<Long, Wallet> walletsById = Map.of();
  private Map<Long, Category> categoriesById = Map.of();
  private LocalDate selectedStartDate = LocalDate.now();
  private LocalDate selectedEndDate = LocalDate.now();
  private Long selectedWalletId;
  private Long selectedCategoryId;

  private TextView periodLabel;
  private ImageButton filterButton;
  private TextView netFlow;
  private TextView monthExpense;
  private TextView monthIncome;
  private LinearLayout cashFlowContainer;
  private LinearLayout walletList;
  private TextView walletTotal;
  private TextView emptyMessage;
  private View adviceCard;
  private ImageView adviceIcon;
  private TextView adviceTitle;
  private TextView adviceMessage;
  private TextView modeNominal;
  private TextView modePersentase;
  private boolean showPercentageMode;
  private ViewGroup contentContainer;
  private SummaryLoadData cachedData;
  private boolean isDataLoaded = false;
  private androidx.recyclerview.widget.RecyclerView cashFlowRecycler;
  private CashFlowAdapter cashFlowAdapter;
  private androidx.recyclerview.widget.RecyclerView walletRecycler;
  private WalletAdapter walletAdapter;

  private View inboxPrompt;
  private TextView inboxTitle;
  private TextView inboxMessage;
  private LinearLayout reconciliationCard;
  private LinearLayout reconciliationLines;
  private LinearLayout weeklyChart;
  private LinearLayout chartLegend;
  private LinearLayout chartWeeksContainer;
  private TextView chartToggleIncome;
  private TextView chartToggleExpense;
  private boolean showWeeklyChartIncome;
  private TextView headlineView;

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_summary;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    services = ServicesProvider.get(requireContext());
  }

  @Override
  public void onDestroyView() {
    loadGeneration++;
    super.onDestroyView();
  }

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    DateRange restoredRange = restoreSelectedRange(savedInstanceState);
    selectedStartDate = restoredRange.start;
    selectedEndDate = restoredRange.end;
    selectedWalletId = restoreFilterId(savedInstanceState, WALLET_FILTER_STATE_KEY);
    selectedCategoryId = restoreFilterId(savedInstanceState, CATEGORY_FILTER_STATE_KEY);
    periodLabel = view.findViewById(R.id.summary_period_label);
    filterButton = view.findViewById(R.id.summary_filter_button);
    netFlow = view.findViewById(R.id.summary_net_flow);
    monthExpense = view.findViewById(R.id.summary_month_expense);
    monthIncome = view.findViewById(R.id.summary_month_income);
    cashFlowContainer = view.findViewById(R.id.summary_cash_flow_container);
    walletList = view.findViewById(R.id.summary_wallet_list);

    cashFlowRecycler = new androidx.recyclerview.widget.RecyclerView(requireContext());
    cashFlowRecycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
    cashFlowRecycler.setNestedScrollingEnabled(false);
    cashFlowContainer.addView(cashFlowRecycler, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    cashFlowAdapter = new CashFlowAdapter();
    cashFlowRecycler.setAdapter(cashFlowAdapter);

    walletRecycler = new androidx.recyclerview.widget.RecyclerView(requireContext());
    walletRecycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
    walletRecycler.setNestedScrollingEnabled(false);
    walletList.addView(walletRecycler, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    walletAdapter = new WalletAdapter();
    walletRecycler.setAdapter(walletAdapter);

    walletTotal = view.findViewById(R.id.summary_wallet_total);
    emptyMessage = view.findViewById(R.id.summary_empty);
    adviceCard = view.findViewById(R.id.summary_advice_card);
    adviceIcon = view.findViewById(R.id.summary_advice_icon);
    adviceTitle = view.findViewById(R.id.summary_advice_title);
    adviceMessage = view.findViewById(R.id.summary_advice_message);
    periodLabel.setOnClickListener(v -> showDateRangePicker());
    filterButton.setOnClickListener(v -> showSummaryFilterDialog());

    inboxPrompt = view.findViewById(R.id.summary_inbox_prompt);
    inboxTitle = view.findViewById(R.id.summary_inbox_title);
    inboxMessage = view.findViewById(R.id.summary_inbox_message);
    reconciliationCard = view.findViewById(R.id.summary_reconciliation_card);
    reconciliationLines = view.findViewById(R.id.summary_reconciliation_lines);
    weeklyChart = view.findViewById(R.id.summary_weekly_chart);
    chartLegend = view.findViewById(R.id.summary_chart_legend);
    chartWeeksContainer = view.findViewById(R.id.summary_chart_weeks);
    chartToggleIncome = view.findViewById(R.id.summary_chart_toggle_income);
    chartToggleExpense = view.findViewById(R.id.summary_chart_toggle_expense);
    chartToggleIncome.setOnClickListener(v -> setWeeklyChartMode(true));
    chartToggleExpense.setOnClickListener(v -> setWeeklyChartMode(false));
    updateChartToggle();
    populateChartLegend();
    headlineView = view.findViewById(R.id.summary_net_flow_label);

    inboxPrompt.setOnClickListener(v -> {
      if (requireActivity() instanceof ScreenNavigator) {
        ((ScreenNavigator) requireActivity()).openCategoriesFiltered("UNCLASSIFIED");
      }
    });

    View thisMonthChip = view.findViewById(R.id.summary_chip_this_month);
    View lastMonthChip = view.findViewById(R.id.summary_chip_last_month);

    thisMonthChip.setOnClickListener(v -> {
        DateRange range = cycleRange(0);
        selectedStartDate = range.start;
        selectedEndDate = range.end;
        loadSummaryAsync();
    });

    lastMonthChip.setOnClickListener(v -> {
        DateRange range = cycleRange(-1);
        selectedStartDate = range.start;
        selectedEndDate = range.end;
        loadSummaryAsync();
    });

    contentContainer = view.findViewById(R.id.summary_content);
    modeNominal = view.findViewById(R.id.summary_mode_nominal);
    modePersentase = view.findViewById(R.id.summary_mode_persentase);
    showPercentageMode = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
        .getBoolean("summary_percentage_mode", false);
    modeNominal.setOnClickListener(v -> setPercentageMode(false));
    modePersentase.setOnClickListener(v -> setPercentageMode(true));
    updateModeToggle();
    updateFilterButton();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!isDataLoaded) {
      loadSummaryAsync();
      isDataLoaded = true;
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putString(START_DATE_STATE_KEY, selectedStartDate.toString());
    outState.putString(END_DATE_STATE_KEY, selectedEndDate.toString());
    outState.putLong(
        WALLET_FILTER_STATE_KEY, selectedWalletId == null ? FILTER_NONE_ID : selectedWalletId);
    outState.putLong(
        CATEGORY_FILTER_STATE_KEY, selectedCategoryId == null ? FILTER_NONE_ID : selectedCategoryId);
    super.onSaveInstanceState(outState);
  }

  private void loadSummaryAsync() {
    int generation = ++loadGeneration;
    LocalDate requestedStartDate = selectedStartDate;
    LocalDate requestedEndDate = selectedEndDate;
    Long requestedWalletId = selectedWalletId;
    Long requestedCategoryId = selectedCategoryId;
    services.dbWorker.compute(
        () -> {
          List<Wallet> wallets = services.walletService.findAll();
          List<Category> categories = services.categoryDao.findAllOrdered();
          Map<Long, Wallet> walletMap = EntityLookup.indexWallets(wallets);
          Map<Long, Category> categoryMap = EntityLookup.indexCategories(categories);
          Long walletId = validWalletIdOrNull(requestedWalletId, walletMap);
          Long categoryId = validCategoryIdOrNull(requestedCategoryId, categoryMap);
          CashFlowReportResult reportResult =
              services.cashFlowReportService.buildReport(
                  requestedStartDate, requestedEndDate, walletId);
          MonthlySummary summary =
              services.summaryService.loadRange(
                  requestedStartDate, requestedEndDate, walletId, categoryId);
          MonthlySummary prevSummary =
              services.summaryService.loadRange(
                  requestedStartDate.minusMonths(1), requestedEndDate.minusMonths(1), walletId, categoryId);
          MonthlySummary prevPrevSummary =
              services.summaryService.loadRange(
                  requestedStartDate.minusMonths(2), requestedEndDate.minusMonths(2), walletId, categoryId);
          long unclassifiedCount = 0L;
          long unclassifiedAmount = 0L;
          List<Category> allCats = categoryMap.isEmpty() ? services.categoryDao.findAllOrdered() : new ArrayList<>(categoryMap.values());
          for (Category cat : allCats) {
            if (CashFlowActivity.UNCLASSIFIED.name().equals(cat.getCashFlowActivity())) {
              unclassifiedCount++;
            }
          }
          for (CashFlowReport report : reportResult.getAllReports()) {
            for (CashFlowActivityTotal act : report.getActivityTotals()) {
              if (act.getActivity() == CashFlowActivity.UNCLASSIFIED) {
                unclassifiedAmount += act.getIncomeMinor() + act.getExpenseMinor();
              }
            }
          }
          return new SummaryLoadData(walletMap, categoryMap, walletId, categoryId,
              reportResult, summary, prevSummary, prevPrevSummary,
              unclassifiedCount, unclassifiedAmount);
        },
        data -> {
          if (!isAdded()
              || getView() == null
              || generation != loadGeneration
              || data == null
              || !requestedStartDate.equals(selectedStartDate)
              || !requestedEndDate.equals(selectedEndDate)) {
            return;
          }
          walletsById = data.walletsById;
          categoriesById = data.categoriesById;
          selectedWalletId = data.walletId;
          selectedCategoryId = data.categoryId;
          cachedData = data;
          bindReport(data.reportResult, data.summary, data.prevSummary, data.prevPrevSummary,
              requestedStartDate, requestedEndDate, data.unclassifiedCount, data.unclassifiedAmount);
          updateFilterButton();
        });
  }

  private void bindReport(
      CashFlowReportResult reportResult,
      MonthlySummary summary,
      MonthlySummary prevSummary,
      MonthlySummary prevPrevSummary,
      LocalDate startDate,
      LocalDate endDate,
      long unclassifiedCount,
      long unclassifiedAmount) {
    periodLabel.setText(formatRangeLabel(startDate, endDate));

    CashFlowReport primaryReport = null;
    for (CashFlowReport r : reportResult.getAllReports()) {
      primaryReport = r;
      break;
    }

    long totalIncome = summary != null ? summary.getMonthIncomeMinor() : 0L;
    long totalExpense = summary != null ? summary.getMonthExpenseMinor() : 0L;
    long net = totalIncome - totalExpense;

    if (primaryReport != null) {
      headlineView.setText(primaryReport.getHeadlineLabel());
    }

    if (showPercentageMode) {
      long base = totalIncome > 0 ? totalIncome : (totalExpense > 0 ? totalExpense : 1);
      netFlow.setText(fmtPct(net, base, true));
      monthIncome.setText(fmtPct(totalIncome, base, false));
      monthExpense.setText(fmtPct(totalExpense, base, false));
    } else {
      netFlow.setText((net >= 0 ? "+" : "") + format(net));
      monthExpense.setText(format(totalExpense));
      monthIncome.setText(format(totalIncome));
    }
    netFlow.setTextColor(ContextCompat.getColor(requireContext(), net >= 0 ? R.color.finan_summary_net_income : R.color.finan_summary_net_expense));

    netFlow.setOnClickListener(v -> {
      if (requireActivity() instanceof ScreenNavigator) {
        long startMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = endDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        ((ScreenNavigator) requireActivity()).openHistoryWithFilter(null, startMillis, endMillis, selectedWalletId, selectedCategoryId);
      }
    });

    monthIncome.setOnClickListener(v -> {
      if (requireActivity() instanceof ScreenNavigator) {
        long startMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = endDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        ((ScreenNavigator) requireActivity()).openHistoryWithFilter("INCOME", startMillis, endMillis, selectedWalletId, selectedCategoryId);
      }
    });

    monthExpense.setOnClickListener(v -> {
      if (requireActivity() instanceof ScreenNavigator) {
        long startMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = endDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        ((ScreenNavigator) requireActivity()).openHistoryWithFilter("EXPENSE", startMillis, endMillis, selectedWalletId, selectedCategoryId);
      }
    });

    FinancialAdvisor.Advice advice = FinancialAdvisor.getAdvice(requireContext(), summary, prevSummary, prevPrevSummary, startDate, endDate);
    if (advice != null) {
      adviceCard.setVisibility(View.VISIBLE);
      adviceCard.setBackgroundResource(advice.bgRes);
      adviceIcon.setImageResource(advice.iconRes);
      adviceIcon.setColorFilter(ContextCompat.getColor(requireContext(), advice.colorRes));
      adviceTitle.setText(advice.title);
      adviceMessage.setText(android.text.Html.fromHtml(advice.message, android.text.Html.FROM_HTML_MODE_LEGACY));
    } else {
      adviceCard.setVisibility(View.GONE);
    }

    bindInboxPrompt(unclassifiedCount, unclassifiedAmount);

    boolean hasActivity = false;
    List<CashFlowActivityTotal> activityTotals = new ArrayList<>();
    for (CashFlowReport report : reportResult.getAllReports()) {
      for (CashFlowActivityTotal act : report.getActivityTotals()) {
        activityTotals.add(act);
        hasActivity = true;
      }
    }
    cashFlowAdapter.setItems(activityTotals);
    emptyMessage.setVisibility(hasActivity ? View.GONE : View.VISIBLE);

    bindReconciliation(reportResult);
    bindWeeklyChart(reportResult);

    long totalBalance = 0L;
    if (summary != null) {
      totalBalance = totalWalletBalance(summary);
    }
    walletTotal.setText(showPercentageMode ? "100%" : format(totalBalance));

    if (summary != null) {
      walletAdapter.setItems(summary.getWalletBalances(), totalBalance);
    } else {
      walletAdapter.setItems(new ArrayList<>(), 0);
    }
  }

  private void bindInboxPrompt(long unclassifiedCount, long unclassifiedAmount) {
    if (unclassifiedCount > 0) {
      inboxPrompt.setVisibility(View.VISIBLE);
      inboxTitle.setText(R.string.java_SummaryFragment_rapikan_arus_kas);
      inboxMessage.setText(unclassifiedCount + getString(R.string.java_SummaryFragment_kategori_belum_dikelompokkan) + format(unclassifiedAmount));
    } else {
      inboxPrompt.setVisibility(View.GONE);
    }
  }

  private void bindReconciliation(CashFlowReportResult reportResult) {
    reconciliationCard.setVisibility(View.GONE);
    reconciliationLines.removeAllViews();

    for (CashFlowReport report : reportResult.getAllReports()) {
      if (report.getWalletId() != null && reportResult.getAllReports().size() > 1) {
        continue;
      }
      reconciliationCard.setVisibility(View.VISIBLE);
      long base = report.getIncomeMinor() > 0 ? report.getIncomeMinor() : (report.getExpenseMinor() > 0 ? report.getExpenseMinor() : 1);
      addReconciliationLine("Saldo awal", report.getOpeningBalanceMinor(), false, false, null, base);
      addReconciliationLine("Pemasukan", report.getIncomeMinor(), true, false, v -> {
        if (requireActivity() instanceof ScreenNavigator) {
          long start = report.getStartDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
          long end = report.getEndDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
          ((ScreenNavigator) requireActivity()).openHistoryWithFilter("INCOME", start, end, report.getWalletId(), null);
        }
      }, base);
      addReconciliationLine("Pengeluaran", report.getExpenseMinor(), false, false, v -> {
        if (requireActivity() instanceof ScreenNavigator) {
          long start = report.getStartDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
          long end = report.getEndDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
          ((ScreenNavigator) requireActivity()).openHistoryWithFilter("EXPENSE", start, end, report.getWalletId(), null);
        }
      }, base);
      if (report.getNetTransferMinor() != 0) {
        addReconciliationLine("Transfer bersih", report.getNetTransferMinor(), true, false, null, base);
      }
      if (report.getNetAdjustmentMinor() != 0) {
        addReconciliationLine("Koreksi saldo", report.getNetAdjustmentMinor(), true, true, null, base);
      }

      View divider = new View(requireContext());
      divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.finan_divider));
      divider.setLayoutParams(new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT, UiComponentStyles.dp(requireContext(), 1)));
      reconciliationLines.addView(divider);

      addReconciliationLine("Saldo akhir", report.getClosingBalanceMinor(), false, false, null, base);

      if (!reportResult.isSingleCurrency()) {
        TextView currencyLabel = new TextView(requireContext());
        currencyLabel.setText(report.getCurrencyCode());
        currencyLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.finan_text_secondary));
        currencyLabel.setTextSize(11f);
        currencyLabel.setPadding(0, UiComponentStyles.dp(requireContext(), 4), 0, 0);
        reconciliationLines.addView(currencyLabel);
      }
    }
  }

  private void addReconciliationLine(String label, long amount, boolean signed, boolean isAdjustment, View.OnClickListener clickListener, long base) {
    Context context = requireContext();
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
    row.setPadding(0, UiComponentStyles.dp(context, 6), 0, UiComponentStyles.dp(context, 6));
    if (clickListener != null) {
      row.setClickable(true);
      row.setFocusable(true);
      row.setForeground(ContextCompat.getDrawable(context, UiComponentStyles.selectableItemBackground(context)));
      row.setOnClickListener(clickListener);
    }

    TextView labelView = new TextView(context);
    labelView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    labelView.setText(label);
    labelView.setTextColor(ContextCompat.getColor(context, isAdjustment ? R.color.finan_warm_accent : R.color.finan_text_primary));
    labelView.setTextSize(14f);
    labelView.setTypeface(labelView.getTypeface(), isAdjustment ? android.graphics.Typeface.ITALIC : android.graphics.Typeface.NORMAL);
    row.addView(labelView);

    TextView amountView = new TextView(context);
    if (showPercentageMode) {
      amountView.setText(fmtPct(amount, base, signed));
    } else {
      amountView.setText(signed && amount >= 0 ? ("+" + format(amount)) : format(amount));
    }
    amountView.setPadding(UiComponentStyles.dp(context, 8), 0, 0, 0);
    amountView.setTextColor(ContextCompat.getColor(context, amount >= 0 ? R.color.finan_primary : R.color.finan_expense));
    amountView.setTextSize(14f);
    amountView.setTypeface(amountView.getTypeface(), android.graphics.Typeface.BOLD);
    row.addView(amountView);

    reconciliationLines.addView(row);
  }

  private static final int[] DAY_COLORS = {
    0xFFE74C3C, // Monday    - Senin   (red)
    0xFF2980B9, // Tuesday   - Selasa  (blue)
    0xFF27AE60, // Wednesday - Rabu    (green)
    0xFFF39C12, // Thursday  - Kamis   (orange)
    0xFF8E44AD, // Friday    - Jumat   (purple)
    0xFF16A085, // Saturday  - Sabtu   (teal)
    0xFFE91E63, // Sunday    - Minggu  (pink)
  };

  private static int dayOfWeekIndex(long dateMillis) {
    if (dateMillis <= 0) return 0;
    return java.time.Instant.ofEpochMilli(dateMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        .getDayOfWeek().getValue() - 1;
  }

  private void bindWeeklyChart(CashFlowReportResult reportResult) {
    chartWeeksContainer.removeAllViews();

    int totalDays = 0;
    long maxWeekTotal = 0;
    long totalAmount = 0;
    for (CashFlowReport report : reportResult.getAllReports()) {
      totalDays += report.getWeekSummaries().size() * 7;
      for (CashFlowReport.WeekSummary week : report.getWeekSummaries()) {
        long weekTotal = showWeeklyChartIncome ? week.getWeekIncome() : week.getWeekExpense();
        totalAmount += weekTotal;
        if (weekTotal > maxWeekTotal) maxWeekTotal = weekTotal;
      }
    }

    if (totalDays == 0) {
      weeklyChart.setVisibility(View.GONE);
      return;
    }

    weeklyChart.setVisibility(View.VISIBLE);

    for (CashFlowReport report : reportResult.getAllReports()) {
      for (CashFlowReport.WeekSummary week : report.getWeekSummaries()) {
        long weekTotal = showWeeklyChartIncome ? week.getWeekIncome() : week.getWeekExpense();
        if (weekTotal > 0) {
          chartWeeksContainer.addView(createWeekRow(week, maxWeekTotal, totalAmount));
        }
      }
    }
  }

  private View createWeekRow(CashFlowReport.WeekSummary week, long maxWeekTotal, long totalAmount) {
    Context context = requireContext();
    long weekTotal = showWeeklyChartIncome ? week.getWeekIncome() : week.getWeekExpense();
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, UiComponentStyles.dp(context, 5), 0, UiComponentStyles.dp(context, 5));

    TextView weekLabel = new TextView(context);
    weekLabel.setText(formatShortDate(week.getStartDate()) + " - " + formatShortDate(week.getEndDate()));
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
      if (val > 0) {
        weightSum += (float) val;
        hasData = true;
      }
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

    row.setClickable(true);
    row.setFocusable(true);
    row.setForeground(ContextCompat.getDrawable(context, UiComponentStyles.selectableItemBackground(context)));
    row.setOnClickListener(v -> showWeekDetailDialog(week, totalAmount));

    return row;
  }

  private String formatShortDate(LocalDate date) {
    return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID")));
  }

  private void showWeekDetailDialog(CashFlowReport.WeekSummary week, long totalAmount) {
    Context context = requireContext();
    Dialog dialog = new Dialog(context, R.style.Finan_BottomSheetDialog);

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundResource(R.drawable.bg_bottom_sheet);
    root.setPadding(UiComponentStyles.dp(context, 20), UiComponentStyles.dp(context, 16),
        UiComponentStyles.dp(context, 20), UiComponentStyles.dp(context, 16));

    long weekTotal = showWeeklyChartIncome ? week.getWeekIncome() : week.getWeekExpense();

    TextView titleView = new TextView(context);
    titleView.setText(formatShortDate(week.getStartDate()) + " - " + formatShortDate(week.getEndDate()));
    titleView.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
    titleView.setTextSize(16f);
    titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
    root.addView(titleView);

    TextView totalView = new TextView(context);
    totalView.setTextColor(ContextCompat.getColor(context, weekTotal >= 0 ? R.color.finan_primary : R.color.finan_expense));
    totalView.setTextSize(14f);
    totalView.setTypeface(totalView.getTypeface(), Typeface.BOLD);
    if (showPercentageMode) {
      totalView.setText(fmtPct(weekTotal, totalAmount, false));
    } else {
      totalView.setText(format(weekTotal));
    }
    root.addView(totalView);

    View divider = new View(context);
    divider.setBackgroundColor(ContextCompat.getColor(context, R.color.finan_divider));
    LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, UiComponentStyles.dp(context, 1));
    divLp.topMargin = UiComponentStyles.dp(context, 12);
    divLp.bottomMargin = UiComponentStyles.dp(context, 8);
    divider.setLayoutParams(divLp);
    root.addView(divider);

    ScrollView scrollView = new ScrollView(context);
    LinearLayout content = new LinearLayout(context);
    content.setOrientation(LinearLayout.VERTICAL);

    TextView loadingView = new TextView(context);
    loadingView.setText(R.string.java_SummaryFragment_memuat_transaksi);
    loadingView.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
    loadingView.setTextSize(13f);
    loadingView.setPadding(0, UiComponentStyles.dp(context, 12), 0, UiComponentStyles.dp(context, 12));
    content.addView(loadingView);

    scrollView.addView(content);
    root.addView(scrollView);

    dialog.setContentView(root);
    dialog.setCancelable(true);
    BottomSheetHelper.show(dialog);

    long startMillis = week.getStartDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    long endMillis = week.getEndDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    TransactionType filterType = showWeeklyChartIncome ? TransactionType.INCOME : TransactionType.EXPENSE;
    long zoneOffsetMillis = java.time.ZoneId.systemDefault()
        .getRules().getOffset(java.time.Instant.now()).getTotalSeconds() * 1000L;

    services.dbWorker.compute(
        () -> {
          HistoryQuery query = new HistoryQuery(selectedWalletId, selectedCategoryId, filterType, startMillis, endMillis, true, null);
          return services.transactionGateway.findHistoryPage(query, null, 10000).getItems();
        },
        transactions -> {
          if (!dialog.isShowing()) return;

          content.removeAllViews();

          java.text.SimpleDateFormat dayHeaderFmt = new java.text.SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID"));
          java.text.SimpleDateFormat timeFmt = new java.text.SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID"));
          String[] dayNames = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};

          Map<Long, List<Transaction>> byDay = new java.util.LinkedHashMap<>();
          for (Transaction tx : transactions) {
            long dayKey = (tx.getOccurredAt() + zoneOffsetMillis) / 86400000;
            byDay.computeIfAbsent(dayKey, k -> new ArrayList<>()).add(tx);
          }

          for (CashFlowReport.DailyTotal day : week.getDays()) {
            long dayKey = (day.getDateMillis() + zoneOffsetMillis) / 86400000;
            List<Transaction> dayTxs = byDay.get(dayKey);
            if (dayTxs == null || dayTxs.isEmpty()) continue;

            long dayVal = showWeeklyChartIncome ? day.getIncomeMinor() : day.getExpenseMinor();

            LinearLayout dayHeader = new LinearLayout(context);
            dayHeader.setOrientation(LinearLayout.HORIZONTAL);
            dayHeader.setGravity(Gravity.CENTER_VERTICAL);
            dayHeader.setPadding(0, UiComponentStyles.dp(context, 8), 0, UiComponentStyles.dp(context, 4));

            View dot = new View(context);
            int dotSize = UiComponentStyles.dp(context, 8);
            GradientDrawable dotShape = new GradientDrawable();
            dotShape.setShape(GradientDrawable.OVAL);
            dotShape.setColor(DAY_COLORS[dayOfWeekIndex(day.getDateMillis())]);
            dot.setBackground(dotShape);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotLp.rightMargin = UiComponentStyles.dp(context, 6);
            dot.setLayoutParams(dotLp);
            dayHeader.addView(dot);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView dayNameView = new TextView(context);
            dayNameView.setText(dayNames[dayOfWeekIndex(day.getDateMillis())]);
            dayNameView.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
            dayNameView.setTextSize(13f);
            dayNameView.setTypeface(dayNameView.getTypeface(), Typeface.BOLD);
            textCol.addView(dayNameView);

            TextView dateView = new TextView(context);
            dateView.setText(dayHeaderFmt.format(new java.util.Date(day.getDateMillis())));
            dateView.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
            dateView.setTextSize(11f);
            textCol.addView(dateView);

            dayHeader.addView(textCol);

            TextView dayAmount = new TextView(context);
            dayAmount.setTextColor(ContextCompat.getColor(context, dayVal >= 0 ? R.color.finan_primary : R.color.finan_expense));
            dayAmount.setTextSize(13f);
            dayAmount.setTypeface(dayAmount.getTypeface(), Typeface.BOLD);
            if (showPercentageMode) {
              dayAmount.setText(fmtPct(dayVal, totalAmount, false));
            } else {
              dayAmount.setText(dayVal >= 0 ? format(dayVal) : "-" + format(-dayVal));
            }
            dayHeader.addView(dayAmount);

            content.addView(dayHeader);

            for (Transaction tx : dayTxs) {
              LinearLayout txRow = new LinearLayout(context);
              txRow.setOrientation(LinearLayout.HORIZONTAL);
              txRow.setGravity(Gravity.CENTER_VERTICAL);
              txRow.setPadding(UiComponentStyles.dp(context, 14), UiComponentStyles.dp(context, 6),
                  0, UiComponentStyles.dp(context, 6));

              Category cat = categoriesById.get(tx.getCategoryId());
              String emoji = cat != null ? cat.getIcon() : null;
              String catName = cat != null ? cat.getName() : "Lainnya";

              TextView emojiView = new TextView(context);
              emojiView.setText(emoji != null && !emoji.trim().isEmpty() ? emoji : "\uD83D\uDCB1");
              emojiView.setTextSize(18f);
              LinearLayout.LayoutParams emojiLp = new LinearLayout.LayoutParams(
                  UiComponentStyles.dp(context, 32), UiComponentStyles.dp(context, 32));
              emojiLp.rightMargin = UiComponentStyles.dp(context, 8);
              emojiView.setGravity(Gravity.CENTER);
              emojiView.setLayoutParams(emojiLp);
              txRow.addView(emojiView);

              LinearLayout txTextCol = new LinearLayout(context);
              txTextCol.setOrientation(LinearLayout.VERTICAL);
              txTextCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

              TextView catNameView = new TextView(context);
              catNameView.setText(catName);
              catNameView.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
              catNameView.setTextSize(13f);
              txTextCol.addView(catNameView);

              Wallet wallet = walletsById.get(tx.getWalletId());
              String walletName = wallet != null ? wallet.getName() : "";
              String note = tx.getNote();
              StringBuilder metaStr = new StringBuilder();
              if (walletName != null && !walletName.isEmpty()) {
                metaStr.append(walletName);
              }
              if (note != null && !note.isEmpty()) {
                if (metaStr.length() > 0) metaStr.append(" \u2022 ");
                metaStr.append(note);
              }
              if (metaStr.length() > 0 || true) {
                TextView metaView = new TextView(context);
                String metaText = metaStr.toString();
                if (metaText.isEmpty()) {
                  metaText = timeFmt.format(new java.util.Date(tx.getOccurredAt()));
                }
                metaView.setText(metaText);
                metaView.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
                metaView.setTextSize(11f);
                txTextCol.addView(metaView);
              }

              txRow.addView(txTextCol);

              boolean isPositive = tx.getType().increasesBalance();
              String txAmountStr;
              if (showPercentageMode) {
                txAmountStr = fmtPct(tx.getAmountMinor(), totalAmount, false);
              } else {
                String amountStr = MoneyFormatter.format(tx.getAmountMinor());
                txAmountStr = isPositive ? "+" + amountStr : "-" + amountStr;
              }

              TextView txAmount = new TextView(context);
              txAmount.setText(txAmountStr);
              txAmount.setTextColor(ContextCompat.getColor(context, isPositive ? R.color.finan_income : R.color.finan_expense));
              txAmount.setTextSize(13f);
              txAmount.setTypeface(txAmount.getTypeface(), Typeface.BOLD);
              txRow.addView(txAmount);

              content.addView(txRow);
            }
          }
        });
  }

  private View createActivityCard(CashFlowActivityTotal actTotal) {
    Context context = requireContext();
    View card = getLayoutInflater().inflate(R.layout.item_cash_flow_card, cashFlowContainer, false);

    TextView title = card.findViewById(R.id.item_cashflow_title);
    TextView netText = card.findViewById(R.id.item_cashflow_net);
    TextView inflowText = card.findViewById(R.id.item_cashflow_inflow);
    TextView outflowText = card.findViewById(R.id.item_cashflow_outflow);
    
    DonutChartView donutChart = card.findViewById(R.id.item_cashflow_donut_chart);
    LinearLayout legendList = card.findViewById(R.id.item_cashflow_chart_legend);

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

    List<CategoryTotal> categories = new ArrayList<>();
    categories.addAll(actTotal.getIncomeCategories());
    categories.addAll(actTotal.getExpenseCategories());
    if (categories.isEmpty()) {
      card.findViewById(R.id.item_cashflow_chart_container).setVisibility(View.GONE);
      card.findViewById(R.id.item_cashflow_divider).setVisibility(View.GONE);
    } else {
      card.findViewById(R.id.item_cashflow_chart_container).setVisibility(View.VISIBLE);
      card.findViewById(R.id.item_cashflow_divider).setVisibility(View.VISIBLE);

      // Separate into Inflow (Income) and Outflow (Expense) categories
      List<CategoryTotal> inflowCategories = new ArrayList<>();
      List<CategoryTotal> outflowCategories = new ArrayList<>();
      for (CategoryTotal cat : categories) {
        if (cat.isIncome()) {
          inflowCategories.add(cat);
        } else {
          outflowCategories.add(cat);
        }
      }

      long legendBase = inflow > 0 ? inflow : (outflow > 0 ? outflow : 1);

      // Configure central status label text
      if (inflow > 0) {
        long remaining = inflow - outflow;
        donutChart.setCenterText("Sisa", String.format(Locale.getDefault(), "%.0f%%", Math.max(0.0, remaining * 100.0 / inflow)));
      } else if (outflow > 0) {
        donutChart.setCenterText("Keluar", showPercentageMode ? "100%" : formatCompact(outflow));
      }

      // Chart View (Concentric Donut & Legend)
      List<DonutChartView.DonutItem> inflowDonutItems = new ArrayList<>();
      List<DonutChartView.DonutItem> outflowDonutItems = new ArrayList<>();
      legendList.removeAllViews();

      // 2a. Inflow
      if (!inflowCategories.isEmpty()) {
        legendList.addView(createLegendHeader(context, "PENDAPATAN"));
        for (CategoryTotal cat : inflowCategories) {
          int color = getCategoryColor(context, cat.getCategoryId());
          inflowDonutItems.add(new DonutChartView.DonutItem(cat.getCategoryName(), cat.getTotalMinor(), color));
          double percentage = cat.getTotalMinor() * 100.0 / legendBase;
          legendList.addView(createLegendRow(context, cat.getCategoryName(), cat.getTotalMinor(), color, percentage));
        }
      }

      // 2b. Outflow
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

    return card;
  }

  private View createLegendRow(Context context, String label, long amount, int color, double percentage) {
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dpToPx(context, 4), 0, dpToPx(context, 4));

    // Color dot
    View dot = new View(context);
    LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(context, 8), dpToPx(context, 8));
    dotParams.setMarginEnd(dpToPx(context, 8));
    dot.setLayoutParams(dotParams);

    GradientDrawable shape = new GradientDrawable();
    shape.setShape(GradientDrawable.OVAL);
    shape.setColor(color);
    dot.setBackground(shape);

    // Label & Percentage
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

    // Amount
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

  private View createLegendHeader(Context context, String title) {
    TextView tv = new TextView(context);
    tv.setText(title);
    tv.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    tv.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    tv.setPadding(0, dpToPx(context, 8), 0, dpToPx(context, 4));
    return tv;
  }

  private int getCategoryColor(Context context, long categoryId) {
    int[] palette = {
        ContextCompat.getColor(context, R.color.finan_primary),
        ContextCompat.getColor(context, R.color.finan_income),
        ContextCompat.getColor(context, R.color.finan_expense),
        ContextCompat.getColor(context, R.color.finan_warm_accent),
        ContextCompat.getColor(context, R.color.finan_accent),
        0xFF4A90E2, // Soft Blue
        0xFF8B6EB8, // Muted Purple
        0xFF4F6D7A  // Slate Blue
    };
    int index = (int) (categoryId % palette.length);
    if (index < 0) {
      index += palette.length;
    }
    return palette[index];
  }

  private int dpToPx(Context context, float dp) {
    return (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
  }

  private String getActivityTitle(CashFlowActivity activity) {
    switch (activity) {
      case OPERATING:
        return getString(R.string.cashflow_operating);
      case INVESTING:
        return getString(R.string.cashflow_investing);
      case FINANCING:
        return getString(R.string.cashflow_financing);
      case UNCLASSIFIED:
      default:
        return getString(R.string.cashflow_unclassified);
    }
  }

  @android.annotation.SuppressLint("InflateParams")
  private View createCategoryRow(CategoryTotal row, long maxTotalMinor) {
    View rowView = getLayoutInflater().inflate(R.layout.item_cash_flow_category, null, false);
    
    TextView name = rowView.findViewById(R.id.item_category_name);
    TextView amount = rowView.findViewById(R.id.item_category_amount);
    ProgressBar progress = rowView.findViewById(R.id.item_category_progress);

    name.setText(row.getCategoryName());
    amount.setText(format(row.getTotalMinor()));
    progress.setProgress(progressFor(row.getTotalMinor(), maxTotalMinor));

    return rowView;
  }

  private View createWalletRow(WalletBalance wallet, long totalBalance) {
    Context context = requireContext();
    LinearLayout row = new LinearLayout(context);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setPadding(0, UiComponentStyles.dp(context, 10), 0, UiComponentStyles.dp(context, 10));

    TextView name = new TextView(context);
    name.setEllipsize(TextUtils.TruncateAt.END);
    name.setMaxLines(1);
    name.setText(wallet.getWalletName());
    name.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
    name.setTextSize(15f);
    row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

    TextView balance = new TextView(context);
    balance.setMaxLines(1);
    if (showPercentageMode && totalBalance > 0) {
      balance.setText(fmtPct(wallet.getBalanceMinor(), totalBalance, false));
    } else {
      balance.setText(format(wallet.getBalanceMinor()));
    }
    balance.setTextColor(
        ContextCompat.getColor(
            context,
            wallet.getBalanceMinor() < 0 ? R.color.finan_expense : R.color.finan_primary));
    balance.setTextSize(15f);
    balance.setTypeface(balance.getTypeface(), Typeface.BOLD);
    row.addView(
        balance,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    return row;
  }

  private View createDivider(Context context) {
    View divider = new View(context);
    divider.setBackgroundColor(ContextCompat.getColor(context, R.color.finan_divider));
    divider.setLayoutParams(
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, UiComponentStyles.dp(context, 1)));
    return divider;
  }

  private void showDateRangePicker() {
    Context context = requireContext();
    android.content.SharedPreferences prefs = context.getSharedPreferences("finan_prefs", Context.MODE_PRIVATE);
    int cutoffDay = prefs.getInt("cutoff_day", 1);

    SummaryDateRangeBottomSheet bottomSheet =
        new SummaryDateRangeBottomSheet(
            context,
            selectedStartDate,
            selectedEndDate,
            cutoffDay,
            (startDate, endDate, newCutoffDay) -> {
              prefs.edit().putInt("cutoff_day", newCutoffDay).apply();
              selectedStartDate = startDate;
              selectedEndDate = endDate;
              loadSummaryAsync();
            });
    bottomSheet.show();
  }

  private String getCycleLabel(int cutoffDay) {
    if (cutoffDay == -1) {
      return "Hari Kerja Terakhir (Akhir Bulan)";
    } else if (cutoffDay == 1) {
      return "Tanggal 1 (Awal Bulan)";
    } else {
      return "Tanggal " + cutoffDay;
    }
  }

  private void showSummaryFilterDialog() {
    services.dbWorker.compute(
        () -> {
          List<Wallet> wallets = services.walletService.findAll();
          List<Category> categories = services.categoryDao.findAllOrdered();
          return new SummaryFilterSource(wallets, categories);
        },
        data -> {
          if (!isAdded() || data == null) {
            return;
          }
          Map<Long, Wallet> walletMap = EntityLookup.indexWallets(data.wallets);
          Map<Long, Category> categoryMap = EntityLookup.indexCategories(data.categories);
          Long walletId = validWalletIdOrNull(selectedWalletId, walletMap);
          Long categoryId = validCategoryIdOrNull(selectedCategoryId, categoryMap);

          SummaryFilterBottomSheet bottomSheet =
              new SummaryFilterBottomSheet(
                  requireContext(),
                  data.wallets,
                  data.categories,
                  walletId,
                  categoryId,
                  (wId, cId) -> {
                    selectedWalletId = wId;
                    selectedCategoryId = cId;
                    loadSummaryAsync();
                  },
                  () -> {
                    selectedWalletId = null;
                    selectedCategoryId = null;
                    loadSummaryAsync();
                  });
          bottomSheet.show();
        });
  }

  private DateRange restoreSelectedRange(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      return defaultRange();
    }
    String rawStartDate = savedInstanceState.getString(START_DATE_STATE_KEY);
    String rawEndDate = savedInstanceState.getString(END_DATE_STATE_KEY);
    if (rawStartDate == null || rawEndDate == null) {
      return defaultRange();
    }
    try {
      LocalDate start = LocalDate.parse(rawStartDate);
      LocalDate end = LocalDate.parse(rawEndDate);
      return normalizeRange(start, end);
    } catch (DateTimeParseException ignored) {
      return defaultRange();
    }
  }

  private Long restoreFilterId(@Nullable Bundle savedInstanceState, String key) {
    if (savedInstanceState == null) {
      return null;
    }
    long value = savedInstanceState.getLong(key, FILTER_NONE_ID);
    return value == FILTER_NONE_ID ? null : value;
  }

  private void updateFilterButton() {
    if (filterButton == null) {
      return;
    }
    String walletLabel = walletFilterLabel(selectedWalletId);
    String categoryLabel = categoryFilterLabel(selectedCategoryId);
    boolean active = selectedWalletId != null || selectedCategoryId != null;
    filterButton.setAlpha(active ? 1f : 0.82f);
    filterButton.setColorFilter(
        ContextCompat.getColor(
            requireContext(), active ? R.color.finan_warm_accent : R.color.finan_primary));
    filterButton.setContentDescription(
        active
            ? getString(
                R.string.summary_filter_active_content_description, walletLabel, categoryLabel)
            : getString(R.string.summary_filter_content_description));
    filterButton.setTooltipText(filterButton.getContentDescription());
  }

  private String walletFilterLabel(Long walletId) {
    if (walletId == null) {
      return getString(R.string.summary_all_wallets);
    }
    Wallet wallet = walletsById.get(walletId);
    return wallet == null ? getString(R.string.summary_all_wallets) : wallet.getName();
  }

  private String categoryFilterLabel(Long categoryId) {
    if (categoryId == null) {
      return getString(R.string.summary_all_categories);
    }
    Category category = categoriesById.get(categoryId);
    return category == null ? getString(R.string.summary_all_categories) : category.getName();
  }

  private static Long validWalletIdOrNull(Long walletId, Map<Long, Wallet> walletsById) {
    return walletId != null && !walletsById.containsKey(walletId) ? null : walletId;
  }

  private static Long validCategoryIdOrNull(Long categoryId, Map<Long, Category> categoriesById) {
    return categoryId != null && !categoriesById.containsKey(categoryId) ? null : categoryId;
  }

  private String formatDateLabel(LocalDate date) {
    Locale locale = Locale.forLanguageTag("id-ID");
    return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale));
  }

  private String formatRangeLabel(LocalDate startDate, LocalDate endDate) {
    if (startDate.equals(endDate)) {
      return formatDateLabel(startDate);
    }
    return formatDateLabel(startDate) + " - " + formatDateLabel(endDate);
  }

  private static DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
    if (startDate.isAfter(endDate)) {
      return new DateRange(endDate, startDate);
    }
    return new DateRange(startDate, endDate);
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

  private DateRange cycleRange(int monthOffset) {
    Context context = getContext();
    int cutoffDay = 1;
    if (context != null) {
      cutoffDay = context.getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
                         .getInt("cutoff_day", 1);
    }
    LocalDate date = LocalDate.now().plusMonths(monthOffset);
    if (cutoffDay == 1) {
      return new DateRange(date.withDayOfMonth(1), date.withDayOfMonth(date.lengthOfMonth()));
    }
    LocalDate start;
    if (cutoffDay == -1) {
      LocalDate thisMonthCutoff = lastBusinessDayOfMonth(date);
      if (date.isBefore(thisMonthCutoff)) {
        start = lastBusinessDayOfMonth(date.minusMonths(1));
      } else {
        start = thisMonthCutoff;
      }
    } else {
      if (date.getDayOfMonth() >= cutoffDay) {
        start = date.withDayOfMonth(cutoffDay);
      } else {
        start = date.minusMonths(1).withDayOfMonth(cutoffDay);
      }
    }
    return new DateRange(start, start.plusMonths(1).minusDays(1));
  }

  private DateRange defaultRange() {
    return cycleRange(0);
  }

  private long totalWalletBalance(MonthlySummary summary) {
    long total = 0L;
    for (WalletBalance wallet : summary.getWalletBalances()) {
      total += wallet.getBalanceMinor();
    }
    return total;
  }

  private int progressFor(long totalMinor, long maxTotalMinor) {
    if (maxTotalMinor <= 0L) {
      return 0;
    }
    double progressRatio = totalMinor / (double) maxTotalMinor;
    return Math.max(1, (int) Math.round(progressRatio * CATEGORY_PROGRESS_MAX));
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

  private void setPercentageMode(boolean enabled) {
    if (showPercentageMode == enabled) return;
    showPercentageMode = enabled;
    requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
        .edit().putBoolean("summary_percentage_mode", showPercentageMode).apply();
    updateModeToggle();
    if (cachedData != null) {
      TransitionManager.beginDelayedTransition(contentContainer);
      bindReport(cachedData.reportResult, cachedData.summary, cachedData.prevSummary, cachedData.prevPrevSummary,
          selectedStartDate, selectedEndDate, cachedData.unclassifiedCount, cachedData.unclassifiedAmount);
    } else {
      loadSummaryAsync();
    }
  }

  private void updateModeToggle() {
    boolean pct = showPercentageMode;
    modeNominal.setBackgroundResource(pct ? android.R.color.transparent : R.drawable.bg_toggle_active);
    modeNominal.setTextColor(pct ? 0xFF4A9E7F : 0xFFFFFFFF);
    modePersentase.setBackgroundResource(pct ? R.drawable.bg_toggle_active : android.R.color.transparent);
    modePersentase.setTextColor(pct ? 0xFFFFFFFF : 0xFF4A9E7F);
  }

  private void setWeeklyChartMode(boolean showIncome) {
    if (showWeeklyChartIncome == showIncome) return;
    showWeeklyChartIncome = showIncome;
    updateChartToggle();
    if (cachedData != null) {
      bindWeeklyChart(cachedData.reportResult);
    }
  }

  private void updateChartToggle() {
    boolean income = showWeeklyChartIncome;
    chartToggleIncome.setBackgroundResource(income ? R.drawable.bg_toggle_active : android.R.color.transparent);
    chartToggleIncome.setTextColor(income ? 0xFFFFFFFF : 0xFF4A9E7F);
    chartToggleExpense.setBackgroundResource(income ? android.R.color.transparent : R.drawable.bg_toggle_active);
    chartToggleExpense.setTextColor(income ? 0xFF4A9E7F : 0xFFFFFFFF);
  }

  private void populateChartLegend() {
    Context context = requireContext();
    String[] labels = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
    int dotSize = UiComponentStyles.dp(context, 6);
    int gap = UiComponentStyles.dp(context, 3);
    GradientDrawable dotShape = new GradientDrawable();
    dotShape.setShape(GradientDrawable.OVAL);

    for (int i = 0; i < 7; i++) {
      dotShape = new GradientDrawable();
      dotShape.setShape(GradientDrawable.OVAL);
      dotShape.setColor(DAY_COLORS[i]);

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

  private static final class DateRange {
    private final LocalDate start;
    private final LocalDate end;

    private DateRange(LocalDate start, LocalDate end) {
      this.start = start;
      this.end = end;
    }
  }

  private static final class SummaryLoadData {
    private final Map<Long, Wallet> walletsById;
    private final Map<Long, Category> categoriesById;
    private final Long walletId;
    private final Long categoryId;
    private final CashFlowReportResult reportResult;
    private final MonthlySummary summary;
    private final MonthlySummary prevSummary;
    private final MonthlySummary prevPrevSummary;
    private final long unclassifiedCount;
    private final long unclassifiedAmount;

    private SummaryLoadData(
        Map<Long, Wallet> walletsById,
        Map<Long, Category> categoriesById,
        Long walletId,
        Long categoryId,
        CashFlowReportResult reportResult,
        MonthlySummary summary,
        MonthlySummary prevSummary,
        MonthlySummary prevPrevSummary,
        long unclassifiedCount,
        long unclassifiedAmount) {
      this.walletsById = walletsById;
      this.categoriesById = categoriesById;
      this.walletId = walletId;
      this.categoryId = categoryId;
      this.reportResult = reportResult;
      this.summary = summary;
      this.prevSummary = prevSummary;
      this.prevPrevSummary = prevPrevSummary;
      this.unclassifiedCount = unclassifiedCount;
      this.unclassifiedAmount = unclassifiedAmount;
    }
  }

  private static final class SummaryFilterSource {
    private final List<Wallet> wallets;
    private final List<Category> categories;

    private SummaryFilterSource(List<Wallet> wallets, List<Category> categories) {
      this.wallets = wallets;
      this.categories = categories;
    }
  }

  private class CashFlowAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CashFlowAdapter.ViewHolder> {
    private List<CashFlowActivityTotal> items = new ArrayList<>();

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    public void setItems(List<CashFlowActivityTotal> newItems) {
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

    class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
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
        Context context = itemView.getContext();
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

        List<CategoryTotal> categories = new ArrayList<>();
        categories.addAll(actTotal.getIncomeCategories());
        categories.addAll(actTotal.getExpenseCategories());
        if (categories.isEmpty()) {
          chartContainer.setVisibility(View.GONE);
          divider.setVisibility(View.GONE);
        } else {
          chartContainer.setVisibility(View.VISIBLE);
          divider.setVisibility(View.VISIBLE);

          List<CategoryTotal> inflowCategories = new ArrayList<>();
          List<CategoryTotal> outflowCategories = new ArrayList<>();
          for (CategoryTotal cat : categories) {
            if (cat.isIncome()) {
              inflowCategories.add(cat);
            } else {
              outflowCategories.add(cat);
            }
          }

          long legendBase = inflow > 0 ? inflow : (outflow > 0 ? outflow : 1);

          if (inflow > 0) {
            long remaining = inflow - outflow;
            donutChart.setCenterText("Sisa", String.format(Locale.getDefault(), "%.0f%%", Math.max(0.0, remaining * 100.0 / inflow)));
          } else if (outflow > 0) {
            donutChart.setCenterText("Keluar", showPercentageMode ? "100%" : formatCompact(outflow));
          }

          List<DonutChartView.DonutItem> inflowDonutItems = new ArrayList<>();
          List<DonutChartView.DonutItem> outflowDonutItems = new ArrayList<>();
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

  private class WalletAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<WalletAdapter.ViewHolder> {
    private List<WalletBalance> items = new ArrayList<>();
    private long totalBalance = 0;

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    public void setItems(List<WalletBalance> newItems, long totalBalance) {
      this.items = newItems;
      this.totalBalance = totalBalance;
      //noinspection NotifyDataSetChanged
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      Context context = parent.getContext();
      LinearLayout root = new LinearLayout(context);
      root.setOrientation(LinearLayout.VERTICAL);
      root.setLayoutParams(new androidx.recyclerview.widget.RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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

    class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
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
}
