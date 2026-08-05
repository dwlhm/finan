package com.dwlhm.finan.ui.wallet;

import android.content.Context;
import android.text.TextUtils;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;

public final class WalletEditBottomSheet extends BottomSheetDialog {

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
    LabeledEditTextView nameField = findViewById(R.id.wallet_name_field);
    EditText nameInput = nameField != null ? nameField.getEditText() : null;
    if (nameInput == null) return;

    FrameLayout iconContainer = findViewById(R.id.wallet_icon_container);
    TextView iconPreview = findViewById(R.id.wallet_icon_preview);
    EditText iconInput = new EditText(getContext());

    String currentIcon = wallet.getIcon();
    if (TextUtils.isEmpty(currentIcon)) {
      currentIcon = EmojiConstants.WALLET_EMOJIS[0];
    }
    if (iconPreview != null) iconPreview.setText(currentIcon);
    iconInput.setText(currentIcon);

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

    nameInput.setText(wallet.getName());
    nameInput.setSelection(nameInput.getText().length());

    CheckBox defaultInput = findViewById(R.id.wallet_default_input);
    if (defaultInput != null) {
      defaultInput.setChecked(wallet.isDefault());
      if (wallet.isDefault()) {
        defaultInput.setEnabled(false);
      }
    }

    DialogActionsView actionsView = findViewById(R.id.wallet_actions);
    if (actionsView != null) {
      actionsView.setOnCancelClickListener(v -> dismiss());
      actionsView.setOnPrimaryClickListener(
          v -> submitUpdate(nameInput, iconInput, defaultInput, actionsView));
    }

    BottomSheetHelper.show(this);
    nameInput.requestFocus();
    if (getWindow() != null) {
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
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

    Wallet duplicate = services.walletDao.findByNameIgnoreCase(name);
    if (duplicate != null && duplicate.getId() != wallet.getId()) {
      nameInput.setError(getContext().getString(R.string.wallet_error_duplicate));
      nameInput.requestFocus();
      return;
    }

    String icon = iconInput.getText().toString().trim();
    boolean makeDefault = defaultInput != null && defaultInput.isChecked();
    actionsView.setPrimaryEnabled(false);

    services.dbWorker.compute(
        () -> {
          try {
            services.walletService.updateNameDefaultAndIcon(
                wallet.getId(), name, makeDefault, icon);
            return Boolean.TRUE;
          } catch (Exception e) {
            return Boolean.FALSE;
          }
        },
        success -> {
          if (!isShowing()) {
            return;
          }
          actionsView.setPrimaryEnabled(true);
          if (!Boolean.TRUE.equals(success)) {
            Toast.makeText(getContext(), R.string.wallet_error_name, Toast.LENGTH_SHORT).show();
            return;
          }
          Toast.makeText(getContext(), R.string.wallet_updated, Toast.LENGTH_SHORT).show();
          dismiss();
          onSaved.run();
        });
  }
}
