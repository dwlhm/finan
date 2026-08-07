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
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.ui.MainActivity;
import com.dwlhm.finan.ui.common.AppServices;

import java.util.List;
import java.util.Locale;

public class ShortcutWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_EXECUTE_SHORTCUT = "com.dwlhm.finan.widget.ACTION_EXECUTE_SHORTCUT";
    public static final String ACTION_UNDO_SHORTCUT = "com.dwlhm.finan.widget.ACTION_UNDO_SHORTCUT";
    public static final String EXTRA_SHORTCUT_ID = "extra_shortcut_id";
    public static final String EXTRA_SHORTCUT_ACTION = "extra_shortcut_action";
    public static final String ACTION_TYPE_UNDO = "undo";
    public static final String ACTION_TYPE_EXECUTE = "execute";
    public static final String EXTRA_NAV_TARGET = "com.dwlhm.finan.EXTRA_NAV_TARGET";

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
        AppServices services = AppServices.create(context);
        TransactionTemplate template = services.transactionTemplateDao.findById(shortcutId);
        if (template == null) {
            return;
        }

        if (template.getAmountMinor() <= 0) {
            Intent captureIntent = new Intent(context, MainActivity.class);
            captureIntent.setAction(Intent.ACTION_MAIN);
            captureIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            captureIntent.putExtra(MainActivity.EXTRA_NAV_TARGET, "capture");
            captureIntent.putExtra(MainActivity.EXTRA_TEMPLATE_ID, template.getId());
            captureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(captureIntent);
            return;
        }

        long walletId = 0L;
        if (template.getWalletId() != null && template.getWalletId() > 0) {
            walletId = template.getWalletId();
        } else {
            Wallet defaultWallet = services.walletDao.findDefault();
            if (defaultWallet != null) {
                walletId = defaultWallet.getId();
            } else {
                List<Wallet> wallets = services.walletDao.findAll();
                if (wallets != null && !wallets.isEmpty()) {
                    walletId = wallets.get(0).getId();
                }
            }
        }

        long categoryId = 0L;
        if (template.getCategoryId() != null && template.getCategoryId() > 0) {
            categoryId = template.getCategoryId();
        } else {
            String typeFilter = template.getType() != null ? template.getType().name() : "EXPENSE";
            List<Category> categories = services.categoryDao.findByTypeFilterOrderByUsage(typeFilter);
            if (categories != null && !categories.isEmpty()) {
                categoryId = categories.get(0).getId();
            } else {
                Category defaultCat = services.categoryDao.findDefault();
                if (defaultCat != null) {
                    categoryId = defaultCat.getId();
                } else {
                    List<Category> allCats = services.categoryDao.findAllOrdered();
                    if (allCats != null && !allCats.isEmpty()) {
                        categoryId = allCats.get(0).getId();
                    }
                }
            }
        }

        String note = (template.getNote() != null && !template.getNote().trim().isEmpty())
                ? template.getNote()
                : template.getName();

        Transaction tx = new Transaction(
                0L,
                template.getAmountMinor(),
                template.getType() != null ? template.getType() : TransactionType.EXPENSE,
                walletId,
                categoryId,
                System.currentTimeMillis(),
                note
        );

        long txId = services.transactionService.save(tx);
        WidgetStateStore.setPendingShortcutUndo(context, template.getId(), txId, System.currentTimeMillis() + 5000L);
        startUndoCountdownHandler(context);

        Intent broadcastIntent = new Intent("com.dwlhm.finan.ACTION_DATA_CHANGED");
        broadcastIntent.setPackage(context.getPackageName());
        context.sendBroadcast(broadcastIntent);

    }

    private void handleUndoShortcut(Context context) {
        long shortcutId = WidgetStateStore.getPendingShortcutUndoId(context);
        long txId = WidgetStateStore.getPendingShortcutUndoTxId(context);
        if (txId > 0) {
            AppServices services = AppServices.create(context);
            services.transactionService.delete(txId);
        }
        WidgetStateStore.clearPendingShortcutUndo(context);
        WidgetStateStore.setPendingShortcutCancelled(context, shortcutId, System.currentTimeMillis() + 1500L);
        updateAllWidgets(context);

        Intent broadcastIntent = new Intent("com.dwlhm.finan.ACTION_DATA_CHANGED");
        broadcastIntent.setPackage(context.getPackageName());
        context.sendBroadcast(broadcastIntent);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            WidgetStateStore.clearPendingShortcutCancelled(context);
            updateAllWidgets(context);
        }, 1500L);
    }

    private void startUndoCountdownHandler(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (WidgetStateStore.isPendingShortcutUndoActive(context)) {
                    updateAllWidgets(context);
                    long remaining = WidgetStateStore.getPendingShortcutUndoDeadline(context) - System.currentTimeMillis();
                    if (remaining > 0) {
                        handler.postDelayed(this, 1000L);
                    } else {
                        WidgetStateStore.clearPendingShortcutUndo(context);
                        updateAllWidgets(context);
                    }
                }
            }
        });
    }
}
