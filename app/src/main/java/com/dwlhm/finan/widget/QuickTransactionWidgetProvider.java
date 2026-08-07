package com.dwlhm.finan.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.util.List;

public class QuickTransactionWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_KEYPAD_DIGIT = "com.dwlhm.finan.widget.ACTION_KEYPAD_DIGIT";
    public static final String ACTION_KEYPAD_DELETE = "com.dwlhm.finan.widget.ACTION_KEYPAD_DELETE";
    public static final String ACTION_KEYPAD_CLEAR = "com.dwlhm.finan.widget.ACTION_KEYPAD_CLEAR";
    public static final String ACTION_TOGGLE_TYPE = "com.dwlhm.finan.widget.ACTION_TOGGLE_TYPE";
    public static final String ACTION_CYCLE_WALLET = "com.dwlhm.finan.widget.ACTION_CYCLE_WALLET";
    public static final String ACTION_CYCLE_CATEGORY = "com.dwlhm.finan.widget.ACTION_CYCLE_CATEGORY";
    public static final String ACTION_SAVE_TRANSACTION = "com.dwlhm.finan.widget.ACTION_SAVE_TRANSACTION";
    public static final String ACTION_UNDO_TRANSACTION = "com.dwlhm.finan.widget.ACTION_UNDO_TRANSACTION";
    public static final String ACTION_OPEN_WALLET_PICKER = "com.dwlhm.finan.widget.ACTION_OPEN_WALLET_PICKER";
    public static final String ACTION_OPEN_CATEGORY_PICKER = "com.dwlhm.finan.widget.ACTION_OPEN_CATEGORY_PICKER";
    public static final String ACTION_SELECT_PICKER_ITEM = "com.dwlhm.finan.widget.ACTION_SELECT_PICKER_ITEM";
    public static final String ACTION_CLOSE_PICKER = "com.dwlhm.finan.widget.ACTION_CLOSE_PICKER";

    public static final String EXTRA_DIGIT = "extra_digit";
    public static final String EXTRA_PICKER_ITEM_ID = "extra_picker_item_id";
    public static final String EXTRA_PICKER_MODE = "extra_picker_mode";
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action) || "com.dwlhm.finan.ACTION_DATA_CHANGED".equals(action)) {
            updateAllWidgets(context);
            return;
        }

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            updateAllWidgets(context);
            return;
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        if (ACTION_KEYPAD_DIGIT.equals(action)) {
            String digit = intent.getStringExtra(EXTRA_DIGIT);
            if (digit != null) {
                WidgetStateStore.appendDigit(context, appWidgetId, digit);
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        } else if (ACTION_KEYPAD_DELETE.equals(action)) {
            WidgetStateStore.deleteDigit(context, appWidgetId);
            updateWidget(context, appWidgetManager, appWidgetId);
        } else if (ACTION_KEYPAD_CLEAR.equals(action)) {
            WidgetStateStore.clearAmount(context, appWidgetId);
            updateWidget(context, appWidgetManager, appWidgetId);
        } else if (ACTION_TOGGLE_TYPE.equals(action)) {
            String newType = WidgetStateStore.toggleType(context, appWidgetId);
            AppServices services = AppServices.create(context);
            List<Category> categories = services.categoryDao.findByTypeFilterOrderByUsage(newType);
            if (categories != null && !categories.isEmpty()) {
                WidgetStateStore.setCategoryId(context, appWidgetId, categories.get(0).getId());
            } else {
                WidgetStateStore.setCategoryId(context, appWidgetId, 0L);
            }
            updateWidget(context, appWidgetManager, appWidgetId);
        } else if (ACTION_CYCLE_WALLET.equals(action) || ACTION_OPEN_WALLET_PICKER.equals(action)) {
            WidgetStateStore.setPickerMode(context, appWidgetId, "WALLET");
            updateWidget(context, appWidgetManager, appWidgetId);
        } else if (ACTION_CYCLE_CATEGORY.equals(action) || ACTION_OPEN_CATEGORY_PICKER.equals(action)) {
            WidgetStateStore.setPickerMode(context, appWidgetId, "CATEGORY");
            updateWidget(context, appWidgetManager, appWidgetId);
        } else if (ACTION_CLOSE_PICKER.equals(action)) {
            WidgetStateStore.clearPickerMode(context, appWidgetId);
            updateWidget(context, appWidgetManager, appWidgetId);
        } else if (ACTION_SELECT_PICKER_ITEM.equals(action)) {
            long itemId = intent.getLongExtra(EXTRA_PICKER_ITEM_ID, 0L);
            String mode = intent.getStringExtra(EXTRA_PICKER_MODE);
            if (itemId > 0L && mode != null) {
                if ("WALLET".equalsIgnoreCase(mode)) {
                    WidgetStateStore.setWalletId(context, appWidgetId, itemId);
                } else if ("CATEGORY".equalsIgnoreCase(mode)) {
                    WidgetStateStore.setCategoryId(context, appWidgetId, itemId);
                }
            }
            WidgetStateStore.clearPickerMode(context, appWidgetId);
            updateWidget(context, appWidgetManager, appWidgetId);
        } else if (ACTION_SAVE_TRANSACTION.equals(action)) {
            handleSaveTransaction(context, appWidgetManager, appWidgetId);
        } else if (ACTION_UNDO_TRANSACTION.equals(action)) {
            handleUndoTransaction(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAllWidgets(Context context) {
        if (context == null) return;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        android.content.ComponentName thisWidget = new android.content.ComponentName(context, QuickTransactionWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_transaction);
        AppServices services = AppServices.create(context);

        String pickerMode = WidgetStateStore.getPickerMode(context, appWidgetId);
        if (pickerMode != null) {
            views.setViewVisibility(R.id.layout_widget_main_content, android.view.View.GONE);
            views.setViewVisibility(R.id.layout_widget_picker_content, android.view.View.VISIBLE);

            PendingIntent backIntent = getPendingIntent(context, appWidgetId, ACTION_CLOSE_PICKER, null, 100);
            views.setOnClickPendingIntent(R.id.btn_picker_back, backIntent);

            int[] itemSlotIds = {
                    R.id.item_picker_0, R.id.item_picker_1, R.id.item_picker_2, R.id.item_picker_3, R.id.item_picker_4,
                    R.id.item_picker_5, R.id.item_picker_6, R.id.item_picker_7, R.id.item_picker_8, R.id.item_picker_9
            };

            if ("WALLET".equalsIgnoreCase(pickerMode)) {
                views.setTextViewText(R.id.tv_picker_title, "Pilih Dompet");
                List<Wallet> wallets = services.walletDao.findAll();
                long currentWalletId = WidgetStateStore.getWalletId(context, appWidgetId);
                int count = wallets != null ? wallets.size() : 0;
                for (int i = 0; i < itemSlotIds.length; i++) {
                    if (i < count) {
                        Wallet w = wallets.get(i);
                        boolean isSelected = w.getId() == currentWalletId;
                        String icon = w.getIcon() != null && !w.getIcon().isEmpty() ? w.getIcon() : "💳";
                        String text = (isSelected ? "✓ " : "  ") + icon + " " + w.getName();

                        views.setViewVisibility(itemSlotIds[i], android.view.View.VISIBLE);
                        views.setTextViewText(itemSlotIds[i], text);
                        views.setTextColor(itemSlotIds[i], context.getColor(isSelected ? R.color.finan_primary : R.color.finan_text_primary));

                        Intent itemIntent = new Intent(context, QuickTransactionWidgetProvider.class);
                        itemIntent.setAction(ACTION_SELECT_PICKER_ITEM);
                        itemIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        itemIntent.putExtra(EXTRA_PICKER_ITEM_ID, w.getId());
                        itemIntent.putExtra(EXTRA_PICKER_MODE, "WALLET");
                        PendingIntent pi = PendingIntent.getBroadcast(
                                context, appWidgetId * 1000 + i, itemIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        views.setOnClickPendingIntent(itemSlotIds[i], pi);
                    } else {
                        views.setViewVisibility(itemSlotIds[i], android.view.View.GONE);
                    }
                }
            } else if ("CATEGORY".equalsIgnoreCase(pickerMode)) {
                views.setTextViewText(R.id.tv_picker_title, "Pilih Kategori");
                String currentType = WidgetStateStore.getType(context, appWidgetId);
                List<Category> categories = services.categoryDao.findByTypeFilterOrderByUsage(currentType);
                long currentCatId = WidgetStateStore.getCategoryId(context, appWidgetId);

                int count = categories != null ? categories.size() : 0;
                for (int i = 0; i < itemSlotIds.length; i++) {
                    if (i < count) {
                        Category c = categories.get(i);
                        boolean isSelected = c.getId() == currentCatId;
                        String icon = c.getIcon() != null && !c.getIcon().isEmpty() ? c.getIcon() : "🏷️";
                        String text = (isSelected ? "✓ " : "  ") + icon + " " + c.getName();

                        views.setViewVisibility(itemSlotIds[i], android.view.View.VISIBLE);
                        views.setTextViewText(itemSlotIds[i], text);
                        views.setTextColor(itemSlotIds[i], context.getColor(isSelected ? R.color.finan_primary : R.color.finan_text_primary));

                        Intent itemIntent = new Intent(context, QuickTransactionWidgetProvider.class);
                        itemIntent.setAction(ACTION_SELECT_PICKER_ITEM);
                        itemIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        itemIntent.putExtra(EXTRA_PICKER_ITEM_ID, c.getId());
                        itemIntent.putExtra(EXTRA_PICKER_MODE, "CATEGORY");
                        PendingIntent pi = PendingIntent.getBroadcast(
                                context, appWidgetId * 1000 + i, itemIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        views.setOnClickPendingIntent(itemSlotIds[i], pi);
                    } else {
                        views.setViewVisibility(itemSlotIds[i], android.view.View.GONE);
                    }
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        // Normal mode
        views.setViewVisibility(R.id.layout_widget_main_content, android.view.View.VISIBLE);
        views.setViewVisibility(R.id.layout_widget_picker_content, android.view.View.GONE);

        // 1. Display formatted amount
        String amountStr = WidgetStateStore.getAmountStr(context, appWidgetId);
        long amountLong = 0L;
        try {
            amountLong = Long.parseLong(amountStr);
        } catch (NumberFormatException ignored) {
        }
        String formattedAmount = MoneyFormatter.format(amountLong);
        views.setTextViewText(R.id.tv_amount_display, formattedAmount);
        // 2. Display type toggle state via RemoteViews setViewVisibility
        String typeStr = WidgetStateStore.getType(context, appWidgetId);
        boolean isIncome = "INCOME".equalsIgnoreCase(typeStr);
        if (isIncome) {
            views.setViewVisibility(R.id.layout_toggle_expense_active, android.view.View.GONE);
            views.setViewVisibility(R.id.layout_toggle_income_active, android.view.View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.layout_toggle_expense_active, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.layout_toggle_income_active, android.view.View.GONE);
        }

        // 3. Display selected wallet
        long walletId = WidgetStateStore.getWalletId(context, appWidgetId);
        Wallet wallet = null;
        if (walletId > 0L) {
            wallet = services.walletDao.findById(walletId);
        }
        if (wallet == null) {
            wallet = services.walletDao.findDefault();
        }
        if (wallet == null) {
            List<Wallet> wallets = services.walletDao.findAll();
            if (wallets != null && !wallets.isEmpty()) {
                wallet = wallets.get(0);
            }
        }
        if (wallet != null) {
            WidgetStateStore.setWalletId(context, appWidgetId, wallet.getId());
            String iconStr = wallet.getIcon();
            String displayWallet = (iconStr != null && !iconStr.isEmpty() ? iconStr + " " : "💳 ") + wallet.getName();
            views.setTextViewText(R.id.btn_select_wallet, displayWallet);
        } else {
            views.setTextViewText(R.id.btn_select_wallet, "💳 Dompet");
        }

        // 4. Display selected category
        long categoryId = WidgetStateStore.getCategoryId(context, appWidgetId);
        Category category = null;
        if (categoryId > 0L) {
            category = services.categoryDao.findById(categoryId);
        }
        if (category == null || (category.getTypeFilter() != null && !"BOTH".equalsIgnoreCase(category.getTypeFilter()) && !typeStr.equalsIgnoreCase(category.getTypeFilter()))) {
            List<Category> categories = services.categoryDao.findByTypeFilterOrderByUsage(typeStr);
            if (categories != null && !categories.isEmpty()) {
                category = categories.get(0);
            } else {
                category = null;
            }
        }
        if (category != null) {
            WidgetStateStore.setCategoryId(context, appWidgetId, category.getId());
            String catIcon = category.getIcon();
            String displayCat = (catIcon != null && !catIcon.isEmpty() ? catIcon + " " : "🏷️ ") + category.getName();
            views.setTextViewText(R.id.btn_select_category, displayCat);
        } else {
            views.setTextViewText(R.id.btn_select_category, "🏷️ Kategori");
        }

        // Bind PendingIntents with unique requestCodes
        PendingIntent toggleIntent = getPendingIntent(context, appWidgetId, ACTION_TOGGLE_TYPE, null, 0);
        views.setOnClickPendingIntent(R.id.btn_toggle_type, toggleIntent);
        views.setOnClickPendingIntent(R.id.layout_toggle_expense_active, toggleIntent);
        views.setOnClickPendingIntent(R.id.layout_toggle_income_active, toggleIntent);
        views.setOnClickPendingIntent(R.id.tv_toggle_expense_on, toggleIntent);
        views.setOnClickPendingIntent(R.id.tv_toggle_income_off, toggleIntent);
        views.setOnClickPendingIntent(R.id.tv_toggle_expense_off, toggleIntent);
        views.setOnClickPendingIntent(R.id.tv_toggle_income_on, toggleIntent);
        // Check undo state for save button
        boolean isUndoActive = WidgetStateStore.isPendingUndoActive(context, appWidgetId);
        if (isUndoActive) {
            long deadline = WidgetStateStore.getUndoDeadline(context, appWidgetId);
            long remainingMs = deadline - System.currentTimeMillis();
            int remainingSec = (int) Math.ceil(remainingMs / 1000.0);
            if (remainingSec <= 0) {
                WidgetStateStore.clearPendingUndo(context, appWidgetId);
                isUndoActive = false;
            } else {
                views.setViewVisibility(R.id.btn_save_transaction, android.view.View.GONE);
                views.setViewVisibility(R.id.layout_undo_transaction, android.view.View.VISIBLE);
                views.setTextViewText(R.id.tv_widget_undo_text, "Tersimpan");
                views.setTextViewText(R.id.btn_widget_undo_action, "Batal (" + remainingSec + "s)");

                PendingIntent undoIntent = getPendingIntent(context, appWidgetId, ACTION_UNDO_TRANSACTION, null, 99);
                views.setOnClickPendingIntent(R.id.btn_widget_undo_action, undoIntent);
            }
        }

        if (!isUndoActive) {
            views.setViewVisibility(R.id.layout_undo_transaction, android.view.View.GONE);
            views.setViewVisibility(R.id.btn_save_transaction, android.view.View.VISIBLE);
            PendingIntent saveIntent = getPendingIntent(context, appWidgetId, ACTION_SAVE_TRANSACTION, null, 5);
            views.setOnClickPendingIntent(R.id.btn_save_transaction, saveIntent);
        }

        views.setOnClickPendingIntent(R.id.btn_select_wallet, getPendingIntent(context, appWidgetId, ACTION_OPEN_WALLET_PICKER, null, 1));
        views.setOnClickPendingIntent(R.id.btn_select_category, getPendingIntent(context, appWidgetId, ACTION_OPEN_CATEGORY_PICKER, null, 2));
        views.setOnClickPendingIntent(R.id.btn_key_clear, getPendingIntent(context, appWidgetId, ACTION_KEYPAD_CLEAR, null, 3));
        views.setOnClickPendingIntent(R.id.btn_key_del, getPendingIntent(context, appWidgetId, ACTION_KEYPAD_DELETE, null, 4));
        int[] btnIds = {
                R.id.btn_key_0, R.id.btn_key_1, R.id.btn_key_2, R.id.btn_key_3, R.id.btn_key_4,
                R.id.btn_key_5, R.id.btn_key_6, R.id.btn_key_7, R.id.btn_key_8, R.id.btn_key_9,
                R.id.btn_key_000
        };
        String[] digits = {
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "000"
        };
        int[] indices = {
                10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20
        };
        for (int i = 0; i < btnIds.length; i++) {
            views.setOnClickPendingIntent(btnIds[i], getPendingIntent(context, appWidgetId, ACTION_KEYPAD_DIGIT, digits[i], indices[i]));
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent getPendingIntent(Context context, int appWidgetId, String action, String digit, int actionIndex) {
        Intent intent = new Intent(context, QuickTransactionWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        if (digit != null) {
            intent.putExtra(EXTRA_DIGIT, digit);
        }
        int requestCode = appWidgetId * 100 + actionIndex;
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void handleSaveTransaction(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String amountStr = WidgetStateStore.getAmountStr(context, appWidgetId);
        long amount = 0L;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException ignored) {
        }

        if (amount <= 0L) {
            Toast.makeText(context, "Masukkan nominal", Toast.LENGTH_SHORT).show();
            return;
        }

        AppServices services = AppServices.create(context);

        long walletId = WidgetStateStore.getWalletId(context, appWidgetId);
        Wallet wallet = walletId > 0L ? services.walletDao.findById(walletId) : null;
        if (wallet == null) {
            wallet = services.walletDao.findDefault();
        }
        if (wallet == null) {
            List<Wallet> wallets = services.walletDao.findAll();
            if (wallets != null && !wallets.isEmpty()) {
                wallet = wallets.get(0);
            }
        }
        if (wallet == null) {
            Toast.makeText(context, "Tidak ada dompet tersedia", Toast.LENGTH_SHORT).show();
            return;
        }
        walletId = wallet.getId();

        String typeStr = WidgetStateStore.getType(context, appWidgetId);
        TransactionType type = "INCOME".equalsIgnoreCase(typeStr) ? TransactionType.INCOME : TransactionType.EXPENSE;

        long categoryId = WidgetStateStore.getCategoryId(context, appWidgetId);
        Category category = categoryId > 0L ? services.categoryDao.findById(categoryId) : null;
        if (category == null || (category.getTypeFilter() != null && !"BOTH".equalsIgnoreCase(category.getTypeFilter()) && !typeStr.equalsIgnoreCase(category.getTypeFilter()))) {
            List<Category> categories = services.categoryDao.findByTypeFilterOrderByUsage(typeStr);
            if (categories != null && !categories.isEmpty()) {
                category = categories.get(0);
            }
        }
        if (category == null) {
            Toast.makeText(context, "Tidak ada kategori tersedia", Toast.LENGTH_SHORT).show();
            return;
        }
        categoryId = category.getId();

        Transaction tx = new Transaction(0L, amount, type, walletId, categoryId, System.currentTimeMillis(), "Widget Quick Transaction");
        long txId = services.transactionService.save(tx);

        String catName = category.getName();
        String formattedAmount = MoneyFormatter.format(amount);
        Toast.makeText(context, "Tersimpan: " + formattedAmount + " (" + catName + ")", Toast.LENGTH_SHORT).show();
        WidgetStateStore.saveDraftSnapshot(context, appWidgetId, amountStr, typeStr, walletId, categoryId);
        WidgetStateStore.setPendingUndo(context, appWidgetId, txId, System.currentTimeMillis() + 5000L);
        WidgetStateStore.resetAmount(context, appWidgetId);

        updateWidget(context, appWidgetManager, appWidgetId);
        startUndoCountdownHandler(context, appWidgetId);

        Intent broadcastIntent = new Intent("com.dwlhm.finan.ACTION_DATA_CHANGED");
        broadcastIntent.setPackage(context.getPackageName());
        context.sendBroadcast(broadcastIntent);
    }

    private static void handleUndoTransaction(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        long txId = WidgetStateStore.getPendingUndoTxId(context, appWidgetId);
        if (txId > 0L) {
            AppServices services = AppServices.create(context);
            try {
                services.transactionService.delete(txId);
                WidgetStateStore.restoreDraftSnapshot(context, appWidgetId);
                Toast.makeText(context, "Transaksi dibatalkan. Input dikembalikan.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "Gagal membatalkan transaksi", Toast.LENGTH_SHORT).show();
            }
        }
        WidgetStateStore.clearPendingUndo(context, appWidgetId);
        updateWidget(context, appWidgetManager, appWidgetId);

        Intent broadcastIntent = new Intent("com.dwlhm.finan.ACTION_DATA_CHANGED");
        broadcastIntent.setPackage(context.getPackageName());
        context.sendBroadcast(broadcastIntent);
    }

    private static void startUndoCountdownHandler(Context context, int appWidgetId) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (WidgetStateStore.isPendingUndoActive(context, appWidgetId)) {
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    updateWidget(context, appWidgetManager, appWidgetId);
                    long remaining = WidgetStateStore.getUndoDeadline(context, appWidgetId) - System.currentTimeMillis();
                    if (remaining > 0) {
                        handler.postDelayed(this, 1000L);
                    } else {
                        WidgetStateStore.clearPendingUndo(context, appWidgetId);
                        WidgetStateStore.clearDraftSnapshot(context, appWidgetId);
                        updateWidget(context, appWidgetManager, appWidgetId);
                    }
                }
            }
        };
        handler.post(updateRunnable);
    }
}
