package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.dao.MerchantDao;
import com.dwlhm.finan.data.entity.Merchant;

public final class MerchantSelectionController {

  private final Context context;
  private final MerchantDao merchantDao;
  private final DbWorker dbWorker;
  private final Button pickButton;
  private final Button clearButton;

  @Nullable private Merchant selectedMerchant;

  public MerchantSelectionController(
      @NonNull Context context,
      @NonNull MerchantDao merchantDao,
      @NonNull DbWorker dbWorker,
      @NonNull Button pickButton,
      @NonNull Button clearButton) {
    this.context = context;
    this.merchantDao = merchantDao;
    this.dbWorker = dbWorker;
    this.pickButton = pickButton;
    this.clearButton = clearButton;
    pickButton.setOnClickListener(v -> openSearchDialog());
    clearButton.setOnClickListener(v -> clear());
  }

  public void setMerchant(@Nullable Merchant merchant) {
    selectedMerchant = merchant;
    updateUi();
  }

  public void setMerchantId(@Nullable Long merchantId) {
    if (merchantId == null || merchantId <= 0L) {
      selectedMerchant = null;
    } else {
      selectedMerchant = merchantDao.findById(merchantId);
    }
    updateUi();
  }

  @Nullable
  public Long getMerchantId() {
    return selectedMerchant == null ? null : selectedMerchant.getId();
  }

  public void clear() {
    selectedMerchant = null;
    updateUi();
  }

  private void openSearchDialog() {
    NamedEntitySearchDialog<Merchant> dialog =
        new NamedEntitySearchDialog<>(
            context,
            new NamedEntitySearchDialog.EntityAccess<Merchant>() {
              @Override
              public java.util.List<Merchant> loadAll() {
                return merchantDao.findAllOrderByUsage();
              }

              @Override
              public Merchant findByNameIgnoreCase(String name) {
                return merchantDao.findByNameIgnoreCase(name);
              }

              @Override
              public Merchant insertIfAbsent(String name) {
                return merchantDao.insertIfAbsent(name);
              }

              @Override
              public String nameOf(Merchant entity) {
                return entity.getName();
              }

              @Override
              public long idOf(Merchant entity) {
                return entity.getId();
              }
            },
            dbWorker,
            (merchant, created) -> setMerchant(merchant),
            R.string.capture_merchant_search_title,
            R.string.capture_merchant_search_hint,
            R.string.capture_merchant_create_from_search,
            R.string.capture_merchant_add_new,
            R.string.capture_merchant_add_new_dialog_title,
            R.string.capture_merchant_name_hint,
            R.string.capture_merchant_created,
            R.string.capture_merchant_name_empty,
            R.string.capture_merchant_already_exists,
            null);
    dialog.show();
  }

  private void updateUi() {
    boolean selected = selectedMerchant != null;
    pickButton.setText(
        selected
            ? selectedMerchant.getName()
            : context.getString(R.string.capture_merchant_pick));
    clearButton.setVisibility(selected ? View.VISIBLE : View.GONE);
    int selectedBackgroundRes = R.drawable.bg_control_selected_expense;
    UiComponentStyles.setSelectButtonSelected(context, pickButton, selected, selectedBackgroundRes);
  }
}
