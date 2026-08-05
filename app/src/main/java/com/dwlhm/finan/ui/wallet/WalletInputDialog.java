package com.dwlhm.finan.ui.wallet;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.EmojiConstants;
import com.dwlhm.finan.ui.common.EmojiPickerBottomSheet;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.security.SecureRandom;

public final class WalletInputDialog extends BottomSheetDialog {

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
    EditText nameInput = nameField != null ? nameField.getEditText() : null;
    EditText balanceInput = balanceField != null ? balanceField.getEditText() : null;
    if (nameInput == null || balanceInput == null) return;

    FrameLayout iconContainer = findViewById(R.id.wallet_icon_container);
    TextView iconPreview = findViewById(R.id.wallet_icon_preview);
    EditText iconInput = new EditText(getContext());

    String initialEmoji = EmojiConstants.WALLET_EMOJIS[new SecureRandom().nextInt(EmojiConstants.WALLET_EMOJIS.length)];
    if (iconPreview != null) iconPreview.setText(initialEmoji);
    iconInput.setText(initialEmoji);

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

    MoneyInputFormatter.attach(balanceInput, true);
    CheckBox defaultInput = findViewById(R.id.wallet_default_input);
    DialogActionsView actionsView = findViewById(R.id.wallet_actions);

    if (actionsView != null) {
      actionsView.setOnCancelClickListener(v -> dismiss());
      actionsView.setOnPrimaryClickListener(
          v -> submitCreateWallet(nameInput, balanceInput, iconInput, defaultInput, actionsView));
    }

    services.dbWorker.compute(
        () -> services.walletService.findAll().isEmpty(),
        firstWallet -> {
          if (!isShowing() || firstWallet == null) {
            return;
          }
          if (defaultInput != null) {
            defaultInput.setChecked(firstWallet);
            defaultInput.setEnabled(!firstWallet);
          }
        });

    BottomSheetHelper.show(this);
    nameInput.requestFocus();
    if (getWindow() != null) {
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
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
      icon = EmojiConstants.WALLET_EMOJIS[new SecureRandom().nextInt(EmojiConstants.WALLET_EMOJIS.length)];
    }

    boolean makeDefault = defaultInput != null && defaultInput.isChecked();
    long parsedBalance = initialBalanceMinor;
    final String finalIcon = icon;
    actionsView.setPrimaryEnabled(false);

    services.dbWorker.compute(
        () -> {
          Wallet created = services.walletService.create(
              name, MoneyFormatter.DEFAULT_CURRENCY_CODE, makeDefault, parsedBalance, finalIcon);
          return created != null ? Boolean.TRUE : Boolean.FALSE;
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
}
