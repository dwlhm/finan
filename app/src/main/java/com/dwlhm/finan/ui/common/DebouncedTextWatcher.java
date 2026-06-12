package com.dwlhm.finan.ui.common;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;

import java.util.function.Consumer;

public final class DebouncedTextWatcher implements TextWatcher {

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final long delayMillis;
  private final Consumer<String> listener;
  private Runnable pending;

  public DebouncedTextWatcher(long delayMillis, Consumer<String> listener) {
    this.delayMillis = delayMillis;
    this.listener = listener;
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    cancel();
    String value = s.toString();
    pending = () -> listener.accept(value);
    handler.postDelayed(pending, delayMillis);
  }

  @Override
  public void afterTextChanged(Editable s) {}

  public void cancel() {
    if (pending != null) {
      handler.removeCallbacks(pending);
      pending = null;
    }
  }
}
