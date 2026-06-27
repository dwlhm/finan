package com.dwlhm.finan.ui.capture;

import android.content.Context;
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

  private final EditText amountInput;
  @Nullable private final TextView validationBanner;

  CaptureFormValidation(@NonNull View root, @Nullable TextView validationBanner) {
    this.validationBanner = validationBanner;
    this.amountInput = root.findViewById(R.id.capture_amount);
  }

  void bindAmountClearListener() {
    if (amountInput == null) return;
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
    if (validationBanner != null) {
      validationBanner.setVisibility(View.GONE);
      validationBanner.setText("");
    }
  }

  void clearAll() {
    if (validationBanner != null) {
      validationBanner.setVisibility(View.GONE);
      validationBanner.setText("");
    }
  }

  void showError(
      @NonNull Context context,
      @NonNull Field field,
      @StringRes int errorMessageRes) {
    
    if (validationBanner != null) {
      validationBanner.setText(context.getString(errorMessageRes));
      validationBanner.setVisibility(View.VISIBLE);
    }
  }

  void scrollToFirstError() {
    // With the new layout, everything is visible, no scrolling needed.
  }
}
