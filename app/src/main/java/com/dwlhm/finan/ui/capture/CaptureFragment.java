package com.dwlhm.finan.ui.capture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.data.prefs.DefaultsStore;
import com.dwlhm.finan.data.prefs.TransactionFormDraft;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.service.transaction.TransactionService;
import android.app.Dialog;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.category.CategoryEditorDialog;
import com.dwlhm.finan.ui.common.EntitySearchBottomSheet;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.TransactionOccurredAtPicker;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;
import com.dwlhm.finan.ui.components.FinancialKeypadView;
import com.dwlhm.finan.ui.components.KeypadAmountManager;
import com.dwlhm.finan.ui.components.FinanToast;

import java.util.ArrayList;
import java.util.List;

public final class CaptureFragment extends ScreenFragment {

  private static final long WALLET_PRESELECTION_DELAY_MS = 250L;
  private AppServices services;
  private TransactionService transactionService;
  private DefaultsStore defaultsStore;

  private TextView typeExpense;
  private TextView typeIncome;
  private TextView typeTransfer;

  private EditText amountInput;
  private TextView sentenceFor;
  private TextView categoryText;
  private TextView sentenceFrom;
  private TextView walletText;
  private TextView dateText;
  private EditText noteInput;
  private Button saveButton;
  private TextView validationBanner;
  private CaptureFormValidation formValidation;
  

  private Wallet activeWallet;
  private Wallet destinationWallet;
  private Category selectedCategory;
  private TransactionType selectedType = TransactionType.EXPENSE;
  private TransactionOccurredAtPicker occurredAtPicker;

  private FinanToast activeToast;
  @Nullable private PendingSaveUndo pendingUndo;

  private List<Wallet> wallets = new ArrayList<>();
  private List<Category> allCategoriesForType = new ArrayList<>();
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
    MoneyInputFormatter.attach(amountInput, false);
    
    typeExpense = view.findViewById(R.id.capture_type_expense);
    typeIncome = view.findViewById(R.id.capture_type_income);
    typeTransfer = view.findViewById(R.id.capture_type_transfer);

    sentenceFor = view.findViewById(R.id.capture_sentence_for);
    categoryText = view.findViewById(R.id.capture_category_text);
    sentenceFrom = view.findViewById(R.id.capture_sentence_from);
    walletText = view.findViewById(R.id.capture_wallet_text);
    dateText = view.findViewById(R.id.capture_date_text);
    noteInput = view.findViewById(R.id.capture_note);
    saveButton = view.findViewById(R.id.capture_save);

    occurredAtPicker = new TransactionOccurredAtPicker(
            requireContext(), dateText, null, System.currentTimeMillis());

    validationBanner = view.findViewById(R.id.capture_validation_banner);
    formValidation = new CaptureFormValidation(view, validationBanner);
    formValidation.bindAmountClearListener();

    FinancialKeypadView financialKeypad = view.findViewById(R.id.capture_financial_keypad);
    amountInput.setShowSoftInputOnFocus(false);
    KeypadAmountManager keypadManager = new KeypadAmountManager(amountInput);
    financialKeypad.setOnKeypadActionListener(keypadManager);
    

    typeExpense.setOnClickListener(v -> setType(TransactionType.EXPENSE));
    typeIncome.setOnClickListener(v -> setType(TransactionType.INCOME));
    typeTransfer.setOnClickListener(v -> setType(TransactionType.TRANSFER_OUT));

    categoryText.setOnClickListener(v -> {
        expireAmountAutoFocus();
        openCategorySearchDialog();
    });

    walletText.setOnClickListener(v -> {
        expireAmountAutoFocus();
        openWalletSearchDialog(false);
    });

    saveButton.setOnClickListener(
        v -> {
          expireAmountAutoFocus();
          saveTransaction();
        });

    installAmountAutoFocusExpiry(view);
    updateCaptureMode();
    requestAmountFocus();
  }
  
  private void setType(TransactionType type) {
      expireAmountAutoFocus();
      selectedType = type;
      selectedCategory = null;
      formValidation.clear(CaptureFormValidation.Field.CATEGORY);
      formValidation.clear(CaptureFormValidation.Field.DESTINATION);
      updateCaptureMode();
      refreshCaptureData(false);
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
          tryRestoreCaptureDraft();
          bindWallets();
          updateCategoryLabel();
        });
  }

  private CaptureState loadCaptureState(TransactionType type, @Nullable Category retainedCategory) {
    List<Wallet> loadedWallets = services.walletDao.findAll();
    Wallet resolvedWallet = resolveActiveWallet(loadedWallets);
    List<Category> categoriesForType =
        type.isTransfer()
            ? new ArrayList<>()
            : services.categoryDao.findByTypeFilterOrderByUsage(type.name());
    Category selected =
        retainedCategory != null && isCategoryInList(retainedCategory, categoriesForType)
            ? retainedCategory
            : null;
            
    if (selected == null && !categoriesForType.isEmpty()) {
      for (Category category : categoriesForType) {
        String catName = category.getName().toLowerCase();
        if (catName.contains("makan") || catName.contains("food") || catName.contains("eat")) {
          selected = category;
          break;
        }
      }
      // Fallback to the first category if "Makan" is not found in the DB
      if (selected == null) {
        selected = categoriesForType.get(0);
      }
    }
    
    return new CaptureState(loadedWallets, resolvedWallet, categoriesForType, selected);
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

  private void bindWallets() {
    if (wallets.isEmpty()) {
      activeWallet = null;
      destinationWallet = null;
      walletText.setEnabled(false);
      return;
    }
    walletText.setEnabled(true);
    
    if (activeWallet == null || walletIndex(activeWallet) < 0) {
        activeWallet = wallets.get(0);
    }
    ensureDestinationWallet();
    updateWalletLabel();
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

  private void updateCaptureMode() {
    boolean transfer = selectedType.isTransfer();
    boolean expense = selectedType == TransactionType.EXPENSE;
    boolean income = selectedType == TransactionType.INCOME;

    typeExpense.setBackgroundResource(expense ? R.drawable.bg_chip_selected : 0);
    if (expense) typeExpense.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.finan_expense)));
    else typeExpense.setBackgroundTintList(null);

    typeIncome.setBackgroundResource(income ? R.drawable.bg_chip_selected : 0);
    if (income) typeIncome.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.finan_income)));
    else typeIncome.setBackgroundTintList(null);

    typeTransfer.setBackgroundResource(transfer ? R.drawable.bg_chip_selected : 0);
    if (transfer) typeTransfer.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.finan_primary)));
    else typeTransfer.setBackgroundTintList(null);

    typeExpense.setTextColor(requireContext().getColor(expense ? R.color.finan_chip_text_selected : R.color.finan_text_secondary));
    typeIncome.setTextColor(requireContext().getColor(income ? R.color.finan_chip_text_selected : R.color.finan_text_secondary));
    typeTransfer.setTextColor(requireContext().getColor(transfer ? R.color.finan_chip_text_selected : R.color.finan_text_secondary));

    if (transfer) {
        sentenceFor.setText("to ");
        sentenceFrom.setText(" from ");
        updateCategoryLabel(); // Will display destination wallet
    } else {
        sentenceFor.setText("for ");
        sentenceFrom.setText(income ? " to " : " from ");
        updateCategoryLabel();
    }
  }

  private void applyErrorBackground(TextView view) {
      int paddingLeft = view.getPaddingLeft();
      int paddingTop = view.getPaddingTop();
      int paddingRight = view.getPaddingRight();
      int paddingBottom = view.getPaddingBottom();
      view.setBackgroundResource(R.drawable.bg_field_error);
      view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
  }

  private void applySelectionBackground(TextView view, boolean isSelected) {
      int paddingLeft = view.getPaddingLeft();
      int paddingTop = view.getPaddingTop();
      int paddingRight = view.getPaddingRight();
      int paddingBottom = view.getPaddingBottom();

      if (isSelected) {
          TypedValue outValue = new TypedValue();
          requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
          view.setBackgroundResource(outValue.resourceId);
      } else {
          view.setBackgroundResource(R.drawable.bg_unselected_field);
      }
      view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
  }

  private void updateCategoryLabel() {
      if (selectedType.isTransfer()) {
          categoryText.setText(destinationWallet != null ? destinationWallet.getName() : "Wallet");
          applySelectionBackground(categoryText, destinationWallet != null);
          categoryText.setOnClickListener(v -> {
              expireAmountAutoFocus();
              openWalletSearchDialog(true);
          });
      } else {
          categoryText.setText(selectedCategory != null ? "#" + selectedCategory.getName() : "#Category");
          applySelectionBackground(categoryText, selectedCategory != null);
          categoryText.setOnClickListener(v -> {
              expireAmountAutoFocus();
              openCategorySearchDialog();
          });
      }
  }

  private void updateWalletLabel() {
      walletText.setText(activeWallet != null ? activeWallet.getName() : "Wallet");
      applySelectionBackground(walletText, activeWallet != null);
  }

  private void openCategorySearchDialog() {
      EntitySearchBottomSheet<Category> bottomSheet = new EntitySearchBottomSheet<>(
          requireContext(),
          allCategoriesForType,
          selectedCategory != null ? selectedCategory.getId() : null,
          new EntitySearchBottomSheet.ItemMapper<Category>() {
              @Override
              public String getName(Category item) { return item.getName(); }
              @Override
              public long getId(Category item) { return item.getId(); }
          },
          item -> {
              selectedCategory = item;
              formValidation.clear(CaptureFormValidation.Field.CATEGORY);
              updateCategoryLabel();
          },
          getString(R.string.capture_category_add_new),
          this::openCategoryCreator
      );
      bottomSheet.show();
  }

  private void openCategoryCreator() {
      new CategoryEditorDialog(
          requireContext(),
          services,
          null,
          0,
          saved -> {
              selectedCategory = saved;
              formValidation.clear(CaptureFormValidation.Field.CATEGORY);
              updateCategoryLabel();
              refreshCaptureData(true);
          },
          null,
          null,
          selectedType.name()
      );
  }

  private void openWalletSearchDialog(boolean isDestination) {
      EntitySearchBottomSheet<Wallet> bottomSheet = new EntitySearchBottomSheet<>(
          requireContext(),
          wallets,
          isDestination ? (destinationWallet != null ? destinationWallet.getId() : null) : (activeWallet != null ? activeWallet.getId() : null),
          new EntitySearchBottomSheet.ItemMapper<Wallet>() {
              @Override
              public String getName(Wallet item) { return item.getName(); }
              @Override
              public long getId(Wallet item) { return item.getId(); }
          },
          item -> {
              if (isDestination) {
                  destinationWallet = item;
                  formValidation.clear(CaptureFormValidation.Field.DESTINATION);
                  updateCategoryLabel(); // Update "to Wallet" text
              } else {
                  activeWallet = item;
                  formValidation.clear(CaptureFormValidation.Field.WALLET);
                  ensureDestinationWallet();
                  updateWalletLabel();
                  if (selectedType.isTransfer()) {
                      updateCategoryLabel();
                  }
              }
          }
      );
      bottomSheet.show();
  }

  private void setAmountInput(long amountMinor) {
    String formatted = MoneyFormatter.formatWithoutCurrency(amountMinor);
    amountInput.setText(formatted);
    amountInput.setSelection(formatted.length());
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
    final String savedNote = TextUtils.isEmpty(note) ? null : note;

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
                  savedId, amountMinor, savedNote, null, new ArrayList<>());
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
    occurredAtPicker.resetToNow();
    selectedCategory = null;
    updateCategoryLabel();
  }

  private void showUndoBar() {
    if (activeToast != null) {
      activeToast.dismiss();
    }
    activeToast = FinanToast.show(requireActivity(), getString(R.string.capture_saved_undo_message), getString(R.string.capture_undo_action), () -> {
        expireAmountAutoFocus();
        performUndo();
    });
  }

  private void dismissUndoBar() {
    pendingUndo = null;
    if (activeToast != null) {
      activeToast.dismiss();
      activeToast = null;
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
    bindWallets();
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
    if (draft.getAmountMinor() > 0L) {
      setAmountInput(draft.getAmountMinor());
    }
    noteInput.setText(draft.getNote() != null ? draft.getNote() : "");
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
      draft.setMerchantId(null);
      draft.setTagIds(new ArrayList<>());
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
    int rawX = Math.round(event.getRawX());
    int rawY = Math.round(event.getRawY());

    Rect amountBounds = new Rect();
    amountInput.getGlobalVisibleRect(amountBounds);
    if (amountBounds.contains(rawX, rawY)) {
      return false;
    }

    View view = getView();
    if (view != null) {
      View keypadView = view.findViewById(R.id.capture_financial_keypad);
      if (keypadView != null && keypadView.getVisibility() == View.VISIBLE) {
        Rect keypadBounds = new Rect();
        keypadView.getGlobalVisibleRect(keypadBounds);
        if (keypadBounds.contains(rawX, rawY)) {
          return false;
        }
      }
    }

    return true;
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
    // Disabled auto-focus on amount input to prevent keypad from popping up automatically.
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
      applyErrorBackground(walletText);
      formValidation.showError(requireContext(), CaptureFormValidation.Field.WALLET, R.string.capture_error_wallet);
      valid = false;
    }

    if (selectedType.isTransfer()) {
      if (destinationWallet == null) {
        applyErrorBackground(categoryText);
        formValidation.showError(
            requireContext(),
            CaptureFormValidation.Field.DESTINATION,
            R.string.capture_error_destination);
        valid = false;
      } else if (activeWallet != null && activeWallet.getId() == destinationWallet.getId()) {
        applyErrorBackground(categoryText);
        formValidation.showError(
            requireContext(),
            CaptureFormValidation.Field.DESTINATION,
            R.string.wallet_transfer_same_wallet);
        valid = false;
      }
    } else if (selectedCategory == null) {
      applyErrorBackground(categoryText);
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

    private CaptureState(
        List<Wallet> wallets,
        Wallet activeWallet,
        List<Category> categoriesForType,
        Category selectedCategory) {
      this.wallets = wallets;
      this.activeWallet = activeWallet;
      this.categoriesForType = categoriesForType;
      this.selectedCategory = selectedCategory;
    }
  }
}
