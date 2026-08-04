package com.dwlhm.finan.ui.category;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.EmojiConstants;
import com.dwlhm.finan.ui.common.EmojiPickerBottomSheet;
import com.dwlhm.finan.ui.common.LabeledEditTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
public final class CategoryEditorDialog extends BottomSheetDialog {

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
    EditText nameInput = nameField != null ? nameField.getEditText() : null;
    if (nameInput == null) return;

    // Custom Icon Preview & Backing Input
    FrameLayout iconContainer = findViewById(R.id.category_icon_container);
    TextView iconPreview = findViewById(R.id.category_icon_preview);
    // Backing EditText for icon value
    EditText iconInput = new EditText(getContext());
    
    CheckBox expenseInput = findViewById(R.id.category_type_expense);
    CheckBox incomeInput = findViewById(R.id.category_type_income);
    RadioGroup activityGroup = findViewById(R.id.category_activity_group);
    Button transactionsButton = findViewById(R.id.category_view_transactions);
    Button deleteButton = findViewById(R.id.category_delete);
    SwitchCompat defaultSwitch = findViewById(R.id.category_default_switch);
    DialogActionsView actions = findViewById(R.id.category_editor_actions);

    // Visual Category Type Cards
    LinearLayout expenseCard = findViewById(R.id.category_type_expense_card);
    LinearLayout incomeCard = findViewById(R.id.category_type_income_card);
    TextView expenseLabel = findViewById(R.id.category_type_expense_label);
    TextView incomeLabel = findViewById(R.id.category_type_income_label);

    EditorDraft draft =
        new EditorDraft(category, nameInput, iconInput, expenseInput, incomeInput, activityGroup, defaultSwitch);

    boolean editing = category != null;
    if (title != null) {
      title.setText(editing ? R.string.category_editor_edit_title : R.string.category_editor_create_title);
    }
    if (actions != null) {
      actions.setPrimaryText(
          getContext().getString(editing ? R.string.category_save_changes : R.string.category_save));
    }

    // Set initial icon
    String initialIcon = editing && category.getIcon() != null && !category.getIcon().isEmpty()
        ? category.getIcon()
        : EmojiConstants.CATEGORY_EMOJIS[new Random().nextInt(EmojiConstants.CATEGORY_EMOJIS.length)];
    if (iconPreview != null) iconPreview.setText(initialIcon);
    iconInput.setText(initialIcon);

    if (iconContainer != null) {
      iconContainer.setOnClickListener(v -> {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
          imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        EmojiPickerBottomSheet picker = new EmojiPickerBottomSheet(
            getContext(),
            iconInput.getText().toString(),
            selectedEmoji -> {
              if (iconPreview != null) iconPreview.setText(selectedEmoji);
              iconInput.setText(selectedEmoji);
            });
        BottomSheetHelper.show(picker);
      });
    }
    if (expenseCard != null && incomeCard != null && expenseInput != null && incomeInput != null) {
      View.OnClickListener typeClickListener = v -> {
        if (v == expenseCard) {
          expenseInput.setChecked(!expenseInput.isChecked());
        } else if (v == incomeCard) {
          incomeInput.setChecked(!incomeInput.isChecked());
        }
        updateTypeCardUI(expenseCard, expenseLabel, expenseInput, incomeCard, incomeLabel, incomeInput);
      };
      expenseCard.setOnClickListener(typeClickListener);
      incomeCard.setOnClickListener(typeClickListener);
    }

    if (editing) {
      draft.bindOriginal();
      if (iconPreview != null) iconPreview.setText(draft.icon());
      updateTypeCardUI(expenseCard, expenseLabel, expenseInput, incomeCard, incomeLabel, incomeInput);

      if (deleteButton != null) {
        deleteButton.setVisibility(View.VISIBLE);
        deleteButton.setOnClickListener(v -> confirmDeleteCategory());
      }
      if (onViewTransactions != null && transactionsButton != null) {
        transactionsButton.setVisibility(View.VISIBLE);
        transactionsButton.setOnClickListener(v -> closeEditor(draft, onViewTransactions));
      }
    } else {
      if (prefillName != null && !prefillName.isEmpty()) {
        nameInput.setText(prefillName);
        nameInput.setSelection(nameInput.length());
      }
      if (prefillType != null && expenseInput != null && incomeInput != null) {
        expenseInput.setChecked("EXPENSE".equals(prefillType) || "BOTH".equals(prefillType));
        incomeInput.setChecked("INCOME".equals(prefillType) || "BOTH".equals(prefillType));
      }
      updateTypeCardUI(expenseCard, expenseLabel, expenseInput, incomeCard, incomeLabel, incomeInput);
    }

    if (actions != null) {
      actions.setOnCancelClickListener(v -> closeEditor(draft, null));
      actions.setOnPrimaryClickListener(v -> submitEditor(draft, actions));
    }
    setOnKeyListener(
        (d, keyCode, event) -> {
          if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            closeEditor(draft, null);
            return true;
          }
          return false;
        });

    BottomSheetHelper.show(this);
    if (!editing) {
      nameInput.requestFocus();
      if (getWindow() != null) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
      }
    }
  }

  private void updateTypeCardUI(
      @Nullable LinearLayout expenseCard,
      @Nullable TextView expenseLabel,
      @Nullable CheckBox expenseInput,
      @Nullable LinearLayout incomeCard,
      @Nullable TextView incomeLabel,
      @Nullable CheckBox incomeInput) {
    if (expenseCard == null || incomeCard == null || expenseInput == null || incomeInput == null) {
      return;
    }
    boolean isExpense = expenseInput.isChecked();
    boolean isIncome = incomeInput.isChecked();

    if (isExpense) {
      expenseCard.setBackgroundResource(R.drawable.bg_category_type_expense_active);
      if (expenseLabel != null) {
        expenseLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_expense));
      }
    } else {
      expenseCard.setBackgroundResource(R.drawable.bg_category_type_inactive);
      if (expenseLabel != null) {
        expenseLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_text_primary));
      }
    }

    if (isIncome) {
      incomeCard.setBackgroundResource(R.drawable.bg_category_type_income_active);
      if (incomeLabel != null) {
        incomeLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_income));
      }
    } else {
      incomeCard.setBackgroundResource(R.drawable.bg_category_type_inactive);
      if (incomeLabel != null) {
        incomeLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_text_primary));
      }
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
      Toast.makeText(getContext(), R.string.category_error_type, Toast.LENGTH_SHORT).show();
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
        new MaterialAlertDialogBuilder(getContext())
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
      int randomIndex = random.nextInt(EmojiConstants.CATEGORY_EMOJIS.length);
      iconValue = EmojiConstants.CATEGORY_EMOJIS[randomIndex];
    }
    final String finalIcon = iconValue;
    final boolean isDefault = draft.isDefault();

    services.dbWorker.compute(
        () -> {
          Category duplicate = services.categoryDao.findByNameIgnoreCase(draft.name());
          if (duplicate != null
              && (draft.original == null || duplicate.getId() != draft.original.getId())) {
            return SaveResult.duplicate();
          }
          try {
            Category saved =
                draft.original == null
                    ? services.categoryClassificationService.create(
                        draft.name(), finalIcon, draft.type(), draft.activity(), isDefault)
                    : services.categoryClassificationService.update(
                        draft.original.getId(),
                        draft.name(),
                        finalIcon,
                        draft.type(),
                        draft.activity(),
                        includeHistory,
                        isDefault);
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
    new MaterialAlertDialogBuilder(getContext())
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
    private final SwitchCompat defaultSwitch;

    private EditorDraft(
        Category original,
        EditText nameInput,
        EditText iconInput,
        CheckBox expenseInput,
        CheckBox incomeInput,
        RadioGroup activityGroup,
        SwitchCompat defaultSwitch) {
      this.original = original;
      this.nameInput = nameInput;
      this.iconInput = iconInput;
      this.expenseInput = expenseInput;
      this.incomeInput = incomeInput;
      this.activityGroup = activityGroup;
      this.defaultSwitch = defaultSwitch;
    }

    private void bindOriginal() {
      nameInput.setText(original.getName());
      nameInput.setSelection(nameInput.length());
      iconInput.setText(original.getIcon());
      expenseInput.setChecked(!"INCOME".equals(original.getTypeFilter()));
      incomeInput.setChecked(!"EXPENSE".equals(original.getTypeFilter()));
      activityGroup.check(activityId(CashFlowActivity.valueOf(original.getCashFlowActivity())));
      defaultSwitch.setChecked(original.isDefault());
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

    private boolean isDefault() {
      return defaultSwitch.isChecked();
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
              || defaultSwitch.isChecked()
          : !original.getName().equals(name())
              || !java.util.Objects.equals(original.getIcon(), icon())
              || !original.getTypeFilter().equals(type())
              || activityChanged()
              || original.isDefault() != defaultSwitch.isChecked();
    }

    private static int activityId(CashFlowActivity activity) {
      return switch (activity) {
        case OPERATING -> R.id.category_activity_operating;
        case INVESTING -> R.id.category_activity_investing;
        case FINANCING -> R.id.category_activity_financing;
        default -> R.id.category_activity_unclassified;
      };
    }
  }

  private void confirmDeleteCategory() {
    if (category == null) return;

    if (transactionCount <= 0) {
      new MaterialAlertDialogBuilder(getContext())
          .setTitle("Hapus Kategori")
          .setMessage("Apakah Anda yakin ingin menghapus kategori \"" + category.getName() + "\"?")
          .setNegativeButton(android.R.string.cancel, null)
          .setPositiveButton("Hapus", (dialog, which) -> deleteCategoryAndReassign(null))
          .show();
    } else {
      new MaterialAlertDialogBuilder(getContext())
          .setTitle("Hapus Kategori \"" + category.getName() + "\"")
          .setMessage("Kategori ini sedang digunakan pada " + transactionCount + " transaksi. Pilih tindakan untuk transaksi tersebut:")
          .setNeutralButton(android.R.string.cancel, null)
          .setNegativeButton("Hapus Tanpa Memindahkan", (dialog, which) -> deleteCategoryAndReassign(null))
          .setPositiveButton("Pindahkan Transaksi", (dialog, which) -> showReassignCategoryPicker())
          .show();
    }
  }

  private void showReassignCategoryPicker() {
    services.dbWorker.compute(
        () -> services.categoryDao.findAllOrdered(),
        categories -> {
          if (!isShowing() || categories == null) return;

          List<Category> targetOptions = new ArrayList<>();
          List<String> displayNames = new ArrayList<>();
          for (Category c : categories) {
            if (category != null && c.getId() == category.getId()) continue;
            targetOptions.add(c);
            String iconStr = c.getIcon() != null && !c.getIcon().isEmpty() ? c.getIcon() + " " : "";
            displayNames.add(iconStr + c.getName());
          }

          if (targetOptions.isEmpty()) {
            Toast.makeText(getContext(), "Tidak ada kategori lain untuk pemindahan", Toast.LENGTH_SHORT).show();
            return;
          }

          new MaterialAlertDialogBuilder(getContext())
              .setTitle("Pilih Kategori Tujuan")
              .setItems(displayNames.toArray(new String[0]), (dialog, which) -> {
                Category selectedTarget = targetOptions.get(which);
                confirmReassignAndDelete(selectedTarget);
              })
              .setNegativeButton(android.R.string.cancel, null)
              .show();
        });
  }

  private void confirmReassignAndDelete(Category target) {
    String targetIcon = target.getIcon() != null && !target.getIcon().isEmpty() ? target.getIcon() + " " : "";
    new MaterialAlertDialogBuilder(getContext())
        .setTitle("Konfirmasi Pemindahan & Penghapusan")
        .setMessage("Sebanyak " + transactionCount + " transaksi akan dipindahkan ke kategori '" + targetIcon + target.getName() + "', lalu kategori '" + category.getName() + "' akan dihapus. Lanjutkan?")
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton("Pindahkan & Hapus", (dialog, which) -> deleteCategoryAndReassign(target.getId()))
        .show();
  }

  private void deleteCategoryAndReassign(@Nullable Long targetCategoryId) {
    services.dbWorker.compute(
        () -> {
          try {
            return services.categoryDao.deleteAndReassign(category.getId(), targetCategoryId);
          } catch (RuntimeException e) {
            return false;
          }
        },
        success -> {
          if (!isShowing()) {
            return;
          }
          if (Boolean.TRUE.equals(success)) {
            String msg = targetCategoryId != null
                ? "Transaksi dipindahkan dan kategori dihapus"
                : "Kategori berhasil dihapus";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            dismiss();
            onSaved.accept(category);
          } else {
            Toast.makeText(getContext(), R.string.category_error_save, Toast.LENGTH_SHORT).show();
          }
        });
  }

  private static final class SaveResult {
    final @Nullable Category saved;
    final boolean duplicate;

    private SaveResult(@Nullable Category saved, boolean duplicate) {
      this.saved = saved;
      this.duplicate = duplicate;
    }

    static SaveResult success(@NonNull Category saved) {
      return new SaveResult(saved, false);
    }

    static SaveResult duplicate() {
      return new SaveResult(null, true);
    }

    static SaveResult failed() {
      return new SaveResult(null, false);
    }
  }
}
