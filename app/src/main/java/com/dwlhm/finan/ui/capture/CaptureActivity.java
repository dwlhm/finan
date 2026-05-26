package com.dwlhm.finan.ui.capture;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.data.prefs.DefaultsStore;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.transaction.TransactionService;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BaseActivity;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.history.TransactionListAdapter;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyParser;

import java.util.ArrayList;
import java.util.List;

public class CaptureActivity extends BaseActivity {

  private static final int RECENT_LIMIT = 8;
  private static final int QUICK_CATEGORY_SLOTS = 4;

  private AppServices services;
  private TransactionService transactionService;
  private DefaultsStore defaultsStore;

  private EditText amountInput;
  private LinearLayout quickCategories;
  private Button categoryMoreButton;
  private Spinner walletSpinner;
  private View detailsPanel;
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

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    services = ServicesProvider.get(this);
    transactionService = services.transactionService;
    defaultsStore = services.defaultsStore;
    super.onCreate(savedInstanceState);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_capture;
  }

  @Override
  protected void onReady() {
    amountInput = findViewById(R.id.capture_amount);
    quickCategories = findViewById(R.id.capture_quick_categories);
    categoryMoreButton = findViewById(R.id.capture_category_more);
    walletSpinner = findViewById(R.id.capture_wallet_spinner);
    detailsPanel = findViewById(R.id.capture_details_panel);
    typeGroup = findViewById(R.id.capture_type_group);
    noteInput = findViewById(R.id.capture_note);
    recentList = findViewById(R.id.capture_recent_list);
    recentEmpty = findViewById(R.id.capture_recent_empty);

    Button saveButton = findViewById(R.id.capture_save);
    Button detailsToggle = findViewById(R.id.capture_details_toggle);

    recentAdapter = new TransactionListAdapter(this, services.categoryDao, services.walletDao);
    recentList.setAdapter(recentAdapter);

    resolveActiveWallet();

    typeGroup.setOnCheckedChangeListener(
        (group, checkedId) -> {
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
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (suppressWalletSpinner || position < 0 || position >= wallets.size()) {
              return;
            }
            activeWallet = wallets.get(position);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    categoryMoreButton.setOnClickListener(v -> openCategorySearchDialog());

    detailsToggle.setOnClickListener(
        v ->
            detailsPanel.setVisibility(
                detailsPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

    saveButton.setOnClickListener(v -> saveTransaction());

    bindWalletSpinner();
    bindCategories();
    refreshRecent();

    amountInput.requestFocus();
  }

  @Override
  protected void onResume() {
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
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
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
      Button chip = new Button(this, null, android.R.attr.borderlessButtonStyle);
      chip.setText(category.getName());
      chip.setAllCaps(false);
      chip.setTextSize(13f);
      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
      params.setMarginEnd(8);
      chip.setLayoutParams(params);
      styleChip(chip, category);
      chip.setOnClickListener(
          v -> {
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
            this,
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
    if (selected) {
      chip.setBackgroundResource(R.drawable.bg_chip_selected);
      chip.setTextColor(ContextCompat.getColor(this, R.color.finan_chip_text_selected));
    } else {
      chip.setBackgroundResource(R.drawable.bg_chip);
      chip.setTextColor(ContextCompat.getColor(this, R.color.finan_chip_text));
    }
  }

  private void saveTransaction() {
    long amountMinor;
    try {
      amountMinor = MoneyParser.parse(amountInput.getText().toString());
    } catch (IllegalArgumentException e) {
      Toast.makeText(this, R.string.capture_error_amount, Toast.LENGTH_SHORT).show();
      return;
    }
    if (selectedCategory == null) {
      Toast.makeText(this, R.string.capture_error_category, Toast.LENGTH_SHORT).show();
      return;
    }
    if (activeWallet == null) {
      Toast.makeText(this, R.string.capture_wallet_unknown, Toast.LENGTH_SHORT).show();
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
      Toast.makeText(this, R.string.capture_error_save, Toast.LENGTH_SHORT).show();
      return;
    }

    defaultsStore.setLastWalletId(activeWallet.getId());
    Toast.makeText(this, R.string.capture_saved, Toast.LENGTH_SHORT).show();
    amountInput.setText("");
    noteInput.setText("");
    selectedCategory = null;
    resolveActiveWallet();
    bindWalletSpinner();
    bindCategories();
    refreshRecent();
    amountInput.requestFocus();
  }

  private void refreshRecent() {
    List<Transaction> recent = transactionService.getRecent(RECENT_LIMIT);
    recentAdapter.setTransactions(recent);
    boolean empty = recent.isEmpty();
    recentEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    recentList.setVisibility(empty ? View.GONE : View.VISIBLE);
  }
}
