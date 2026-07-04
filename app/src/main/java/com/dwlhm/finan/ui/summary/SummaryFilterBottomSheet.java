package com.dwlhm.finan.ui.summary;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
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

public final class SummaryFilterBottomSheet extends Dialog {

  public interface OnApplyListener {
    void onApply(@Nullable Long walletId, @Nullable Long categoryId);
  }

  public interface OnResetListener {
    void onReset();
  }

  private static final long ALL_SENTINEL_ID = -1L;

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

  private TextView walletValue;
  private TextView categoryValue;

  public SummaryFilterBottomSheet(
      @NonNull Context context,
      @NonNull List<Wallet> wallets,
      @NonNull List<Category> categories,
      @Nullable Long initialWalletId,
      @Nullable Long initialCategoryId,
      @NonNull OnApplyListener applyListener,
      @NonNull OnResetListener resetListener) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.wallets = wallets;
    this.categories = categories;
    this.pendingWalletId = initialWalletId;
    this.pendingCategoryId = initialCategoryId;
    this.applyListener = applyListener;
    this.resetListener = resetListener;
  }

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_summary_filter_bottom_sheet);

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

    updateWalletLabel();
    updateCategoryLabel();

    walletValue.setOnClickListener(v -> openWalletSheet());
    categoryValue.setOnClickListener(v -> openCategorySheet());

    TextView resetButton = findViewById(R.id.filter_reset);
    resetButton.setOnClickListener(
        v -> {
          pendingWalletId = null;
          pendingCategoryId = null;
          updateWalletLabel();
          updateCategoryLabel();
          resetListener.onReset();
          dismiss();
        });

    DialogActionsView actions = findViewById(R.id.filter_actions);
    actions.setOnPrimaryClickListener(
        v -> {
          applyListener.onApply(pendingWalletId, pendingCategoryId);
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
}
