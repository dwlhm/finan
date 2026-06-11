package com.dwlhm.finan.ui.common.infinitescroll;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Runs background page loads and delivers results on the main thread. */
public interface PageLoadExecutor {

  <R> void compute(Callable<R> background, Consumer<R> onResult);
}
