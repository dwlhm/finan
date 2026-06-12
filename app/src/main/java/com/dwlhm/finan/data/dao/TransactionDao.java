package com.dwlhm.finan.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.entity.Transaction;
import com.dwlhm.finan.domain.model.HistoryQuery;
import com.dwlhm.finan.domain.model.HistorySearch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class TransactionDao {

  private final SQLiteDatabase db;

  public TransactionDao(SQLiteDatabase db) {
    this.db = db;
  }

  public long insert(
      long amountMinor,
      String type,
      long walletId,
      long categoryId,
      long occurredAt,
      String note,
      Long merchantId,
      long createdAt,
      long updatedAt) {
    return insert(
        amountMinor,
        type,
        walletId,
        categoryId,
        occurredAt,
        note,
        merchantId,
        null,
        createdAt,
        updatedAt);
  }

  public long insert(
      long amountMinor,
      String type,
      long walletId,
      long categoryId,
      long occurredAt,
      String note,
      Long merchantId,
      Long transferId,
      long createdAt,
      long updatedAt) {
    ContentValues values = new ContentValues();
    values.put("amount_minor", amountMinor);
    values.put("type", type);
    values.put("wallet_id", walletId);
    putCategoryId(values, categoryId);
    values.put("occurred_at", occurredAt);
    putNote(values, note);
    putMerchantId(values, merchantId);
    putTransferId(values, transferId);
    values.put("created_at", createdAt);
    values.put("updated_at", updatedAt);
    return db.insert("transactions", null, values);
  }

  public boolean update(
      long id,
      long amountMinor,
      String type,
      long walletId,
      long categoryId,
      long occurredAt,
      String note,
      Long merchantId,
      long createdAt,
      long updatedAt) {
    return update(
        id,
        amountMinor,
        type,
        walletId,
        categoryId,
        occurredAt,
        note,
        merchantId,
        null,
        createdAt,
        updatedAt);
  }

  public boolean update(
      long id,
      long amountMinor,
      String type,
      long walletId,
      long categoryId,
      long occurredAt,
      String note,
      Long merchantId,
      Long transferId,
      long createdAt,
      long updatedAt) {
    ContentValues values = new ContentValues();
    values.put("amount_minor", amountMinor);
    values.put("type", type);
    values.put("wallet_id", walletId);
    putCategoryId(values, categoryId);
    values.put("occurred_at", occurredAt);
    putNote(values, note);
    putMerchantId(values, merchantId);
    putTransferId(values, transferId);
    values.put("created_at", createdAt);
    values.put("updated_at", updatedAt);
    return db.update("transactions", values, "id = ?", new String[]{String.valueOf(id)}) > 0;
  }

  public boolean delete(long id) {
    return db.delete("transactions", "id = ?", new String[]{String.valueOf(id)}) > 0;
  }

  public Transaction findById(long id) {
    try (Cursor c =
        db.query(
            "transactions",
            null,
            "id = ?",
            new String[]{String.valueOf(id)},
            null,
            null,
            null)) {
      if (!c.moveToFirst()) {
        return null;
      }
      return map(c);
    }
  }

  public List<Transaction> findAll() {
    List<Transaction> transactions = new ArrayList<>();
    forEachOrdered(transactions::add);
    return transactions;
  }

  public void forEachOrdered(Consumer<Transaction> consumer) {
    try (Cursor c =
        db.query(
            "transactions",
            null,
            null,
            null,
            null,
            null,
            "occurred_at DESC, id DESC")) {
      while (c.moveToNext()) {
        consumer.accept(map(c));
      }
    }
  }

  public List<Transaction> findRecent(int limit) {
    List<Transaction> transactions = new ArrayList<>();
    try (Cursor c =
        db.query(
            "transactions",
            null,
            null,
            null,
            null,
            null,
            "occurred_at DESC, id DESC",
            String.valueOf(limit))) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  public List<Transaction> findHistory(HistoryQuery query) {
    List<Transaction> transactions = new ArrayList<>();
    List<String> args = new ArrayList<>();
    String selection = historySelection(query, args);
    try (Cursor c =
        db.query(
            "transactions",
            null,
            selection,
            args.isEmpty() ? null : args.toArray(new String[0]),
            null,
            null,
            orderBy(query))) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  public List<Transaction> findHistoryPage(
      HistoryQuery query,
      Long cursorOccurredAt,
      Long cursorId,
      int limit) {
    List<Transaction> transactions = new ArrayList<>();
    List<String> args = new ArrayList<>();
    String selection =
        historyPageSelection(query, cursorOccurredAt, cursorId, args);
    try (Cursor c =
        db.query(
            "transactions",
            null,
            selection,
            args.isEmpty() ? null : args.toArray(new String[0]),
            null,
            null,
            orderBy(query),
            String.valueOf(limit))) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  public HistoryTotalsRow findHistoryTotals(HistoryQuery query) {
    List<String> args = new ArrayList<>();
    String selection = historySelection(query, args);
    String[] selectionArgs = args.isEmpty() ? null : args.toArray(new String[0]);
    int count = 0;
    long incomeMinor = 0L;
    long expenseMinor = 0L;
    try (Cursor c =
        db.rawQuery(
            "SELECT COUNT(*) AS tx_count,"
                + " COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount_minor ELSE 0 END), 0)"
                + " AS income_minor,"
                + " COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount_minor ELSE 0 END), 0)"
                + " AS expense_minor"
                + " FROM transactions"
                + (selection == null ? "" : " WHERE " + selection),
            selectionArgs)) {
      if (c.moveToFirst()) {
        count = c.getInt(c.getColumnIndexOrThrow("tx_count"));
        incomeMinor = c.getLong(c.getColumnIndexOrThrow("income_minor"));
        expenseMinor = c.getLong(c.getColumnIndexOrThrow("expense_minor"));
      }
    }
    return new HistoryTotalsRow(count, incomeMinor, expenseMinor);
  }

  public List<Transaction> findByWalletId(long walletId) {
    List<Transaction> transactions = new ArrayList<>();
    try (Cursor c =
        db.query(
            "transactions",
            null,
            "wallet_id = ?",
            new String[] {String.valueOf(walletId)},
            null,
            null,
            "occurred_at ASC, id ASC",
            null)) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  public List<Transaction> findRecentByWallet(long walletId, int limit) {
    List<Transaction> transactions = new ArrayList<>();
    try (Cursor c =
        db.query(
            "transactions",
            null,
            "wallet_id = ?",
            new String[]{String.valueOf(walletId)},
            null,
            null,
            "occurred_at DESC, id DESC",
            String.valueOf(limit))) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  public List<Transaction> findByTransferId(long transferId) {
    List<Transaction> transactions = new ArrayList<>();
    try (Cursor c =
        db.query(
            "transactions",
            null,
            "transfer_id = ?",
            new String[] {String.valueOf(transferId)},
            null,
            null,
            "id ASC")) {
      while (c.moveToNext()) {
        transactions.add(map(c));
      }
    }
    return transactions;
  }

  private static void putCategoryId(ContentValues values, long categoryId) {
    if (categoryId <= 0L) {
      values.putNull("category_id");
    } else {
      values.put("category_id", categoryId);
    }
  }

  private static void putNote(ContentValues values, String note) {
    if (note == null) {
      values.putNull("note");
    } else {
      values.put("note", note);
    }
  }

  private static void putMerchantId(ContentValues values, Long merchantId) {
    if (merchantId == null) {
      values.putNull("merchant_id");
    } else {
      values.put("merchant_id", merchantId);
    }
  }

  private static void putTransferId(ContentValues values, Long transferId) {
    if (transferId == null) {
      values.putNull("transfer_id");
    } else {
      values.put("transfer_id", transferId);
    }
  }

  private static String historySelection(HistoryQuery query, List<String> args) {
    StringBuilder selection = new StringBuilder();
    appendFilter(selection, args, "wallet_id = ?", query.walletId());
    appendFilter(selection, args, "category_id = ?", query.categoryId());
    appendFilter(selection, args, "type = ?", query.type() == null ? null : query.type().name());
    appendFilter(selection, args, "occurred_at >= ?", query.startInclusiveMillis());
    appendFilter(selection, args, "occurred_at < ?", query.endExclusiveMillis());
    appendSearch(selection, args, query.search());
    return hasContent(selection) ? selection.toString() : null;
  }

  private static void appendFilter(
      StringBuilder selection, List<String> args, String clause, Object value) {
    if (value == null) {
      return;
    }
    appendCondition(selection, clause);
    args.add(String.valueOf(value));
  }

  private static void appendCondition(StringBuilder selection, String clause) {
    if (hasContent(selection)) {
      selection.append(" AND ");
    }
    selection.append(clause);
  }

  private static String historyPageSelection(
      HistoryQuery query,
      Long cursorOccurredAt,
      Long cursorId,
      List<String> args) {
    StringBuilder selection = new StringBuilder();
    String base = historySelection(query, args);
    if (base != null) {
      selection.append(base);
    }
    if (cursorOccurredAt != null && cursorId != null) {
      if (hasContent(selection)) {
        selection.append(" AND ");
      }
      if (query.oldestFirst()) {
        selection.append("(occurred_at > ? OR (occurred_at = ? AND id > ?))");
      } else {
        selection.append("(occurred_at < ? OR (occurred_at = ? AND id < ?))");
      }
      args.add(String.valueOf(cursorOccurredAt));
      args.add(String.valueOf(cursorOccurredAt));
      args.add(String.valueOf(cursorId));
    }
    return hasContent(selection) ? selection.toString() : null;
  }

  private static void appendSearch(
      StringBuilder selection, List<String> args, HistorySearch search) {
    if (search == null || search.isEmpty()) {
      return;
    }
    StringBuilder clauses = new StringBuilder("note LIKE ? ESCAPE '\\' COLLATE NOCASE");
    args.add("%" + escapeLike(search.text()) + "%");
    if (search.amountMinor() != null) {
      appendSearchOr(clauses);
      clauses.append("amount_minor = ?");
      args.add(String.valueOf(search.amountMinor()));
    }
    appendInSearchClause(clauses, "wallet_id", search.walletIds());
    appendInSearchClause(clauses, "category_id", search.categoryIds());
    appendInSearchClause(clauses, "merchant_id", search.merchantIds());
    if (!search.tagIds().isEmpty()) {
      String tagIds = idLiterals(search.tagIds());
      if (tagIds.isEmpty()) {
        appendCondition(selection, "(" + clauses + ")");
        return;
      }
      appendSearchOr(clauses);
      clauses
          .append("EXISTS (SELECT 1 FROM transaction_tags tt WHERE tt.transaction_id = ")
          .append("transactions.id AND tt.tag_id IN (")
          .append(tagIds)
          .append("))");
    }
    appendCondition(selection, "(" + clauses + ")");
  }

  private static void appendInSearchClause(
      StringBuilder clauses, String column, List<Long> ids) {
    if (ids.isEmpty()) {
      return;
    }
    String literals = idLiterals(ids);
    if (literals.isEmpty()) {
      return;
    }
    appendSearchOr(clauses);
    clauses.append(column).append(" IN (").append(literals).append(")");
  }

  private static void appendSearchOr(StringBuilder clauses) {
    if (hasContent(clauses)) {
      clauses.append(" OR ");
    }
  }

  private static String idLiterals(List<Long> ids) {
    StringBuilder values = new StringBuilder();
    for (Long id : ids) {
      if (id == null || id <= 0L) {
        continue;
      }
      if (hasContent(values)) {
        values.append(',');
      }
      values.append(id);
    }
    return values.toString();
  }

  @SuppressWarnings("SizeReplaceableByIsEmpty")
  private static boolean hasContent(StringBuilder value) {
    return value.length() != 0;
  }

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static String orderBy(HistoryQuery query) {
    return query.oldestFirst() ? "occurred_at ASC, id ASC" : "occurred_at DESC, id DESC";
  }

  public static final class HistoryTotalsRow {
    public final int count;
    public final long incomeMinor;
    public final long expenseMinor;

    public HistoryTotalsRow(int count, long incomeMinor, long expenseMinor) {
      this.count = count;
      this.incomeMinor = incomeMinor;
      this.expenseMinor = expenseMinor;
    }
  }

  private static Transaction map(Cursor c) {
    int merchantIndex = c.getColumnIndex("merchant_id");
    Long merchantId = null;
    if (merchantIndex >= 0 && !c.isNull(merchantIndex)) {
      merchantId = c.getLong(merchantIndex);
    }
    int transferIndex = c.getColumnIndex("transfer_id");
    Long transferId = null;
    if (transferIndex >= 0 && !c.isNull(transferIndex)) {
      transferId = c.getLong(transferIndex);
    }
    return new Transaction(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getLong(c.getColumnIndexOrThrow("amount_minor")),
        c.getString(c.getColumnIndexOrThrow("type")),
        c.getLong(c.getColumnIndexOrThrow("wallet_id")),
        c.getLong(c.getColumnIndexOrThrow("category_id")),
        c.getLong(c.getColumnIndexOrThrow("occurred_at")),
        c.getString(c.getColumnIndexOrThrow("note")),
        merchantId,
        transferId,
        c.getLong(c.getColumnIndexOrThrow("created_at")),
        c.getLong(c.getColumnIndexOrThrow("updated_at")));
  }
}
