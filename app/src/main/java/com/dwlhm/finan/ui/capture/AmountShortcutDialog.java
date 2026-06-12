package com.dwlhm.finan.ui.capture;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.money.MoneyInputFormatter;
import com.dwlhm.finan.util.money.MoneyParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AmountShortcutDialog extends Dialog {

  public interface Listener {
    void onShortcutsSaved(List<Long> shortcuts);
  }

  private final List<Long> shortcuts;
  private final Listener listener;

  private EditText amountInput;
  private ListView shortcutListView;
  private TextView emptyView;
  private ShortcutAdapter adapter;

  public AmountShortcutDialog(
      @NonNull Context context, List<Long> initialShortcuts, Listener listener) {
    super(context);
    this.shortcuts = new ArrayList<>();
    if (initialShortcuts != null) {
      for (Long amount : initialShortcuts) {
        if (amount != null && amount > 0L && !shortcuts.contains(amount)) {
          shortcuts.add(amount);
        }
      }
    }
    this.listener = listener;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_amount_shortcuts);
    if (getWindow() != null) {
      getWindow()
          .setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    LabeledEditTextView amountField = findViewById(R.id.amount_shortcut_field);
    amountInput = amountField.getEditText();
    MoneyInputFormatter.attach(amountInput, true);

    Button addButton = findViewById(R.id.amount_shortcut_add);
    shortcutListView = findViewById(R.id.amount_shortcut_list);
    emptyView = findViewById(R.id.amount_shortcut_empty);
    DialogActionsView actionsView = findViewById(R.id.amount_shortcut_actions);

    adapter = new ShortcutAdapter();
    shortcutListView.setAdapter(adapter);

    addButton.setOnClickListener(v -> addShortcut());
    actionsView.setOnCancelClickListener(v -> dismiss());
    actionsView.setOnPrimaryClickListener(
        v -> {
          listener.onShortcutsSaved(new ArrayList<>(shortcuts));
          dismiss();
        });

    renderEmptyState();
    amountInput.requestFocus();
    amountInput.post(this::showKeyboard);
  }

  private void addShortcut() {
    long amount;
    try {
      amount = MoneyParser.parse(amountInput.getText().toString());
    } catch (IllegalArgumentException e) {
      Toast.makeText(getContext(), R.string.capture_amount_shortcut_invalid, Toast.LENGTH_SHORT)
          .show();
      return;
    }

    if (amount <= 0L) {
      Toast.makeText(getContext(), R.string.capture_amount_shortcut_invalid, Toast.LENGTH_SHORT)
          .show();
      return;
    }

    if (shortcuts.contains(amount)) {
      Toast.makeText(getContext(), R.string.capture_amount_shortcut_duplicate, Toast.LENGTH_SHORT)
          .show();
      return;
    }

    shortcuts.add(amount);
    amountInput.setText("");
    adapter.notifyDataSetChanged();
    renderEmptyState();
  }

  private void moveShortcut(int position, int direction) {
    int target = position + direction;
    if (position < 0 || position >= shortcuts.size() || target < 0 || target >= shortcuts.size()) {
      return;
    }
    Collections.swap(shortcuts, position, target);
    adapter.notifyDataSetChanged();
  }

  private void deleteShortcut(int position) {
    if (position < 0 || position >= shortcuts.size()) {
      return;
    }
    shortcuts.remove(position);
    adapter.notifyDataSetChanged();
    renderEmptyState();
  }

  private void renderEmptyState() {
    boolean empty = shortcuts.isEmpty();
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    shortcutListView.setVisibility(empty ? View.GONE : View.VISIBLE);
  }

  private void showKeyboard() {
    InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.showSoftInput(amountInput, 0);
    }
  }

  private final class ShortcutAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      return shortcuts.size();
    }

    @Override
    public Object getItem(int position) {
      return shortcuts.get(position);
    }

    @Override
    public long getItemId(int position) {
      return shortcuts.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row = convertView;
      ViewHolder holder;
      if (row == null) {
        row = LayoutInflater.from(getContext()).inflate(R.layout.item_amount_shortcut, parent, false);
        holder = new ViewHolder(row);
        row.setTag(holder);
      } else {
        holder = (ViewHolder) row.getTag();
      }

      holder.amount.setText(MoneyFormatter.format(shortcuts.get(position)));
      holder.up.setEnabled(position > 0);
      holder.down.setEnabled(position < shortcuts.size() - 1);
      holder.up.setAlpha(position > 0 ? 1f : 0.35f);
      holder.down.setAlpha(position < shortcuts.size() - 1 ? 1f : 0.35f);

      holder.up.setOnClickListener(v -> moveShortcut(position, -1));
      holder.down.setOnClickListener(v -> moveShortcut(position, 1));
      holder.delete.setOnClickListener(v -> deleteShortcut(position));
      return row;
    }
  }

  private static final class ViewHolder {
    final TextView amount;
    final ImageButton up;
    final ImageButton down;
    final ImageButton delete;

    ViewHolder(View row) {
      amount = row.findViewById(R.id.amount_shortcut_item_amount);
      up = row.findViewById(R.id.amount_shortcut_move_up);
      down = row.findViewById(R.id.amount_shortcut_move_down);
      delete = row.findViewById(R.id.amount_shortcut_delete);
    }
  }
}
