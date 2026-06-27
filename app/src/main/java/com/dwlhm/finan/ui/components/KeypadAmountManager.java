package com.dwlhm.finan.ui.components;

import android.text.Editable;
import android.widget.EditText;

public class KeypadAmountManager implements OnKeypadActionListener {

    private final EditText editText;

    public KeypadAmountManager(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void onDigitEntered(int digit) {
        insertText(String.valueOf(digit));
    }

    @Override
    public void onBackspace() {
        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);
        
        if (start != end) {
            editText.getText().delete(Math.min(start, end), Math.max(start, end));
        } else if (start > 0) {
            editText.getText().delete(start - 1, start);
        }
    }

    @Override
    public void onClear() {
        editText.setText("");
    }

    @Override
    public void onShortcut(String shortcut) {
        insertText(shortcut);
    }

    @Override
    public void onDecimalPoint() {
        // If the text already has a decimal, don't add another (assuming localized or standard '.')
        String currentText = editText.getText().toString();
        if (!currentText.contains(".") && !currentText.contains(",")) { // Rough check, real check depends on MoneyFormatter
            insertText(".");
        }
    }

    @Override
    public void onDone() {
        // Can be handled by closing the keypad or moving focus
        editText.clearFocus();
    }

    private void insertText(String text) {
        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);
        
        Editable editable = editText.getText();
        editable.replace(Math.min(start, end), Math.max(start, end), text);
    }
}
