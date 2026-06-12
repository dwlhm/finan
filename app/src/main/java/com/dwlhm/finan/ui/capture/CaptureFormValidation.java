package com.dwlhm.finan.ui.capture;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.dwlhm.finan.R;

final class CaptureFormValidation {

  enum Field {
    AMOUNT,
    WALLET,
    DESTINATION,
    CATEGORY
  }

  private final View amountSection;
  private final TextView amountError;
  private final EditText amountInput;
  private final View walletSection;
  private final TextView walletError;
  private final View destinationSection;
  private final TextView destinationError;
  private final View categorySection;
  private final TextView categoryError;
  @Nullable private final TextView validationBanner;

  CaptureFormValidation(@NonNull View root, @Nullable TextView validationBanner) {
    this.validationBanner = validationBanner;
    amountSection = root.findViewById(R.id.capture_amount_section);
    amountError = root.findViewById(R.id.capture_amount_error);
    amountInput = root.findViewById(R.id.capture_amount);
    walletSection = root.findViewById(R.id.capture_wallet_section);
    walletError = root.findViewById(R.id.capture_wallet_error);
    destinationSection = root.findViewById(R.id.capture_destination_section);
    destinationError = root.findViewById(R.id.capture_destination_error);
    categorySection = root.findViewById(R.id.capture_category_section);
    categoryError = root.findViewById(R.id.capture_category_error);
  }

  void bindAmountClearListener() {
    amountInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            clear(Field.AMOUNT);
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });
  }

  void clear(@NonNull Field field) {
    switch (field) {
      case AMOUNT:
        amountSection.setBackgroundResource(R.drawable.bg_panel_emphasis);
        amountError.setVisibility(View.GONE);
        amountError.setText("");
        break;
      case WALLET:
        clearSectionHighlight(walletSection);
        walletError.setVisibility(View.GONE);
        walletError.setText("");
        break;
      case DESTINATION:
        clearSectionHighlight(destinationSection);
        destinationError.setVisibility(View.GONE);
        destinationError.setText("");
        break;
      case CATEGORY:
        categorySection.setBackgroundResource(R.drawable.bg_card);
        categoryError.setVisibility(View.GONE);
        categoryError.setText("");
        break;
      default:
        break;
    }
    updateValidationBanner();
  }

  void clearAll() {
    for (Field field : Field.values()) {
      clear(field);
    }
  }

  void showError(
      @NonNull Context context, @NonNull Field field, @StringRes int messageRes) {
    String message = context.getString(messageRes);
    switch (field) {
      case AMOUNT:
        amountSection.setBackgroundResource(R.drawable.bg_panel_emphasis_error);
        amountError.setText(message);
        amountError.setVisibility(View.VISIBLE);
        amountInput.requestFocus();
        break;
      case WALLET:
        walletSection.setBackgroundResource(R.drawable.bg_field_error);
        walletError.setText(message);
        walletError.setVisibility(View.VISIBLE);
        break;
      case DESTINATION:
        destinationSection.setBackgroundResource(R.drawable.bg_field_error);
        destinationError.setText(message);
        destinationError.setVisibility(View.VISIBLE);
        break;
      case CATEGORY:
        categorySection.setBackgroundResource(R.drawable.bg_field_error);
        categoryError.setText(message);
        categoryError.setVisibility(View.VISIBLE);
        break;
      default:
        break;
    }
    updateValidationBanner();
  }

  void scrollToFirstError() {
    View target = firstErrorTarget();
    if (target == null) {
      return;
    }
    target.post(
        () ->
            target.requestRectangleOnScreen(
                new Rect(0, 0, target.getWidth(), target.getHeight()), true));
  }

  @Nullable
  private View firstErrorTarget() {
    if (amountError.getVisibility() == View.VISIBLE) {
      return amountSection;
    }
    if (walletError.getVisibility() == View.VISIBLE) {
      return walletSection;
    }
    if (destinationError.getVisibility() == View.VISIBLE) {
      return destinationSection;
    }
    if (categoryError.getVisibility() == View.VISIBLE) {
      return categorySection;
    }
    return null;
  }

  private static void clearSectionHighlight(@NonNull View section) {
    section.setBackground(null);
  }

  private void updateValidationBanner() {
    if (validationBanner == null) {
      return;
    }
    if (hasVisibleErrors()) {
      validationBanner.setVisibility(View.VISIBLE);
    } else {
      validationBanner.setVisibility(View.GONE);
    }
  }

  private boolean hasVisibleErrors() {
    return amountError.getVisibility() == View.VISIBLE
        || walletError.getVisibility() == View.VISIBLE
        || destinationError.getVisibility() == View.VISIBLE
        || categoryError.getVisibility() == View.VISIBLE;
  }
}
