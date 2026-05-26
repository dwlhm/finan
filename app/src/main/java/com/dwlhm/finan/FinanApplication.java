package com.dwlhm.finan;

import android.app.Application;

import com.dwlhm.finan.ui.common.AppServices;

public final class FinanApplication extends Application {

  private AppServices services;

  @Override
  public void onCreate() {
    super.onCreate();
    services = AppServices.create(this);
  }

  public AppServices getServices() {
    return services;
  }
}
