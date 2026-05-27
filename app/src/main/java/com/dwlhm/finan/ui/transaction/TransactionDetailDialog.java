package com.dwlhm.finan.ui.transaction;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.CategorySearchDialog;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
import com.dwlhm.finan.ui.common.UiComponentStyles;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class TransactionDetailDialog extends Dialog {

  public interface Listener {
    void onTransactionChanged();
  }

  private final AppServices services;
  private final long transactionId;
  private final Listener listener;
  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("d MMM yyyy, HH:mm", new Locale("id", "ID"));

  private Transaction transaction;
  private boolean editing;

  private TextView titleView;
  private TextView amountView;
  private TextView typeView;
  private TextView walletView;
  private TextView categoryView;
  private TextView dateView;
  private TextView noteView;
  private android.view.View detailPanel;
  private android.view.View editPanel;
  private EditText amountInput;
  private RadioGroup typeGroup;
  private Spinner walletSpinner;
  private Button categoryButton;
  private EditText noteInput;
  private Button secondaryButton;
  private Button primaryButton;

  private List<Wallet> wallets = new ArrayList<>();
  private List<Category> allCategoriesForType = new ArrayList<>();
  private Wallet selectedWallet;
  private Category selectedCategory;
  private TransactionType selectedType = TransactionType.EXPENSE;
  private boolean suppressWalletSpinner;

  public TransactionDetailDialog(
      @NonNull Context context,
      @NonNull AppServices services,
      @NonNull Transaction transaction,
      @Nullable Listener listener) {
    super(context);
    this.services = services;
    this.transactionId = transaction.getId();
    this.transaction = transaction;
    this.listener = listener;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_transaction_detail);
    bindViews();
    MoneyInputFormatter.attach(amountInput, false);
    categoryButton.setOnClickListener(v -> openCategorySearchDialog());
    if (!reloadTransaction()) {
      dismiss();
      return;
    }
    bind();
  }

  @Override
  protected void onStart() {
    super.onStart();
    Window window = getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      window.setLayout(
          WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
      window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
  }

  private void bindViews() {
    titleView = findViewById(R.id.transaction_dialog_title);
    detailPanel = findViewById(R.id.transaction_detail_panel);
    editPanel = findViewById(R.id.transaction_edit_panel);
    amountView = findViewById(R.id.transaction_detail_amount);
    typeView = findViewById(R.id.transaction_detail_type);
    walletView = findViewById(R.id.transaction_detail_wallet);
    categoryView = findViewById(R.id.transaction_detail_category);
    dateView = findViewById(R.id.transaction_detail_date);
    noteView = findViewById(R.id.transaction_detail_note);
    LabeledEditTextView amountField = findViewById(R.id.transaction_edit_amount_field);
    amountInput = amountField.getEditText();
    typeGroup = findViewById(R.id.transaction_edit_type_group);
    walletSpinner = findViewById(R.id.transaction_edit_wallet_spinner);
    categoryButton = findViewById(R.id.transaction_edit_category_button);
    noteInput = findViewById(R.id.transaction_edit_note);
    secondaryButton = findViewById(R.id.transaction_action_secondary);
    primaryButton = findViewById(R.id.transaction_action_primary);
  }

  private boolean reloadTransaction() {
    Transaction refreshed = services.transactionGateway.findById(transactionId);
    if (refreshed == null) {
      Toast.makeText(getContext(), R.string.transaction_not_found, Toast.LENGTH_SHORT).show();
      return false;
    }
    transaction = refreshed;
    return true;
  }

  private void bind() {
    titleView.setText(editing ? R.string.transaction_edit_title : R.string.transaction_detail_title);
    detailPanel.setVisibility(editing ? android.view.View.GONE : android.view.View.VISIBLE);
    editPanel.setVisibility(editing ? android.view.View.VISIBLE : android.view.View.GONE);
    if (editing) {
      bindEdit();
    } else {
      bindDetail();
    }
  }

  private void bindDetail() {
    Category category = services.categoryDao.findById(transaction.getCategoryId());
    Wallet wallet = services.walletDao.findById(transaction.getWalletId());
    boolean income = transaction.getType() == TransactionType.INCOME;

    amountView.setText((income ? "+" : "-") + MoneyFormatter.format(transaction.getAmountMinor()));
    amountView.setTextColor(
        ContextCompat.getColor(getContext(), income ? R.color.finan_income : R.color.finan_expense));
    typeView.setText(income ? R.string.capture_type_income : R.string.capture_type_expense);
    walletView.setText(wallet != null ? wallet.getName() : "—");
    categoryView.setText(category != null ? category.getName() : "—");
    dateView.setText(dateFormat.format(new Date(transaction.getOccurredAt())));
    String note = transaction.getNote();
    boolean hasNote = !TextUtils.isEmpty(note);
    noteView.setText(hasNote ? note : getContext().getString(R.string.transaction_note_empty));
    noteView.setTextColor(
        ContextCompat.getColor(
            getContext(), hasNote ? R.color.finan_text_primary : R.color.finan_text_hint));

    secondaryButton.setText(android.R.string.cancel);
    secondaryButton.setOnClickListener(v -> dismiss());
    primaryButton.setText(R.string.transaction_edit_action);
    primaryButton.setOnClickListener(v -> beginEdit());
  }

  private void beginEdit() {
    editing = true;
    selectedType = transaction.getType();
    selectedWallet = services.walletDao.findById(transaction.getWalletId());
    selectedCategory = services.categoryDao.findById(transaction.getCategoryId());
    bind();
    amountInput.requestFocus();
  }

  private void bindEdit() {
    amountInput.setText(MoneyFormatter.formatWithoutCurrency(transaction.getAmountMinor()));
    amountInput.setSelection(amountInput.getText().length());
    noteInput.setText(transaction.getNote() == null ? "" : transaction.getNote());
    noteInput.setSelection(noteInput.getText().length());

    typeGroup.setOnCheckedChangeListener(null);
    typeGroup.check(
        selectedType == TransactionType.INCOME
            ? R.id.transaction_edit_type_income
            : R.id.transaction_edit_type_expense);
    typeGroup.setOnCheckedChangeListener(
        (group, checkedId) -> {
          selectedType =
              checkedId == R.id.transaction_edit_type_income
                  ? TransactionType.INCOME
                  : TransactionType.EXPENSE;
          selectedCategory = null;
          bindCategories();
        });

    bindWalletSpinner();
    bindCategories();

    secondaryButton.setText(android.R.string.cancel);
    secondaryButton.setOnClickListener(
        v -> {
          editing = false;
          bind();
        });
    primaryButton.setText(R.string.transaction_edit_save);
    primaryButton.setOnClickListener(v -> saveEdit());
  }

  private void bindWalletSpinner() {
    wallets = services.walletDao.findAll();
    if (wallets.isEmpty()) {
      selectedWallet = null;
      walletSpinner.setAdapter(null);
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
      if (selectedWallet != null && wallet.getId() == selectedWallet.getId()) {
        selectedIndex = i;
      }
    }

    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, labels);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    suppressWalletSpinner = true;
    walletSpinner.setAdapter(adapter);
    walletSpinner.setSelection(selectedIndex);
    selectedWallet = wallets.get(selectedIndex);
    suppressWalletSpinner = false;
    walletSpinner.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(
              android.widget.AdapterView<?> parent,
              android.view.View selectedView,
              int position,
              long id) {
            if (suppressWalletSpinner || position < 0 || position >= wallets.size()) {
              return;
            }
            selectedWallet = wallets.get(position);
          }

          @Override
          public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
  }

  private void bindCategories() {
    allCategoriesForType =
        services.categoryDao.findByTypeFilterOrderByUsage(selectedType.name());
    if (selectedCategory != null && !isCategoryAvailable(selectedCategory)) {
      selectedCategory = null;
    }
    updateCategoryButton();
  }

  private boolean isCategoryAvailable(Category category) {
    for (Category option : allCategoriesForType) {
      if (option.getId() == category.getId()) {
        return true;
      }
    }
    return false;
  }

  private void updateCategoryButton() {
    boolean selected = selectedCategory != null;
    categoryButton.setText(
        selected
            ? selectedCategory.getName()
            : getContext().getString(R.string.transaction_category_pick));
    int selectedBackgroundRes =
        selectedType == TransactionType.INCOME
            ? R.drawable.bg_control_selected_income
            : R.drawable.bg_control_selected_expense;
    UiComponentStyles.setSelectButtonSelected(
        getContext(), categoryButton, selected, selectedBackgroundRes);
  }

  private void openCategorySearchDialog() {
    CategorySearchDialog dialog =
        new CategorySearchDialog(
            getContext(),
            services.categoryDao,
            selectedType,
            allCategoriesForType,
            (category, created) -> {
              selectedCategory = category;
              if (created) {
                bindCategories();
              } else {
                updateCategoryButton();
              }
            });
    dialog.show();
  }

  private void saveEdit() {
    long amountMinor;
    try {
      amountMinor = MoneyParser.parse(amountInput.getText().toString());
    } catch (IllegalArgumentException e) {
      amountInput.setError(getContext().getString(R.string.transaction_error_amount));
      return;
    }
    if (selectedCategory == null) {
      Toast.makeText(getContext(), R.string.transaction_error_category, Toast.LENGTH_SHORT).show();
      return;
    }
    if (selectedWallet == null) {
      Toast.makeText(getContext(), R.string.transaction_error_wallet, Toast.LENGTH_SHORT).show();
      return;
    }

    String note = noteInput.getText().toString().trim();
    Transaction updated =
        new Transaction(
            transaction.getId(),
            amountMinor,
            selectedType,
            selectedWallet.getId(),
            selectedCategory.getId(),
            transaction.getOccurredAt(),
            TextUtils.isEmpty(note) ? null : note);
    try {
      services.transactionService.edit(updated);
    } catch (IllegalArgumentException e) {
      Toast.makeText(getContext(), R.string.transaction_error_update, Toast.LENGTH_SHORT).show();
      return;
    }

    if (listener != null) {
      listener.onTransactionChanged();
    }
    Toast.makeText(getContext(), R.string.transaction_updated, Toast.LENGTH_SHORT).show();
    if (!reloadTransaction()) {
      dismiss();
      return;
    }
    editing = false;
    bind();
  }
}
