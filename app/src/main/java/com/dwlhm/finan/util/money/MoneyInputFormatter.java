package com.dwlhm.finan.util.money;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public final class MoneyInputFormatter {

  private MoneyInputFormatter() {}

  public static void attach(EditText input, boolean includeCurrencyPrefix) {
    input.addTextChangedListener(new FormattingWatcher(input, includeCurrencyPrefix));
  }

  private static final class FormattingWatcher implements TextWatcher {

    private final EditText input;
    private final boolean includeCurrencyPrefix;
    private boolean selfChange;

    FormattingWatcher(EditText input, boolean includeCurrencyPrefix) {
      this.input = input;
      this.includeCurrencyPrefix = includeCurrencyPrefix;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
      if (selfChange) {
        return;
      }

      String raw = s.toString();
      if (raw.trim().isEmpty()) {
        return;
      }

      boolean negative = raw.trim().startsWith("-");
      String digits = raw.replaceAll("[^0-9]", "");
      if (digits.isEmpty()) {
        return;
      }

      long amount;
      try {
        amount = Long.parseLong(digits);
      } catch (NumberFormatException e) {
        return;
      }
      if (negative) {
        amount = -amount;
      }

      String formatted =
          includeCurrencyPrefix
              ? MoneyFormatter.format(amount)
              : MoneyFormatter.formatWithoutCurrency(amount);
      if (formatted.equals(raw)) {
        return;
      }

      selfChange = true;
      input.setText(formatted);
      input.setSelection(formatted.length());
      selfChange = false;
    }
  }
}
