package com.dwlhm.finan.ui.history;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.HistoryPageCursor;
import com.dwlhm.finan.domain.model.HistoryQuery;
import com.dwlhm.finan.domain.model.HistorySearch;
import com.dwlhm.finan.domain.model.HistoryTotals;
import com.dwlhm.finan.domain.model.PageResult;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.transaction.TransactionSearchResolver;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.DebouncedTextWatcher;
import com.dwlhm.finan.ui.common.EntityLookup;
import com.dwlhm.finan.ui.common.FilterDialog;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.UiComponentStyles;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollListView;
import com.dwlhm.finan.ui.transaction.TransactionDetailDialog;
import com.dwlhm.finan.ui.transaction.TransactionRecyclerAdapter;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class HistoryFragment extends ScreenFragment {

  private static final String START_DATE_STATE_KEY = "history_start_date";
  private static final String END_DATE_STATE_KEY = "history_end_date";
  private static final String DATE_RANGE_ALL_STATE_KEY = "history_date_range_all";
  private static final String WALLET_FILTER_STATE_KEY = "history_wallet_filter";
  private static final String CATEGORY_FILTER_STATE_KEY = "history_category_filter";
  private static final String TYPE_FILTER_STATE_KEY = "history_type_filter";
  private static final String SORT_STATE_KEY = "history_sort";
  private static final String SEARCH_STATE_KEY = "history_search";
  private static final long SEARCH_DEBOUNCE_MS = 300L;
  private static final long FILTER_NONE_ID = -1L;
  private static final long TYPE_EXPENSE_ID = 1L;
  private static final long TYPE_INCOME_ID = 2L;
  private static final long SORT_OLDEST_ID = 1L;

  private AppServices services;
  private InfiniteScrollListView historyList;
  private TransactionRecyclerAdapter adapter;
  private View emptyView;
  private ImageButton filterButton;
  private ImageButton searchClearButton;
  private EditText searchInput;
  private TextView dateRangeView;
  private TextView countView;
  private TextView totalBalanceView;
  private TextView incomeTotalView;
  private TextView expenseTotalView;
  private TextView emptyTitleView;
  private TextView emptyHintView;
  private LocalDate selectedStartDate;
  private LocalDate selectedEndDate;
  private Long selectedWalletId;
  private Long selectedCategoryId;
  private Long selectedTypeId;
  private Long selectedSortId;
  private String searchQuery = "";
  private HistoryQuery activeQuery =
      new HistoryQuery(null, null, null, null, null, false, HistorySearch.empty());
  private DebouncedTextWatcher searchWatcher;
  private volatile HistorySources historySources;
  private int reloadGeneration;
  private Map<Long, Category> categoriesById = Map.of();
  private Map<Long, Wallet> walletsById = Map.of();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    services = ServicesProvider.get(requireContext());
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_history;
  }

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    DateRange restoredRange = restoreDateRange(savedInstanceState);
    selectedStartDate = restoredRange.start;
    selectedEndDate = restoredRange.end;
    selectedWalletId = restoreFilterId(savedInstanceState, WALLET_FILTER_STATE_KEY);
    selectedCategoryId = restoreFilterId(savedInstanceState, CATEGORY_FILTER_STATE_KEY);
    selectedTypeId = restoreFilterId(savedInstanceState, TYPE_FILTER_STATE_KEY);
    selectedSortId = restoreFilterId(savedInstanceState, SORT_STATE_KEY);
    searchQuery =
        savedInstanceState == null ? "" : savedInstanceState.getString(SEARCH_STATE_KEY, "");
    historyList = view.findViewById(R.id.history_list);
    emptyView = view.findViewById(R.id.history_empty);
    filterButton = view.findViewById(R.id.history_filter_button);
    searchInput = view.findViewById(R.id.history_search_input);
    searchClearButton = view.findViewById(R.id.history_search_clear);
    dateRangeView = view.findViewById(R.id.history_date_range);
    countView = view.findViewById(R.id.history_count);
    totalBalanceView = view.findViewById(R.id.history_total_balance);
    incomeTotalView = view.findViewById(R.id.history_income_total);
    expenseTotalView = view.findViewById(R.id.history_expense_total);
    emptyTitleView = view.findViewById(R.id.history_empty_title);
    emptyHintView = view.findViewById(R.id.history_empty_hint);
    filterButton.setOnClickListener(v -> showHistoryFilterDialog());
    dateRangeView.setOnClickListener(v -> showDateRangeDialog());
    
    View thisMonthChip = view.findViewById(R.id.history_chip_this_month);
    View lastMonthChip = view.findViewById(R.id.history_chip_last_month);
    
    thisMonthChip.setOnClickListener(v -> {
        LocalDate today = LocalDate.now();
        selectedStartDate = today.withDayOfMonth(1);
        selectedEndDate = today.withDayOfMonth(today.lengthOfMonth());
        updateDateRangeView();
        reload();
    });
    
    lastMonthChip.setOnClickListener(v -> {
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);
        selectedStartDate = lastMonth.withDayOfMonth(1);
        selectedEndDate = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
        updateDateRangeView();
        reload();
    });
    
    searchInput.setText(searchQuery);
    searchInput.setSelection(searchInput.length());
    searchWatcher =
        new DebouncedTextWatcher(
            SEARCH_DEBOUNCE_MS,
            value -> {
              String normalized = value.trim();
              if (!normalized.equals(searchQuery)) {
                searchQuery = normalized;
                updateSearchControls();
                reload(false);
              }
            });
    searchInput.addTextChangedListener(searchWatcher);
    searchClearButton.setOnClickListener(
        v -> {
          searchInput.setText("");
          searchWatcher.cancel();
          if (!searchQuery.isEmpty()) {
            searchQuery = "";
            updateSearchControls();
            reload(false);
          }
        });
    adapter = new TransactionRecyclerAdapter(requireContext());
    adapter.setOnTransactionClickListener(
        (transaction, position) -> openTransactionDetail(position));
    historyList.setup(adapter, this::loadHistoryPage, services.dbWorker);
    normalizeDateRange();
    updateDateRangeView();
    updateSearchControls();
  }

  @Override
  public void onDestroyView() {
    reloadGeneration++;
    if (searchWatcher != null) {
      searchWatcher.cancel();
    }
    super.onDestroyView();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putBoolean(
        DATE_RANGE_ALL_STATE_KEY, selectedStartDate == null || selectedEndDate == null);
    if (selectedStartDate != null) {
      outState.putString(START_DATE_STATE_KEY, selectedStartDate.toString());
    }
    if (selectedEndDate != null) {
      outState.putString(END_DATE_STATE_KEY, selectedEndDate.toString());
    }
    outState.putLong(
        WALLET_FILTER_STATE_KEY, selectedWalletId == null ? FILTER_NONE_ID : selectedWalletId);
    outState.putLong(
        CATEGORY_FILTER_STATE_KEY, selectedCategoryId == null ? FILTER_NONE_ID : selectedCategoryId);
    outState.putLong(
        TYPE_FILTER_STATE_KEY, selectedTypeId == null ? FILTER_NONE_ID : selectedTypeId);
    outState.putLong(
        SORT_STATE_KEY, selectedSortId == null ? FILTER_NONE_ID : selectedSortId);
    outState.putString(
        SEARCH_STATE_KEY,
        searchInput == null ? searchQuery : searchInput.getText().toString().trim());
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onResume() {
    super.onResume();
    updateDateRangeView();
    reload(true);
  }

  public void setCategoryFilter(long categoryId) {
    selectedCategoryId = categoryId > 0L ? categoryId : null;
    if (getView() != null) {
      reload(true);
    }
  }

  private void reload() {
    reload(false);
  }

  private void reload(boolean refreshSources) {
    normalizeDateRange();
    int generation = ++reloadGeneration;
    Long requestWalletId = selectedWalletId;
    Long requestCategoryId = selectedCategoryId;
    Long requestTypeId = selectedTypeId;
    Long requestSortId = selectedSortId;
    Long startMillis = selectedStartMillis();
    Long endExclusiveMillis = selectedEndExclusiveMillis();
    String requestSearchQuery = searchQuery;
    HistorySources requestSources = refreshSources ? null : historySources;

    services.dbWorker.compute(
        () -> {
          HistorySources sources = requestSources;
          if (sources == null && !refreshSources) {
            sources = historySources;
          }
          if (sources == null) {
            sources = loadHistorySources();
            historySources = sources;
          }
          Long walletId = validWalletIdOrNull(requestWalletId, sources.walletsById);
          Long categoryId = validCategoryIdOrNull(requestCategoryId, sources.categoriesById);
          Long typeId = validTypeIdOrNull(requestTypeId);
          Long sortId = validSortIdOrNull(requestSortId);
          TransactionType type = transactionTypeFor(typeId);
          HistorySearch search = sources.searchResolver.resolve(requestSearchQuery);
          HistoryQuery query =
              new HistoryQuery(
                  walletId,
                  categoryId,
                  type,
                  startMillis,
                  endExclusiveMillis,
                  Objects.equals(sortId, SORT_OLDEST_ID),
                  search);
          HistoryTotals totals = services.transactionGateway.findHistoryTotals(query);
          return new HistoryReloadData(
              sources,
              walletId,
              categoryId,
              typeId,
              sortId,
              query,
              totals);
        },
        data -> {
          if (!isAdded() || generation != reloadGeneration || data == null) {
            return;
          }
          historySources = data.sources;
          walletsById = data.sources.walletsById;
          categoriesById = data.sources.categoriesById;
          selectedWalletId = data.walletId;
          selectedCategoryId = data.categoryId;
          selectedTypeId = data.typeId;
          selectedSortId = data.sortId;
          activeQuery = data.query;
          adapter.setEntityLookups(
              categoriesById,
              walletsById,
              data.sources.tagsById,
              data.sources.merchantsById);
          renderSummary(data.totals);
          historyList.reload();
          updateEmptyState(data.totals.getCount() == 0);
          updateFilterButton();
        });
  }

  private HistorySources loadHistorySources() {
    List<Wallet> wallets = services.walletDao.findAll();
    List<Category> categories = services.categoryDao.findAllOrdered();
    List<com.dwlhm.finan.data.entity.Tag> tags = services.tagDao.findAllOrderByUsage();
    List<com.dwlhm.finan.data.entity.Merchant> merchants =
        services.merchantDao.findAllOrderByUsage();
    return new HistorySources(
        EntityLookup.indexWallets(wallets),
        EntityLookup.indexCategories(categories),
        EntityLookup.indexTags(tags),
        EntityLookup.indexMerchants(merchants),
        new TransactionSearchResolver(wallets, categories, merchants, tags));
  }

  private PageResult<Transaction, HistoryPageCursor> loadHistoryPage(
      @Nullable HistoryPageCursor cursor) {
    return services.transactionGateway.findHistoryPage(
        activeQuery, cursor, historyList.getPageSize());
  }

  private void updateEmptyState(boolean empty) {
    boolean searching = !activeQuery.search().isEmpty();
    emptyTitleView.setText(searching ? R.string.history_search_empty : R.string.history_empty);
    emptyHintView.setText(
        searching ? R.string.history_search_empty_hint : R.string.history_empty_hint);
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    historyList.setVisibility(empty ? View.GONE : View.VISIBLE);
  }

  private void updateSearchControls() {
    if (searchClearButton != null) {
      searchClearButton.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
    }
  }

  private void openTransactionDetail(int position) {
    new TransactionDetailDialog(
            requireContext(),
            services,
            adapter.getTransactionAt(position),
            () -> reload(true))
        .show();
  }

  private void showHistoryFilterDialog() {
    services.dbWorker.compute(
        () -> {
          List<Wallet> wallets = services.walletDao.findAll();
          List<Category> categories = services.categoryDao.findAllOrdered();
          return new FilterSourceData(wallets, categories);
        },
        data -> {
          if (!isAdded() || data == null) {
            return;
          }
          Long walletId = validWalletIdOrNull(selectedWalletId, EntityLookup.indexWallets(data.wallets));
          Long categoryId =
              validCategoryIdOrNull(
                  selectedCategoryId, EntityLookup.indexCategories(data.categories));
          Long typeId = validTypeIdOrNull(selectedTypeId);
          Long sortId = validSortIdOrNull(selectedSortId);

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
          groups.add(
              new FilterDialog.Group(
                  getString(R.string.history_type_filter_label),
                  getString(R.string.history_type_filter_title),
                  typeFilterOptions(),
                  typeId));
          groups.add(
              new FilterDialog.Group(
                  getString(R.string.history_sort_label),
                  getString(R.string.history_sort_title),
                  sortOptions(),
                  sortId));

          FilterDialog.show(
              requireContext(),
              getString(R.string.history_filter_title),
              getString(R.string.summary_range_apply),
              getString(R.string.summary_filter_reset),
              groups,
              selectedIds -> {
                selectedWalletId = selectedIds.get(0);
                selectedCategoryId = selectedIds.get(1);
                selectedTypeId = selectedIds.get(2);
                selectedSortId = selectedIds.get(3);
                reload();
              },
              () -> {
                selectedWalletId = null;
                selectedCategoryId = null;
                selectedTypeId = null;
                selectedSortId = null;
                reload();
              });
        });
  }

  private void showDateRangeDialog() {
    Context context = requireContext();
    LocalDate defaultEnd = LocalDate.now();
    LocalDate defaultStart = defaultEnd.withDayOfMonth(1);
    LocalDate[] pendingStart = {selectedStartDate};
    LocalDate[] pendingEnd = {selectedEndDate};

    LinearLayout content = new LinearLayout(context);
    content.setOrientation(LinearLayout.VERTICAL);
    int horizontalPadding = UiComponentStyles.dp(context, 20);
    int topPadding = UiComponentStyles.dp(context, 6);
    content.setPadding(horizontalPadding, topPadding, horizontalPadding, 0);

    TextView startValue =
        createDateValue(context, pendingStart[0], getString(R.string.history_date_start_placeholder));
    TextView endValue =
        createDateValue(context, pendingEnd[0], getString(R.string.history_date_end_placeholder));
    content.addView(createDateRow(context, R.string.summary_range_start_label, startValue));
    content.addView(createDateRow(context, R.string.summary_range_end_label, endValue));

    startValue.setOnClickListener(
        v ->
            showDatePicker(
                R.string.summary_range_start_label,
                datePickerInitialDate(pendingStart[0], pendingEnd[0], defaultStart),
                selected -> {
                  pendingStart[0] = selected;
                  if (pendingEnd[0] == null || pendingEnd[0].isBefore(selected)) {
                    pendingEnd[0] = selected;
                  }
                  setDateValue(
                      startValue,
                      pendingStart[0],
                      getString(R.string.history_date_start_placeholder));
                  setDateValue(
                      endValue,
                      pendingEnd[0],
                      getString(R.string.history_date_end_placeholder));
                }));
    endValue.setOnClickListener(
        v ->
            showDatePicker(
                R.string.summary_range_end_label,
                datePickerInitialDate(pendingEnd[0], pendingStart[0], defaultEnd),
                selected -> {
                  pendingEnd[0] = selected;
                  if (pendingStart[0] == null || pendingStart[0].isAfter(selected)) {
                    pendingStart[0] = selected;
                  }
                  setDateValue(
                      startValue,
                      pendingStart[0],
                      getString(R.string.history_date_start_placeholder));
                  setDateValue(
                      endValue,
                      pendingEnd[0],
                      getString(R.string.history_date_end_placeholder));
                }));

    new AlertDialog.Builder(context)
        .setTitle(R.string.history_date_range_title)
        .setView(content)
        .setNeutralButton(
            R.string.history_date_range_all,
            (dialog, which) -> {
              selectedStartDate = null;
              selectedEndDate = null;
              updateDateRangeView();
              reload();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(
            R.string.summary_range_apply,
            (dialog, which) -> {
              selectedStartDate = pendingStart[0];
              selectedEndDate = pendingEnd[0];
              normalizeDateRange();
              updateDateRangeView();
              reload();
            })
        .show();
  }

  private void renderSummary(HistoryTotals totals) {
    int count = totals.getCount();
    countView.setText(
        getResources().getQuantityString(R.plurals.history_count, count, count));
    
    long netBalance = totals.getIncomeMinor() - totals.getExpenseMinor();
    totalBalanceView.setText(MoneyFormatter.format(netBalance));
    
    incomeTotalView.setText(MoneyFormatter.format(totals.getIncomeMinor()));
    expenseTotalView.setText(MoneyFormatter.format(totals.getExpenseMinor()));
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

  private List<FilterDialog.Option> typeFilterOptions() {
    ArrayList<FilterDialog.Option> options = new ArrayList<>();
    options.add(new FilterDialog.Option(null, getString(R.string.history_all_types)));
    options.add(new FilterDialog.Option(TYPE_EXPENSE_ID, getString(R.string.capture_type_expense)));
    options.add(new FilterDialog.Option(TYPE_INCOME_ID, getString(R.string.capture_type_income)));
    return options;
  }

  private List<FilterDialog.Option> sortOptions() {
    ArrayList<FilterDialog.Option> options = new ArrayList<>();
    options.add(new FilterDialog.Option(null, getString(R.string.history_sort_newest)));
    options.add(new FilterDialog.Option(SORT_OLDEST_ID, getString(R.string.history_sort_oldest)));
    return options;
  }

  private void updateFilterButton() {
    if (filterButton == null) {
      return;
    }
    String walletLabel = walletFilterLabel(selectedWalletId);
    String categoryLabel = categoryFilterLabel(selectedCategoryId);
    String typeLabel = typeFilterLabel(selectedTypeId);
    String sortLabel = sortLabel(selectedSortId);
    boolean active =
        selectedWalletId != null
            || selectedCategoryId != null
            || selectedTypeId != null
            || selectedSortId != null;
    filterButton.setAlpha(active ? 1f : 0.82f);
    filterButton.setColorFilter(
        ContextCompat.getColor(
            requireContext(), active ? R.color.finan_warm_accent : R.color.finan_primary));
    filterButton.setContentDescription(
        active
            ? getString(
                R.string.history_filter_active_content_description,
                walletLabel,
                categoryLabel,
                typeLabel,
                sortLabel)
            : getString(R.string.history_filter_content_description));
    filterButton.setTooltipText(filterButton.getContentDescription());
  }

  private void updateDateRangeView() {
    if (dateRangeView == null) {
      return;
    }
    normalizeDateRange();
    String label =
        selectedStartDate == null || selectedEndDate == null
            ? getString(R.string.history_date_range_all)
            : formatRangeLabel(selectedStartDate, selectedEndDate);
    dateRangeView.setText(label);
    dateRangeView.setContentDescription(
        getString(R.string.history_date_range_content_description) + ": " + label);
    dateRangeView.setTooltipText(dateRangeView.getContentDescription());
  }

  private LinearLayout createDateRow(
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

  private TextView createDateValue(
      Context context, @Nullable LocalDate date, String placeholder) {
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
        UiComponentStyles.dp(context, 12), 0, UiComponentStyles.dp(context, 12), 0);
    value.setHintTextColor(ContextCompat.getColor(context, R.color.finan_text_hint));
    setDateValue(value, date, placeholder);
    value.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
    value.setTextSize(15f);
    return value;
  }

  private void setDateValue(TextView value, @Nullable LocalDate date, String placeholder) {
    value.setHint(placeholder);
    value.setText(date == null ? "" : formatDateLabel(date));
  }

  private LocalDate datePickerInitialDate(
      @Nullable LocalDate selectedDate, @Nullable LocalDate fallbackDate, LocalDate defaultDate) {
    if (selectedDate != null) {
      return selectedDate;
    }
    return fallbackDate == null ? defaultDate : fallbackDate;
  }

  private void showDatePicker(
      @StringRes int titleRes, LocalDate initialDate, DateSelectionListener listener) {
    DatePickerDialog dialog =
        new DatePickerDialog(
            requireContext(),
            (picker, year, monthOfYear, dayOfMonth) ->
                listener.onDateSelected(LocalDate.of(year, monthOfYear + 1, dayOfMonth)),
            initialDate.getYear(),
            initialDate.getMonthValue() - 1,
            initialDate.getDayOfMonth());
    dialog.setTitle(titleRes);
    dialog.show();
  }

  private void normalizeDateRange() {
    if (selectedStartDate == null || selectedEndDate == null) {
      selectedStartDate = null;
      selectedEndDate = null;
      return;
    }
    if (selectedStartDate.isAfter(selectedEndDate)) {
      LocalDate swap = selectedStartDate;
      selectedStartDate = selectedEndDate;
      selectedEndDate = swap;
    }
  }

  private Long selectedStartMillis() {
    if (selectedStartDate == null) {
      return null;
    }
    return selectedStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  private Long selectedEndExclusiveMillis() {
    if (selectedEndDate == null) {
      return null;
    }
    return selectedEndDate
        .plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli();
  }

  private Long restoreFilterId(@Nullable Bundle savedInstanceState, String key) {
    if (savedInstanceState == null) {
      return null;
    }
    long value = savedInstanceState.getLong(key, FILTER_NONE_ID);
    return value == FILTER_NONE_ID ? null : value;
  }

  private LocalDate restoreDate(@Nullable Bundle savedInstanceState, String key) {
    if (savedInstanceState == null) {
      return null;
    }
    String rawDate = savedInstanceState.getString(key);
    if (rawDate == null) {
      return null;
    }
    try {
      return LocalDate.parse(rawDate);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  private DateRange restoreDateRange(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      return defaultDateRange();
    }
    if (savedInstanceState.getBoolean(DATE_RANGE_ALL_STATE_KEY, false)) {
      return new DateRange(null, null);
    }

    LocalDate startDate = restoreDate(savedInstanceState, START_DATE_STATE_KEY);
    LocalDate endDate = restoreDate(savedInstanceState, END_DATE_STATE_KEY);
    if (startDate == null || endDate == null) {
      return defaultDateRange();
    }
    return new DateRange(startDate, endDate);
  }

  private DateRange defaultDateRange() {
    LocalDate today = LocalDate.now();
    return new DateRange(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
  }

  private static Long validWalletIdOrNull(Long walletId, Map<Long, Wallet> walletsById) {
    return walletId != null && !walletsById.containsKey(walletId) ? null : walletId;
  }

  private static Long validCategoryIdOrNull(Long categoryId, Map<Long, Category> categoriesById) {
    return categoryId != null && !categoriesById.containsKey(categoryId) ? null : categoryId;
  }

  private Long validTypeIdOrNull(Long typeId) {
    if (typeId == null || typeId == TYPE_EXPENSE_ID || typeId == TYPE_INCOME_ID) {
      return typeId;
    }
    return null;
  }

  private Long validSortIdOrNull(Long sortId) {
    return sortId == null || sortId == SORT_OLDEST_ID ? sortId : null;
  }

  private static TransactionType transactionTypeFor(Long typeId) {
    if (typeId == null) {
      return null;
    }
    return typeId == TYPE_INCOME_ID ? TransactionType.INCOME : TransactionType.EXPENSE;
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

  private String typeFilterLabel(Long typeId) {
    if (typeId == null) {
      return getString(R.string.history_all_types);
    }
    return typeId == TYPE_INCOME_ID
        ? getString(R.string.capture_type_income)
        : getString(R.string.capture_type_expense);
  }

  private String sortLabel(Long sortId) {
    return Objects.equals(sortId, SORT_OLDEST_ID)
        ? getString(R.string.history_sort_oldest)
        : getString(R.string.history_sort_newest);
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

  private interface DateSelectionListener {
    void onDateSelected(LocalDate date);
  }

  private static final class DateRange {
    private final LocalDate start;
    private final LocalDate end;

    private DateRange(@Nullable LocalDate start, @Nullable LocalDate end) {
      this.start = start;
      this.end = end;
    }
  }

  private static final class HistoryReloadData {
    private final HistorySources sources;
    private final Long walletId;
    private final Long categoryId;
    private final Long typeId;
    private final Long sortId;
    private final HistoryQuery query;
    private final HistoryTotals totals;

    private HistoryReloadData(
        HistorySources sources,
        Long walletId,
        Long categoryId,
        Long typeId,
        Long sortId,
        HistoryQuery query,
        HistoryTotals totals) {
      this.sources = sources;
      this.walletId = walletId;
      this.categoryId = categoryId;
      this.typeId = typeId;
      this.sortId = sortId;
      this.query = query;
      this.totals = totals;
    }
  }

  private static final class HistorySources {
    private final Map<Long, Wallet> walletsById;
    private final Map<Long, Category> categoriesById;
    private final Map<Long, com.dwlhm.finan.data.entity.Tag> tagsById;
    private final Map<Long, com.dwlhm.finan.data.entity.Merchant> merchantsById;
    private final TransactionSearchResolver searchResolver;

    private HistorySources(
        Map<Long, Wallet> walletsById,
        Map<Long, Category> categoriesById,
        Map<Long, com.dwlhm.finan.data.entity.Tag> tagsById,
        Map<Long, com.dwlhm.finan.data.entity.Merchant> merchantsById,
        TransactionSearchResolver searchResolver) {
      this.walletsById = walletsById;
      this.categoriesById = categoriesById;
      this.tagsById = tagsById;
      this.merchantsById = merchantsById;
      this.searchResolver = searchResolver;
    }
  }

  private static final class FilterSourceData {
    private final List<Wallet> wallets;
    private final List<Category> categories;

    private FilterSourceData(List<Wallet> wallets, List<Category> categories) {
      this.wallets = wallets;
      this.categories = categories;
    }
  }
}
