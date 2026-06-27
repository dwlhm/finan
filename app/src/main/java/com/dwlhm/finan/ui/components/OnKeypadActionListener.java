package com.dwlhm.finan.ui.components;

public interface OnKeypadActionListener {
    void onDigitEntered(int digit);
    void onBackspace();
    void onClear();
    void onShortcut(String shortcut);
    void onDecimalPoint();
    void onDone();
}
