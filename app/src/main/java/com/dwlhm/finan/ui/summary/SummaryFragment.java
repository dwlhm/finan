package com.dwlhm.finan.ui.summary;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.domain.model.CategoryTotal;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.domain.model.WalletBalance;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.EntityLookup;
import com.dwlhm.finan.ui.common.FilterDialog;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
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

  private ProgressBar loading;
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
    loading = view.findViewById(R.id.summary_loading);
    periodLabel = view.findViewById(R.id.summary_period_label);
    filterButton = view.findViewById(R.id.summary_filter_button);
    netFlow = view.findViewById(R.id.summary_net_flow);
    monthExpense = view.findViewById(R.id.summary_month_expense);
    monthIncome = view.findViewById(R.id.summary_month_income);
    cashFlowContainer = view.findViewById(R.id.summary_cash_flow_container);
    walletList = view.findViewById(R.id.summary_wallet_list);
    walletTotal = view.findViewById(R.id.summary_wallet_total);
    emptyMessage = view.findViewById(R.id.summary_empty);
    adviceCard = view.findViewById(R.id.summary_advice_card);
    adviceIcon = view.findViewById(R.id.summary_advice_icon);
    adviceTitle = view.findViewById(R.id.summary_advice_title);
    adviceMessage = view.findViewById(R.id.summary_advice_message);
    periodLabel.setOnClickListener(v -> showDateRangePicker());
    filterButton.setOnClickListener(v -> showSummaryFilterDialog());
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
    loadSummaryAsync();
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
    loading.setVisibility(View.VISIBLE);
    emptyMessage.setVisibility(View.GONE);
    services.dbWorker.compute(
        () -> {
          List<Wallet> wallets = services.walletDao.findAll();
          List<Category> categories = services.categoryDao.findAllOrdered();
          Map<Long, Wallet> walletMap = EntityLookup.indexWallets(wallets);
          Map<Long, Category> categoryMap = EntityLookup.indexCategories(categories);
          Long walletId = validWalletIdOrNull(requestedWalletId, walletMap);
          Long categoryId = validCategoryIdOrNull(requestedCategoryId, categoryMap);
          MonthlySummary summary =
              services.summaryService.loadRange(
                  requestedStartDate, requestedEndDate, walletId, categoryId);
          MonthlySummary prevSummary =
              services.summaryService.loadRange(
                  requestedStartDate.minusMonths(1), requestedEndDate.minusMonths(1), walletId, categoryId);
          MonthlySummary prevPrevSummary =
              services.summaryService.loadRange(
                  requestedStartDate.minusMonths(2), requestedEndDate.minusMonths(2), walletId, categoryId);
          return new SummaryLoadData(walletMap, categoryMap, walletId, categoryId, summary, prevSummary, prevPrevSummary);
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
          bindSummary(data.summary, data.prevSummary, data.prevPrevSummary, requestedStartDate, requestedEndDate);
          updateFilterButton();
          loading.setVisibility(View.GONE);
        });
  }

  private void bindSummary(
      MonthlySummary summary,
      MonthlySummary prevSummary,
      MonthlySummary prevPrevSummary,
      LocalDate startDate,
      LocalDate endDate) {
    periodLabel.setText(formatRangeLabel(startDate, endDate));
    
    long totalIncome = summary.getMonthIncomeMinor();
    long totalExpense = summary.getMonthExpenseMinor();
    long net = summary.getNetFlowMinor();
    
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

    FinancialAdvisor.Advice advice = FinancialAdvisor.getAdvice(requireContext(), summary, prevSummary, prevPrevSummary);
    if (advice != null) {
      adviceCard.setVisibility(View.VISIBLE);
      adviceCard.setBackgroundResource(advice.bgRes);
      adviceIcon.setImageResource(advice.iconRes);
      adviceIcon.setColorFilter(ContextCompat.getColor(requireContext(), advice.colorRes));
      adviceTitle.setText(advice.title);
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        adviceMessage.setText(android.text.Html.fromHtml(advice.message, android.text.Html.FROM_HTML_MODE_LEGACY));
      } else {
        adviceMessage.setText(android.text.Html.fromHtml(advice.message));
      }
    } else {
      adviceCard.setVisibility(View.GONE);
    }

    cashFlowContainer.removeAllViews();
    if (summary.getActivitySummaries().isEmpty()) {
      emptyMessage.setVisibility(View.VISIBLE);
    } else {
      emptyMessage.setVisibility(View.GONE);
      for (MonthlySummary.ActivitySummary activitySummary : summary.getActivitySummaries()) {
        cashFlowContainer.addView(createActivityCard(activitySummary));
      }
    }

    long totalBalance = totalWalletBalance(summary);
    walletTotal.setText(showPercentageMode ? "100%" : format(totalBalance));

    walletList.removeAllViews();
    for (WalletBalance wallet : summary.getWalletBalances()) {
      if (walletList.getChildCount() > 0) {
        walletList.addView(createDivider(requireContext()));
      }
      walletList.addView(createWalletRow(wallet, totalBalance));
    }
  }

  private View createActivityCard(MonthlySummary.ActivitySummary actSummary) {
    Context context = requireContext();
    View card = getLayoutInflater().inflate(R.layout.item_cash_flow_card, cashFlowContainer, false);

    TextView title = card.findViewById(R.id.item_cashflow_title);
    TextView netText = card.findViewById(R.id.item_cashflow_net);
    TextView inflowText = card.findViewById(R.id.item_cashflow_inflow);
    TextView outflowText = card.findViewById(R.id.item_cashflow_outflow);
    
    DonutChartView donutChart = card.findViewById(R.id.item_cashflow_donut_chart);
    LinearLayout legendList = card.findViewById(R.id.item_cashflow_chart_legend);

    title.setText(getActivityTitle(actSummary.getActivity()));
    
    long net = actSummary.getNetFlowMinor();
    long inflow = actSummary.getInflowMinor();
    long outflow = actSummary.getOutflowMinor();

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

    List<CategoryTotal> categories = actSummary.getTopCategories();
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
    LocalDate[] pendingStart = {selectedStartDate};
    LocalDate[] pendingEnd = {selectedEndDate};

    android.content.SharedPreferences prefs = context.getSharedPreferences("finan_prefs", Context.MODE_PRIVATE);
    int[] pendingCutoff = {prefs.getInt("cutoff_day", 1)};

    LinearLayout content = new LinearLayout(context);
    content.setOrientation(LinearLayout.VERTICAL);
    int horizontalPadding = UiComponentStyles.dp(context, 20);
    int topPadding = UiComponentStyles.dp(context, 6);
    content.setPadding(horizontalPadding, topPadding, horizontalPadding, 0);

    TextView startValue = createRangeDateValue(context, pendingStart[0]);
    TextView endValue = createRangeDateValue(context, pendingEnd[0]);
    TextView cycleValue = createPickerValue(context, getCycleLabel(pendingCutoff[0]));

    content.addView(createRangeDateRow(context, R.string.summary_range_start_label, startValue));
    content.addView(createRangeDateRow(context, R.string.summary_range_end_label, endValue));

    LinearLayout cycleRow = new LinearLayout(context);
    cycleRow.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams cycleRowParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    cycleRowParams.topMargin = UiComponentStyles.dp(context, 10);
    cycleRow.setLayoutParams(cycleRowParams);

    TextView cycleLabel = new TextView(context);
    cycleLabel.setText("Mulai Siklus Bulanan (Tanggal Gajian)");
    cycleLabel.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
    cycleLabel.setTextSize(12f);
    cycleLabel.setTypeface(cycleLabel.getTypeface(), Typeface.BOLD);
    cycleRow.addView(cycleLabel);

    LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    valueParams.topMargin = UiComponentStyles.dp(context, 6);
    cycleRow.addView(cycleValue, valueParams);

    content.addView(cycleRow);

    startValue.setOnClickListener(
        v ->
            showDatePicker(
                R.string.summary_range_start_label,
                pendingStart[0],
                selected -> {
                  pendingStart[0] = selected;
                  if (pendingEnd[0].isBefore(selected)) {
                    pendingEnd[0] = selected;
                  }
                  startValue.setText(formatDateLabel(pendingStart[0]));
                  endValue.setText(formatDateLabel(pendingEnd[0]));
                }));
    endValue.setOnClickListener(
        v ->
            showDatePicker(
                R.string.summary_range_end_label,
                pendingEnd[0],
                selected -> {
                  pendingEnd[0] = selected;
                  if (pendingStart[0].isAfter(selected)) {
                    pendingStart[0] = selected;
                  }
                  startValue.setText(formatDateLabel(pendingStart[0]));
                  endValue.setText(formatDateLabel(pendingEnd[0]));
                }));

    String[] cycleOptions = {
      "Tanggal 1 (Awal Bulan)",
      "Tanggal 5",
      "Tanggal 10",
      "Tanggal 15",
      "Tanggal 20",
      "Tanggal 25",
      "Tanggal 28",
      "Hari Kerja Terakhir (Akhir Bulan)"
    };
    int[] cycleValues = {1, 5, 10, 15, 20, 25, 28, -1};

    cycleValue.setOnClickListener(v -> {
      new AlertDialog.Builder(context)
          .setTitle("Pilih Siklus Gajian")
          .setItems(cycleOptions, (dialog, which) -> {
            int newDay = cycleValues[which];
            pendingCutoff[0] = newDay;
            cycleValue.setText(getCycleLabel(newDay));

            LocalDate now = LocalDate.now();
            LocalDate start;
            LocalDate end;
            if (newDay == 1) {
              start = now.withDayOfMonth(1);
              end = start.withDayOfMonth(start.lengthOfMonth());
            } else if (newDay == -1) {
              LocalDate thisMonthCutoff = lastBusinessDayOfMonth(now);
              if (now.isBefore(thisMonthCutoff)) {
                start = lastBusinessDayOfMonth(now.minusMonths(1));
                end = thisMonthCutoff.minusDays(1);
              } else {
                start = thisMonthCutoff;
                end = lastBusinessDayOfMonth(now.plusMonths(1)).minusDays(1);
              }
            } else {
              if (now.getDayOfMonth() >= newDay) {
                start = now.withDayOfMonth(newDay);
              } else {
                start = now.minusMonths(1).withDayOfMonth(newDay);
              }
              end = start.plusMonths(1).minusDays(1);
            }
            pendingStart[0] = start;
            pendingEnd[0] = end;
            startValue.setText(formatDateLabel(pendingStart[0]));
            endValue.setText(formatDateLabel(pendingEnd[0]));
          })
          .show();
    });

    new AlertDialog.Builder(context)
        .setTitle(R.string.summary_date_picker_title)
        .setView(content)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(
            R.string.summary_range_apply,
            (dialog, which) -> {
              prefs.edit().putInt("cutoff_day", pendingCutoff[0]).apply();
              selectedStartDate = pendingStart[0];
              selectedEndDate = pendingEnd[0];
              loadSummaryAsync();
            })
        .show();
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
          List<Wallet> wallets = services.walletDao.findAll();
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

          ArrayList<FilterDialog.Group> groups = new ArrayList<>();
          groups.add(
              new FilterDialog.Group(
                  getString(R.string.summary_wallet_filter_label),
                  getString(R.string.summary_wallet_filter_title),
                  walletFilterOptions(data.wallets),
                  walletId));
          groups.add(
              new FilterDialog.Group(
                  getString(R.string.summary_category_filter_label),
                  getString(R.string.summary_category_filter_title),
                  categoryFilterOptions(data.categories),
                  categoryId));

          FilterDialog.show(
              requireContext(),
              getString(R.string.summary_filter_title),
              getString(R.string.summary_range_apply),
              getString(R.string.summary_filter_reset),
              groups,
              selectedIds -> {
                selectedWalletId = selectedIds.get(0);
                selectedCategoryId = selectedIds.get(1);
                loadSummaryAsync();
              },
              () -> {
                selectedWalletId = null;
                selectedCategoryId = null;
                loadSummaryAsync();
              });
        });
  }

  private void showDatePicker(
      @StringRes int titleRes, LocalDate initialDate, DateSelectionListener listener) {
    DatePickerDialog dialog =
        new DatePickerDialog(
            requireContext(),
            (picker, year, monthOfYear, dayOfMonth) -> {
              listener.onDateSelected(LocalDate.of(year, monthOfYear + 1, dayOfMonth));
            },
            initialDate.getYear(),
            initialDate.getMonthValue() - 1,
            initialDate.getDayOfMonth());
    dialog.setTitle(titleRes);
    dialog.show();
  }

  private LinearLayout createRangeDateRow(
      Context context, @StringRes int labelRes, TextView valueView) {
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams rowParams =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = UiComponentStyles.dp(context, 10);
    row.setLayoutParams(rowParams);

    TextView label = new TextView(context);
    label.setText(labelRes);
    label.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
    label.setTextSize(12f);
    label.setTypeface(label.getTypeface(), Typeface.BOLD);
    row.addView(label);

    LinearLayout.LayoutParams valueParams =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    valueParams.topMargin = UiComponentStyles.dp(context, 6);
    row.addView(valueView, valueParams);
    return row;
  }

  private TextView createRangeDateValue(Context context, LocalDate date) {
    return createPickerValue(context, formatDateLabel(date));
  }

  private TextView createPickerValue(Context context, String text) {
    TextView value = new TextView(context);
    value.setBackgroundResource(R.drawable.bg_control_surface);
    value.setClickable(true);
    value.setEllipsize(TextUtils.TruncateAt.END);
    value.setFocusable(true);
    value.setForeground(
        ContextCompat.getDrawable(context, UiComponentStyles.selectableItemBackground(context)));
    value.setGravity(Gravity.CENTER_VERTICAL);
    value.setMaxLines(1);
    value.setMinHeight(UiComponentStyles.dp(context, 46));
    value.setPadding(
        UiComponentStyles.dp(context, 12),
        0,
        UiComponentStyles.dp(context, 12),
        0);
    value.setText(text);
    value.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
    value.setTextSize(15f);
    return value;
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

  private List<FilterDialog.Option> walletFilterOptions(List<Wallet> wallets) {
    ArrayList<FilterDialog.Option> options = new ArrayList<>();
    options.add(new FilterDialog.Option(null, getString(R.string.summary_all_wallets)));
    for (Wallet wallet : wallets) {
      options.add(new FilterDialog.Option(wallet.getId(), wallet.getName()));
    }
    return options;
  }

  private List<FilterDialog.Option> categoryFilterOptions(List<Category> categories) {
    ArrayList<FilterDialog.Option> options = new ArrayList<>();
    options.add(new FilterDialog.Option(null, getString(R.string.summary_all_categories)));
    for (Category category : categories) {
      options.add(new FilterDialog.Option(category.getId(), category.getName()));
    }
    return options;
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

  private DateRange defaultRange() {
    Context context = getContext();
    int cutoffDay = 1;
    if (context != null) {
      cutoffDay = context.getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
                         .getInt("cutoff_day", 1);
    }
    LocalDate now = LocalDate.now();
    if (cutoffDay == 1) {
      return new DateRange(now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()));
    }
    LocalDate start;
    LocalDate end;
    if (cutoffDay == -1) {
      LocalDate thisMonthCutoff = lastBusinessDayOfMonth(now);
      if (now.isBefore(thisMonthCutoff)) {
        start = lastBusinessDayOfMonth(now.minusMonths(1));
        end = thisMonthCutoff.minusDays(1);
      } else {
        start = thisMonthCutoff;
        end = lastBusinessDayOfMonth(now.plusMonths(1)).minusDays(1);
      }
    } else {
      if (now.getDayOfMonth() >= cutoffDay) {
        start = now.withDayOfMonth(cutoffDay);
      } else {
        start = now.minusMonths(1).withDayOfMonth(cutoffDay);
      }
      end = start.plusMonths(1).minusDays(1);
    }
    return new DateRange(start, end);
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
      bindSummary(cachedData.summary, cachedData.prevSummary, cachedData.prevPrevSummary, selectedStartDate, selectedEndDate);
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

  private String fmtPct(long amount, long total, boolean signed) {
    if (total <= 0) return "0%";
    double pct = (amount * 100.0) / total;
    return (signed && amount >= 0 ? "+" : "") + String.format(Locale.getDefault(), "%.1f%%", pct);
  }

  private interface DateSelectionListener {
    void onDateSelected(LocalDate date);
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
    private final MonthlySummary summary;
    private final MonthlySummary prevSummary;
    private final MonthlySummary prevPrevSummary;

    private SummaryLoadData(
        Map<Long, Wallet> walletsById,
        Map<Long, Category> categoriesById,
        Long walletId,
        Long categoryId,
        MonthlySummary summary,
        MonthlySummary prevSummary,
        MonthlySummary prevPrevSummary) {
      this.walletsById = walletsById;
      this.categoriesById = categoriesById;
      this.walletId = walletId;
      this.categoryId = categoryId;
      this.summary = summary;
      this.prevSummary = prevSummary;
      this.prevPrevSummary = prevPrevSummary;
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
}
