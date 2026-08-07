package com.dwlhm.finan.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.annotation.Nullable;
import android.widget.TextView;
import com.dwlhm.finan.R;

public final class BottomSheetHelper {

  public interface OnOptionSelectedListener {
    void onOptionSelected(int index, String option);
  }

  public static BottomSheetDialog showOptionPicker(
      Context context,
      CharSequence title,
      String[] options,
      int selectedIndex,
      OnOptionSelectedListener listener) {
    BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Finan_BottomSheetDialog);
    View view = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_option_picker_bottom_sheet, null);
    dialog.setContentView(view);

    TextView titleView = view.findViewById(R.id.option_picker_title);
    if (title != null && title.length() > 0) {
      titleView.setText(title);
      titleView.setVisibility(View.VISIBLE);
    } else {
      titleView.setVisibility(View.GONE);
    }

    android.widget.ListView listView = view.findViewById(R.id.option_picker_list);
    listView.setAdapter(new android.widget.BaseAdapter() {
      @Override
      public int getCount() {
        return options.length;
      }

      @Override
      public Object getItem(int position) {
        return options[position];
      }

      @Override
      public long getItemId(int position) {
        return position;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
          row = android.view.LayoutInflater.from(context).inflate(R.layout.item_option_picker, parent, false);
        }
        TextView text = row.findViewById(R.id.option_text);
        View checkmark = row.findViewById(R.id.option_checkmark);

        text.setText(options[position]);
        boolean isSelected = (position == selectedIndex);
        if (isSelected) {
          text.setTypeface(null, android.graphics.Typeface.BOLD);
          checkmark.setVisibility(View.VISIBLE);
        } else {
          text.setTypeface(null, android.graphics.Typeface.NORMAL);
          checkmark.setVisibility(View.GONE);
        }
        return row;
      }
    });

    listView.setOnItemClickListener((parent, v, position, id) -> {
      dialog.dismiss();
      if (listener != null) {
        listener.onOptionSelected(position, options[position]);
      }
    });

    show(dialog);
    return dialog;
  }

  public static BottomSheetDialog showConfirmation(
      Context context,
      CharSequence title,
      CharSequence message,
      CharSequence confirmText,
      @Nullable Integer confirmTextColor,
      CharSequence cancelText,
      Runnable onConfirm) {
    BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Finan_BottomSheetDialog);
    dialog.setContentView(R.layout.dialog_confirm_delete);

    TextView titleView = dialog.findViewById(R.id.confirm_delete_title);
    TextView messageView = dialog.findViewById(R.id.confirm_delete_message);
    TextView cancelBtn = dialog.findViewById(R.id.confirm_delete_cancel);
    TextView confirmBtn = dialog.findViewById(R.id.confirm_delete_confirm);

    if (titleView != null) {
      if (title != null && title.length() > 0) {
        titleView.setText(title);
        titleView.setVisibility(View.VISIBLE);
      } else {
        titleView.setVisibility(View.GONE);
      }
    }

    if (messageView != null) {
      if (message != null && message.length() > 0) {
        messageView.setText(message);
        messageView.setVisibility(View.VISIBLE);
      } else {
        messageView.setVisibility(View.GONE);
      }
    }

    if (cancelBtn != null) {
      if (cancelText != null) {
        cancelBtn.setText(cancelText);
      }
      cancelBtn.setOnClickListener(v -> dialog.dismiss());
    }

    if (confirmBtn != null) {
      if (confirmText != null) {
        confirmBtn.setText(confirmText);
      }
      if (confirmTextColor != null) {
        confirmBtn.setTextColor(confirmTextColor);
      }
      confirmBtn.setOnClickListener(v -> {
        dialog.dismiss();
        if (onConfirm != null) {
          onConfirm.run();
        }
      });
    }

    show(dialog);
    return dialog;
  }

  private static final String PREFS_NAME = "finan_prefs";
  private static final String MASKED_MODE_KEY = "settings_wallet_masked_mode";

  public static void show(BottomSheetDialog dialog) {
    dialog.show();
    FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
    if (bottomSheet != null) {
      bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
      BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
      behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      behavior.setSkipCollapsed(true);

      ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (v, insets) -> {
        int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
        v.setPadding(
            v.getPaddingLeft(),
            v.getPaddingTop(),
            v.getPaddingRight(),
            bottomInset);
        return insets;
      });
    }
  }

  public static void show(Dialog dialog) {
    if (dialog instanceof BottomSheetDialog) {
      show((BottomSheetDialog) dialog);
      return;
    }
    dialog.show();
    Window window = dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      WindowManager.LayoutParams params = window.getAttributes();
      params.width = WindowManager.LayoutParams.MATCH_PARENT;
      params.height = WindowManager.LayoutParams.WRAP_CONTENT;
      params.gravity = Gravity.BOTTOM;
      window.setAttributes(params);
      window.setWindowAnimations(android.R.style.Animation_InputMethod);
    }
    makeDraggable(dialog);
  }

  public static void makeDraggable(Dialog dialog) {
    if (dialog instanceof BottomSheetDialog) {
      return;
    }
    Window window = dialog.getWindow();
    if (window == null) return;

    ViewGroup contentFrame = window.getDecorView().findViewById(android.R.id.content);
    if (contentFrame == null || contentFrame.getChildCount() == 0) return;

    View originalContent = contentFrame.getChildAt(0);
    contentFrame.removeView(originalContent);

    DraggableBottomSheetLayout wrapper = new DraggableBottomSheetLayout(dialog.getContext());
    wrapper.setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    wrapper.addView(originalContent);
    wrapper.setOnDismissListener(dialog::dismiss);
    contentFrame.addView(wrapper);
  }

  public static boolean isMasked(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    return prefs.getBoolean(MASKED_MODE_KEY, false);
  }
}
