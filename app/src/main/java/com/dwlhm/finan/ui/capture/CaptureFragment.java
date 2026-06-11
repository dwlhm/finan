package com.dwlhm.finan.ui.capture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.data.prefs.DefaultsStore;
import com.dwlhm.finan.data.prefs.TransactionFormDraft;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.transaction.TransactionService;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.CategorySearchDialog;
import com.dwlhm.finan.ui.common.CollapsibleController;
import com.dwlhm.finan.ui.common.EntityLookup;
import com.dwlhm.finan.ui.common.MerchantSelectionController;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.TagSelectionController;
import com.dwlhm.finan.ui.common.TransactionOccurredAtPicker;
import com.dwlhm.finan.ui.common.UiComponentStyles;
import com.dwlhm.finan.ui.transaction.TransactionDetailDialog;
import com.dwlhm.finan.ui.transaction.TransactionListAdapter;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CaptureFragment extends ScreenFragment {

  private static final int RECENT_LIMIT = 8;
  private static final int QUICK_CATEGORY_SLOTS = 4;
  private static final long UNDO_TIMEOUT_MS = 8_000L;

  private AppServices services;
  private TransactionService transactionService;
  private DefaultsStore defaultsStore;

  private EditText amountInput;
  private LinearLayout amountShortcuts;
  private LinearLayout quickCategories;
  private Button categoryMoreButton;
  private Spinner walletSpinner;
  private RadioGroup typeGroup;
  private EditText noteInput;
  private LinearLayout recentList;
  private TextView recentEmpty;

  private Wallet activeWallet;
  private Category selectedCategory;
  private TransactionType selectedType = TransactionType.EXPENSE;
  private TransactionListAdapter recentAdapter;
  private TransactionOccurredAtPicker occurredAtPicker;
  private CaptureFormValidation formValidation;
  private TextView validationBanner;
  private TagSelectionController tagSelection;
  private MerchantSelectionController merchantSelection;
  private View undoBar;
  private final Handler undoHandler = new Handler(Looper.getMainLooper());
  @Nullable private Runnable undoDismissRunnable;
  @Nullable private PendingSaveUndo pendingUndo;

  private List<Long> amountShortcutList = new ArrayList<>();
  private List<Wallet> wallets = new ArrayList<>();
  private List<Category> allCategoriesForType = new ArrayList<>();
  private List<Category> quickCategoryList = new ArrayList<>();
  private boolean suppressWalletSpinner;
  private boolean amountAutoFocusExpired;
  private boolean captureDraftRestored;
  private int refreshGeneration;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    services = ServicesProvider.get(requireContext());
    transactionService = services.transactionService;
    defaultsStore = services.defaultsStore;
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_capture;
  }

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    amountInput = view.findViewById(R.id.capture_amount);
    MoneyInputFormatter.attach(amountInput, true);
    amountShortcuts = view.findViewById(R.id.capture_amount_shortcuts);
    ImageButton amountShortcutsEditButton =
        view.findViewById(R.id.capture_amount_shortcuts_edit);
    quickCategories = view.findViewById(R.id.capture_quick_categories);
    categoryMoreButton = view.findViewById(R.id.capture_category_more);
    walletSpinner = view.findViewById(R.id.capture_wallet_spinner);
    typeGroup = view.findViewById(R.id.capture_type_group);
    noteInput = view.findViewById(R.id.capture_note);
    recentList = view.findViewById(R.id.capture_recent_list);
    recentEmpty = view.findViewById(R.id.capture_recent_empty);
    TextView occurredDateView = view.findViewById(R.id.transaction_occurred_date);
    TextView occurredTimeView = view.findViewById(R.id.transaction_occurred_time);
    occurredAtPicker =
        new TransactionOccurredAtPicker(
            requireContext(), occurredDateView, occurredTimeView, System.currentTimeMillis());
    validationBanner = view.findViewById(R.id.capture_validation_banner);
    formValidation = new CaptureFormValidation(view, validationBanner);
    formValidation.bindAmountClearListener();

    Button saveButton = view.findViewById(R.id.capture_save);

    recentAdapter = new TransactionListAdapter(requireContext());

    typeGroup.setOnCheckedChangeListener(
        (group, checkedId) -> {
          expireAmountAutoFocus();
          selectedType =
              checkedId == R.id.capture_type_income
                  ? TransactionType.INCOME
                  : TransactionType.EXPENSE;
          selectedCategory = null;
          formValidation.clear(CaptureFormValidation.Field.CATEGORY);
          refreshCaptureData(false);
        });

    walletSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
            if (suppressWalletSpinner || position < 0 || position >= wallets.size()) {
              return;
            }
            expireAmountAutoFocus();
            activeWallet = wallets.get(position);
            formValidation.clear(CaptureFormValidation.Field.WALLET);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    categoryMoreButton.setOnClickListener(
        v -> {
          expireAmountAutoFocus();
          openCategorySearchDialog();
        });

    amountShortcutsEditButton.setOnClickListener(
        v -> {
          expireAmountAutoFocus();
          openAmountShortcutDialog();
        });

    CollapsibleController.bind(
        view,
        R.id.capture_details_toggle,
        R.id.capture_details_panel,
        R.id.capture_details_chevron);

    tagSelection =
        new TagSelectionController(
            requireContext(),
            services.tagDao,
            services.dbWorker,
            view.findViewById(R.id.transaction_tag_chips),
            view.findViewById(R.id.transaction_tag_add));
    merchantSelection =
        new MerchantSelectionController(
            requireContext(),
            services.merchantDao,
            services.dbWorker,
            view.findViewById(R.id.transaction_merchant_pick),
            view.findViewById(R.id.transaction_merchant_clear));

    saveButton.setOnClickListener(
        v -> {
          expireAmountAutoFocus();
          saveTransaction();
        });

    undoBar = view.findViewById(R.id.capture_undo_bar);
    Button undoActionButton = view.findViewById(R.id.capture_undo_action);
    undoActionButton.setOnClickListener(
        v -> {
          expireAmountAutoFocus();
          performUndo();
        });

    installAmountAutoFocusExpiry(view);

    bindAmountShortcuts();
    requestAmountFocus();
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshCaptureData(true);
  }

  @Override
  public void onPause() {
    persistCaptureDraft();
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    dismissUndoBar();
    captureDraftRestored = false;
    refreshGeneration++;
    super.onDestroyView();
  }

  private void refreshCaptureData(boolean keepSelectedCategory) {
    int generation = ++refreshGeneration;
    TransactionType loadType = selectedType;
    if (!captureDraftRestored) {
      TransactionFormDraft pendingDraft = defaultsStore.getCaptureDraft();
      if (pendingDraft != null) {
        loadType = pendingDraft.getType();
      }
    }
    final TransactionType typeForLoad = loadType;
    Category retainedCategory = keepSelectedCategory ? selectedCategory : null;
    services.dbWorker.compute(
        () -> loadCaptureState(typeForLoad, retainedCategory),
        state -> {
          if (!isAdded() || generation != refreshGeneration || state == null) {
            return;
          }
          wallets = state.wallets;
          activeWallet = state.activeWallet;
          allCategoriesForType = state.categoriesForType;
          if (!keepSelectedCategory) {
            selectedCategory = state.selectedCategory;
          } else if (selectedCategory != null && !isCategoryInList(selectedCategory, allCategoriesForType)) {
            selectedCategory = null;
          }
          recentAdapter.setEntityLookups(
              state.categoriesById,
              state.walletsById,
              state.tagsById,
              state.merchantsById);
          recentAdapter.setTransactions(state.recentTransactions);
          renderRecentTransactions();
          tryRestoreCaptureDraft();
          bindWalletSpinner();
          bindCategories();
          boolean empty = state.recentTransactions.isEmpty();
          recentEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
          recentList.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
  }

  private CaptureState loadCaptureState(TransactionType type, @Nullable Category retainedCategory) {
    List<Wallet> loadedWallets = services.walletDao.findAll();
    Wallet resolvedWallet = resolveActiveWallet(loadedWallets);
    List<Category> categoriesForType =
        services.categoryDao.findByTypeFilterOrderByUsage(type.name());
    List<Transaction> recent = transactionService.getRecent(RECENT_LIMIT);
    Category selected =
        retainedCategory != null && isCategoryInList(retainedCategory, categoriesForType)
            ? retainedCategory
            : null;
    List<com.dwlhm.finan.data.entity.Tag> allTags = services.tagDao.findAllOrderByUsage();
    List<com.dwlhm.finan.data.entity.Merchant> allMerchants =
        services.merchantDao.findAllOrderByUsage();
    return new CaptureState(
        loadedWallets,
        resolvedWallet,
        categoriesForType,
        selected,
        recent,
        categoryLookupForRecent(categoriesForType, recent),
        EntityLookup.indexWallets(loadedWallets),
        EntityLookup.tagLookupForTransactions(
            allTags, recent, services.tagDao::findById),
        EntityLookup.merchantLookupForTransactions(
            allMerchants, recent, services.merchantDao::findById));
  }

  private Map<Long, Category> categoryLookupForRecent(
      List<Category> categoriesForType, List<Transaction> recent) {
    Map<Long, Category> lookup = new HashMap<>(EntityLookup.indexCategories(categoriesForType));
    for (Transaction transaction : recent) {
      long categoryId = transaction.getCategoryId();
      if (lookup.containsKey(categoryId)) {
        continue;
      }
      Category category = services.categoryDao.findById(categoryId);
      if (category != null) {
        lookup.put(categoryId, category);
      }
    }
    return lookup;
  }

  private Wallet resolveActiveWallet(List<Wallet> loadedWallets) {
    Long lastWalletId = defaultsStore.getLastWalletId();
    if (lastWalletId != null) {
      for (Wallet wallet : loadedWallets) {
        if (wallet.getId() == lastWalletId) {
          return wallet;
        }
      }
    }
    if (defaultsStore.hasDefaultWalletId()) {
      long defaultId = defaultsStore.getDefaultWalletId();
      for (Wallet wallet : loadedWallets) {
        if (wallet.getId() == defaultId) {
          return wallet;
        }
      }
    }
    Wallet defaultWallet = services.walletDao.findDefault();
    if (defaultWallet != null && !defaultsStore.hasDefaultWalletId()) {
      defaultsStore.setDefaultWalletId(defaultWallet.getId());
    }
    return defaultWallet;
  }

  private static boolean isCategoryInList(Category category, List<Category> categories) {
    for (Category option : categories) {
      if (option.getId() == category.getId()) {
        return true;
      }
    }
    return false;
  }

  private void renderRecentTransactions() {
    recentList.removeAllViews();
    for (int position = 0; position < recentAdapter.getCount(); position++) {
      Transaction transaction = recentAdapter.getItem(position);
      View itemView = recentAdapter.getView(position, null, recentList);
      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      if (position > 0) {
        params.topMargin = UiComponentStyles.dp(requireContext(), 8);
      }
      itemView.setLayoutParams(params);
      itemView.setOnClickListener(
          v ->
              new TransactionDetailDialog(
                      requireContext(),
                      services,
                      transaction,
                      () -> refreshCaptureData(true))
                  .show());
      recentList.addView(itemView);
    }
  }

  private void bindWalletSpinner() {
    if (wallets.isEmpty()) {
      walletSpinner.setEnabled(false);
      return;
    }
    walletSpinner.setEnabled(true);

    List<String> labels = new ArrayList<>();
    int selectedIndex = 0;
    for (int i = 0; i < wallets.size(); i++) {
      Wallet wallet = wallets.get(i);
      labels.add(
          wallet.getName()
              + " · "
              + MoneyFormatter.format(wallet.getCachedBalanceMinor()));
      if (activeWallet != null && wallet.getId() == activeWallet.getId()) {
        selectedIndex = i;
      }
    }

    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    suppressWalletSpinner = true;
    walletSpinner.setAdapter(adapter);
    walletSpinner.setSelection(selectedIndex);
    activeWallet = wallets.get(selectedIndex);
    suppressWalletSpinner = false;
  }

  private void bindCategories() {
    quickCategoryList = new ArrayList<>();
    int quickCount = Math.min(QUICK_CATEGORY_SLOTS, allCategoriesForType.size());
    for (int i = 0; i < quickCount; i++) {
      quickCategoryList.add(allCategoriesForType.get(i));
    }

    bindQuickCategoryChips();
    updateCategoryMoreButtonLabel();
  }

  private void bindAmountShortcuts() {
    amountShortcutList = defaultsStore.getAmountShortcuts();
    amountShortcuts.removeAllViews();
    for (Long amount : amountShortcutList) {
      if (amount == null || amount <= 0L) {
        continue;
      }
      Button chip = new Button(requireContext(), null, android.R.attr.borderlessButtonStyle);
      chip.setText(MoneyFormatter.format(amount));
      UiComponentStyles.prepareChip(chip);
      chip.setMinWidth(UiComponentStyles.dp(requireContext(), 78));
      chip.setMinHeight(UiComponentStyles.dp(requireContext(), 34));
      int horizontalPadding = UiComponentStyles.dp(requireContext(), 6);
      chip.setPadding(horizontalPadding, 0, horizontalPadding, 0);
      chip.setTextSize(12f);
      UiComponentStyles.setChipSelected(requireContext(), chip, false, R.drawable.bg_chip);
      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      params.setMarginEnd(UiComponentStyles.dp(requireContext(), 6));
      chip.setLayoutParams(params);
      chip.setOnClickListener(
          v -> {
            expireAmountAutoFocus();
            setAmountInput(amount);
          });
      amountShortcuts.addView(chip);
    }
  }

  private void setAmountInput(long amountMinor) {
    String formatted = MoneyFormatter.format(amountMinor);
    amountInput.setText(formatted);
    amountInput.setSelection(formatted.length());
  }

  private void openAmountShortcutDialog() {
    new AmountShortcutDialog(
            requireContext(),
            amountShortcutList,
            shortcuts -> {
              defaultsStore.setAmountShortcuts(shortcuts);
              bindAmountShortcuts();
            })
        .show();
  }

  private void bindQuickCategoryChips() {
    quickCategories.removeAllViews();
    for (Category category : quickCategoryList) {
      Button chip = new Button(requireContext(), null, android.R.attr.borderlessButtonStyle);
      chip.setText(category.getName());
      UiComponentStyles.prepareChip(chip);
      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      params.setMarginEnd(UiComponentStyles.dp(requireContext(), 8));
      chip.setLayoutParams(params);
      styleChip(chip, category);
      chip.setOnClickListener(
          v -> {
            expireAmountAutoFocus();
            selectedCategory = category;
            formValidation.clear(CaptureFormValidation.Field.CATEGORY);
            updateCategoryMoreButtonLabel();
            bindQuickCategoryChips();
          });
      quickCategories.addView(chip);
    }
  }

  private void updateCategoryMoreButtonLabel() {
    boolean fromMoreList = selectedCategory != null && !isInQuickList(selectedCategory);
    if (fromMoreList) {
      categoryMoreButton.setText(selectedCategory.getName());
    } else {
      categoryMoreButton.setText(R.string.capture_category_more);
    }
    styleCategoryMoreButton(fromMoreList);
  }

  private boolean isInQuickList(Category category) {
    for (Category quick : quickCategoryList) {
      if (quick.getId() == category.getId()) {
        return true;
      }
    }
    return false;
  }

  private void openCategorySearchDialog() {
    CategorySearchDialog dialog =
        new CategorySearchDialog(
            requireContext(),
            services.categoryDao,
            selectedType,
            allCategoriesForType,
            (category, created) -> {
              selectedCategory = category;
              formValidation.clear(CaptureFormValidation.Field.CATEGORY);
              if (created) {
                refreshCaptureData(true);
              } else {
                updateCategoryMoreButtonLabel();
                bindQuickCategoryChips();
              }
            });
    dialog.show();
  }

  private void styleChip(Button chip, Category category) {
    boolean selected = selectedCategory != null && selectedCategory.getId() == category.getId();
    int selectedBackgroundRes =
        selectedType == TransactionType.INCOME
            ? R.drawable.bg_chip_selected_income
            : R.drawable.bg_chip_selected_expense;
    UiComponentStyles.setChipSelected(requireContext(), chip, selected, selectedBackgroundRes);
  }

  private void styleCategoryMoreButton(boolean selected) {
    int selectedBackgroundRes =
        selectedType == TransactionType.INCOME
            ? R.drawable.bg_control_selected_income
            : R.drawable.bg_control_selected_expense;
    UiComponentStyles.setSelectButtonSelected(
        requireContext(), categoryMoreButton, selected, selectedBackgroundRes);
  }

  private void saveTransaction() {
    ValidatedCaptureInput input = validateCaptureInput();
    if (input == null) {
      return;
    }
    long amountMinor = input.amountMinor;

    Transaction transaction =
        new Transaction(
            0L,
            amountMinor,
            selectedType,
            activeWallet.getId(),
            selectedCategory.getId(),
            occurredAtPicker.getOccurredAtMillis(),
            null);
    String note = noteInput.getText().toString().trim();
    if (!TextUtils.isEmpty(note)) {
      transaction.setNote(note);
    }
    transaction.setMerchantId(merchantSelection.getMerchantId());
    transaction.setTagIds(tagSelection.getSelectedTagIds());
    final String savedNote = TextUtils.isEmpty(note) ? null : note;
    final Long savedMerchantId = merchantSelection.getMerchantId();
    final List<Long> savedTagIds = tagSelection.getSelectedTagIds();

    Wallet walletToRemember = activeWallet;
    services.dbWorker.compute(
        () -> {
          try {
            return transactionService.save(transaction);
          } catch (IllegalArgumentException e) {
            return 0L;
          }
        },
        savedId -> {
          if (!isAdded()) {
            return;
          }
          if (savedId == null || savedId <= 0L) {
            Toast.makeText(requireContext(), R.string.capture_error_save, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          defaultsStore.setLastWalletId(walletToRemember.getId());
          defaultsStore.clearCaptureDraft();
          dismissUndoBar();
          pendingUndo =
              snapshotPendingSaveUndo(
                  savedId, amountMinor, savedNote, savedMerchantId, savedTagIds);
          formValidation.clearAll();
          validationBanner.setVisibility(View.GONE);
          amountInput.setText("");
          noteInput.setText("");
          tagSelection.clear();
          merchantSelection.clear();
          occurredAtPicker.resetToNow();
          selectedCategory = null;
          updateCategoryMoreButtonLabel();
          styleCategoryMoreButton(false);
          showUndoBar();
          refreshCaptureData(false);
          requestAmountFocus();
        });
  }

  private void showUndoBar() {
    if (undoBar == null) {
      return;
    }
    undoBar.setVisibility(View.VISIBLE);
    if (undoDismissRunnable != null) {
      undoHandler.removeCallbacks(undoDismissRunnable);
    }
    undoDismissRunnable = this::dismissUndoBar;
    undoHandler.postDelayed(undoDismissRunnable, UNDO_TIMEOUT_MS);
  }

  private void dismissUndoBar() {
    if (undoDismissRunnable != null) {
      undoHandler.removeCallbacks(undoDismissRunnable);
      undoDismissRunnable = null;
    }
    pendingUndo = null;
    if (undoBar != null) {
      undoBar.setVisibility(View.GONE);
    }
  }

  private void performUndo() {
    PendingSaveUndo draft = pendingUndo;
    if (draft == null) {
      return;
    }
    services.dbWorker.compute(
        () -> {
          if (services.transactionGateway.findById(draft.transactionId) == null) {
            return Boolean.FALSE;
          }
          transactionService.delete(draft.transactionId);
          return Boolean.TRUE;
        },
        deleted -> {
          if (!isAdded()) {
            return;
          }
          dismissUndoBar();
          if (!Boolean.TRUE.equals(deleted)) {
            Toast.makeText(requireContext(), R.string.capture_undo_failed, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          restoreDraft(draft);
          persistCaptureDraft();
          Toast.makeText(requireContext(), R.string.capture_undo_done, Toast.LENGTH_SHORT).show();
          refreshCaptureData(true);
          requestAmountFocus();
        });
  }

  private PendingSaveUndo snapshotPendingSaveUndo(
      long transactionId,
      long amountMinor,
      @Nullable String note,
      @Nullable Long merchantId,
      @NonNull List<Long> tagIds) {
    return new PendingSaveUndo(
        transactionId,
        amountMinor,
        selectedType,
        activeWallet.getId(),
        selectedCategory.getId(),
        occurredAtPicker.getOccurredAtMillis(),
        note,
        merchantId,
        tagIds);
  }

  private void restoreDraft(@NonNull PendingSaveUndo draft) {
    applyFormDraft(
        TransactionFormDraft.fromPendingSave(
            draft.amountMinor,
            draft.type,
            draft.walletId,
            draft.categoryId,
            draft.occurredAtMillis,
            draft.note,
            draft.merchantId,
            draft.tagIds));
    bindWalletSpinner();
    bindCategories();
  }

  private void tryRestoreCaptureDraft() {
    if (captureDraftRestored || amountInput == null) {
      return;
    }
    captureDraftRestored = true;
    TransactionFormDraft draft = defaultsStore.getCaptureDraft();
    if (draft == null) {
      return;
    }
    applyFormDraft(draft);
  }

  private void applyFormDraft(@NonNull TransactionFormDraft draft) {
    selectedType = draft.getType();
    typeGroup.check(
        selectedType == TransactionType.INCOME
            ? R.id.capture_type_income
            : R.id.capture_type_expense);
    if (draft.getAmountMinor() > 0L) {
      setAmountInput(draft.getAmountMinor());
    }
    noteInput.setText(draft.getNote() != null ? draft.getNote() : "");
    merchantSelection.setMerchantId(draft.getMerchantId());
    tagSelection.setSelectedTagIds(draft.getTagIds());
    occurredAtPicker.setOccurredAtMillis(draft.getOccurredAtMillis());

    Long walletId = draft.getWalletId();
    if (walletId != null) {
      for (Wallet wallet : wallets) {
        if (wallet.getId() == walletId) {
          activeWallet = wallet;
          break;
        }
      }
    }

    Long categoryId = draft.getCategoryId();
    if (categoryId != null) {
      Category category = services.categoryDao.findById(categoryId);
      if (category != null && selectedType.name().equals(category.getTypeFilter())) {
        selectedCategory = category;
      } else {
        selectedCategory = null;
      }
    }

    formValidation.clearAll();
    validationBanner.setVisibility(View.GONE);
    updateCategoryMoreButtonLabel();
  }

  private void persistCaptureDraft() {
    if (amountInput == null) {
      return;
    }
    TransactionFormDraft draft = buildCaptureDraft();
    if (draft.hasContent()) {
      defaultsStore.setCaptureDraft(draft);
    } else {
      defaultsStore.clearCaptureDraft();
    }
  }

  @NonNull
  private TransactionFormDraft buildCaptureDraft() {
    TransactionFormDraft draft = new TransactionFormDraft();
    draft.setType(selectedType);
    draft.setOccurredAtMillis(occurredAtPicker.getOccurredAtMillis());
    if (activeWallet != null) {
      draft.setWalletId(activeWallet.getId());
    }
    if (selectedCategory != null) {
      draft.setCategoryId(selectedCategory.getId());
    }
    String note = noteInput.getText().toString().trim();
    if (!TextUtils.isEmpty(note)) {
      draft.setNote(note);
    }
    draft.setMerchantId(merchantSelection.getMerchantId());
    draft.setTagIds(tagSelection.getSelectedTagIds());
    String amountText = amountInput.getText().toString().trim();
    if (!amountText.isEmpty()) {
      try {
        long amountMinor = MoneyParser.parse(amountText);
        if (amountMinor > 0L) {
          draft.setAmountMinor(amountMinor);
        }
      } catch (IllegalArgumentException ignored) {
        // Keep partial draft without amount when input is not yet parseable.
      }
    }
    return draft;
  }

  private static final class PendingSaveUndo {
    private final long transactionId;
    private final long amountMinor;
    private final TransactionType type;
    private final long walletId;
    private final long categoryId;
    private final long occurredAtMillis;
    @Nullable private final String note;
    @Nullable private final Long merchantId;
    @NonNull private final List<Long> tagIds;

    private PendingSaveUndo(
        long transactionId,
        long amountMinor,
        TransactionType type,
        long walletId,
        long categoryId,
        long occurredAtMillis,
        @Nullable String note,
        @Nullable Long merchantId,
        @NonNull List<Long> tagIds) {
      this.transactionId = transactionId;
      this.amountMinor = amountMinor;
      this.type = type;
      this.walletId = walletId;
      this.categoryId = categoryId;
      this.occurredAtMillis = occurredAtMillis;
      this.note = note;
      this.merchantId = merchantId;
      this.tagIds = tagIds;
    }
  }

  private void installAmountAutoFocusExpiry(@NonNull View root) {
    bindAmountAutoFocusExpiryOnTouchTree(root);
  }

  @SuppressLint("ClickableViewAccessibility")
  private void bindAmountAutoFocusExpiryOnTouchTree(View view) {
    if (view == null || view == amountInput) {
      return;
    }
    view.setOnTouchListener(
        (touchedView, event) -> {
          if (event.getActionMasked() == MotionEvent.ACTION_DOWN && isOutsideAmountInput(event)) {
            expireAmountAutoFocus();
          }
          return false;
        });
    if (!(view instanceof ViewGroup group)) {
      return;
    }
    for (int i = 0; i < group.getChildCount(); i++) {
      bindAmountAutoFocusExpiryOnTouchTree(group.getChildAt(i));
    }
  }

  private boolean isOutsideAmountInput(MotionEvent event) {
    Rect amountBounds = new Rect();
    amountInput.getGlobalVisibleRect(amountBounds);
    return !amountBounds.contains(Math.round(event.getRawX()), Math.round(event.getRawY()));
  }

  private void expireAmountAutoFocus() {
    amountAutoFocusExpired = true;
    amountInput.clearFocus();
    InputMethodManager imm =
        (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.hideSoftInputFromWindow(amountInput.getWindowToken(), 0);
    }
  }

  private void requestAmountFocus() {
    if (!amountAutoFocusExpired) {
      amountInput.requestFocus();
    }
  }

  @Nullable
  private ValidatedCaptureInput validateCaptureInput() {
    formValidation.clearAll();
    boolean valid = true;
    long amountMinor = 0L;

    String amountText = amountInput.getText().toString().trim();
    if (amountText.isEmpty()) {
      formValidation.showError(requireContext(), CaptureFormValidation.Field.AMOUNT, R.string.capture_error_amount_empty);
      valid = false;
    } else {
      try {
        amountMinor = MoneyParser.parse(amountText);
        if (amountMinor <= 0L) {
          formValidation.showError(
              requireContext(), CaptureFormValidation.Field.AMOUNT, R.string.capture_error_amount_positive);
          valid = false;
        }
      } catch (IllegalArgumentException e) {
        formValidation.showError(requireContext(), CaptureFormValidation.Field.AMOUNT, R.string.capture_error_amount);
        valid = false;
      }
    }

    if (activeWallet == null) {
      formValidation.showError(requireContext(), CaptureFormValidation.Field.WALLET, R.string.capture_error_wallet);
      valid = false;
    }

    if (selectedCategory == null) {
      formValidation.showError(requireContext(), CaptureFormValidation.Field.CATEGORY, R.string.capture_error_category);
      valid = false;
    }

    if (!valid) {
      formValidation.scrollToFirstError();
      return null;
    }
    return new ValidatedCaptureInput(amountMinor);
  }

  private static final class ValidatedCaptureInput {
    private final long amountMinor;

    private ValidatedCaptureInput(long amountMinor) {
      this.amountMinor = amountMinor;
    }
  }

  private static final class CaptureState {
    private final List<Wallet> wallets;
    private final Wallet activeWallet;
    private final List<Category> categoriesForType;
    private final Category selectedCategory;
    private final List<Transaction> recentTransactions;
    private final Map<Long, Category> categoriesById;
    private final Map<Long, Wallet> walletsById;
    private final Map<Long, com.dwlhm.finan.data.entity.Tag> tagsById;
    private final Map<Long, com.dwlhm.finan.data.entity.Merchant> merchantsById;

    private CaptureState(
        List<Wallet> wallets,
        Wallet activeWallet,
        List<Category> categoriesForType,
        Category selectedCategory,
        List<Transaction> recentTransactions,
        Map<Long, Category> categoriesById,
        Map<Long, Wallet> walletsById,
        Map<Long, com.dwlhm.finan.data.entity.Tag> tagsById,
        Map<Long, com.dwlhm.finan.data.entity.Merchant> merchantsById) {
      this.wallets = wallets;
      this.activeWallet = activeWallet;
      this.categoriesForType = categoriesForType;
      this.selectedCategory = selectedCategory;
      this.recentTransactions = recentTransactions;
      this.categoriesById = categoriesById;
      this.walletsById = walletsById;
      this.tagsById = tagsById;
      this.merchantsById = merchantsById;
    }
  }
}
