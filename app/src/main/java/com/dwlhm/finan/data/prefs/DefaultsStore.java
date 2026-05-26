package com.dwlhm.finan.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public final class DefaultsStore {

  private static final String PREFS_NAME = "finan_defaults";

  private static final String KEY_DEFAULT_WALLET_ID = "default_wallet_id";
  private static final String KEY_LAST_WALLET_ID = "last_wallet_id";
  private static final String KEY_DRAFT_JSON = "draft_json";

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
}
