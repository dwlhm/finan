package com.dwlhm.finan.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.domain.model.Transfer;

public final class TransferDao {

  private final SQLiteDatabase db;

  public TransferDao(SQLiteDatabase db) {
    this.db = db;
  }

  public long insert(
      long sourceWalletId,
      long destinationWalletId,
      long amountMinor,
      long occurredAt,
      String note,
      long now) {
    ContentValues values = values(sourceWalletId, destinationWalletId, amountMinor, occurredAt, note);
    values.put("created_at", now);
    values.put("updated_at", now);
    return db.insertOrThrow("transfers", null, values);
  }

  public void update(Transfer transfer, long now) {
    ContentValues values =
        values(
            transfer.getSourceWalletId(),
            transfer.getDestinationWalletId(),
            transfer.getAmountMinor(),
            transfer.getOccurredAt(),
            transfer.getNote());
    values.put("updated_at", now);
    int updated =
        db.update(
            "transfers", values, "id = ?", new String[] {String.valueOf(transfer.getId())});
    if (updated <= 0) {
      throw new IllegalArgumentException("Transfer not found");
    }
  }

  public void delete(long transferId) {
    db.delete("transfers", "id = ?", new String[] {String.valueOf(transferId)});
  }

  public Transfer findById(long transferId) {
    try (Cursor c =
        db.query(
            "transfers",
            null,
            "id = ?",
            new String[] {String.valueOf(transferId)},
            null,
            null,
            null)) {
      if (!c.moveToFirst()) {
        return null;
      }
      return map(c);
    }
  }

  private static ContentValues values(
      long sourceWalletId,
      long destinationWalletId,
      long amountMinor,
      long occurredAt,
      String note) {
    ContentValues values = new ContentValues();
    values.put("source_wallet_id", sourceWalletId);
    values.put("destination_wallet_id", destinationWalletId);
    values.put("amount_minor", amountMinor);
    values.put("occurred_at", occurredAt);
    if (note == null) {
      values.putNull("note");
    } else {
      values.put("note", note);
    }
    return values;
  }

  private static Transfer map(Cursor c) {
    return new Transfer(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getLong(c.getColumnIndexOrThrow("source_wallet_id")),
        c.getLong(c.getColumnIndexOrThrow("destination_wallet_id")),
        c.getLong(c.getColumnIndexOrThrow("amount_minor")),
        c.getLong(c.getColumnIndexOrThrow("occurred_at")),
        c.getString(c.getColumnIndexOrThrow("note")));
  }
}
