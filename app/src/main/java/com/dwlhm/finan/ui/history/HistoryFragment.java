package com.dwlhm.finan.ui.history;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollListView;
import com.dwlhm.finan.ui.transaction.TransactionDetailDialog;
import com.dwlhm.finan.ui.transaction.TransactionRecyclerAdapter;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class HistoryFragment extends ScreenFragment {

  private static final String MASKED_MODE_PREFS_KEY = "history_masked_mode";
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
  private boolean maskedMode;
  private TextView modeNominal;
  private TextView modeMasked;
  private HistoryTotals cachedTotals;
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
  private boolean isDataLoaded = false;

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
        DateRange range = cycleDateRange(0);
        selectedStartDate = range.start;
        selectedEndDate = range.end;
        updateDateRangeView();
        reload();
    });
    
    lastMonthChip.setOnClickListener(v -> {
        DateRange range = cycleDateRange(-1);
        selectedStartDate = range.start;
        selectedEndDate = range.end;
        updateDateRangeView();
        reload();
    });
    
    modeNominal = view.findViewById(R.id.history_mode_nominal);
    modeMasked = view.findViewById(R.id.history_mode_masked);
    maskedMode = requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
        .getBoolean(MASKED_MODE_PREFS_KEY, false);
    updateModeToggle();
    modeNominal.setOnClickListener(v -> setMaskedMode(false));
    modeMasked.setOnClickListener(v -> setMaskedMode(true));
    
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
    adapter.setMaskedMode(maskedMode);
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
    if (!isDataLoaded) {
      reload(true);
      isDataLoaded = true;
    }
  }

  public void setCategoryFilter(long categoryId) {
    selectedCategoryId = categoryId > 0L ? categoryId : null;
    if (getView() != null) {
      reload(true);
    }
  }

  public void setWalletFilter(long walletId) {
    selectedWalletId = walletId > 0L ? walletId : null;
    if (getView() != null) {
      reload(true);
    }
  }

  public void setTypeFilter(String type) {
    if ("EXPENSE".equals(type)) {
      selectedTypeId = TYPE_EXPENSE_ID;
    } else if ("INCOME".equals(type)) {
      selectedTypeId = TYPE_INCOME_ID;
    } else {
      selectedTypeId = null;
    }
    if (getView() != null) {
      reload(true);
    }
  }

  public void setDateRange(long startMillis, long endMillis) {
    selectedStartDate = java.time.Instant.ofEpochMilli(startMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    selectedEndDate = java.time.Instant.ofEpochMilli(endMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    if (getView() != null) {
      updateDateRangeView();
      reload(true);
    }
  }

  public void setActivityFilter(String activity) {
    selectedCategoryId = null;
    selectedTypeId = null;
    if (getView() != null) {
      updateDateRangeView();
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
              walletsById);
          renderSummary(data.totals);
          historyList.reload();
          updateEmptyState(data.totals.getCount() == 0);
          updateFilterButton();
        });
  }

  private HistorySources loadHistorySources() {
    List<Wallet> wallets = services.walletService.findAll();
    List<Category> categories = services.categoryDao.findAllOrdered();
    return new HistorySources(
        wallets,
        categories,
        EntityLookup.indexWallets(wallets),
        EntityLookup.indexCategories(categories),
        new TransactionSearchResolver(wallets, categories));
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
    if (historySources != null) {
      showFilterDialogWithData(historySources.wallets, historySources.categories);
      return;
    }
    services.dbWorker.compute(
        () -> {
          List<Wallet> wallets = services.walletService.findAll();
          List<Category> categories = services.categoryDao.findAllOrdered();
          return new FilterSourceData(wallets, categories);
        },
        data -> {
          if (!isAdded() || data == null) {
            return;
          }
          showFilterDialogWithData(data.wallets, data.categories);
        });
  }

  private void showFilterDialogWithData(List<Wallet> wallets, List<Category> categories) {
    Long walletId = validWalletIdOrNull(selectedWalletId, EntityLookup.indexWallets(wallets));
    Long categoryId =
        validCategoryIdOrNull(
            selectedCategoryId, EntityLookup.indexCategories(categories));
    Long typeId = validTypeIdOrNull(selectedTypeId);
    Long sortId = validSortIdOrNull(selectedSortId);

    HistoryFilterBottomSheet sheet =
        new HistoryFilterBottomSheet(
            requireContext(),
            wallets,
            categories,
            walletId,
            categoryId,
            typeId,
            sortId,
            (selectedWalletId1, selectedCategoryId1, selectedTypeId1, selectedSortId1) -> {
              selectedWalletId = selectedWalletId1;
              selectedCategoryId = selectedCategoryId1;
              selectedTypeId = selectedTypeId1;
              selectedSortId = selectedSortId1;
              reload();
            },
            () -> {
              selectedWalletId = null;
              selectedCategoryId = null;
              selectedTypeId = null;
              selectedSortId = null;
              reload();
            });
    sheet.show();
  }

  private void showDateRangeDialog() {
    HistoryDateRangeBottomSheet sheet =
        new HistoryDateRangeBottomSheet(
            requireContext(),
            selectedStartDate,
            selectedEndDate,
            (start, end) -> {
              selectedStartDate = start;
              selectedEndDate = end;
              normalizeDateRange();
              updateDateRangeView();
              reload();
            });
    sheet.show();
  }

  private void renderSummary(HistoryTotals totals) {
    cachedTotals = totals;
    int count = totals.getCount();
    countView.setText(
        getResources().getQuantityString(R.plurals.history_count, count, count));
    
    if (maskedMode) {
      totalBalanceView.setText("Rp ***");
      incomeTotalView.setText("Rp ***");
      expenseTotalView.setText("Rp ***");
    } else {
      long netBalance = totals.getIncomeMinor() - totals.getExpenseMinor();
      totalBalanceView.setText(MoneyFormatter.format(netBalance));
      incomeTotalView.setText(MoneyFormatter.format(totals.getIncomeMinor()));
      expenseTotalView.setText(MoneyFormatter.format(totals.getExpenseMinor()));
    }
  }

  private void setMaskedMode(boolean masked) {
    maskedMode = masked;
    requireContext().getSharedPreferences("finan_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean(MASKED_MODE_PREFS_KEY, masked)
        .apply();
    updateModeToggle();
    if (cachedTotals != null) {
      renderSummary(cachedTotals);
    }
    adapter.setMaskedMode(masked);
  }

  private void updateModeToggle() {
    if (modeNominal == null || modeMasked == null) return;
    if (maskedMode) {
      modeNominal.setBackground(null);
      modeNominal.setTextColor(0xFF4A9E7F);
      modeMasked.setBackgroundResource(R.drawable.bg_toggle_active);
      modeMasked.setTextColor(0xFFFFFFFF);
    } else {
      modeNominal.setBackgroundResource(R.drawable.bg_toggle_active);
      modeNominal.setTextColor(0xFFFFFFFF);
      modeMasked.setBackground(null);
      modeMasked.setTextColor(0xFF4A9E7F);
    }
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
    return cycleDateRange(0);
  }

  private DateRange cycleDateRange(int monthOffset) {
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
    LocalDate nextCutoff;
    if (cutoffDay == -1) {
      LocalDate thisMonthCutoff = lastBusinessDayOfMonth(date);
      if (date.isBefore(thisMonthCutoff)) {
        start = lastBusinessDayOfMonth(date.minusMonths(1));
        nextCutoff = thisMonthCutoff;
      } else {
        start = thisMonthCutoff;
        nextCutoff = lastBusinessDayOfMonth(date.plusMonths(1));
      }
    } else {
      if (date.getDayOfMonth() >= cutoffDay) {
        start = date.withDayOfMonth(cutoffDay);
        nextCutoff = date.plusMonths(1).withDayOfMonth(cutoffDay);
      } else {
        start = date.minusMonths(1).withDayOfMonth(cutoffDay);
        nextCutoff = date.withDayOfMonth(cutoffDay);
      }
    }
    return new DateRange(start, nextCutoff.minusDays(1));
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
    private final List<Wallet> wallets;
    private final List<Category> categories;
    private final Map<Long, Wallet> walletsById;
    private final Map<Long, Category> categoriesById;
    private final TransactionSearchResolver searchResolver;

    private HistorySources(
        List<Wallet> wallets,
        List<Category> categories,
        Map<Long, Wallet> walletsById,
        Map<Long, Category> categoriesById,
        TransactionSearchResolver searchResolver) {
      this.wallets = wallets;
      this.categories = categories;
      this.walletsById = walletsById;
      this.categoriesById = categoriesById;
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
