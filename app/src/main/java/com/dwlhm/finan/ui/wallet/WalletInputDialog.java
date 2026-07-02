package com.dwlhm.finan.ui.wallet;

import android.app.Dialog;
import android.content.Context;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;

import java.security.SecureRandom;

public final class WalletInputDialog extends Dialog {

  private final AppServices services;
  private final Runnable onSaved;

  public WalletInputDialog(
      @NonNull Context context,
      @NonNull AppServices services,
      @NonNull Runnable onSaved) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.services = services;
    this.onSaved = onSaved;
    setContentView(R.layout.dialog_wallet_input_bottom_sheet);
    setCancelable(false);
    setupViews();
  }

  private void setupViews() {
    LabeledEditTextView nameField = findViewById(R.id.wallet_name_field);
    LabeledEditTextView balanceField = findViewById(R.id.wallet_balance_field);
    LabeledEditTextView iconField = findViewById(R.id.wallet_icon_field);
    EditText nameInput = nameField.getEditText();
    EditText balanceInput = balanceField.getEditText();
    EditText iconInput = iconField.getEditText();
    iconInput.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(2) });
    MoneyInputFormatter.attach(balanceInput, true);
    CheckBox defaultInput = findViewById(R.id.wallet_default_input);
    DialogActionsView actionsView = findViewById(R.id.wallet_actions);

    actionsView.setOnCancelClickListener(v -> dismiss());
    actionsView.setOnPrimaryClickListener(
        v -> submitCreateWallet(nameInput, balanceInput, iconInput, defaultInput, actionsView));

    services.dbWorker.compute(
        () -> services.walletDao.findAll().isEmpty(),
        firstWallet -> {
          if (!isShowing() || firstWallet == null) {
            return;
          }
          defaultInput.setChecked(firstWallet);
          defaultInput.setEnabled(!firstWallet);
        });

    BottomSheetHelper.show(this);
    nameInput.requestFocus();
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
  }

  private void submitCreateWallet(
      EditText nameInput,
      EditText balanceInput,
      EditText iconInput,
      CheckBox defaultInput,
      DialogActionsView actionsView) {
    String name = nameInput.getText().toString().trim();
    if (name.isEmpty()) {
      nameInput.setError(getContext().getString(R.string.wallet_error_name));
      return;
    }

    long initialBalanceMinor = 0L;
    String balanceText = balanceInput.getText().toString().trim();
    if (!balanceText.isEmpty()) {
      try {
        initialBalanceMinor = MoneyParser.parse(balanceText);
      } catch (IllegalArgumentException e) {
        balanceInput.setError(getContext().getString(R.string.wallet_error_balance));
        return;
      }
    }

    String icon = iconInput.getText().toString().trim();
    if (icon.isEmpty()) {
      icon = WALLET_EMOJIS[new SecureRandom().nextInt(WALLET_EMOJIS.length)];
    }

    boolean makeDefault = defaultInput.isChecked();
    long parsedBalance = initialBalanceMinor;
    final String finalIcon = icon;
    actionsView.setPrimaryEnabled(false);

    services.dbWorker.compute(
        () -> {
          long walletId =
              services.walletDao.insert(
                  name,
                  MoneyFormatter.DEFAULT_CURRENCY_CODE,
                  makeDefault,
                  parsedBalance,
                  System.currentTimeMillis(),
                  finalIcon);
          if (walletId <= 0L) {
            return Boolean.FALSE;
          }
          if (makeDefault) {
            services.walletDao.clearDefaultWalletsExcept(walletId);
            services.defaultsStore.setDefaultWalletId(walletId);
          }
          return Boolean.TRUE;
        },
        created -> {
          if (!isShowing()) {
            return;
          }
          actionsView.setPrimaryEnabled(true);
          if (!Boolean.TRUE.equals(created)) {
            Toast.makeText(getContext(), R.string.wallet_error_create, Toast.LENGTH_SHORT).show();
            return;
          }
          Toast.makeText(getContext(), R.string.wallet_created, Toast.LENGTH_SHORT).show();
          dismiss();
          onSaved.run();
        });
  }

  private static final String[] WALLET_EMOJIS = {
    "💳", "💰", "🏦", "💵", "💎", "🏠", "🚗", "🎓",
    "✈️", "🛒", "🍔", "☕", "🎮", "👕", "💊", "🐾",
    "🎵", "📱", "💻", "🏋️"
  };
}
