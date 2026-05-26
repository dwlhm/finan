package com.dwlhm.finan.ui.common;

import android.content.Context;

import com.dwlhm.finan.FinanApplication;

public final class ServicesProvider {

  private ServicesProvider() {}

  public static AppServices get(Context context) {
    return ((FinanApplication) context.getApplicationContext()).getServices();
  }
}
