package com.dwlhm.finan.ui.summary;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.CategoryTotal;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.domain.model.WalletBalance;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.FilterDialog;
import com.dwlhm.finan.service.summary.SummaryService;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.UiComponentStyles;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SummaryFragment extends ScreenFragment {

  private static final String START_DATE_STATE_KEY = "summary_start_date";
  private static final String END_DATE_STATE_KEY = "summary_end_date";
  private static final String WALLET_FILTER_STATE_KEY = "summary_wallet_filter";
  private static final String CATEGORY_FILTER_STATE_KEY = "summary_category_filter";
  private static final long FILTER_NONE_ID = -1L;
  private static final int CATEGORY_PROGRESS_MAX = 1000;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
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
  private TextView todayExpense;
  private TextView todayIncome;
  private LinearLayout categoryList;
  private LinearLayout walletList;
  private TextView walletTotal;
  private TextView emptyMessage;

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_summary;
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
    todayExpense = view.findViewById(R.id.summary_today_expense);
    todayIncome = view.findViewById(R.id.summary_today_income);
    categoryList = view.findViewById(R.id.summary_category_list);
    walletList = view.findViewById(R.id.summary_wallet_list);
    walletTotal = view.findViewById(R.id.summary_wallet_total);
    emptyMessage = view.findViewById(R.id.summary_empty);
    periodLabel.setOnClickListener(v -> showDateRangePicker());
    filterButton.setOnClickListener(v -> showSummaryFilterDialog());
    updateFilterButton();
  }

  @Override
  public void onResume() {
    super.onResume();
    loadSummaryAsync();
  }

  @Override
  public void onDestroy() {
    mainHandler.removeCallbacksAndMessages(null);
    executor.shutdownNow();
    super.onDestroy();
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
    LocalDate requestedStartDate = selectedStartDate;
    LocalDate requestedEndDate = selectedEndDate;
    Long requestedWalletId = selectedWalletId;
    Long requestedCategoryId = selectedCategoryId;
    loading.setVisibility(View.VISIBLE);
    emptyMessage.setVisibility(View.GONE);
    SummaryService summaryService = ServicesProvider.get(requireContext()).summaryService;
    executor.execute(
        () -> {
          MonthlySummary summary =
              summaryService.loadRange(
                  requestedStartDate,
                  requestedEndDate,
                  requestedWalletId,
                  requestedCategoryId);
          mainHandler.post(
              () -> {
                if (!isAdded()
                    || getView() == null
                    || !requestedStartDate.equals(selectedStartDate)
                    || !requestedEndDate.equals(selectedEndDate)
                    || !Objects.equals(requestedWalletId, selectedWalletId)
                    || !Objects.equals(requestedCategoryId, selectedCategoryId)) {
                  return;
                }
                bindSummary(summary, requestedStartDate, requestedEndDate);
                loading.setVisibility(View.GONE);
              });
        });
  }

  private void bindSummary(MonthlySummary summary, LocalDate startDate, LocalDate endDate) {
    periodLabel.setText(formatRangeLabel(startDate, endDate));
    netFlow.setText(format(summary.getMonthIncomeMinor() - summary.getMonthExpenseMinor()));
    monthExpense.setText(format(summary.getMonthExpenseMinor()));
    monthIncome.setText(format(summary.getMonthIncomeMinor()));
    todayExpense.setText(format(summary.getTodayExpenseMinor()));
    todayIncome.setText(format(summary.getTodayIncomeMinor()));
    walletTotal.setText(format(totalWalletBalance(summary)));

    categoryList.removeAllViews();
    if (summary.getTopExpenseCategories().isEmpty()) {
      emptyMessage.setVisibility(View.VISIBLE);
      emptyMessage.setText(R.string.summary_no_expense);
    } else {
      emptyMessage.setVisibility(View.GONE);
      long maxTotalMinor = maxCategoryTotal(summary);
      for (CategoryTotal row : summary.getTopExpenseCategories()) {
        categoryList.addView(createCategoryRow(row, maxTotalMinor));
      }
    }

    walletList.removeAllViews();
    for (WalletBalance wallet : summary.getWalletBalances()) {
      if (walletList.getChildCount() > 0) {
        walletList.addView(createDivider(requireContext()));
      }
      walletList.addView(createWalletRow(wallet));
    }
  }

  private void showDateRangePicker() {
    Context context = requireContext();
    LocalDate[] pendingStart = {selectedStartDate};
    LocalDate[] pendingEnd = {selectedEndDate};

    LinearLayout content = new LinearLayout(context);
    content.setOrientation(LinearLayout.VERTICAL);
    int horizontalPadding = UiComponentStyles.dp(context, 20);
    int topPadding = UiComponentStyles.dp(context, 6);
    content.setPadding(horizontalPadding, topPadding, horizontalPadding, 0);

    TextView startValue = createRangeDateValue(context, pendingStart[0]);
    TextView endValue = createRangeDateValue(context, pendingEnd[0]);
    content.addView(
        createRangeDateRow(context, R.string.summary_range_start_label, startValue));
    content.addView(createRangeDateRow(context, R.string.summary_range_end_label, endValue));

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

    new AlertDialog.Builder(context)
        .setTitle(R.string.summary_date_picker_title)
        .setView(content)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(
            R.string.summary_range_apply,
            (dialog, which) -> {
              selectedStartDate = pendingStart[0];
              selectedEndDate = pendingEnd[0];
              loadSummaryAsync();
            })
        .show();
  }

  private void showSummaryFilterDialog() {
    Context context = requireContext();
    AppServices services = ServicesProvider.get(context);
    selectedWalletId = validWalletIdOrNull(services, selectedWalletId);
    selectedCategoryId = validCategoryIdOrNull(services, selectedCategoryId);

    ArrayList<FilterDialog.Group> groups = new ArrayList<>();
    groups.add(
        new FilterDialog.Group(
            getString(R.string.summary_wallet_filter_label),
            getString(R.string.summary_wallet_filter_title),
            walletFilterOptions(services.walletDao.findAll()),
            selectedWalletId));
    groups.add(
        new FilterDialog.Group(
            getString(R.string.summary_category_filter_label),
            getString(R.string.summary_category_filter_title),
            categoryFilterOptions(services.categoryDao.findAllOrdered()),
            selectedCategoryId));

    FilterDialog.show(
        context,
        getString(R.string.summary_filter_title),
        getString(R.string.summary_range_apply),
        getString(R.string.summary_filter_reset),
        groups,
        selectedIds -> {
          selectedWalletId = selectedIds.get(0);
          selectedCategoryId = selectedIds.get(1);
          updateFilterButton();
          loadSummaryAsync();
        },
        () -> {
          selectedWalletId = null;
          selectedCategoryId = null;
          updateFilterButton();
          loadSummaryAsync();
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

  private View createCategoryRow(CategoryTotal row, long maxTotalMinor) {
    Context context = requireContext();
    LinearLayout container = new LinearLayout(context);
    container.setOrientation(LinearLayout.VERTICAL);

    LinearLayout.LayoutParams containerParams =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    if (categoryList.getChildCount() > 0) {
      containerParams.topMargin = UiComponentStyles.dp(context, 14);
    }
    container.setLayoutParams(containerParams);

    LinearLayout labelRow = new LinearLayout(context);
    labelRow.setGravity(Gravity.CENTER_VERTICAL);
    labelRow.setOrientation(LinearLayout.HORIZONTAL);

    TextView name = new TextView(context);
    name.setEllipsize(TextUtils.TruncateAt.END);
    name.setMaxLines(1);
    name.setText(row.getCategoryName());
    name.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
    name.setTextSize(15f);
    name.setTypeface(name.getTypeface(), Typeface.BOLD);
    labelRow.addView(
        name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

    TextView amount = new TextView(context);
    amount.setMaxLines(1);
    amount.setText(format(row.getTotalMinor()));
    amount.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
    amount.setTextSize(14f);
    labelRow.addView(
        amount,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    container.addView(labelRow);

    ProgressBar progress =
        new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
    progress.setIndeterminate(false);
    progress.setMax(CATEGORY_PROGRESS_MAX);
    progress.setProgress(progressFor(row.getTotalMinor(), maxTotalMinor));
    progress.setProgressDrawable(
        ContextCompat.getDrawable(context, R.drawable.bg_summary_category_progress));

    LinearLayout.LayoutParams progressParams =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, UiComponentStyles.dp(context, 6));
    progressParams.topMargin = UiComponentStyles.dp(context, 8);
    container.addView(progress, progressParams);

    return container;
  }

  private View createWalletRow(WalletBalance wallet) {
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
    balance.setText(format(wallet.getBalanceMinor()));
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
    AppServices services = ServicesProvider.get(requireContext());
    selectedWalletId = validWalletIdOrNull(services, selectedWalletId);
    selectedCategoryId = validCategoryIdOrNull(services, selectedCategoryId);

    String walletLabel = walletFilterLabel(services, selectedWalletId);
    String categoryLabel = categoryFilterLabel(services, selectedCategoryId);
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

  private String walletFilterLabel(AppServices services, Long walletId) {
    if (walletId == null) {
      return getString(R.string.summary_all_wallets);
    }
    Wallet wallet = services.walletDao.findById(walletId);
    return wallet == null ? getString(R.string.summary_all_wallets) : wallet.getName();
  }

  private String categoryFilterLabel(AppServices services, Long categoryId) {
    if (categoryId == null) {
      return getString(R.string.summary_all_categories);
    }
    Category category = services.categoryDao.findById(categoryId);
    return category == null ? getString(R.string.summary_all_categories) : category.getName();
  }

  private Long validWalletIdOrNull(AppServices services, Long walletId) {
    return walletId != null && services.walletDao.findById(walletId) == null ? null : walletId;
  }

  private Long validCategoryIdOrNull(AppServices services, Long categoryId) {
    return categoryId != null && services.categoryDao.findById(categoryId) == null
        ? null
        : categoryId;
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

  private static DateRange defaultRange() {
    LocalDate now = LocalDate.now();
    return new DateRange(now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()));
  }

  private long totalWalletBalance(MonthlySummary summary) {
    long total = 0L;
    for (WalletBalance wallet : summary.getWalletBalances()) {
      total += wallet.getBalanceMinor();
    }
    return total;
  }

  private long maxCategoryTotal(MonthlySummary summary) {
    long max = 0L;
    for (CategoryTotal category : summary.getTopExpenseCategories()) {
      max = Math.max(max, category.getTotalMinor());
    }
    return max;
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
}
