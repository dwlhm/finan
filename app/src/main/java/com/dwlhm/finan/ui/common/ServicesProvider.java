package com.dwlhm.finan.ui.common;

import android.content.Context;

import com.dwlhm.finan.FinanApplication;

public final class ServicesProvider {

  private ServicesProvider() {}

  public static AppServices get(Context context) {
    FinanApplication application = (FinanApplication) context.getApplicationContext();
    AppServices services = application.getServices();
    if (services != null) return services;
    return application.awaitServices();
  }
}
