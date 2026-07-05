package com.dwlhm.finan.ui.history;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.EntitySearchBottomSheet;

import java.util.ArrayList;
import java.util.List;

public final class HistoryFilterBottomSheet extends Dialog {

  public interface OnApplyListener {
    void onApply(
        @Nullable Long walletId,
        @Nullable Long categoryId,
        @Nullable Long typeId,
        @Nullable Long sortId);
  }

  public interface OnResetListener {
    void onReset();
  }

  private static final long ALL_SENTINEL_ID = -1L;
  static final long TYPE_EXPENSE_ID = 1L;
  static final long TYPE_INCOME_ID = 2L;
  static final long SORT_OLDEST_ID = 1L;

  private static class FilterItem {
    @Nullable final Long id;
    final String name;

    FilterItem(@Nullable Long id, String name) {
      this.id = id;
      this.name = name;
    }
  }

  private final List<Wallet> wallets;
  private final List<Category> categories;
  private final OnApplyListener applyListener;
  private final OnResetListener resetListener;

  private Long pendingWalletId;
  private Long pendingCategoryId;
  private Long pendingTypeId;
  private Long pendingSortId;

  private TextView walletValue;
  private TextView categoryValue;
  private TextView typeValue;
  private TextView sortValue;

  public HistoryFilterBottomSheet(
      @NonNull Context context,
      @NonNull List<Wallet> wallets,
      @NonNull List<Category> categories,
      @Nullable Long initialWalletId,
      @Nullable Long initialCategoryId,
      @Nullable Long initialTypeId,
      @Nullable Long initialSortId,
      @NonNull OnApplyListener applyListener,
      @NonNull OnResetListener resetListener) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.wallets = wallets;
    this.categories = categories;
    this.pendingWalletId = initialWalletId;
    this.pendingCategoryId = initialCategoryId;
    this.pendingTypeId = initialTypeId;
    this.pendingSortId = initialSortId;
    this.applyListener = applyListener;
    this.resetListener = resetListener;
  }

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_history_filter_bottom_sheet);

    Window window = getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      WindowManager.LayoutParams params = window.getAttributes();
      params.width = WindowManager.LayoutParams.MATCH_PARENT;
      params.height = WindowManager.LayoutParams.WRAP_CONTENT;
      params.gravity = android.view.Gravity.BOTTOM;
      window.setAttributes(params);
      window.setWindowAnimations(android.R.style.Animation_InputMethod);
    }

    walletValue = findViewById(R.id.filter_wallet_value);
    categoryValue = findViewById(R.id.filter_category_value);
    typeValue = findViewById(R.id.filter_type_value);
    sortValue = findViewById(R.id.filter_sort_value);

    updateWalletLabel();
    updateCategoryLabel();
    updateTypeLabel();
    updateSortLabel();

    walletValue.setOnClickListener(v -> openWalletSheet());
    categoryValue.setOnClickListener(v -> openCategorySheet());
    typeValue.setOnClickListener(v -> openTypePicker());
    sortValue.setOnClickListener(v -> openSortPicker());

    TextView resetButton = findViewById(R.id.filter_reset);
    resetButton.setOnClickListener(
        v -> {
          pendingWalletId = null;
          pendingCategoryId = null;
          pendingTypeId = null;
          pendingSortId = null;
          updateWalletLabel();
          updateCategoryLabel();
          updateTypeLabel();
          updateSortLabel();
          resetListener.onReset();
          dismiss();
        });

    DialogActionsView actions = findViewById(R.id.filter_actions);
    actions.setOnPrimaryClickListener(
        v -> {
          applyListener.onApply(pendingWalletId, pendingCategoryId, pendingTypeId, pendingSortId);
          dismiss();
        });
    actions.setOnCancelClickListener(v -> dismiss());

    BottomSheetHelper.makeDraggable(this);
  }

  private void openWalletSheet() {
    List<FilterItem> items = new ArrayList<>();
    items.add(new FilterItem(null, getContext().getString(R.string.summary_all_wallets)));
    for (Wallet w : wallets) {
      items.add(new FilterItem(w.getId(), w.getName()));
    }

    long selectedId = pendingWalletId != null ? pendingWalletId : ALL_SENTINEL_ID;

    EntitySearchBottomSheet<FilterItem> sheet =
        new EntitySearchBottomSheet<>(
            getContext(),
            items,
            selectedId,
            new EntitySearchBottomSheet.ItemMapper<FilterItem>() {
              @Override
              public String getName(FilterItem item) {
                return item.name;
              }

              @Override
              public long getId(FilterItem item) {
                return item.id != null ? item.id : ALL_SENTINEL_ID;
              }
            },
            item -> {
              pendingWalletId = item.id;
              updateWalletLabel();
            });
    sheet.show();
  }

  private void openCategorySheet() {
    List<FilterItem> items = new ArrayList<>();
    items.add(new FilterItem(null, getContext().getString(R.string.summary_all_categories)));
    for (Category c : categories) {
      items.add(new FilterItem(c.getId(), c.getName()));
    }

    long selectedId = pendingCategoryId != null ? pendingCategoryId : ALL_SENTINEL_ID;

    EntitySearchBottomSheet<FilterItem> sheet =
        new EntitySearchBottomSheet<>(
            getContext(),
            items,
            selectedId,
            new EntitySearchBottomSheet.ItemMapper<FilterItem>() {
              @Override
              public String getName(FilterItem item) {
                return item.name;
              }

              @Override
              public long getId(FilterItem item) {
                return item.id != null ? item.id : ALL_SENTINEL_ID;
              }
            },
            item -> {
              pendingCategoryId = item.id;
              updateCategoryLabel();
            });
    sheet.show();
  }

  private void openTypePicker() {
    List<FilterItem> items = new ArrayList<>();
    items.add(new FilterItem(null, getContext().getString(R.string.history_all_types)));
    items.add(new FilterItem(TYPE_EXPENSE_ID, getContext().getString(R.string.capture_type_expense)));
    items.add(new FilterItem(TYPE_INCOME_ID, getContext().getString(R.string.capture_type_income)));

    long selectedId = pendingTypeId != null ? pendingTypeId : ALL_SENTINEL_ID;

    EntitySearchBottomSheet<FilterItem> sheet =
        new EntitySearchBottomSheet<>(
            getContext(),
            items,
            selectedId,
            new EntitySearchBottomSheet.ItemMapper<FilterItem>() {
              @Override
              public String getName(FilterItem item) {
                return item.name;
              }

              @Override
              public long getId(FilterItem item) {
                return item.id != null ? item.id : ALL_SENTINEL_ID;
              }
            },
            item -> {
              pendingTypeId = item.id;
              updateTypeLabel();
            });
    sheet.show();
  }

  private void openSortPicker() {
    List<FilterItem> items = new ArrayList<>();
    items.add(new FilterItem(null, getContext().getString(R.string.history_sort_newest)));
    items.add(new FilterItem(SORT_OLDEST_ID, getContext().getString(R.string.history_sort_oldest)));

    long selectedId = pendingSortId != null ? pendingSortId : ALL_SENTINEL_ID;

    EntitySearchBottomSheet<FilterItem> sheet =
        new EntitySearchBottomSheet<>(
            getContext(),
            items,
            selectedId,
            new EntitySearchBottomSheet.ItemMapper<FilterItem>() {
              @Override
              public String getName(FilterItem item) {
                return item.name;
              }

              @Override
              public long getId(FilterItem item) {
                return item.id != null ? item.id : ALL_SENTINEL_ID;
              }
            },
            item -> {
              pendingSortId = item.id;
              updateSortLabel();
            });
    sheet.show();
  }

  private void updateWalletLabel() {
    if (pendingWalletId == null) {
      walletValue.setText(getContext().getString(R.string.summary_all_wallets));
      walletValue.setTextColor(getContext().getColor(R.color.finan_text_secondary));
    } else {
      for (Wallet w : wallets) {
        if (w.getId() == pendingWalletId) {
          walletValue.setText(w.getName());
          walletValue.setTextColor(getContext().getColor(R.color.finan_text_primary));
          return;
        }
      }
      walletValue.setText(getContext().getString(R.string.summary_all_wallets));
      walletValue.setTextColor(getContext().getColor(R.color.finan_text_secondary));
    }
  }

  private void updateCategoryLabel() {
    if (pendingCategoryId == null) {
      categoryValue.setText(getContext().getString(R.string.summary_all_categories));
      categoryValue.setTextColor(getContext().getColor(R.color.finan_text_secondary));
    } else {
      for (Category c : categories) {
        if (c.getId() == pendingCategoryId) {
          categoryValue.setText(c.getName());
          categoryValue.setTextColor(getContext().getColor(R.color.finan_text_primary));
          return;
        }
      }
      categoryValue.setText(getContext().getString(R.string.summary_all_categories));
      categoryValue.setTextColor(getContext().getColor(R.color.finan_text_secondary));
    }
  }

  private void updateTypeLabel() {
    if (pendingTypeId == null) {
      typeValue.setText(getContext().getString(R.string.history_all_types));
      typeValue.setTextColor(getContext().getColor(R.color.finan_text_secondary));
    } else if (pendingTypeId == TYPE_EXPENSE_ID) {
      typeValue.setText(getContext().getString(R.string.capture_type_expense));
      typeValue.setTextColor(getContext().getColor(R.color.finan_text_primary));
    } else {
      typeValue.setText(getContext().getString(R.string.capture_type_income));
      typeValue.setTextColor(getContext().getColor(R.color.finan_text_primary));
    }
  }

  private void updateSortLabel() {
    if (pendingSortId == null) {
      sortValue.setText(getContext().getString(R.string.history_sort_newest));
      sortValue.setTextColor(getContext().getColor(R.color.finan_text_secondary));
    } else {
      sortValue.setText(getContext().getString(R.string.history_sort_oldest));
      sortValue.setTextColor(getContext().getColor(R.color.finan_text_primary));
    }
  }

}
