package com.dwlhm.finan.ui.common.infinitescroll;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InfiniteScrollLoadPolicyTest {

  @Test
  public void canLoadMore_requiresIdleSessionWithRemainingPages() {
    assertTrue(InfiniteScrollLoadPolicy.canLoadMore(false, false, true));
    assertFalse(InfiniteScrollLoadPolicy.canLoadMore(true, false, true));
    assertFalse(InfiniteScrollLoadPolicy.canLoadMore(false, true, true));
    assertFalse(InfiniteScrollLoadPolicy.canLoadMore(false, false, false));
  }

  @Test
  public void shouldPrefetch_whenNearEndOfVisibleContent() {
    assertTrue(InfiniteScrollLoadPolicy.shouldPrefetch(8, 10, false, 2));
    assertFalse(InfiniteScrollLoadPolicy.shouldPrefetch(5, 10, false, 2));
    assertFalse(InfiniteScrollLoadPolicy.shouldPrefetch(-1, 10, false, 2));
    assertFalse(InfiniteScrollLoadPolicy.shouldPrefetch(8, 0, false, 2));
  }

  @Test
  public void shouldPrefetch_accountsForLoadingFooter() {
    assertTrue(InfiniteScrollLoadPolicy.shouldPrefetch(7, 10, true, 2));
    assertFalse(InfiniteScrollLoadPolicy.shouldPrefetch(5, 10, true, 2));
  }
}
