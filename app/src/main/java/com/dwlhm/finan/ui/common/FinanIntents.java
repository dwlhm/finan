package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.content.Intent;

import com.dwlhm.finan.ui.capture.CaptureActivity;
import com.dwlhm.finan.ui.category.CategoryListActivity;
import com.dwlhm.finan.ui.history.HistoryActivity;
import com.dwlhm.finan.ui.settings.SettingsActivity;
import com.dwlhm.finan.ui.summary.SummaryActivity;
import com.dwlhm.finan.ui.wallet.WalletListActivity;

public final class FinanIntents {

  private FinanIntents() {}

  public static Intent capture(Context context) {
    return new Intent(context, CaptureActivity.class);
  }

  public static Intent history(Context context) {
    return new Intent(context, HistoryActivity.class);
  }

  public static Intent summary(Context context) {
    return new Intent(context, SummaryActivity.class);
  }

  public static Intent wallets(Context context) {
    return new Intent(context, WalletListActivity.class);
  }

  public static Intent categories(Context context) {
    return new Intent(context, CategoryListActivity.class);
  }

  public static Intent settings(Context context) {
    return new Intent(context, SettingsActivity.class);
  }
}
