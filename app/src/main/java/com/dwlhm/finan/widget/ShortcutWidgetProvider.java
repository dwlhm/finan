package com.dwlhm.finan.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;

import com.dwlhm.finan.R;
import com.dwlhm.finan.service.privacy.AppLock;
import com.dwlhm.finan.ui.MainActivity;
import com.dwlhm.finan.ui.common.ServicesProvider;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShortcutWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_EXECUTE_SHORTCUT = "com.dwlhm.finan.widget.ACTION_EXECUTE_SHORTCUT";
    public static final String ACTION_UNDO_SHORTCUT = "com.dwlhm.finan.widget.ACTION_UNDO_SHORTCUT";
    public static final String EXTRA_SHORTCUT_ID = "extra_shortcut_id";
    public static final String EXTRA_SHORTCUT_ACTION = "extra_shortcut_action";
    public static final String ACTION_TYPE_UNDO = "undo";
    public static final String ACTION_TYPE_EXECUTE = "execute";
    public static final String EXTRA_NAV_TARGET = "com.dwlhm.finan.EXTRA_NAV_TARGET";

    private static final ExecutorService widgetExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "finan-widget-db");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (appWidgetIds != null) {
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (AppLock.privateWidgets(context)) {
            updateAllWidgets(context);
            return;
        }
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (ACTION_EXECUTE_SHORTCUT.equals(action)) {
            String shortcutAction = intent.getStringExtra(EXTRA_SHORTCUT_ACTION);
            if (ACTION_TYPE_UNDO.equals(shortcutAction)) {
                handleUndoShortcut(context);
            } else {
                long shortcutId = intent.getLongExtra(EXTRA_SHORTCUT_ID, -1L);
                if (shortcutId > 0) {
                    handleExecuteShortcut(context, shortcutId);
                }
            }
        } else if (ACTION_UNDO_SHORTCUT.equals(action)) {
            handleUndoShortcut(context);
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action) || "com.dwlhm.finan.ACTION_DATA_CHANGED".equals(action)) {
            updateAllWidgets(context);
        }
    }

    public static void updateAllWidgets(Context context) {
        if (context == null) return;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, ShortcutWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.grid_shortcut_items);
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        if (AppLock.privateWidgets(context)) {
            appWidgetManager.updateAppWidget(appWidgetId, AppLock.privateWidgetViews(context, appWidgetId));
            return;
        }
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_shortcut);

        Intent intent = new Intent(context, ShortcutWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        views.setRemoteAdapter(R.id.grid_shortcut_items, intent);
        views.setEmptyView(R.id.grid_shortcut_items, R.id.tv_shortcut_empty);

        Intent clickIntent = new Intent(context, ShortcutWidgetProvider.class);
        clickIntent.setAction(ACTION_EXECUTE_SHORTCUT);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        views.setPendingIntentTemplate(R.id.grid_shortcut_items, clickPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static String formatCompactAmount(long amountMinor) {
        if (amountMinor <= 0) {
            return "Bebas";
        }
        if (amountMinor >= 1_000_000) {
            long jtBase = amountMinor / 1_000_000;
            long remainder = amountMinor % 1_000_000;
            if (remainder == 0) {
                return jtBase + "Jt";
            }
            long tenths = remainder / 100_000;
            if (remainder % 100_000 == 0) {
                return jtBase + "." + tenths + "Jt";
            }
            double val = amountMinor / 1_000_000.0;
            return String.format(Locale.US, "%.1fJt", val);
        } else if (amountMinor >= 1_000) {
            long rbBase = amountMinor / 1_000;
            long remainder = amountMinor % 1_000;
            if (remainder == 0) {
                return rbBase + "rb";
            }
            long tenths = remainder / 100;
            if (remainder % 100 == 0) {
                return rbBase + "." + tenths + "rb";
            }
            double val = amountMinor / 1_000.0;
            return String.format(Locale.US, "%.1frb", val);
        } else {
            return "Rp " + amountMinor;
        }
    }

    private void handleExecuteShortcut(Context context, long shortcutId) {
        if (shortcutId <= 0) {
            return;
        }
        Intent captureIntent = new Intent(context, MainActivity.class);
        captureIntent.setAction(Intent.ACTION_MAIN);
        captureIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        captureIntent.putExtra(MainActivity.EXTRA_NAV_TARGET, MainActivity.NAV_TARGET_CAPTURE);
        captureIntent.putExtra(MainActivity.EXTRA_TEMPLATE_ID, shortcutId);
        captureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(captureIntent);
    }

    private void handleUndoShortcut(Context context) {
        widgetExecutor.execute(() -> {
            long shortcutId = WidgetStateStore.getPendingShortcutUndoId(context);
            long txId = WidgetStateStore.getPendingShortcutUndoTxId(context);
            if (txId > 0) {
                ServicesProvider.get(context).transactionService.delete(txId);
            }
            WidgetStateStore.clearPendingShortcutUndo(context);
            WidgetStateStore.setPendingShortcutCancelled(context, shortcutId, System.currentTimeMillis() + 1500L);
            new Handler(Looper.getMainLooper()).post(() -> {
                updateAllWidgets(context);

                Intent broadcastIntent = new Intent("com.dwlhm.finan.ACTION_DATA_CHANGED");
                broadcastIntent.setPackage(context.getPackageName());
                context.sendBroadcast(broadcastIntent);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    WidgetStateStore.clearPendingShortcutCancelled(context);
                    updateAllWidgets(context);
                }, 1500L);
            });
        });
    }

}
