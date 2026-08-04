package com.dwlhm.finan.ui.common;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.dwlhm.finan.R;

import java.util.function.Consumer;

public final class EmojiPickerBottomSheet extends BottomSheetDialog {

  private final Consumer<String> onEmojiSelected;

  public EmojiPickerBottomSheet(
      @NonNull Context context,
      @NonNull String initialEmoji,
      @NonNull Consumer<String> onEmojiSelected) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.onEmojiSelected = onEmojiSelected;
    setContentView(R.layout.dialog_emoji_picker_bottom_sheet);

    CustomEmojiKeyboardView keyboardView = findViewById(R.id.emoji_keyboard_view);
    if (keyboardView != null) {
      keyboardView.setSelectedEmoji(initialEmoji);
      keyboardView.setOnEmojiSelectedListener(emoji -> {
        if (onEmojiSelected != null) {
          onEmojiSelected.accept(emoji);
        }
        dismiss();
      });
      keyboardView.setOnCloseClickListener(this::dismiss);
    }
  }
}
