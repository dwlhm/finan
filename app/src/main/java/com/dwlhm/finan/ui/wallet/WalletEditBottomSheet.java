package com.dwlhm.finan.ui.wallet;

import android.app.Dialog;
import android.content.Context;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.LabeledEditTextView;

public final class WalletEditBottomSheet extends Dialog {

  private final AppServices services;
  private final Wallet wallet;
  private final Runnable onSaved;

  public WalletEditBottomSheet(
      @NonNull Context context,
      @NonNull AppServices services,
      @NonNull Wallet wallet,
      @NonNull Runnable onSaved) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.services = services;
    this.wallet = wallet;
    this.onSaved = onSaved;
    setContentView(R.layout.dialog_wallet_edit_bottom_sheet);
    setCancelable(false);
    setupViews();
  }

  private void setupViews() {
    LabeledEditTextView iconField = findViewById(R.id.wallet_icon_field);
    LabeledEditTextView nameField = findViewById(R.id.wallet_name_field);
    EditText iconInput = iconField.getEditText();
    EditText nameInput = nameField.getEditText();
    iconInput.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(2) });
    CheckBox defaultInput = findViewById(R.id.wallet_default_input);
    DialogActionsView actionsView = findViewById(R.id.wallet_actions);

    String currentIcon = wallet.getIcon();
    if (currentIcon != null) {
      iconInput.setText(currentIcon);
    }
    nameInput.setText(wallet.getName());
    nameInput.setSelection(nameInput.getText().length());
    defaultInput.setChecked(wallet.isDefault());
    defaultInput.setEnabled(!wallet.isDefault());
    defaultInput.setAlpha(wallet.isDefault() ? 0.72f : 1f);

    actionsView.setOnCancelClickListener(v -> dismiss());
    actionsView.setOnPrimaryClickListener(
        v -> submitUpdate(nameInput, iconInput, defaultInput, actionsView));

    BottomSheetHelper.show(this);
    nameInput.requestFocus();
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
  }

  private void submitUpdate(
      EditText nameInput,
      EditText iconInput,
      CheckBox defaultInput,
      DialogActionsView actionsView) {
    String name = nameInput.getText().toString().trim();
    if (name.isEmpty()) {
      nameInput.setError(getContext().getString(R.string.wallet_error_name));
      return;
    }
    String icon = iconInput.getText().toString().trim();
    if (icon.isEmpty()) {
      icon = WALLET_EMOJIS[new java.security.SecureRandom().nextInt(WALLET_EMOJIS.length)];
    }
    boolean makeDefault = defaultInput.isChecked();
    final String finalIcon = icon;
    actionsView.setPrimaryEnabled(false);

    services.dbWorker.compute(
        () -> services.walletDao.updateNameDefaultAndIcon(
            wallet.getId(), name, makeDefault, finalIcon),
        updated -> {
          if (!isShowing()) {
            return;
          }
          actionsView.setPrimaryEnabled(true);
          if (!Boolean.TRUE.equals(updated)) {
            Toast.makeText(getContext(), R.string.wallet_error_update, Toast.LENGTH_SHORT).show();
            return;
          }
          if (makeDefault) {
            services.defaultsStore.setDefaultWalletId(wallet.getId());
          }
          Toast.makeText(getContext(), R.string.wallet_updated, Toast.LENGTH_SHORT).show();
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
