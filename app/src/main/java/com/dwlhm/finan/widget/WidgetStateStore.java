package com.dwlhm.finan.widget;

import android.content.Context;
import android.content.SharedPreferences;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;

import java.util.List;

public final class WidgetStateStore {

    private static final String PREF_NAME = "finan_widget_state";
    private static final String KEY_AMOUNT_PREFIX = "amount_";
    private static final String KEY_TYPE_PREFIX = "type_";
    private static final String KEY_WALLET_ID_PREFIX = "wallet_id_";
    private static final String KEY_CATEGORY_ID_PREFIX = "category_id_";
    private static final String KEY_UNDO_TX_ID_PREFIX = "undo_tx_id_";
    private static final String KEY_UNDO_DEADLINE_PREFIX = "undo_deadline_";
    private static final String KEY_SNAPSHOT_AMOUNT_PREFIX = "snapshot_amount_";
    private static final String KEY_SNAPSHOT_TYPE_PREFIX = "snapshot_type_";
    private static final String KEY_SNAPSHOT_WALLET_ID_PREFIX = "snapshot_wallet_id_";
    private static final String KEY_SNAPSHOT_CATEGORY_ID_PREFIX = "snapshot_category_id_";
    private static final String KEY_PICKER_MODE_PREFIX = "picker_mode_";
    private static final String KEY_SHORTCUT_UNDO_ID = "key_shortcut_undo_id";
    private static final String KEY_SHORTCUT_UNDO_TX_ID = "key_shortcut_undo_tx_id";
    private static final String KEY_SHORTCUT_UNDO_DEADLINE = "key_shortcut_undo_deadline";
    private static final String KEY_SHORTCUT_CANCELLED_ID = "key_shortcut_cancelled_id";
    private static final String KEY_SHORTCUT_CANCELLED_EXPIRY = "key_shortcut_cancelled_expiry";
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getAmountStr(Context context, int widgetId) {
        return getPrefs(context).getString(KEY_AMOUNT_PREFIX + widgetId, "0");
    }

    public static void setAmountStr(Context context, int widgetId, String val) {
        if (val == null || val.trim().isEmpty()) {
            val = "0";
        }
        getPrefs(context).edit().putString(KEY_AMOUNT_PREFIX + widgetId, val).apply();
    }

    public static void appendDigit(Context context, int widgetId, String digit) {
        if (digit == null || digit.isEmpty()) {
            return;
        }
        String current = getAmountStr(context, widgetId);
        if (current == null || current.isEmpty() || "0".equals(current)) {
            if ("0".equals(digit) || "000".equals(digit)) {
                setAmountStr(context, widgetId, "0");
            } else {
                setAmountStr(context, widgetId, digit);
            }
        } else {
            if (current.length() + digit.length() <= 12) {
                setAmountStr(context, widgetId, current + digit);
            }
        }
    }

    public static void deleteDigit(Context context, int widgetId) {
        String current = getAmountStr(context, widgetId);
        if (current != null && current.length() > 1) {
            setAmountStr(context, widgetId, current.substring(0, current.length() - 1));
        } else {
            setAmountStr(context, widgetId, "0");
        }
    }

    public static void clearAmount(Context context, int widgetId) {
        setAmountStr(context, widgetId, "0");
    }

    public static void resetAmount(Context context, int widgetId) {
        setAmountStr(context, widgetId, "0");
    }

    public static String getType(Context context, int widgetId) {
        return getPrefs(context).getString(KEY_TYPE_PREFIX + widgetId, "EXPENSE");
    }

    public static String toggleType(Context context, int widgetId) {
        String current = getType(context, widgetId);
        String next = "EXPENSE".equalsIgnoreCase(current) ? "INCOME" : "EXPENSE";
        getPrefs(context).edit().putString(KEY_TYPE_PREFIX + widgetId, next).apply();
        return next;
    }

    public static long getWalletId(Context context, int widgetId) {
        return getPrefs(context).getLong(KEY_WALLET_ID_PREFIX + widgetId, 0L);
    }

    public static void setWalletId(Context context, int widgetId, long id) {
        getPrefs(context).edit().putLong(KEY_WALLET_ID_PREFIX + widgetId, id).apply();
    }

    public static long cycleWallet(Context context, int widgetId, WalletDao walletDao) {
        if (walletDao == null) {
            return 0L;
        }
        List<Wallet> wallets = walletDao.findAll();
        if (wallets == null || wallets.isEmpty()) {
            setWalletId(context, widgetId, 0L);
            return 0L;
        }
        long currentId = getWalletId(context, widgetId);
        int currentIndex = -1;
        for (int i = 0; i < wallets.size(); i++) {
            if (wallets.get(i).getId() == currentId) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = (currentIndex + 1) % wallets.size();
        long nextId = wallets.get(nextIndex).getId();
        setWalletId(context, widgetId, nextId);
        return nextId;
    }

    public static long getCategoryId(Context context, int widgetId) {
        return getPrefs(context).getLong(KEY_CATEGORY_ID_PREFIX + widgetId, 0L);
    }

    public static void setCategoryId(Context context, int widgetId, long id) {
        getPrefs(context).edit().putLong(KEY_CATEGORY_ID_PREFIX + widgetId, id).apply();
    }

    public static long cycleCategory(Context context, int widgetId, CategoryDao categoryDao) {
        if (categoryDao == null) {
            return 0L;
        }
        String type = getType(context, widgetId);
        List<Category> categories = categoryDao.findByTypeFilterOrderByUsage(type);
        if (categories == null || categories.isEmpty()) {
            setCategoryId(context, widgetId, 0L);
            return 0L;
        }
        long currentId = getCategoryId(context, widgetId);
        int currentIndex = -1;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == currentId) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = (currentIndex + 1) % categories.size();
        long nextId = categories.get(nextIndex).getId();
        setCategoryId(context, widgetId, nextId);
        return nextId;
    }
    public static void setPendingUndo(Context context, int widgetId, long txId, long deadlineMs) {
        getPrefs(context).edit()
                .putLong(KEY_UNDO_TX_ID_PREFIX + widgetId, txId)
                .putLong(KEY_UNDO_DEADLINE_PREFIX + widgetId, deadlineMs)
                .apply();
    }

    public static long getPendingUndoTxId(Context context, int widgetId) {
        return getPrefs(context).getLong(KEY_UNDO_TX_ID_PREFIX + widgetId, 0L);
    }

    public static long getUndoDeadline(Context context, int widgetId) {
        return getPrefs(context).getLong(KEY_UNDO_DEADLINE_PREFIX + widgetId, 0L);
    }

    public static void clearPendingUndo(Context context, int widgetId) {
        getPrefs(context).edit()
                .remove(KEY_UNDO_TX_ID_PREFIX + widgetId)
                .remove(KEY_UNDO_DEADLINE_PREFIX + widgetId)
                .apply();
    }

    public static boolean isPendingUndoActive(Context context, int widgetId) {
        long deadline = getUndoDeadline(context, widgetId);
        long txId = getPendingUndoTxId(context, widgetId);
        return txId > 0L && System.currentTimeMillis() < deadline;
    }
    public static void saveDraftSnapshot(Context context, int widgetId, String amountStr, String typeStr, long walletId, long categoryId) {
        getPrefs(context).edit()
                .putString(KEY_SNAPSHOT_AMOUNT_PREFIX + widgetId, amountStr)
                .putString(KEY_SNAPSHOT_TYPE_PREFIX + widgetId, typeStr)
                .putLong(KEY_SNAPSHOT_WALLET_ID_PREFIX + widgetId, walletId)
                .putLong(KEY_SNAPSHOT_CATEGORY_ID_PREFIX + widgetId, categoryId)
                .apply();
    }

    public static void restoreDraftSnapshot(Context context, int widgetId) {
        SharedPreferences prefs = getPrefs(context);
        String amountStr = prefs.getString(KEY_SNAPSHOT_AMOUNT_PREFIX + widgetId, null);
        String typeStr = prefs.getString(KEY_SNAPSHOT_TYPE_PREFIX + widgetId, null);
        long walletId = prefs.getLong(KEY_SNAPSHOT_WALLET_ID_PREFIX + widgetId, 0L);
        long categoryId = prefs.getLong(KEY_SNAPSHOT_CATEGORY_ID_PREFIX + widgetId, 0L);

        if (amountStr != null) {
            setAmountStr(context, widgetId, amountStr);
        }
        if (typeStr != null) {
            prefs.edit().putString(KEY_TYPE_PREFIX + widgetId, typeStr).apply();
        }
        if (walletId > 0L) {
            setWalletId(context, widgetId, walletId);
        }
        if (categoryId > 0L) {
            setCategoryId(context, widgetId, categoryId);
        }

        clearDraftSnapshot(context, widgetId);
    }

    public static void clearDraftSnapshot(Context context, int widgetId) {
        getPrefs(context).edit()
                .remove(KEY_SNAPSHOT_AMOUNT_PREFIX + widgetId)
                .remove(KEY_SNAPSHOT_TYPE_PREFIX + widgetId)
                .remove(KEY_SNAPSHOT_WALLET_ID_PREFIX + widgetId)
                .remove(KEY_SNAPSHOT_CATEGORY_ID_PREFIX + widgetId)
                .apply();
    }
    public static void setPickerMode(Context context, int widgetId, String mode) {
        if (mode == null) {
            getPrefs(context).edit().remove(KEY_PICKER_MODE_PREFIX + widgetId).apply();
        } else {
            getPrefs(context).edit().putString(KEY_PICKER_MODE_PREFIX + widgetId, mode).apply();
        }
    }

    public static String getPickerMode(Context context, int widgetId) {
        return getPrefs(context).getString(KEY_PICKER_MODE_PREFIX + widgetId, null);
    }

    public static void clearPickerMode(Context context, int widgetId) {
        setPickerMode(context, widgetId, null);
    }

    public static void setPendingShortcutUndo(Context context, long shortcutId, long txId, long deadlineMs) {
        getPrefs(context).edit()
                .putLong(KEY_SHORTCUT_UNDO_ID, shortcutId)
                .putLong(KEY_SHORTCUT_UNDO_TX_ID, txId)
                .putLong(KEY_SHORTCUT_UNDO_DEADLINE, deadlineMs)
                .apply();
    }

    public static long getPendingShortcutUndoId(Context context) {
        return getPrefs(context).getLong(KEY_SHORTCUT_UNDO_ID, 0L);
    }

    public static long getPendingShortcutUndoTxId(Context context) {
        return getPrefs(context).getLong(KEY_SHORTCUT_UNDO_TX_ID, 0L);
    }

    public static long getPendingShortcutUndoDeadline(Context context) {
        return getPrefs(context).getLong(KEY_SHORTCUT_UNDO_DEADLINE, 0L);
    }

    public static void clearPendingShortcutUndo(Context context) {
        getPrefs(context).edit()
                .remove(KEY_SHORTCUT_UNDO_ID)
                .remove(KEY_SHORTCUT_UNDO_TX_ID)
                .remove(KEY_SHORTCUT_UNDO_DEADLINE)
                .apply();
    }

    public static boolean isPendingShortcutUndoActive(Context context) {
        long deadline = getPendingShortcutUndoDeadline(context);
        long txId = getPendingShortcutUndoTxId(context);
        return txId > 0L && System.currentTimeMillis() < deadline;
    }

    public static void setPendingShortcutCancelled(Context context, long shortcutId, long expiryMs) {
        getPrefs(context).edit()
                .putLong(KEY_SHORTCUT_CANCELLED_ID, shortcutId)
                .putLong(KEY_SHORTCUT_CANCELLED_EXPIRY, expiryMs)
                .apply();
    }

    public static long getPendingShortcutCancelledId(Context context) {
        return getPrefs(context).getLong(KEY_SHORTCUT_CANCELLED_ID, 0L);
    }

    public static void clearPendingShortcutCancelled(Context context) {
        getPrefs(context).edit()
                .remove(KEY_SHORTCUT_CANCELLED_ID)
                .remove(KEY_SHORTCUT_CANCELLED_EXPIRY)
                .apply();
    }

    public static boolean isPendingShortcutCancelledActive(Context context, long shortcutId) {
        long expiry = getPrefs(context).getLong(KEY_SHORTCUT_CANCELLED_EXPIRY, 0L);
        long cancelledId = getPrefs(context).getLong(KEY_SHORTCUT_CANCELLED_ID, 0L);
        return cancelledId == shortcutId && System.currentTimeMillis() < expiry;
    }

}
