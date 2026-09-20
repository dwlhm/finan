package com.dwlhm.finan.ui;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.dwlhm.finan.service.privacy.AppLock;
import com.dwlhm.finan.ui.settings.SettingsFragment;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityIntentAndroidTest {
  private static void assertSettings(ActivityScenario<MainActivity> scenario) {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    scenario.onActivity(activity -> {
      assertTrue(activity.getSupportFragmentManager().getFragments().stream()
          .anyMatch(f -> f instanceof SettingsFragment && f.isVisible()));
      assertFalse(activity.getIntent().hasExtra(MainActivity.EXTRA_NAV_TARGET));
    });
  }

  @Test public void coldAndDeliveredSettingsIntentConsumeAfterResumeAndStayConsumedOnRecreation() {
    Context context = ApplicationProvider.getApplicationContext();
    android.content.SharedPreferences prefs = context.getSharedPreferences(AppLock.PREFS, 0);
    boolean old = prefs.getBoolean("app_lock", false);
    prefs.edit().putBoolean("app_lock", false).commit();
    Intent cold = new Intent(context, MainActivity.class)
        .putExtra(MainActivity.EXTRA_NAV_TARGET, MainActivity.NAV_TARGET_SETTINGS);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(cold)) {
      assertSettings(scenario);
      scenario.recreate();
      assertSettings(scenario);
      scenario.onActivity(activity -> activity.onNewIntent(new Intent(context, MainActivity.class)
          .putExtra(MainActivity.EXTRA_NAV_TARGET, MainActivity.NAV_TARGET_CAPTURE)));
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(activity -> activity.onNewIntent(new Intent(context, MainActivity.class)
          .putExtra(MainActivity.EXTRA_NAV_TARGET, MainActivity.NAV_TARGET_SETTINGS)));
      assertSettings(scenario);
      scenario.recreate();
      assertSettings(scenario);
    } finally { prefs.edit().putBoolean("app_lock", old).commit(); }
  }

  @Test public void rootNavigationSettlesEachSurfaceAndSurvivesRecreation() {
    Context context = ApplicationProvider.getApplicationContext();
    android.content.SharedPreferences prefs = context.getSharedPreferences(AppLock.PREFS, 0);
    boolean old = prefs.getBoolean("app_lock", false);
    prefs.edit().putBoolean("app_lock", false).commit();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(
        new Intent(context, MainActivity.class))) {
      navigateAndAssert(scenario, context, MainActivity.NAV_TARGET_DASHBOARD,
          View.GONE, View.VISIBLE, View.VISIBLE);
      navigateAndAssert(scenario, context, MainActivity.NAV_TARGET_SETTINGS,
          View.INVISIBLE, View.VISIBLE, View.VISIBLE);
      navigateAndAssert(scenario, context, MainActivity.NAV_TARGET_CAPTURE,
          View.VISIBLE, View.GONE, View.GONE);
      scenario.recreate();
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(activity -> {
        assertEquals(View.VISIBLE, activity.findViewById(com.dwlhm.finan.R.id.capture_container).getVisibility());
        assertEquals(View.GONE, activity.findViewById(com.dwlhm.finan.R.id.bottom_bar_buku_kas).getVisibility());
      });
    } finally { prefs.edit().putBoolean("app_lock", old).commit(); }
  }

  private static void navigateAndAssert(ActivityScenario<MainActivity> scenario, Context context,
      String target, int captureVisibility, int sheetVisibility, int bottomBarVisibility) {
    scenario.onActivity(activity -> activity.onNewIntent(new Intent(context, MainActivity.class)
        .putExtra(MainActivity.EXTRA_NAV_TARGET, target)));
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    scenario.onActivity(activity -> {
      assertEquals(captureVisibility,
          activity.findViewById(com.dwlhm.finan.R.id.capture_container).getVisibility());
      assertEquals(sheetVisibility,
          activity.findViewById(com.dwlhm.finan.R.id.white_sliding_sheet).getVisibility());
      assertEquals(bottomBarVisibility,
          activity.findViewById(com.dwlhm.finan.R.id.bottom_bar_buku_kas).getVisibility());
      assertEquals(MainActivity.NAV_TARGET_SETTINGS.equals(target) ? View.VISIBLE : View.GONE,
          activity.findViewById(com.dwlhm.finan.R.id.settings_overlay_container).getVisibility());
      String expectedDescription = MainActivity.NAV_TARGET_SETTINGS.equals(target)
          ? activity.getString(com.dwlhm.finan.R.string.nav_dashboard)
          : activity.getString(com.dwlhm.finan.R.string.nav_settings);
      assertEquals(expectedDescription,
          activity.findViewById(com.dwlhm.finan.R.id.bottom_bar_btn_settings).getContentDescription());
    });
  }
}
