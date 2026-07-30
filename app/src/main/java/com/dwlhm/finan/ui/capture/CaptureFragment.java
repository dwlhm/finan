package com.dwlhm.finan.ui.capture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
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
import com.dwlhm.finan.service.transaction.TransactionService;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.category.CategoryEditorDialog;
import com.dwlhm.finan.ui.common.EntitySearchBottomSheet;
import com.dwlhm.finan.ui.wallet.WalletInputDialog;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.DateTimeBottomSheet;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;
import com.dwlhm.finan.ui.components.CalculatorStripView;
import com.dwlhm.finan.ui.components.FinancialKeypadView;
import com.dwlhm.finan.ui.components.KeypadAmountManager;
import com.dwlhm.finan.ui.components.FinanToast;
import com.dwlhm.finan.util.math.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CaptureFragment extends ScreenFragment {

    private AppServices services;
  private TransactionService transactionService;
  private DefaultsStore defaultsStore;


  private EditText amountInput;
  private TextView sentencePrefix;
  private TextView sentenceFor;
  private TextView categoryText;
  private TextView sentenceFrom;
  private TextView walletText;
  private TextView dateText;
  private EditText noteInput;
  private TextView validationBanner;
  private Button saveButton;
  private CaptureFormValidation formValidation;

  private CalculatorStripView calculatorStrip;
  private StringBuilder exprString;
  private StringBuilder calcOperand;

  private Wallet activeWallet;
  private Wallet destinationWallet;
  private Category selectedCategory;
  private TransactionType selectedType = TransactionType.EXPENSE;
  private long occurredAtMillis;

  private FinanToast activeToast;
  @Nullable private PendingSaveUndo pendingUndo;

  private List<Wallet> wallets = new ArrayList<>();
  private List<Category> allCategoriesForType = new ArrayList<>();
  private boolean captureDraftRestored;
  private int refreshGeneration;
  private boolean isDataLoaded = false;

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

  private boolean saveInProgress;

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    amountInput = view.findViewById(R.id.capture_amount);
    MoneyInputFormatter.attach(amountInput, false);
    amountInput.addTextChangedListener(calcTextWatcher);
    

    sentencePrefix = view.findViewById(R.id.capture_sentence_prefix);
    sentenceFor = view.findViewById(R.id.capture_sentence_for);
    categoryText = view.findViewById(R.id.capture_category_text);
    sentenceFrom = view.findViewById(R.id.capture_sentence_from);
    walletText = view.findViewById(R.id.capture_wallet_text);
    dateText = view.findViewById(R.id.capture_date_text);
    noteInput = view.findViewById(R.id.capture_note);
    saveButton = view.findViewById(R.id.capture_save);

    occurredAtMillis = System.currentTimeMillis();
    updateDateLabel();
    dateText.setOnClickListener(v -> {
        expireAmountAutoFocus();
        new DateTimeBottomSheet(
            requireContext(), occurredAtMillis,
            millis -> {
                occurredAtMillis = millis;
                updateDateLabel();
            }).show();
    });

    validationBanner = view.findViewById(R.id.capture_validation_banner);
    formValidation = new CaptureFormValidation(view, validationBanner);
    formValidation.bindAmountClearListener();

    FinancialKeypadView financialKeypad = view.findViewById(R.id.capture_financial_keypad);
    amountInput.setShowSoftInputOnFocus(false);
    KeypadAmountManager keypadManager = new KeypadAmountManager(amountInput);

    financialKeypad.setOnKeypadActionListener(new com.dwlhm.finan.ui.components.OnKeypadActionListener() {
        @Override
        public void onDigitEntered(int digit) {
            if (exprString != null && exprString.length() > 0) {
                if (calcOperand == null) calcOperand = new StringBuilder();
                calcOperand.append(digit);
                updateCalcDisplay();
            } else {
                keypadManager.onDigitEntered(digit);
            }
        }

        @Override
        public void onBackspace() {
            if (exprString != null && exprString.length() > 0) {
                if (calcOperand != null && calcOperand.length() > 0) {
                    calcOperand.deleteCharAt(calcOperand.length() - 1);
                    updateCalcDisplay();
                } else {
                    popLastOperator();
                }
            } else {
                keypadManager.onBackspace();
            }
        }

        @Override
        public void onClear() {
            exprString = null;
            calcOperand = null;
            keypadManager.onClear();
            calculatorStrip.show("");
            calculatorStrip.showPreview(null);
        }

        @Override
        public void onShortcut(String shortcut) {
            if (exprString != null && exprString.length() > 0) {
                for (int i = 0; i < shortcut.length(); i++) {
                    char c = shortcut.charAt(i);
                    if (c >= '0' && c <= '9') {
                        onDigitEntered(c - '0');
                    }
                }
            } else {
                keypadManager.onShortcut(shortcut);
            }
        }

        @Override
        public void onDecimalPoint() {
            keypadManager.onDecimalPoint();
        }

        @Override
        public void onOperator(String op) {
            String current = calcOperand != null ? calcOperand.toString() : getRawInput();
            if (current.isEmpty() && (exprString == null || exprString.length() == 0)) return;

            if (exprString == null) exprString = new StringBuilder();
            if (current.isEmpty()) {
                int len = exprString.length();
                if (len >= 2) exprString.replace(len - 2, len, op + " ");
                else exprString.append(op).append(" ");
            } else {
                exprString.append(current).append(" ").append(op).append(" ");
                calcOperand = new StringBuilder();
            }
            updateCalcDisplay();
        }

        @Override
        public void onDone() {
            keypadManager.onDone();
        }
    });

    calculatorStrip = view.findViewById(R.id.capture_calculator_strip);
    

    sentencePrefix.setOnClickListener(v -> showTypeBottomSheet());

    categoryText.setOnClickListener(v -> {
        expireAmountAutoFocus();
        formValidation.clear(CaptureFormValidation.Field.CATEGORY);
        formValidation.clearErrorBackground(categoryText);
        openCategorySearchDialog();
    });

    walletText.setOnClickListener(v -> {
        expireAmountAutoFocus();
        formValidation.clear(CaptureFormValidation.Field.WALLET);
        formValidation.clearErrorBackground(walletText);
        openWalletSearchDialog(false);
    });

    saveButton.setOnClickListener(
        v -> {
          expireAmountAutoFocus();
          saveTransaction(false);
        });

    saveButton.setOnLongClickListener(v -> {
        expireAmountAutoFocus();
        saveTransaction(true);
        return true;
    });

    installAmountAutoFocusExpiry(view);
    updateCaptureMode();
  }
  private void updateInteractiveFieldTheme(TextView view, int textColorRes, int bgColorRes) {
      view.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), textColorRes));
      view.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), textColorRes)));
      android.graphics.drawable.Drawable bg = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.bg_madlibs_field).mutate();
      androidx.core.graphics.drawable.DrawableCompat.setTint(bg, androidx.core.content.ContextCompat.getColor(requireContext(), bgColorRes));
      view.setBackground(bg);
  }

  private void showTypeBottomSheet() {
      java.util.List<TransactionType> types = java.util.Arrays.asList(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER_OUT);
      
      com.dwlhm.finan.ui.common.EntitySearchBottomSheet<TransactionType> bottomSheet = new com.dwlhm.finan.ui.common.EntitySearchBottomSheet<>(
          requireContext(),
          types,
          (long) selectedType.ordinal(),
          new com.dwlhm.finan.ui.common.EntitySearchBottomSheet.ItemMapper<TransactionType>() {
              @Override
              public String getName(TransactionType item) {
                  if (item == TransactionType.EXPENSE) return getString(R.string.capture_type_expense);
                  if (item == TransactionType.INCOME) return getString(R.string.capture_type_income);
                  return getString(R.string.capture_type_transfer);
              }
              @Override
              public long getId(TransactionType item) {
                  return item.ordinal();
              }
          },
          item -> setType(item)
      );
      
      bottomSheet.show();
      android.view.View searchInput = bottomSheet.findViewById(R.id.search_input);
      if (searchInput != null) {
          searchInput.setVisibility(android.view.View.GONE);
      }
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
    if (!isDataLoaded) {
      refreshCaptureData(true);
      isDataLoaded = true;
    }
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
          if (!keepSelectedCategory || selectedCategory == null) {
            selectedCategory = state.selectedCategory;
          } else if (!isCategoryInList(selectedCategory, allCategoriesForType)) {
            selectedCategory = null;
          }
          tryRestoreCaptureDraft();
          bindWallets();
          updateCategoryLabel();
        });
  }

  private CaptureState loadCaptureState(TransactionType type, @Nullable Category retainedCategory) {
    List<Wallet> loadedWallets = services.walletService.findAll();
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
      Category defaultCat = services.categoryDao.findDefault();
      if (defaultCat != null && isCategoryInList(defaultCat, categoriesForType)) {
        selected = defaultCat;
      } else {
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
    Wallet foundDefault = services.walletService.findDefault();
    if (foundDefault != null) {
      for (Wallet wallet : loadedWallets) {
        if (wallet.getId() == foundDefault.getId()) {
          return foundDefault;
        }
      }
    }
    return foundDefault;
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

  private void applyCurrentTheme() {
      if (selectedType == null) return;
      int textColorRes;
      int bgColorRes;
      if (selectedType == TransactionType.EXPENSE) {
          textColorRes = R.color.finan_expense;
          bgColorRes = R.color.finan_expense_bg;
      } else if (selectedType == TransactionType.INCOME) {
          textColorRes = R.color.finan_income;
          bgColorRes = R.color.finan_income_bg;
      } else {
          textColorRes = R.color.finan_primary;
          bgColorRes = R.color.finan_chip_bg;
      }
      
      updateInteractiveFieldTheme(sentencePrefix, textColorRes, bgColorRes);
  }

  private void updateCaptureMode() {
    boolean transfer = selectedType.isTransfer();
    boolean expense = selectedType == TransactionType.EXPENSE;
    boolean income = selectedType == TransactionType.INCOME;

    if (transfer) {
        sentencePrefix.setText("Transfer ");
        sentenceFor.setText(" ke ");
        sentenceFrom.setText(" dari ");
        saveButton.setText("Simpan Transfer");
        updateCategoryLabel(); // Will display destination wallet
    } else {
        sentencePrefix.setText(income ? "Masuk " : "Keluar ");
        sentenceFor.setText(" untuk ");
        sentenceFrom.setText(income ? " ke " : " dari ");
        saveButton.setText(income ? "Simpan Pemasukan" : "Simpan Pengeluaran");
        updateCategoryLabel();
    }
    applyCurrentTheme();
  }

  private final Set<TextView> errorBackgroundFields = new HashSet<>();

  private void applyErrorBackground(TextView view) {
      view.setTextColor(getResources().getColor(R.color.finan_error, null));
      errorBackgroundFields.add(view);
  }

  private void clearErrorBackground(TextView view) {
      errorBackgroundFields.remove(view);
      view.setTextColor(getResources().getColor(R.color.finan_primary, null));
  }



  private void updateCategoryLabel() {
      if (selectedType.isTransfer()) {
          categoryText.setText(destinationWallet != null ? destinationWallet.getName() : "Wallet");
          categoryText.setOnClickListener(v -> {
              expireAmountAutoFocus();
              openWalletSearchDialog(true);
          });
      } else {
          categoryText.setText(selectedCategory != null ? "#" + selectedCategory.getName() : "#Category");
          categoryText.setOnClickListener(v -> {
              expireAmountAutoFocus();
              openCategorySearchDialog();
          });
      }
      applyCurrentTheme();
  }

  private void updateWalletLabel() {
      walletText.setText(activeWallet != null ? activeWallet.getName() : "Wallet");
      applyCurrentTheme();
  }

  private void updateDateLabel() {
    java.time.LocalDateTime dt = java.time.LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(occurredAtMillis), java.time.ZoneId.systemDefault());
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalDate d = dt.toLocalDate();
    String datePart;
    if (d.equals(today)) datePart = "@hari ini";
    else if (d.equals(today.minusDays(1))) datePart = "@kemarin";
    else if (d.equals(today.plusDays(1))) datePart = "@besok";
    else datePart = "@" + dt.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", new java.util.Locale("id", "ID")));
    dateText.setText(datePart + ", " + dt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
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
          this::openCategoryCreator,
          Category::isDefault
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
      boolean isTransfer = selectedType.isTransfer();
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
                  updateCategoryLabel();
              } else {
                  activeWallet = item;
                  formValidation.clear(CaptureFormValidation.Field.WALLET);
                  ensureDestinationWallet();
                  updateWalletLabel();
                  if (isTransfer) {
                      updateCategoryLabel();
                  }
              }
          },
          getString(R.string.wallet_add),
          () -> {
              AppServices s = ServicesProvider.get(requireContext());
              new WalletInputDialog(requireContext(), s, () ->
                  services.dbWorker.compute(
                      () -> s.walletService.findAll(),
                      newWallets -> {
                          if (!isAdded() || newWallets == null) return;
                          wallets = newWallets;
                          openWalletSearchDialog(isDestination);
                      }));
          }
      );
      bottomSheet.show();
  }

  private String getRawInput() {
      return amountInput.getText().toString().replaceAll("[^0-9]", "");
  }

  private void updateCalcDisplay() {
      if (exprString == null || exprString.length() == 0) return;

      String operand = calcOperand != null ? calcOperand.toString() : "";
      String fullExpr = exprString.toString() + operand;

      try {
          long result = ExpressionEvaluator.evaluate(ensureValidExpr(fullExpr));
          amountInput.removeTextChangedListener(calcTextWatcher);
          setAmountInput(result);
          amountInput.addTextChangedListener(calcTextWatcher);
          calculatorStrip.show(fullExpr);
          calculatorStrip.showPreview("= " + MoneyFormatter.formatWithoutCurrency(result));
      } catch (Exception ignored) {
          calculatorStrip.showPreview(null);
      }
      checkSaveButtonState();
  }

  private void checkSaveButtonState() {
      if (saveButton == null) return;
      if (saveInProgress) return;
      String raw = getRawInput();
      long amount = raw.isEmpty() ? 0 : Long.parseLong(raw);
      if (amount <= 0) {
          saveButton.setEnabled(false);
      } else {
          saveButton.setEnabled(true);
      }
  }

  private void finishCalculation() {
      if (exprString == null || exprString.length() == 0) return;

      String operand = calcOperand != null ? calcOperand.toString() : "";
      String fullExpr = exprString.toString() + operand;

      try {
          long result = ExpressionEvaluator.evaluate(ensureValidExpr(fullExpr));
          amountInput.removeTextChangedListener(calcTextWatcher);
          setAmountInput(result);
          amountInput.addTextChangedListener(calcTextWatcher);
      } catch (Exception ignored) {
      }

      exprString = null;
      calcOperand = null;
  }

  private final android.text.TextWatcher calcTextWatcher = new android.text.TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override
      public void afterTextChanged(android.text.Editable s) {
          if (exprString != null && exprString.length() > 0) {
              updateCalcDisplay();
          } else if (calculatorStrip != null) {
              String raw = s.toString().replaceAll("[^0-9]", "");
              calculatorStrip.show(raw.isEmpty() ? "" : raw);
              calculatorStrip.showPreview(null);
          }
          checkSaveButtonState();
      }
  };

  private void popLastOperator() {
      if (exprString == null || exprString.length() < 2) return;

      exprString.delete(exprString.length() - 2, exprString.length());
      String remaining = exprString.toString().trim();

      if (remaining.isEmpty()) {
          exprString = null;
          calcOperand = null;
          calculatorStrip.show(getRawInput());
          calculatorStrip.showPreview(null);
          return;
      }

      int lastSpace = remaining.lastIndexOf(' ');
      if (lastSpace >= 0) {
          String lastNum = remaining.substring(lastSpace + 1);
          if (lastNum.matches("-?\\d+")) {
              calcOperand = new StringBuilder(lastNum);
              exprString.setLength(lastSpace + 1);
          }
      } else if (remaining.matches("-?\\d+")) {
          calcOperand = new StringBuilder(remaining);
          exprString.setLength(0);
      }

      if (exprString.length() == 0) {
          exprString = null;
          calcOperand = null;
          calculatorStrip.show(getRawInput());
          calculatorStrip.showPreview(null);
      } else {
          updateCalcDisplay();
      }
  }

  private static String ensureValidExpr(String expr) {
      expr = expr.trim();
      while (expr.length() > 0) {
          char last = expr.charAt(expr.length() - 1);
          if (last == '+' || last == '-' || last == '*' || last == '/') {
              expr = expr.substring(0, expr.length() - 1).trim();
          } else {
              break;
          }
      }
      if (expr.isEmpty()) expr = "0";
      return expr;
  }

  private void setAmountInput(long amountMinor) {
    String formatted = MoneyFormatter.formatWithoutCurrency(amountMinor);
    amountInput.setText(formatted);
    amountInput.setSelection(formatted.length());
  }

  private void forceClearSavedForm() {
    formValidation.clearAll();
    validationBanner.setVisibility(View.GONE);
    amountInput.setText("");
    noteInput.setText("");
    occurredAtMillis = System.currentTimeMillis();
    updateDateLabel();
    selectedCategory = null;
    updateCategoryLabel();
  }

  private void saveTransaction(boolean clearAfterSave) {
    if (saveInProgress) return;
    ValidatedCaptureInput input = validateCaptureInput();
    if (input == null) {
      return;
    }
    long amountMinor = input.amountMinor;
    if (selectedType.isTransfer()) {
      saveTransfer(amountMinor, clearAfterSave);
      return;
    }

    Transaction transaction =
        new Transaction(
            0L,
            amountMinor,
            selectedType,
            activeWallet.getId(),
            selectedCategory.getId(),
            occurredAtMillis,
            null);
    String note = noteInput.getText().toString().trim();
    if (!TextUtils.isEmpty(note)) {
      transaction.setNote(note);
    }
    final String savedNote = TextUtils.isEmpty(note) ? null : note;

    Wallet walletToRemember = activeWallet;
    saveInProgress = true;
    services.dbWorker.compute(
        () -> {
          try {
            return transactionService.save(transaction);
          } catch (IllegalArgumentException e) {
            return 0L;
          }
        },
        savedId -> {
          saveInProgress = false;
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
                  savedId, amountMinor, savedNote);
          
          saveButton.setText("✓ Tersimpan");
          saveButton.setEnabled(false);
          saveButton.postDelayed(() -> {
              updateCaptureMode();
              checkSaveButtonState();
          }, 1500);

          if (clearAfterSave) {
              forceClearSavedForm();
          } else {
              clearSavedForm();
          }
          refreshCaptureData(false);
        });
  }

  private void saveTransfer(long amountMinor, boolean clearAfterSave) {
    Wallet source = activeWallet;
    Wallet destination = destinationWallet;
    long occurredAt = occurredAtMillis;
    String note = noteInput.getText().toString().trim();
    String savedNote = TextUtils.isEmpty(note) ? null : note;
    saveInProgress = true;
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
          saveInProgress = false;
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

          saveButton.setText("✓ Tersimpan");
          saveButton.setEnabled(false);
          saveButton.postDelayed(() -> {
              updateCaptureMode();
              checkSaveButtonState();
          }, 1500);

          if (clearAfterSave) {
              forceClearSavedForm();
          } else {
              clearSavedForm();
          }
          refreshCaptureData(false);
        });
  }

  private void clearSavedForm() {
    formValidation.clearAll();
    validationBanner.setVisibility(View.GONE);
    // User requested to keep the last state including nominal amount, so we don't reset inputs here.
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
        });
  }

  private PendingSaveUndo snapshotPendingSaveUndo(
      long transactionId,
      long amountMinor,
      @Nullable String note) {
    return new PendingSaveUndo(
        transactionId,
        false,
        amountMinor,
        selectedType,
        activeWallet.getId(),
        selectedCategory.getId(),
        null,
        occurredAtMillis,
        note);
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
                draft.note);
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
    occurredAtMillis = draft.getOccurredAtMillis();
    updateDateLabel();

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
      services.dbWorker.compute(
          () -> services.categoryDao.findById(categoryId),
          category -> {
            if (!isAdded()) return;
            if (category != null && selectedType.name().equals(category.getTypeFilter())) {
              selectedCategory = category;
            } else {
              selectedCategory = null;
            }
            formValidation.clearAll();
            validationBanner.setVisibility(View.GONE);
            updateCaptureMode();
          });
    } else {
      formValidation.clearAll();
      validationBanner.setVisibility(View.GONE);
      updateCaptureMode();
    }
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
    draft.setOccurredAtMillis(occurredAtMillis);
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

    private PendingSaveUndo(
        long recordId,
        boolean transfer,
        long amountMinor,
        TransactionType type,
        long walletId,
        long categoryId,
        @Nullable Long destinationWalletId,
        long occurredAtMillis,
        @Nullable String note) {
      this.recordId = recordId;
      this.transfer = transfer;
      this.amountMinor = amountMinor;
      this.type = type;
      this.walletId = walletId;
      this.categoryId = categoryId;
      this.destinationWalletId = destinationWalletId;
      this.occurredAtMillis = occurredAtMillis;
      this.note = note;
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
          note);
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
          return !keypadBounds.contains(rawX, rawY);
      }
    }

    return true;
  }

  private void expireAmountAutoFocus() {
      if (exprString != null && exprString.length() > 0) {
          finishCalculation();
      }
      amountInput.clearFocus();
    InputMethodManager imm =
        (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.hideSoftInputFromWindow(amountInput.getWindowToken(), 0);
    }
  }

  @Nullable
  private ValidatedCaptureInput validateCaptureInput() {
    if (exprString != null && exprString.length() > 0) {
        finishCalculation();
    }

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
