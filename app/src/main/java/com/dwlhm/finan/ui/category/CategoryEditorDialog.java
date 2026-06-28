package com.dwlhm.finan.ui.category;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.LabeledEditTextView;

import java.util.function.Consumer;

public final class CategoryEditorDialog extends Dialog {

  private final AppServices services;
  private final Category category;
  private final int transactionCount;
  private final Consumer<Category> onSaved;
  private final @Nullable Runnable onViewTransactions;
  private final @Nullable String prefillName;
  private final @Nullable String prefillType;

  public CategoryEditorDialog(
      @NonNull Context context,
      @NonNull AppServices services,
      @Nullable Category category,
      int transactionCount,
      @NonNull Consumer<Category> onSaved,
      @Nullable Runnable onViewTransactions) {
    this(context, services, category, transactionCount, onSaved, onViewTransactions, null, null);
  }

  public CategoryEditorDialog(
      @NonNull Context context,
      @NonNull AppServices services,
      @Nullable Category category,
      int transactionCount,
      @NonNull Consumer<Category> onSaved,
      @Nullable Runnable onViewTransactions,
      @Nullable String prefillName,
      @Nullable String prefillType) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.services = services;
    this.category = category;
    this.transactionCount = transactionCount;
    this.onSaved = onSaved;
    this.onViewTransactions = onViewTransactions;
    this.prefillName = prefillName;
    this.prefillType = prefillType;
    setContentView(R.layout.dialog_category_editor);
    setCancelable(false);
    setupViews();
  }

  private void setupViews() {
    TextView title = findViewById(R.id.category_editor_title);
    LabeledEditTextView nameField = findViewById(R.id.category_name_field);
    EditText nameInput = nameField.getEditText();
    LabeledEditTextView iconField = findViewById(R.id.category_icon_field);
    EditText iconInput = iconField.getEditText();
    iconInput.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(5) });
    CheckBox expenseInput = findViewById(R.id.category_type_expense);
    CheckBox incomeInput = findViewById(R.id.category_type_income);
    RadioGroup activityGroup = findViewById(R.id.category_activity_group);
    TextView countView = findViewById(R.id.category_transaction_count);
    Button transactionsButton = findViewById(R.id.category_view_transactions);
    Button deleteButton = findViewById(R.id.category_delete);
    DialogActionsView actions = findViewById(R.id.category_editor_actions);

    EditorDraft draft =
        new EditorDraft(category, nameInput, iconInput, expenseInput, incomeInput, activityGroup);

    boolean editing = category != null;
    title.setText(editing ? R.string.category_editor_edit_title : R.string.category_editor_create_title);
    actions.setPrimaryText(
        getContext().getString(editing ? R.string.category_save_changes : R.string.category_save));

    if (editing) {
      draft.bindOriginal();
      countView.setText(
          getContext().getResources()
              .getQuantityString(
                  R.plurals.category_transaction_count, transactionCount, transactionCount));
      countView.setVisibility(android.view.View.VISIBLE);
      deleteButton.setVisibility(android.view.View.VISIBLE);
      deleteButton.setOnClickListener(v -> confirmDeleteCategory());
      if (onViewTransactions != null) {
        transactionsButton.setVisibility(android.view.View.VISIBLE);
        transactionsButton.setOnClickListener(v -> closeEditor(draft, onViewTransactions));
      }
    } else {
      if (prefillName != null && !prefillName.isEmpty()) {
        nameInput.setText(prefillName);
        nameInput.setSelection(nameInput.length());
      }
      if (prefillType != null) {
        expenseInput.setChecked("EXPENSE".equals(prefillType) || "BOTH".equals(prefillType));
        incomeInput.setChecked("INCOME".equals(prefillType) || "BOTH".equals(prefillType));
      }
    }

    actions.setOnCancelClickListener(v -> closeEditor(draft, null));
    actions.setOnPrimaryClickListener(v -> submitEditor(draft, actions));
    setOnKeyListener(
        (d, keyCode, event) -> {
          if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            closeEditor(draft, null);
            return true;
          }
          return false;
        });

    showEditorWindow();
    if (!editing) {
      nameInput.requestFocus();
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
  }

  private void showEditorWindow() {
    show();
    Window window = getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      WindowManager.LayoutParams params = window.getAttributes();
      params.width = WindowManager.LayoutParams.MATCH_PARENT;
      params.height = WindowManager.LayoutParams.WRAP_CONTENT;
      params.gravity = Gravity.BOTTOM;
      window.setAttributes(params);
      window.setWindowAnimations(android.R.style.Animation_InputMethod);
    }
  }

  private void submitEditor(EditorDraft draft, DialogActionsView actions) {
    String name = draft.name();
    if (name.isEmpty()) {
      draft.nameInput.setError(getContext().getString(R.string.category_error_name));
      draft.nameInput.requestFocus();
      return;
    }
    if (!draft.hasType()) {
      draft.expenseInput.setError(getContext().getString(R.string.category_error_type));
      draft.expenseInput.requestFocus();
      return;
    }
    draft.expenseInput.setError(null);
    if (draft.original != null && draft.activityChanged()) {
      showHistoryScope(draft, actions);
    } else {
      save(draft, actions, false);
    }
  }

  private void showHistoryScope(EditorDraft draft, DialogActionsView actions) {
    boolean defaultAll =
        CashFlowActivity.UNCLASSIFIED.name().equals(draft.original.getCashFlowActivity());
    int[] selected = {defaultAll ? 1 : 0};
    AlertDialog dialog =
        new AlertDialog.Builder(getContext())
            .setTitle(R.string.category_history_scope_title)
            .setMessage(
                defaultAll
                    ? R.string.category_history_scope_unclassified_hint
                    : R.string.category_history_scope_classified_hint)
            .setSingleChoiceItems(
                new String[] {
                  getContext().getString(R.string.category_history_scope_future),
                  getContext().getString(R.string.category_history_scope_all)
                },
                selected[0],
                (d, which) -> selected[0] = which)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(
                R.string.category_history_scope_apply,
                (d, which) -> save(draft, actions, selected[0] == 1))
            .create();
    dialog.show();
  }

  private void save(EditorDraft draft, DialogActionsView actions, boolean includeHistory) {
    actions.setPrimaryEnabled(false);
    String iconValue = draft.icon();
    if (iconValue.isEmpty()) {
      java.security.SecureRandom random = new java.security.SecureRandom();
      int randomIndex = random.nextInt(DEFAULT_EMOJIS.length);
      iconValue = DEFAULT_EMOJIS[randomIndex];
    }
    final String finalIcon = iconValue;

    services.dbWorker.compute(
        () -> {
          Category duplicate = services.categoryDao.findByNameIgnoreCase(draft.name());
          if (duplicate != null
              && (draft.original == null || duplicate.getId() != draft.original.getId())) {
            return SaveResult.duplicate(duplicate);
          }
          try {
            Category saved =
                draft.original == null
                    ? services.categoryClassificationService.create(
                        draft.name(), finalIcon, draft.type(), draft.activity())
                    : services.categoryClassificationService.update(
                        draft.original.getId(),
                        draft.name(),
                        finalIcon,
                        draft.type(),
                        draft.activity(),
                        includeHistory);
            return SaveResult.success(saved);
          } catch (RuntimeException e) {
            return SaveResult.failed();
          }
        },
        result -> {
          if (!isShowing()) {
            return;
          }
          actions.setPrimaryEnabled(true);
          if (result == null || (result.saved == null && !result.duplicate)) {
            Toast.makeText(getContext(), R.string.category_error_save, Toast.LENGTH_SHORT).show();
            return;
          }
          if (result.duplicate) {
            draft.nameInput.setError(getContext().getString(R.string.category_error_duplicate));
            draft.nameInput.requestFocus();
            return;
          }
          Toast.makeText(getContext(), R.string.category_saved, Toast.LENGTH_SHORT).show();
          dismiss();
          onSaved.accept(result.saved);
        });
  }

  private void closeEditor(EditorDraft draft, @Nullable Runnable afterClose) {
    if (!draft.dirty()) {
      dismiss();
      if (afterClose != null) {
        afterClose.run();
      }
      return;
    }
    new AlertDialog.Builder(getContext())
        .setTitle(R.string.category_discard_title)
        .setMessage(R.string.category_discard_message)
        .setNegativeButton(R.string.category_keep_editing, null)
        .setPositiveButton(
            R.string.category_discard,
            (d, which) -> {
              dismiss();
              if (afterClose != null) {
                afterClose.run();
              }
            })
        .show();
  }

  private static final class EditorDraft {
    private final Category original;
    private final EditText nameInput;
    private final EditText iconInput;
    private final CheckBox expenseInput;
    private final CheckBox incomeInput;
    private final RadioGroup activityGroup;

    private EditorDraft(
        Category original,
        EditText nameInput,
        EditText iconInput,
        CheckBox expenseInput,
        CheckBox incomeInput,
        RadioGroup activityGroup) {
      this.original = original;
      this.nameInput = nameInput;
      this.iconInput = iconInput;
      this.expenseInput = expenseInput;
      this.incomeInput = incomeInput;
      this.activityGroup = activityGroup;
    }

    private void bindOriginal() {
      nameInput.setText(original.getName());
      nameInput.setSelection(nameInput.length());
      iconInput.setText(original.getIcon());
      expenseInput.setChecked(!"INCOME".equals(original.getTypeFilter()));
      incomeInput.setChecked(!"EXPENSE".equals(original.getTypeFilter()));
      activityGroup.check(activityId(CashFlowActivity.valueOf(original.getCashFlowActivity())));
    }

    private String name() {
      return nameInput.getText().toString().trim();
    }

    private String icon() {
      return iconInput.getText().toString().trim();
    }

    private String type() {
      if (expenseInput.isChecked()) {
        return incomeInput.isChecked() ? "BOTH" : "EXPENSE";
      }
      return incomeInput.isChecked() ? "INCOME" : "";
    }

    private boolean hasType() {
      return expenseInput.isChecked() || incomeInput.isChecked();
    }

    private CashFlowActivity activity() {
      int id = activityGroup.getCheckedRadioButtonId();
      if (id == R.id.category_activity_operating) {
        return CashFlowActivity.OPERATING;
      }
      if (id == R.id.category_activity_investing) {
        return CashFlowActivity.INVESTING;
      }
      return id == R.id.category_activity_financing
          ? CashFlowActivity.FINANCING
          : CashFlowActivity.UNCLASSIFIED;
    }

    private boolean activityChanged() {
      return original != null && !original.getCashFlowActivity().equals(activity().name());
    }

    private boolean dirty() {
      return original == null
          ? !name().isEmpty()
              || !icon().isEmpty()
              || !"EXPENSE".equals(type())
              || activity() != CashFlowActivity.UNCLASSIFIED
          : !original.getName().equals(name())
              || !java.util.Objects.equals(original.getIcon(), icon())
              || !original.getTypeFilter().equals(type())
              || activityChanged();
    }

    private static int activityId(CashFlowActivity activity) {
      switch (activity) {
        case OPERATING:
          return R.id.category_activity_operating;
        case INVESTING:
          return R.id.category_activity_investing;
        case FINANCING:
          return R.id.category_activity_financing;
        default:
          return R.id.category_activity_unclassified;
      }
    }
  }

  private void confirmDeleteCategory() {
    String message;
    if (transactionCount > 0) {
      message = "Kategori ini digunakan dalam " + transactionCount + " transaksi. Menghapus kategori ini juga akan memengaruhi transaksi tersebut. Hapus?";
    } else {
      message = "Apakah Anda yakin ingin menghapus kategori ini?";
    }

    new AlertDialog.Builder(getContext())
        .setTitle("Hapus Kategori")
        .setMessage(message)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton("Hapus", (dialog, which) -> deleteCategory())
        .show();
  }

  private void deleteCategory() {
    services.dbWorker.compute(
        () -> services.categoryDao.delete(category.getId()),
        success -> {
          if (!isShowing()) {
            return;
          }
          if (Boolean.TRUE.equals(success)) {
            Toast.makeText(getContext(), "Kategori berhasil dihapus", Toast.LENGTH_SHORT).show();
            dismiss();
            onSaved.accept(category);
          } else {
            Toast.makeText(getContext(), "Gagal menghapus kategori", Toast.LENGTH_SHORT).show();
          }
        });
  }

  private static final String[] DEFAULT_EMOJIS = {
      "🍔", "🚗", "🏠", "🛍️", "💡", "🏥", "🎓", "🎮", "✈️", "☕",
      "💰", "🍿", "👔", "💆", "🐾", "🎁", "🥦", "🚌", "🏋️", "🎨"
  };

  private static final class SaveResult {
    private final Category saved;
    private final Category category;
    private final boolean duplicate;

    private SaveResult(Category saved, Category category, boolean duplicate) {
      this.saved = saved;
      this.category = category;
      this.duplicate = duplicate;
    }

    private static SaveResult success(Category saved) {
      return new SaveResult(saved, null, false);
    }

    private static SaveResult duplicate(Category category) {
      return new SaveResult(null, category, true);
    }

    private static SaveResult failed() {
      return new SaveResult(null, null, false);
    }
  }
}
