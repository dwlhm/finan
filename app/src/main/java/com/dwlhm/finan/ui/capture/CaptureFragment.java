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
  private Spinner destinationWalletSpinner;
  private RadioGroup typeGroup;
  private TextView walletLabel;
  private View categorySection;
  private View destinationSection;
  private View transactionMetadataFields;
  private Button saveButton;
  private EditText noteInput;
  private LinearLayout recentList;
  private TextView recentEmpty;

  private Wallet activeWallet;
  private Wallet destinationWallet;
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
  private boolean suppressDestinationSpinner;
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
    destinationWalletSpinner = view.findViewById(R.id.capture_destination_spinner);
    typeGroup = view.findViewById(R.id.capture_type_group);
    walletLabel = view.findViewById(R.id.capture_wallet_label);
    categorySection = view.findViewById(R.id.capture_category_section);
    destinationSection = view.findViewById(R.id.capture_destination_section);
    transactionMetadataFields = view.findViewById(R.id.capture_transaction_metadata_fields);
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

    saveButton = view.findViewById(R.id.capture_save);

    recentAdapter = new TransactionListAdapter(requireContext());

    typeGroup.setOnCheckedChangeListener(
        (group, checkedId) -> {
          expireAmountAutoFocus();
          selectedType =
              checkedId == R.id.capture_type_transfer
                  ? TransactionType.TRANSFER_OUT
                  : checkedId == R.id.capture_type_income
                      ? TransactionType.INCOME
                      : TransactionType.EXPENSE;
          selectedCategory = null;
          formValidation.clear(CaptureFormValidation.Field.CATEGORY);
          formValidation.clear(CaptureFormValidation.Field.DESTINATION);
          updateCaptureMode();
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
            ensureDestinationWallet();
            selectDestinationWallet();
            formValidation.clear(CaptureFormValidation.Field.DESTINATION);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    destinationWalletSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (suppressDestinationSpinner || position < 0 || position >= wallets.size()) {
              return;
            }
            expireAmountAutoFocus();
            destinationWallet = wallets.get(position);
            formValidation.clear(CaptureFormValidation.Field.DESTINATION);
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
    updateCaptureMode();
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
          bindWalletSpinners();
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
        type.isTransfer()
            ? new ArrayList<>()
            : services.categoryDao.findByTypeFilterOrderByUsage(type.name());
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

  private void bindWalletSpinners() {
    if (wallets.isEmpty()) {
      activeWallet = null;
      destinationWallet = null;
      walletSpinner.setEnabled(false);
      destinationWalletSpinner.setEnabled(false);
      return;
    }
    walletSpinner.setEnabled(true);
    destinationWalletSpinner.setEnabled(wallets.size() > 1);

    List<String> labels = new ArrayList<>();
    for (Wallet wallet : wallets) {
      labels.add(
          wallet.getName()
              + " · "
              + MoneyFormatter.format(wallet.getCachedBalanceMinor()));
    }
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    suppressWalletSpinner = true;
    suppressDestinationSpinner = true;
    walletSpinner.setAdapter(adapter);
    destinationWalletSpinner.setAdapter(adapter);
    int sourceIndex = Math.max(0, walletIndex(activeWallet));
    walletSpinner.setSelection(sourceIndex);
    activeWallet = wallets.get(sourceIndex);
    ensureDestinationWallet();
    destinationWalletSpinner.setSelection(Math.max(0, walletIndex(destinationWallet)));
    suppressWalletSpinner = false;
    suppressDestinationSpinner = false;
    updateCaptureMode();
  }

  private void ensureDestinationWallet() {
    if (activeWallet == null) {
      destinationWallet = null;
      return;
    }
    if (destinationWallet != null
        && destinationWallet.getId() != activeWallet.getId()
        && walletIndex(destinationWallet) >= 0) {
      return;
    }
    destinationWallet = null;
    for (Wallet wallet : wallets) {
      if (wallet.getId() != activeWallet.getId()) {
        destinationWallet = wallet;
        return;
      }
    }
  }

  private int walletIndex(@Nullable Wallet selected) {
    for (int i = 0; i < wallets.size(); i++) {
      if (selected != null && wallets.get(i).getId() == selected.getId()) {
        return i;
      }
    }
    return -1;
  }

  private void selectDestinationWallet() {
    suppressDestinationSpinner = true;
    destinationWalletSpinner.setSelection(Math.max(0, walletIndex(destinationWallet)));
    suppressDestinationSpinner = false;
  }

  private void updateCaptureMode() {
    boolean transfer = selectedType.isTransfer();
    categorySection.setVisibility(transfer ? View.GONE : View.VISIBLE);
    destinationSection.setVisibility(transfer ? View.VISIBLE : View.GONE);
    transactionMetadataFields.setVisibility(transfer ? View.GONE : View.VISIBLE);
    walletLabel.setText(
        transfer ? R.string.wallet_transfer_source_label : R.string.capture_wallet_label);
    saveButton.setText(transfer ? R.string.wallet_transfer_save : R.string.capture_save);
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
    if (selectedType.isTransfer()) {
      saveTransfer(amountMinor);
      return;
    }

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
          clearSavedForm();
          showUndoBar();
          refreshCaptureData(false);
          requestAmountFocus();
        });
  }

  private void saveTransfer(long amountMinor) {
    Wallet source = activeWallet;
    Wallet destination = destinationWallet;
    long occurredAt = occurredAtPicker.getOccurredAtMillis();
    String note = noteInput.getText().toString().trim();
    String savedNote = TextUtils.isEmpty(note) ? null : note;
    services.dbWorker.compute(
        () -> {
          try {
            return services.transferService.create(
                source.getId(),
                destination.getId(),
                amountMinor,
                occurredAt,
                savedNote);
          } catch (RuntimeException e) {
            return 0L;
          }
        },
        transferId -> {
          if (!isAdded()) {
            return;
          }
          if (transferId == null || transferId <= 0L) {
            Toast.makeText(
                    requireContext(), R.string.wallet_transfer_error_save, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          defaultsStore.setLastWalletId(source.getId());
          defaultsStore.clearCaptureDraft();
          dismissUndoBar();
          pendingUndo =
              PendingSaveUndo.transfer(
                  transferId,
                  amountMinor,
                  source.getId(),
                  destination.getId(),
                  occurredAt,
                  savedNote);
          clearSavedForm();
          showUndoBar();
          refreshCaptureData(false);
          requestAmountFocus();
        });
  }

  private void clearSavedForm() {
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
          if (draft.transfer) {
            try {
              services.transferService.delete(draft.recordId);
              return Boolean.TRUE;
            } catch (RuntimeException e) {
              return Boolean.FALSE;
            }
          }
          if (services.transactionGateway.findById(draft.recordId) == null) {
            return Boolean.FALSE;
          }
          transactionService.delete(draft.recordId);
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
        false,
        amountMinor,
        selectedType,
        activeWallet.getId(),
        selectedCategory.getId(),
        null,
        occurredAtPicker.getOccurredAtMillis(),
        note,
        merchantId,
        tagIds);
  }

  private void restoreDraft(@NonNull PendingSaveUndo draft) {
    TransactionFormDraft formDraft =
        draft.transfer
            ? transferDraft(draft)
            : TransactionFormDraft.fromPendingSave(
                draft.amountMinor,
                draft.type,
                draft.walletId,
                draft.categoryId,
                draft.occurredAtMillis,
                draft.note,
                draft.merchantId,
                draft.tagIds);
    applyFormDraft(formDraft);
    bindWalletSpinners();
    bindCategories();
  }

  private static TransactionFormDraft transferDraft(PendingSaveUndo draft) {
    TransactionFormDraft formDraft = new TransactionFormDraft();
    formDraft.setAmountMinor(draft.amountMinor);
    formDraft.setType(TransactionType.TRANSFER_OUT);
    formDraft.setWalletId(draft.walletId);
    formDraft.setDestinationWalletId(draft.destinationWalletId);
    formDraft.setOccurredAtMillis(draft.occurredAtMillis);
    formDraft.setNote(draft.note);
    return formDraft;
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
        selectedType.isTransfer()
            ? R.id.capture_type_transfer
            : selectedType == TransactionType.INCOME
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

    Long destinationWalletId = draft.getDestinationWalletId();
    if (destinationWalletId != null) {
      for (Wallet wallet : wallets) {
        if (wallet.getId() == destinationWalletId) {
          destinationWallet = wallet;
          break;
        }
      }
    }

    Long categoryId = draft.getCategoryId();
    if (!selectedType.isTransfer() && categoryId != null) {
      Category category = services.categoryDao.findById(categoryId);
      if (category != null && selectedType.name().equals(category.getTypeFilter())) {
        selectedCategory = category;
      } else {
        selectedCategory = null;
      }
    }

    formValidation.clearAll();
    validationBanner.setVisibility(View.GONE);
    updateCaptureMode();
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
    if (selectedType.isTransfer()) {
      if (destinationWallet != null) {
        draft.setDestinationWalletId(destinationWallet.getId());
      }
    } else {
      if (selectedCategory != null) {
        draft.setCategoryId(selectedCategory.getId());
      }
      draft.setMerchantId(merchantSelection.getMerchantId());
      draft.setTagIds(tagSelection.getSelectedTagIds());
    }
    String note = noteInput.getText().toString().trim();
    if (!TextUtils.isEmpty(note)) {
      draft.setNote(note);
    }
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
    private final long recordId;
    private final boolean transfer;
    private final long amountMinor;
    private final TransactionType type;
    private final long walletId;
    private final long categoryId;
    @Nullable private final Long destinationWalletId;
    private final long occurredAtMillis;
    @Nullable private final String note;
    @Nullable private final Long merchantId;
    @NonNull private final List<Long> tagIds;

    private PendingSaveUndo(
        long recordId,
        boolean transfer,
        long amountMinor,
        TransactionType type,
        long walletId,
        long categoryId,
        @Nullable Long destinationWalletId,
        long occurredAtMillis,
        @Nullable String note,
        @Nullable Long merchantId,
        @NonNull List<Long> tagIds) {
      this.recordId = recordId;
      this.transfer = transfer;
      this.amountMinor = amountMinor;
      this.type = type;
      this.walletId = walletId;
      this.categoryId = categoryId;
      this.destinationWalletId = destinationWalletId;
      this.occurredAtMillis = occurredAtMillis;
      this.note = note;
      this.merchantId = merchantId;
      this.tagIds = tagIds;
    }

    private static PendingSaveUndo transfer(
        long transferId,
        long amountMinor,
        long sourceWalletId,
        long destinationWalletId,
        long occurredAtMillis,
        @Nullable String note) {
      return new PendingSaveUndo(
          transferId,
          true,
          amountMinor,
          TransactionType.TRANSFER_OUT,
          sourceWalletId,
          0L,
          destinationWalletId,
          occurredAtMillis,
          note,
          null,
          new ArrayList<>());
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

    if (selectedType.isTransfer()) {
      if (destinationWallet == null) {
        formValidation.showError(
            requireContext(),
            CaptureFormValidation.Field.DESTINATION,
            R.string.capture_error_destination);
        valid = false;
      } else if (activeWallet != null && activeWallet.getId() == destinationWallet.getId()) {
        formValidation.showError(
            requireContext(),
            CaptureFormValidation.Field.DESTINATION,
            R.string.wallet_transfer_same_wallet);
        valid = false;
      }
    } else if (selectedCategory == null) {
      formValidation.showError(
          requireContext(),
          CaptureFormValidation.Field.CATEGORY,
          R.string.capture_error_category);
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
