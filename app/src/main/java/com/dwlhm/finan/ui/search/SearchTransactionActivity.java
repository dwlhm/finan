package com.dwlhm.finan.ui.search;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollListView;
import com.dwlhm.finan.ui.history.HistoryDateRangeBottomSheet;
import com.dwlhm.finan.ui.history.HistoryFilterBottomSheet;
import com.dwlhm.finan.ui.transaction.TransactionDetailDialog;
import com.dwlhm.finan.ui.transaction.TransactionRecyclerAdapter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SearchTransactionActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY = "EXTRA_QUERY";

    private static final long SEARCH_DEBOUNCE_MS = 500L;
    private static final long FILTER_NONE_ID = -1L;
    private static final long TYPE_EXPENSE_ID = 1L;
    private static final long TYPE_INCOME_ID = 2L;
    private static final long SORT_OLDEST_ID = 1L;

    private AppServices services;
    private InfiniteScrollListView searchList;
    private TransactionRecyclerAdapter adapter;
    private View emptyView;
    private ImageButton filterButton;
    private TextView filterBadge;
    private ImageButton searchClearButton;
    private ImageButton backButton;
    private EditText searchInput;
    private TextView dateRangeView;
    private TextView chipTypeAll;
    private TextView chipTypeExpense;
    private TextView chipTypeIncome;
    private TextView emptyTitleView;
    private TextView emptyHintView;
    private View emptyResetButton;
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        services = ServicesProvider.get(this);
        setContentView(R.layout.activity_search_transaction);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_QUERY)) {
            searchQuery = getIntent().getStringExtra(EXTRA_QUERY);
        }

        searchList = findViewById(R.id.search_list);
        emptyView = findViewById(R.id.search_empty);
        filterButton = findViewById(R.id.search_filter_button);
        filterBadge = findViewById(R.id.search_filter_badge);
        searchInput = findViewById(R.id.search_input);
        searchClearButton = findViewById(R.id.search_clear);
        backButton = findViewById(R.id.search_back_button);
        dateRangeView = findViewById(R.id.search_date_range);
        chipTypeAll = findViewById(R.id.chip_type_all);
        chipTypeExpense = findViewById(R.id.chip_type_expense);
        chipTypeIncome = findViewById(R.id.chip_type_income);
        emptyTitleView = findViewById(R.id.search_empty_title);
        emptyHintView = findViewById(R.id.search_empty_hint);
        emptyResetButton = findViewById(R.id.search_empty_reset);

        backButton.setOnClickListener(v -> finish());
        filterButton.setOnClickListener(v -> showFilterDialog());
        dateRangeView.setOnClickListener(v -> showDateRangeDialog());

        if (chipTypeAll != null) {
            chipTypeAll.setOnClickListener(v -> selectTypeFilter(null));
        }
        if (chipTypeExpense != null) {
            chipTypeExpense.setOnClickListener(v -> selectTypeFilter(TYPE_EXPENSE_ID));
        }
        if (chipTypeIncome != null) {
            chipTypeIncome.setOnClickListener(v -> selectTypeFilter(TYPE_INCOME_ID));
        }
        if (emptyResetButton != null) {
            emptyResetButton.setOnClickListener(v -> resetAllFilters());
        }

        searchInput.requestFocus();
        searchInput.post(() -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
        searchInput.setText(searchQuery);
        searchInput.setSelection(searchInput.length());
        searchWatcher = new DebouncedTextWatcher(SEARCH_DEBOUNCE_MS, value -> {
            String normalized = value.trim();
            if (!normalized.equals(searchQuery)) {
                searchQuery = normalized;
                updateSearchControls();
                reload(false);
            }
        });
        searchInput.addTextChangedListener(searchWatcher);
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                String query = searchInput.getText().toString().trim();
                if (!query.equals(searchQuery)) {
                    searchQuery = query;
                    updateSearchControls();
                    reload(false);
                }
                return true;
            }
            return false;
        });

        searchClearButton.setOnClickListener(v -> {
            searchInput.setText("");
            searchWatcher.cancel();
            if (!searchQuery.isEmpty()) {
                searchQuery = "";
                updateSearchControls();
                reload(false);
            }
        });

        adapter = new TransactionRecyclerAdapter(this);
        // Using nominal display mode for search results
        adapter.setDisplayMode(com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.NOMINAL, 0L);
        adapter.setOnTransactionClickListener((transaction, position) -> openTransactionDetail(position));
        searchList.setup(adapter, this::loadSearchPage, services.dbWorker);

        com.dwlhm.finan.ui.transaction.StickyHeaderItemDecoration stickyDecoration =
                new com.dwlhm.finan.ui.transaction.StickyHeaderItemDecoration(adapter);
        searchList.getRecyclerView().addItemDecoration(stickyDecoration);

        View headerContainer = findViewById(R.id.header_container);
        if (headerContainer != null) {
            headerContainer.post(() -> {
                int height = headerContainer.getHeight();
                androidx.recyclerview.widget.RecyclerView rv = searchList.getRecyclerView();
                rv.setPadding(rv.getPaddingLeft(), height, rv.getPaddingRight(), rv.getPaddingBottom());
                stickyDecoration.setStickyYOffset(height);
            });
        }

        normalizeDateRange();
        updateDateRangeView();
        updateSearchControls();
        reload(true);
    }

    @Override
    protected void onDestroy() {
        reloadGeneration++;
        if (searchWatcher != null) {
            searchWatcher.cancel();
        }
        super.onDestroy();
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
                    if (isDestroyed() || generation != reloadGeneration || data == null) {
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
                    adapter.setEntityLookups(categoriesById, walletsById);
                    searchList.reload();
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

    private PageResult<Transaction, HistoryPageCursor> loadSearchPage(@Nullable HistoryPageCursor cursor) {
        return services.transactionGateway.findHistoryPage(
                activeQuery, cursor, searchList.getPageSize());
    }

    private void updateEmptyState(boolean empty) {
        if (empty && emptyView.getVisibility() != View.VISIBLE) {
            emptyView.setAlpha(0f);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.animate().alpha(1f).setDuration(200).setListener(null);
            searchList.animate().alpha(0f).setDuration(200).setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    searchList.setVisibility(View.GONE);
                }
            });
        } else if (!empty && searchList.getVisibility() != View.VISIBLE) {
            searchList.setAlpha(0f);
            searchList.setVisibility(View.VISIBLE);
            searchList.animate().alpha(1f).setDuration(200).setListener(null);
            emptyView.animate().alpha(0f).setDuration(200).setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    emptyView.setVisibility(View.GONE);
                }
            });
        }
    }

    private void updateSearchControls() {
        if (searchClearButton != null) {
            searchClearButton.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void openTransactionDetail(int position) {
        com.dwlhm.finan.ui.common.BottomSheetHelper.show(new TransactionDetailDialog(
                this,
                services,
                adapter.getTransactionAt(position),
                () -> reload(true)));
    }

    private void showFilterDialog() {
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
                    if (isDestroyed() || data == null) {
                        return;
                    }
                    showFilterDialogWithData(data.wallets, data.categories);
                });
    }

    private void showFilterDialogWithData(List<Wallet> wallets, List<Category> categories) {
        Long walletId = validWalletIdOrNull(selectedWalletId, EntityLookup.indexWallets(wallets));
        Long categoryId = validCategoryIdOrNull(selectedCategoryId, EntityLookup.indexCategories(categories));
        Long typeId = validTypeIdOrNull(selectedTypeId);
        Long sortId = validSortIdOrNull(selectedSortId);

        HistoryFilterBottomSheet sheet = new HistoryFilterBottomSheet(
                this,
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
                    reload(false);
                },
                () -> {
                    selectedWalletId = null;
                    selectedCategoryId = null;
                    selectedTypeId = null;
                    selectedSortId = null;
                    reload(false);
                });
        sheet.show();
    }

    private void showDateRangeDialog() {
        HistoryDateRangeBottomSheet sheet = new HistoryDateRangeBottomSheet(
                this,
                selectedStartDate,
                selectedEndDate,
                (start, end) -> {
                    selectedStartDate = start;
                    selectedEndDate = end;
                    normalizeDateRange();
                    updateDateRangeView();
                    reload(false);
                });
        sheet.show();
    }

    private void selectTypeFilter(@Nullable Long typeId) {
        if (Objects.equals(selectedTypeId, typeId)) {
            return;
        }
        selectedTypeId = typeId;
        updateChipVisualStates();
        reload(false);
    }

    private void updateChipVisualStates() {
        updateChipStyle(chipTypeAll, selectedTypeId == null);
        updateChipStyle(chipTypeExpense, Objects.equals(selectedTypeId, TYPE_EXPENSE_ID));
        updateChipStyle(chipTypeIncome, Objects.equals(selectedTypeId, TYPE_INCOME_ID));
    }

    private void updateChipStyle(@Nullable TextView chip, boolean selected) {
        if (chip == null) return;
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.finan_chip_text_selected));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setTextColor(ContextCompat.getColor(this, R.color.finan_chip_text));
        }
    }

    private void resetAllFilters() {
        searchQuery = "";
        selectedTypeId = null;
        selectedWalletId = null;
        selectedCategoryId = null;
        selectedStartDate = null;
        selectedEndDate = null;
        selectedSortId = null;
        if (searchInput != null) {
            searchInput.setText("");
        }
        if (searchWatcher != null) {
            searchWatcher.cancel();
        }
        updateSearchControls();
        updateDateRangeView();
        updateChipVisualStates();
        reload(false);
    }

    private void updateFilterButton() {
        if (filterButton == null) return;
        int activeCount = 0;
        if (selectedWalletId != null) activeCount++;
        if (selectedCategoryId != null) activeCount++;
        if (selectedTypeId != null) activeCount++;
        if (selectedSortId != null) activeCount++;
        if (selectedStartDate != null || selectedEndDate != null) activeCount++;

        boolean active = activeCount > 0;
        filterButton.setAlpha(active ? 1f : 0.82f);
        filterButton.setColorFilter(
                ContextCompat.getColor(
                        this, active ? R.color.finan_warm_accent : R.color.finan_primary));

        if (filterBadge != null) {
            if (activeCount > 0) {
                filterBadge.setText(String.valueOf(activeCount));
                filterBadge.setVisibility(View.VISIBLE);
            } else {
                filterBadge.setVisibility(View.GONE);
            }
        }
        updateChipVisualStates();
    }

    private void updateDateRangeView() {
        if (dateRangeView == null) return;
        normalizeDateRange();
        String label = selectedStartDate == null || selectedEndDate == null
                ? getString(R.string.history_date_range_all)
                : formatRangeLabel(selectedStartDate, selectedEndDate);
        dateRangeView.setText(label);
    }

    private String formatRangeLabel(LocalDate start, LocalDate end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yy");
        if (start.equals(end)) {
            return start.format(formatter);
        }
        return start.format(formatter) + " - " + end.format(formatter);
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
        if (selectedStartDate == null) return null;
        return selectedStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private Long selectedEndExclusiveMillis() {
        if (selectedEndDate == null) return null;
        return selectedEndDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private Long validWalletIdOrNull(@Nullable Long id, Map<Long, Wallet> index) {
        if (id == null || id == FILTER_NONE_ID || !index.containsKey(id)) return null;
        return id;
    }

    private Long validCategoryIdOrNull(@Nullable Long id, Map<Long, Category> index) {
        if (id == null || id == FILTER_NONE_ID || !index.containsKey(id)) return null;
        return id;
    }

    private Long validTypeIdOrNull(@Nullable Long id) {
        if (id == null || id == FILTER_NONE_ID) return null;
        if (id == TYPE_EXPENSE_ID || id == TYPE_INCOME_ID) return id;
        return null;
    }

    private Long validSortIdOrNull(@Nullable Long id) {
        if (id == null || id == FILTER_NONE_ID) return null;
        return id;
    }

    private TransactionType transactionTypeFor(@Nullable Long typeId) {
        if (typeId == null) return null;
        if (typeId == TYPE_EXPENSE_ID) return TransactionType.EXPENSE;
        if (typeId == TYPE_INCOME_ID) return TransactionType.INCOME;
        return null;
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
