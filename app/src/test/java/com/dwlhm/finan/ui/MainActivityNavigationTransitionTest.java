package com.dwlhm.finan.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MainActivityNavigationTransitionTest {
  @Test public void captureToDashboardSlidesSheetAndFadesBottomBar() {
    assertTransition(MainActivity.Screen.CAPTURE, MainActivity.Screen.DASHBOARD,
        MainActivity.OverlayMotion.SLIDE_DOWN, MainActivity.BottomBarMotion.FADE_IN);
  }

  @Test public void captureToSettingsSlidesOverlayAndFadesBottomBar() {
    assertTransition(MainActivity.Screen.CAPTURE, MainActivity.Screen.SETTINGS,
        MainActivity.OverlayMotion.SLIDE_DOWN, MainActivity.BottomBarMotion.FADE_IN);
  }

  @Test public void dashboardToCaptureSlidesUpAndFadesBottomBarOut() {
    assertTransition(MainActivity.Screen.DASHBOARD, MainActivity.Screen.CAPTURE,
        MainActivity.OverlayMotion.SLIDE_UP, MainActivity.BottomBarMotion.FADE_OUT);
  }

  @Test public void settingsToCaptureSlidesUpAndFadesBottomBarOut() {
    assertTransition(MainActivity.Screen.SETTINGS, MainActivity.Screen.CAPTURE,
        MainActivity.OverlayMotion.SLIDE_UP, MainActivity.BottomBarMotion.FADE_OUT);
  }

  @Test public void dashboardToSettingsFadesInWithoutBottomBarMotion() {
    assertTransition(MainActivity.Screen.DASHBOARD, MainActivity.Screen.SETTINGS,
        MainActivity.OverlayMotion.FADE_IN, MainActivity.BottomBarMotion.NONE);
  }

  @Test public void settingsToDashboardFadesInWithoutBottomBarMotion() {
    assertTransition(MainActivity.Screen.SETTINGS, MainActivity.Screen.DASHBOARD,
        MainActivity.OverlayMotion.FADE_IN, MainActivity.BottomBarMotion.NONE);
  }

  @Test public void sameScreenAndNullScreensAreImmediate() {
    assertTransition(MainActivity.Screen.CAPTURE, MainActivity.Screen.CAPTURE,
        MainActivity.OverlayMotion.NONE, MainActivity.BottomBarMotion.NONE);
    assertTransition(MainActivity.Screen.DASHBOARD, MainActivity.Screen.DASHBOARD,
        MainActivity.OverlayMotion.NONE, MainActivity.BottomBarMotion.NONE);
    assertTransition(MainActivity.Screen.SETTINGS, MainActivity.Screen.SETTINGS,
        MainActivity.OverlayMotion.NONE, MainActivity.BottomBarMotion.NONE);

    MainActivity.NavigationTransition transitionNullBoth = MainActivity.NavigationTransition.resolve(null, null);
    assertEquals(MainActivity.OverlayMotion.NONE, transitionNullBoth.overlayMotion());
    assertEquals(MainActivity.BottomBarMotion.NONE, transitionNullBoth.bottomBarMotion());

    MainActivity.NavigationTransition transitionNullFrom = MainActivity.NavigationTransition.resolve(null, MainActivity.Screen.CAPTURE);
    assertEquals(MainActivity.OverlayMotion.NONE, transitionNullFrom.overlayMotion());
    assertEquals(MainActivity.BottomBarMotion.NONE, transitionNullFrom.bottomBarMotion());

    MainActivity.NavigationTransition transitionNullTo = MainActivity.NavigationTransition.resolve(MainActivity.Screen.CAPTURE, null);
    assertEquals(MainActivity.OverlayMotion.NONE, transitionNullTo.overlayMotion());
    assertEquals(MainActivity.BottomBarMotion.NONE, transitionNullTo.bottomBarMotion());
  }

  private static void assertTransition(MainActivity.Screen from, MainActivity.Screen to,
      MainActivity.OverlayMotion overlay, MainActivity.BottomBarMotion bottomBar) {
    MainActivity.NavigationTransition transition = MainActivity.NavigationTransition.resolve(from, to);
    assertEquals(overlay, transition.overlayMotion());
    assertEquals(bottomBar, transition.bottomBarMotion());
  }
}
