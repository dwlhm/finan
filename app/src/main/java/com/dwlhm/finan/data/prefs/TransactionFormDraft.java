package com.dwlhm.finan.data.prefs;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;

import org.json.JSONException;
import org.json.JSONObject;

public final class TransactionFormDraft {

  private static final String KEY_AMOUNT_MINOR = "amountMinor";
  private static final String KEY_TYPE = "type";
  private static final String KEY_WALLET_ID = "walletId";
  private static final String KEY_DESTINATION_WALLET_ID = "destinationWalletId";
  private static final String KEY_CATEGORY_ID = "categoryId";
  private static final String KEY_OCCURRED_AT = "occurredAtMillis";
  private static final String KEY_NOTE = "note";
  private static final String KEY_TRANSACTION_ID = "transactionId";

  private long amountMinor;
  @NonNull private TransactionType type = TransactionType.EXPENSE;
  @Nullable private Long walletId;
  @Nullable private Long destinationWalletId;
  @Nullable private Long categoryId;
  private long occurredAtMillis;
  @Nullable private String note;
  @Nullable private Long transactionId;

  public TransactionFormDraft() {}

  public long getAmountMinor() {
    return amountMinor;
  }

  public void setAmountMinor(long amountMinor) {
    this.amountMinor = amountMinor;
  }

  @NonNull
  public TransactionType getType() {
    return type;
  }

  public void setType(@NonNull TransactionType type) {
    this.type = type;
  }

  @Nullable
  public Long getWalletId() {
    return walletId;
  }

  public void setWalletId(@Nullable Long walletId) {
    this.walletId = walletId;
  }

  @Nullable
  public Long getDestinationWalletId() {
    return destinationWalletId;
  }

  public void setDestinationWalletId(@Nullable Long destinationWalletId) {
    this.destinationWalletId = destinationWalletId;
  }

  @Nullable
  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(@Nullable Long categoryId) {
    this.categoryId = categoryId;
  }

  public long getOccurredAtMillis() {
    return occurredAtMillis;
  }

  public void setOccurredAtMillis(long occurredAtMillis) {
    this.occurredAtMillis = occurredAtMillis;
  }

  @Nullable
  public String getNote() {
    return note;
  }

  public void setNote(@Nullable String note) {
    this.note = note;
  }

  @Nullable
  public Long getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(@Nullable Long transactionId) {
    this.transactionId = transactionId;
  }

  public boolean hasContent() {
    return amountMinor > 0L
        || !TextUtils.isEmpty(note);
  }

  public boolean equalsSavedTransaction(@NonNull Transaction transaction) {
    String savedNote = transaction.getNote();
    String draftNote = note == null ? "" : note.trim();
    String normalizedSavedNote = savedNote == null ? "" : savedNote.trim();
    return amountMinor == transaction.getAmountMinor()
        && type == transaction.getType()
        && walletId != null
        && walletId == transaction.getWalletId()
        && categoryId != null
        && categoryId == transaction.getCategoryId()
        && occurredAtMillis == transaction.getOccurredAt()
        && draftNote.equals(normalizedSavedNote);
  }

  @NonNull
  public String toJson() {
    JSONObject object = new JSONObject();
    try {
      if (amountMinor > 0L) {
        object.put(KEY_AMOUNT_MINOR, amountMinor);
      }
      object.put(KEY_TYPE, type.name());
      if (walletId != null && walletId > 0L) {
        object.put(KEY_WALLET_ID, walletId);
      }
      if (destinationWalletId != null && destinationWalletId > 0L) {
        object.put(KEY_DESTINATION_WALLET_ID, destinationWalletId);
      }
      if (categoryId != null && categoryId > 0L) {
        object.put(KEY_CATEGORY_ID, categoryId);
      }
      object.put(KEY_OCCURRED_AT, occurredAtMillis);
      if (!TextUtils.isEmpty(note)) {
        object.put(KEY_NOTE, note);
      }
      if (transactionId != null && transactionId > 0L) {
        object.put(KEY_TRANSACTION_ID, transactionId);
      }
      return object.toString();
    } catch (JSONException e) {
      throw new IllegalStateException("Failed to serialize draft", e);
    }
  }

  @Nullable
  public static TransactionFormDraft fromJson(@Nullable String json) {
    if (json == null || json.trim().isEmpty()) {
      return null;
    }
    try {
      JSONObject object = new JSONObject(json);
      TransactionFormDraft draft = new TransactionFormDraft();
      if (object.has(KEY_AMOUNT_MINOR)) {
        draft.amountMinor = object.getLong(KEY_AMOUNT_MINOR);
      }
      String typeName = object.optString(KEY_TYPE, TransactionType.EXPENSE.name());
      draft.type = TransactionType.valueOf(typeName);
      if (object.has(KEY_WALLET_ID)) {
        draft.walletId = object.getLong(KEY_WALLET_ID);
      }
      if (object.has(KEY_DESTINATION_WALLET_ID)) {
        draft.destinationWalletId = object.getLong(KEY_DESTINATION_WALLET_ID);
      }
      if (object.has(KEY_CATEGORY_ID)) {
        draft.categoryId = object.getLong(KEY_CATEGORY_ID);
      }
      draft.occurredAtMillis = object.optLong(KEY_OCCURRED_AT, System.currentTimeMillis());
      if (object.has(KEY_NOTE) && !object.isNull(KEY_NOTE)) {
        draft.note = object.getString(KEY_NOTE);
      }
      if (object.has(KEY_TRANSACTION_ID)) {
        draft.transactionId = object.getLong(KEY_TRANSACTION_ID);
      }
      return draft.hasContent() ? draft : null;
    } catch (JSONException | IllegalArgumentException ignored) {
      return null;
    }
  }

  @NonNull
  public static TransactionFormDraft fromPendingSave(
      long amountMinor,
      @NonNull TransactionType type,
      long walletId,
      long categoryId,
      long occurredAtMillis,
      @Nullable String note) {
    TransactionFormDraft draft = new TransactionFormDraft();
    draft.amountMinor = amountMinor;
    draft.type = type;
    draft.walletId = walletId;
    draft.categoryId = categoryId;
    draft.occurredAtMillis = occurredAtMillis;
    draft.note = note;
    return draft;
  }
}
