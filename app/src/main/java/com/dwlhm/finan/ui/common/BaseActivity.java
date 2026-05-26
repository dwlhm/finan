package com.dwlhm.finan.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dwlhm.finan.R;

public abstract class BaseActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(getLayoutResId());
    setupBottomNav();
    onReady();
  }

  @LayoutRes
  protected abstract int getLayoutResId();

  protected void onReady() {}

  private void setupBottomNav() {
    View nav = findViewById(R.id.bottom_nav);
    if (nav == null) {
      return;
    }

    Button capture = findViewById(R.id.nav_capture);
    Button history = findViewById(R.id.nav_history);
    Button summary = findViewById(R.id.nav_summary);
    Button wallet = findViewById(R.id.nav_wallet);
    Button settings = findViewById(R.id.nav_settings);

    if (capture != null) {
      capture.setOnClickListener(v -> navigateIfNeeded(FinanIntents.capture(this)));
    }
    if (history != null) {
      history.setOnClickListener(v -> navigateIfNeeded(FinanIntents.history(this)));
    }
    if (summary != null) {
      summary.setOnClickListener(v -> navigateIfNeeded(FinanIntents.summary(this)));
    }
    if (wallet != null) {
      wallet.setOnClickListener(v -> navigateIfNeeded(FinanIntents.wallets(this)));
    }
    if (settings != null) {
      settings.setOnClickListener(v -> navigateIfNeeded(FinanIntents.settings(this)));
    }
  }

  private void navigateIfNeeded(Intent intent) {
    if (getClass().getName().equals(intent.getComponent().getClassName())) {
      return;
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startActivity(intent);
  }
}
