package com.dwlhm.finan.service.privacy;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppLockTest {
  @Test public void coldProcessRequiresAuthenticationOnlyWhenEnabled() {
    AppLock.SessionState state = new AppLock.SessionState();
    assertTrue(state.isLocked(0, true));
    assertFalse(state.isLocked(0, false));
    state.onAuthenticated();
    assertFalse(state.isLocked(0, true));
    assertTrue(new AppLock.SessionState().isLocked(0, true));
  }
  @Test public void graceExpiresAtBoundaryIncludingBackgroundAtZero() {
    AppLock.SessionState state = new AppLock.SessionState();
    state.onAuthenticated();
    state.onBackground(0);
    assertFalse(state.isLocked(AppLock.LOCK_TIMEOUT_MS - 1, true));
    assertTrue(state.isLocked(AppLock.LOCK_TIMEOUT_MS, true));
    state.onForeground(AppLock.LOCK_TIMEOUT_MS);
    assertTrue(state.isLocked(AppLock.LOCK_TIMEOUT_MS, true));
  }
  @Test public void foregroundWithinGraceStopsBackgroundClock() {
    AppLock.SessionState state = new AppLock.SessionState();
    state.onAuthenticated();
    state.onBackground(0);
    state.onForeground(AppLock.LOCK_TIMEOUT_MS - 1);
    assertFalse(state.isLocked(AppLock.LOCK_TIMEOUT_MS * 2, true));
    state.onBackground(AppLock.LOCK_TIMEOUT_MS * 2);
    state.onBackground(AppLock.LOCK_TIMEOUT_MS * 3);
    assertTrue(state.isLocked(AppLock.LOCK_TIMEOUT_MS * 3, true));
  }
}
