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
import com.dwlhm.finan.data.entity.Merchant;
import com.dwlhm.finan.data.entity.Tag;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.data.prefs.TransactionFormDraft;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.CategorySearchDialog;
import com.dwlhm.finan.ui.common.EntityLookup;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
import com.dwlhm.finan.ui.common.MerchantSelectionController;
import com.dwlhm.finan.ui.common.TagSelectionController;
import com.dwlhm.finan.ui.common.TransactionOccurredAtPicker;
import com.dwlhm.finan.ui.common.UiComponentStyles;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TransactionDetailDialog extends Dialog {

  public interface Listener {
    void onTransactionChanged();
  }

  private final AppServices services;
  private final long transactionId;
  private final Listener listener;
  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"));

  private Transaction transaction;
  private boolean editing;
  private int loadGeneration;

  private TextView titleView;
  private TextView amountView;
  private TextView typeView;
  private TextView walletView;
  private TextView categoryView;
  private TextView dateView;
  private TextView noteView;
  private TextView merchantView;
  private TextView tagsView;
  private android.view.View detailPanel;
  private android.view.View editPanel;
  private EditText amountInput;
  private RadioGroup typeGroup;
  private Spinner walletSpinner;
  private Button categoryButton;
  private EditText noteInput;
  private Button secondaryButton;
  private Button primaryButton;
  private TransactionOccurredAtPicker occurredAtPicker;
  private TagSelectionController tagSelection;
  private MerchantSelectionController merchantSelection;

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
    MoneyInputFormatter.attach(amountInput, true);
    categoryButton.setOnClickListener(v -> openCategorySearchDialog());
    loadDetail();
  }

  @Override
  protected void onStart() {
    super.onStart();
    Window window = getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      window.setLayout(
          WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
  }

  @Override
  protected void onStop() {
    persistEditDraft();
    super.onStop();
  }

  @Override
  public void dismiss() {
    persistEditDraft();
    loadGeneration++;
    super.dismiss();
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
    merchantView = findViewById(R.id.transaction_detail_merchant);
    tagsView = findViewById(R.id.transaction_detail_tags);
    LabeledEditTextView amountField = findViewById(R.id.transaction_edit_amount_field);
    amountInput = amountField.getEditText();
    typeGroup = findViewById(R.id.transaction_edit_type_group);
    walletSpinner = findViewById(R.id.transaction_edit_wallet_spinner);
    categoryButton = findViewById(R.id.transaction_edit_category_button);
    noteInput = findViewById(R.id.transaction_edit_note);
    secondaryButton = findViewById(R.id.transaction_action_secondary);
    primaryButton = findViewById(R.id.transaction_action_primary);
    occurredAtPicker =
        new TransactionOccurredAtPicker(
            getContext(),
            findViewById(R.id.transaction_occurred_date),
            findViewById(R.id.transaction_occurred_time),
            System.currentTimeMillis());
    tagSelection =
        new TagSelectionController(
            getContext(),
            services.tagDao,
            services.dbWorker,
            findViewById(R.id.transaction_tag_chips),
            findViewById(R.id.transaction_tag_add));
    merchantSelection =
        new MerchantSelectionController(
            getContext(),
            services.merchantDao,
            services.dbWorker,
            findViewById(R.id.transaction_merchant_pick),
            findViewById(R.id.transaction_merchant_clear));
  }

  private void loadDetail() {
    int generation = ++loadGeneration;
    services.dbWorker.compute(
        () -> {
          Transaction refreshed = services.transactionGateway.findById(transactionId);
          if (refreshed == null) {
            return null;
          }
          Category category = services.categoryDao.findById(refreshed.getCategoryId());
          Wallet wallet = services.walletDao.findById(refreshed.getWalletId());
          Merchant merchant =
              refreshed.getMerchantId() == null
                  ? null
                  : services.merchantDao.findById(refreshed.getMerchantId());
          Map<Long, Tag> tagsById =
              EntityLookup.tagLookupForTransactions(
                  services.tagDao.findAllOrderByUsage(),
                  java.util.Collections.singletonList(refreshed),
                  services.tagDao::findById);
          return new DetailData(refreshed, category, wallet, merchant, tagsById);
        },
        data -> {
          if (generation != loadGeneration) {
            return;
          }
          if (data == null) {
            Toast.makeText(getContext(), R.string.transaction_not_found, Toast.LENGTH_SHORT).show();
            dismiss();
            return;
          }
          transaction = data.transaction;
          if (editing) {
            bindEdit();
          } else {
            bindDetail(data.category, data.wallet, data.merchant, data.tagsById);
          }
        });
  }

  private void bindDetail(
      @Nullable Category category,
      @Nullable Wallet wallet,
      @Nullable Merchant merchant,
      @NonNull Map<Long, Tag> tagsById) {
    titleView.setText(R.string.transaction_detail_title);
    detailPanel.setVisibility(android.view.View.VISIBLE);
    editPanel.setVisibility(android.view.View.GONE);
    boolean income = transaction.getType() == TransactionType.INCOME;

    amountView.setText(
        getContext()
            .getString(
                income
                    ? R.string.transaction_income_amount_format
                    : R.string.transaction_expense_amount_format,
                MoneyFormatter.format(transaction.getAmountMinor())));
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

    boolean hasMerchant = merchant != null;
    merchantView.setText(
        hasMerchant ? merchant.getName() : getContext().getString(R.string.transaction_merchant_empty));
    merchantView.setTextColor(
        ContextCompat.getColor(
            getContext(), hasMerchant ? R.color.finan_text_primary : R.color.finan_text_hint));

    String tagLine = TransactionRowLabels.formatTagLine(transaction, tagsById);
    boolean hasTags = !TextUtils.isEmpty(tagLine);
    tagsView.setText(
        hasTags ? tagLine : getContext().getString(R.string.transaction_tags_empty));
    tagsView.setTextColor(
        ContextCompat.getColor(
            getContext(), hasTags ? R.color.finan_text_primary : R.color.finan_text_hint));

    secondaryButton.setText(android.R.string.cancel);
    secondaryButton.setOnClickListener(v -> dismiss());
    primaryButton.setText(R.string.transaction_edit_action);
    primaryButton.setOnClickListener(v -> beginEdit());
  }

  private void beginEdit() {
    int generation = ++loadGeneration;
    TransactionType type = transaction.getType();
    TransactionFormDraft pendingDraft = services.defaultsStore.getEditDraft(transactionId);
    if (pendingDraft != null) {
      type = pendingDraft.getType();
    }
    final TransactionType loadType = type;
    services.dbWorker.compute(
        () -> {
          List<Wallet> loadedWallets = services.walletDao.findAll();
          List<Category> categories =
              services.categoryDao.findByTypeFilterOrderByUsage(loadType.name());
          Wallet wallet = services.walletDao.findById(transaction.getWalletId());
          Category category = services.categoryDao.findById(transaction.getCategoryId());
          return new EditData(loadedWallets, categories, wallet, category);
        },
        data -> {
          if (generation != loadGeneration || data == null) {
            return;
          }
          editing = true;
          selectedType = loadType;
          wallets = data.wallets;
          allCategoriesForType = data.categoriesForType;
          selectedWallet = data.selectedWallet;
          selectedCategory = data.selectedCategory;
          bindEdit();
          amountInput.requestFocus();
        });
  }

  private void bindEdit() {
    titleView.setText(R.string.transaction_edit_title);
    detailPanel.setVisibility(android.view.View.GONE);
    editPanel.setVisibility(android.view.View.VISIBLE);
    amountInput.setText(MoneyFormatter.format(transaction.getAmountMinor()));
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
          loadCategoriesForEdit();
        });

    bindWalletSpinner();
    updateCategoryButton();
    occurredAtPicker.setOccurredAtMillis(transaction.getOccurredAt());
    merchantSelection.setMerchantId(transaction.getMerchantId());
    tagSelection.setSelectedTagIds(transaction.getTagIds());

    tryRestoreEditDraft();

    secondaryButton.setText(android.R.string.cancel);
    secondaryButton.setOnClickListener(
        v -> {
          services.defaultsStore.clearEditDraft(transactionId);
          editing = false;
          loadDetail();
        });
    primaryButton.setText(R.string.transaction_edit_save);
    primaryButton.setOnClickListener(v -> saveEdit());
  }

  private void loadCategoriesForEdit() {
    TransactionType type = selectedType;
    services.dbWorker.compute(
        () -> services.categoryDao.findByTypeFilterOrderByUsage(type.name()),
        categories -> {
          if (!isShowing() || categories == null) {
            return;
          }
          allCategoriesForType = categories;
          if (selectedCategory != null && isCategoryUnavailable(selectedCategory)) {
            selectedCategory = null;
          }
          updateCategoryButton();
        });
  }

  private void bindWalletSpinner() {
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

  private boolean isCategoryUnavailable(Category category) {
    for (Category option : allCategoriesForType) {
      if (option.getId() == category.getId()) {
        return false;
      }
    }
    return true;
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
                loadCategoriesForEdit();
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
            occurredAtPicker.getOccurredAtMillis(),
            TextUtils.isEmpty(note) ? null : note);
    updated.setMerchantId(merchantSelection.getMerchantId());
    updated.setTagIds(tagSelection.getSelectedTagIds());
    services.dbWorker.compute(
        () -> {
          try {
            services.transactionService.edit(updated);
            return Boolean.TRUE;
          } catch (IllegalArgumentException e) {
            return Boolean.FALSE;
          }
        },
        saved -> {
          if (!isShowing()) {
            return;
          }
          if (!Boolean.TRUE.equals(saved)) {
            Toast.makeText(getContext(), R.string.transaction_error_update, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          if (listener != null) {
            listener.onTransactionChanged();
          }
          Toast.makeText(getContext(), R.string.transaction_updated, Toast.LENGTH_SHORT).show();
          services.defaultsStore.clearEditDraft(transactionId);
          transaction = updated;
          editing = false;
          loadDetail();
        });
  }

  private void tryRestoreEditDraft() {
    TransactionFormDraft draft = services.defaultsStore.getEditDraft(transactionId);
    if (draft == null) {
      return;
    }
    Long draftTransactionId = draft.getTransactionId();
    if (draftTransactionId != null && draftTransactionId != transactionId) {
      return;
    }
    applyEditDraft(draft);
  }

  private void applyEditDraft(@NonNull TransactionFormDraft draft) {
    selectedType = draft.getType();
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
          loadCategoriesForEdit();
        });

    if (draft.getAmountMinor() > 0L) {
      String formatted = MoneyFormatter.format(draft.getAmountMinor());
      amountInput.setText(formatted);
      amountInput.setSelection(formatted.length());
    }
    noteInput.setText(draft.getNote() != null ? draft.getNote() : "");
    merchantSelection.setMerchantId(draft.getMerchantId());
    tagSelection.setSelectedTagIds(draft.getTagIds());
    occurredAtPicker.setOccurredAtMillis(draft.getOccurredAtMillis());

    Long walletId = draft.getWalletId();
    if (walletId != null) {
      for (Wallet wallet : wallets) {
        if (wallet.getId() == walletId) {
          selectedWallet = wallet;
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

    bindWalletSpinner();
    updateCategoryButton();
    if (selectedType != transaction.getType()
        || (selectedCategory != null && isCategoryUnavailable(selectedCategory))) {
      loadCategoriesForEdit();
    }
  }

  private void persistEditDraft() {
    if (!editing || amountInput == null) {
      return;
    }
    TransactionFormDraft draft = buildEditDraft();
    if (!formDiffersFromTransaction(draft)) {
      services.defaultsStore.clearEditDraft(transactionId);
      return;
    }
    if (draft.hasContent()) {
      services.defaultsStore.setEditDraft(transactionId, draft);
    } else {
      services.defaultsStore.clearEditDraft(transactionId);
    }
  }

  private boolean formDiffersFromTransaction(@NonNull TransactionFormDraft draft) {
    return !draft.equalsSavedTransaction(transaction);
  }

  @NonNull
  private TransactionFormDraft buildEditDraft() {
    TransactionFormDraft draft = new TransactionFormDraft();
    draft.setTransactionId(transactionId);
    draft.setType(selectedType);
    draft.setOccurredAtMillis(occurredAtPicker.getOccurredAtMillis());
    if (selectedWallet != null) {
      draft.setWalletId(selectedWallet.getId());
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
    try {
      long amountMinor = MoneyParser.parse(amountInput.getText().toString());
      if (amountMinor > 0L) {
        draft.setAmountMinor(amountMinor);
      }
    } catch (IllegalArgumentException ignored) {
      draft.setAmountMinor(transaction.getAmountMinor());
    }
    return draft;
  }

  private static final class DetailData {
    private final Transaction transaction;
    private final Category category;
    private final Wallet wallet;
    private final Merchant merchant;
    private final Map<Long, Tag> tagsById;

    private DetailData(
        Transaction transaction,
        Category category,
        Wallet wallet,
        Merchant merchant,
        Map<Long, Tag> tagsById) {
      this.transaction = transaction;
      this.category = category;
      this.wallet = wallet;
      this.merchant = merchant;
      this.tagsById = tagsById;
    }
  }

  private static final class EditData {
    private final List<Wallet> wallets;
    private final List<Category> categoriesForType;
    private final Wallet selectedWallet;
    private final Category selectedCategory;

    private EditData(
        List<Wallet> wallets,
        List<Category> categoriesForType,
        Wallet selectedWallet,
        Category selectedCategory) {
      this.wallets = wallets;
      this.categoriesForType = categoriesForType;
      this.selectedWallet = selectedWallet;
      this.selectedCategory = selectedCategory;
    }
  }
}
