package com.dwlhm.finan.ui.common;

import com.dwlhm.finan.ui.common.infinitescroll.PageLoadExecutor;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Single-thread executor for all SQLite access. */
public final class DbWorker implements PageLoadExecutor {

  private final ExecutorService executor;
  private final Handler mainHandler;
  private final Object token = new Object();

  public DbWorker() {
    executor =
        Executors.newSingleThreadExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "finan-db");
              thread.setDaemon(true);
              return thread;
            });
    mainHandler = new Handler(Looper.getMainLooper());
  }

  public void run(@NonNull Runnable background) {
    executor.execute(background);
  }

  public void run(@NonNull Runnable background, @NonNull Runnable ui) {
    executor.execute(
        () -> {
          background.run();
          Message msg = Message.obtain(mainHandler, ui);
          msg.obj = token;
          mainHandler.sendMessage(msg);
        });
  }

  public void runOnUi(@NonNull Runnable ui) {
    Message msg = Message.obtain(mainHandler, ui);
    msg.obj = token;
    mainHandler.sendMessage(msg);
  }

  @Override
  public <T> void compute(@NonNull Callable<T> background, @NonNull Consumer<T> ui) {
    executor.execute(
        () -> {
          T value;
          try {
            value = background.call();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          T result = value;
          Message msg = Message.obtain(mainHandler, () -> ui.accept(result));
          msg.obj = token;
          mainHandler.sendMessage(msg);
        });
  }

  public void cancelUiCallbacks() {
    mainHandler.removeCallbacksAndMessages(token);
  }

  public void shutdown() {
    cancelUiCallbacks();
    executor.shutdownNow();
  }

  public static boolean isMainThread() {
    return Looper.myLooper() == Looper.getMainLooper();
  }

  public static void requireBackgroundThread() {
    if (isMainThread()) {
      throw new IllegalStateException("Database work must not run on the main thread");
    }
  }
}
