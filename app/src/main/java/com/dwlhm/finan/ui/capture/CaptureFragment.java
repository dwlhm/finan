package com.dwlhm.finan.ui.capture;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.transaction.TransactionService;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.CategorySearchDialog;
import com.dwlhm.finan.ui.common.CollapsibleController;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.UiComponentStyles;
import com.dwlhm.finan.ui.transaction.TransactionDetailDialog;
import com.dwlhm.finan.ui.transaction.TransactionListAdapter;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;

import java.util.ArrayList;
import java.util.List;

public final class CaptureFragment extends ScreenFragment {

  private static final int RECENT_LIMIT = 8;
  private static final int QUICK_CATEGORY_SLOTS = 4;

  private AppServices services;
  private TransactionService transactionService;
  private DefaultsStore defaultsStore;

  private EditText amountInput;
  private LinearLayout quickCategories;
  private Button categoryMoreButton;
  private Spinner walletSpinner;
  private RadioGroup typeGroup;
  private EditText noteInput;
  private ListView recentList;
  private TextView recentEmpty;

  private Wallet activeWallet;
  private Category selectedCategory;
  private TransactionType selectedType = TransactionType.EXPENSE;
  private TransactionListAdapter recentAdapter;

  private List<Wallet> wallets = new ArrayList<>();
  private List<Category> allCategoriesForType = new ArrayList<>();
  private List<Category> quickCategoryList = new ArrayList<>();
  private boolean suppressWalletSpinner;
  private boolean amountAutoFocusExpired;

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
    quickCategories = view.findViewById(R.id.capture_quick_categories);
    categoryMoreButton = view.findViewById(R.id.capture_category_more);
    walletSpinner = view.findViewById(R.id.capture_wallet_spinner);
    typeGroup = view.findViewById(R.id.capture_type_group);
    noteInput = view.findViewById(R.id.capture_note);
    recentList = view.findViewById(R.id.capture_recent_list);
    recentEmpty = view.findViewById(R.id.capture_recent_empty);

    Button saveButton = view.findViewById(R.id.capture_save);

    recentAdapter =
        new TransactionListAdapter(requireContext(), services.categoryDao, services.walletDao);
    recentList.setAdapter(recentAdapter);
    recentList.setOnItemClickListener(
        (parent, itemView, position, id) ->
            new TransactionDetailDialog(
                    requireContext(), services, recentAdapter.getItem(position), this::refreshRecent)
                .show());

    resolveActiveWallet();

    typeGroup.setOnCheckedChangeListener(
        (group, checkedId) -> {
          expireAmountAutoFocus();
          selectedType =
              checkedId == R.id.capture_type_income
                  ? TransactionType.INCOME
                  : TransactionType.EXPENSE;
          selectedCategory = null;
          bindCategories();
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
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    categoryMoreButton.setOnClickListener(
        v -> {
          expireAmountAutoFocus();
          openCategorySearchDialog();
        });

    CollapsibleController.bind(
        view,
        R.id.capture_details_toggle,
        R.id.capture_details_panel,
        R.id.capture_details_chevron);

    saveButton.setOnClickListener(
        v -> {
          expireAmountAutoFocus();
          saveTransaction();
        });
    installAmountAutoFocusExpiry(view);

    bindWalletSpinner();
    bindCategories();
    refreshRecent();

    requestAmountFocus();
  }

  @Override
  public void onResume() {
    super.onResume();
    resolveActiveWallet();
    bindWalletSpinner();
    refreshRecent();
  }

  private void resolveActiveWallet() {
    Long lastWalletId = defaultsStore.getLastWalletId();
    if (lastWalletId != null) {
      Wallet last = services.walletDao.findById(lastWalletId);
      if (last != null) {
        activeWallet = last;
        return;
      }
    }
    if (defaultsStore.hasDefaultWalletId()) {
      Wallet configured = services.walletDao.findById(defaultsStore.getDefaultWalletId());
      if (configured != null) {
        activeWallet = configured;
        return;
      }
    }
    activeWallet = services.walletDao.findDefault();
    if (activeWallet != null && !defaultsStore.hasDefaultWalletId()) {
      defaultsStore.setDefaultWalletId(activeWallet.getId());
    }
  }

  private void bindWalletSpinner() {
    wallets = services.walletDao.findAll();
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
    allCategoriesForType =
        services.categoryDao.findByTypeFilterOrderByUsage(selectedType.name());

    quickCategoryList = new ArrayList<>();
    int quickCount = Math.min(QUICK_CATEGORY_SLOTS, allCategoriesForType.size());
    for (int i = 0; i < quickCount; i++) {
      quickCategoryList.add(allCategoriesForType.get(i));
    }

    bindQuickCategoryChips();
    updateCategoryMoreButtonLabel();
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
              if (created) {
                bindCategories();
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
    long amountMinor;
    try {
      amountMinor = MoneyParser.parse(amountInput.getText().toString());
    } catch (IllegalArgumentException e) {
      Toast.makeText(requireContext(), R.string.capture_error_amount, Toast.LENGTH_SHORT).show();
      return;
    }
    if (selectedCategory == null) {
      Toast.makeText(requireContext(), R.string.capture_error_category, Toast.LENGTH_SHORT).show();
      return;
    }
    if (activeWallet == null) {
      Toast.makeText(requireContext(), R.string.capture_wallet_unknown, Toast.LENGTH_SHORT).show();
      return;
    }

    Transaction transaction =
        new Transaction(
            0L,
            amountMinor,
            selectedType,
            activeWallet.getId(),
            selectedCategory.getId(),
            0L,
            null);
    String note = noteInput.getText().toString().trim();
    if (!TextUtils.isEmpty(note)) {
      transaction.setNote(note);
    }

    try {
      transactionService.save(transaction);
    } catch (IllegalArgumentException e) {
      Toast.makeText(requireContext(), R.string.capture_error_save, Toast.LENGTH_SHORT).show();
      return;
    }

    defaultsStore.setLastWalletId(activeWallet.getId());
    Toast.makeText(requireContext(), R.string.capture_saved, Toast.LENGTH_SHORT).show();
    amountInput.setText("");
    noteInput.setText("");
    selectedCategory = null;
    resolveActiveWallet();
    bindWalletSpinner();
    bindCategories();
    refreshRecent();
    requestAmountFocus();
  }

  private void installAmountAutoFocusExpiry(@NonNull View root) {
    bindAmountAutoFocusExpiryOnTouchTree(root);
  }

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
    if (!(view instanceof ViewGroup)) {
      return;
    }
    ViewGroup group = (ViewGroup) view;
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

  private void refreshRecent() {
    List<Transaction> recent = transactionService.getRecent(RECENT_LIMIT);
    recentAdapter.setTransactions(recent);
    boolean empty = recent.isEmpty();
    recentEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    recentList.setVisibility(empty ? View.GONE : View.VISIBLE);
  }
}
