package com.dwlhm.finan.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class DefaultsStore {

  private static final String PREFS_NAME = "finan_defaults";

  private static final String KEY_DEFAULT_WALLET_ID = "default_wallet_id";
  private static final String KEY_LAST_WALLET_ID = "last_wallet_id";
  private static final String KEY_DRAFT_JSON = "draft_json";
  private static final String KEY_EDIT_DRAFT_PREFIX = "edit_draft_";
  private static final String KEY_AMOUNT_SHORTCUTS = "amount_shortcuts";

  private final SharedPreferences prefs;

  public DefaultsStore(Context context) {
    this(context, PREFS_NAME);
  }

  DefaultsStore(Context context, String prefsName) {
    prefs = context.getApplicationContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE);
  }

  public boolean hasDefaultWalletId() {
    return prefs.contains(KEY_DEFAULT_WALLET_ID);
  }

  public long getDefaultWalletId() {
    return prefs.getLong(KEY_DEFAULT_WALLET_ID, -1L);
  }

  public void setDefaultWalletId(long walletId) {
    prefs.edit().putLong(KEY_DEFAULT_WALLET_ID, walletId).apply();
  }

  public Long getLastWalletId() {
    if (!prefs.contains(KEY_LAST_WALLET_ID)) {
      return null;
    }
    return prefs.getLong(KEY_LAST_WALLET_ID, -1L);
  }

  public void setLastWalletId(long walletId) {
    prefs.edit().putLong(KEY_LAST_WALLET_ID, walletId).apply();
  }

  public String getDraftJson() {
    return prefs.getString(KEY_DRAFT_JSON, null);
  }

  public void setDraftJson(String draftJson) {
    prefs.edit().putString(KEY_DRAFT_JSON, draftJson).apply();
  }

  public void clearDraft() {
    prefs.edit().remove(KEY_DRAFT_JSON).apply();
  }

  @Nullable
  public TransactionFormDraft getCaptureDraft() {
    return TransactionFormDraft.fromJson(getDraftJson());
  }

  public void setCaptureDraft(@NonNull TransactionFormDraft draft) {
    setDraftJson(draft.toJson());
  }

  public void clearCaptureDraft() {
    clearDraft();
  }

  @Nullable
  public TransactionFormDraft getEditDraft(long transactionId) {
    return TransactionFormDraft.fromJson(
        prefs.getString(editDraftKey(transactionId), null));
  }

  public void setEditDraft(long transactionId, @NonNull TransactionFormDraft draft) {
    draft.setTransactionId(transactionId);
    prefs.edit().putString(editDraftKey(transactionId), draft.toJson()).apply();
  }

  public void clearEditDraft(long transactionId) {
    prefs.edit().remove(editDraftKey(transactionId)).apply();
  }

  private static String editDraftKey(long transactionId) {
    return KEY_EDIT_DRAFT_PREFIX + transactionId;
  }

  public List<Long> getAmountShortcuts() {
    if (!prefs.contains(KEY_AMOUNT_SHORTCUTS)) {
      return defaultAmountShortcuts();
    }

    String serialized = prefs.getString(KEY_AMOUNT_SHORTCUTS, "");
    List<Long> shortcuts = new ArrayList<>();
    if (serialized == null || serialized.trim().isEmpty()) {
      return shortcuts;
    }

    String[] parts = serialized.split(",");
    for (String part : parts) {
      try {
        long amount = Long.parseLong(part.trim());
        if (amount > 0L) {
          shortcuts.add(amount);
        }
      } catch (NumberFormatException ignored) {
        // Skip malformed entries so one bad value does not reset the user's list.
      }
    }
    return shortcuts;
  }

  public void setAmountShortcuts(List<Long> amountShortcuts) {
    StringBuilder serialized = new StringBuilder();
    if (amountShortcuts != null) {
      for (Long amount : amountShortcuts) {
        if (amount == null || amount <= 0L) {
          continue;
        }
        if (serialized.length() > 0) {
          serialized.append(',');
        }
        serialized.append(amount);
      }
    }
    prefs.edit().putString(KEY_AMOUNT_SHORTCUTS, serialized.toString()).apply();
  }

  private List<Long> defaultAmountShortcuts() {
    List<Long> shortcuts = new ArrayList<>();
    shortcuts.add(5_000L);
    shortcuts.add(10_000L);
    shortcuts.add(20_000L);
    shortcuts.add(50_000L);
    shortcuts.add(100_000L);
    return shortcuts;
  }
}
