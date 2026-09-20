package com.dwlhm.finan.service.backup;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.dwlhm.finan.domain.model.RecurringFrequency;
import com.dwlhm.finan.domain.model.TransactionType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Complete logical snapshot using the current database schema only (no cross-schema migration).
 * Encrypted files are limited to 32 MiB. The caller owns streams and passwords.
 */
public final class BackupService {
  private static final String[] TABLES = {"wallets", "categories", "transfers", "transactions",
      "transaction_templates", "scheduled_occurrences"};
  private static final String[] PREFS = {"finan_defaults", "finan_prefs"};
  private static final Set<String> DISPLAY_KEYS = new HashSet<>(Arrays.asList(
      "cutoff_day", "summary_percentage_mode", "history_masked_mode", "dashboard_display_mode", "settings_wallet_masked_mode"));
  private static final String JOURNAL = "backup_pending_preferences";
  private static final int MAX_ROWS = 250_000;
  private final Context context;
  private final SQLiteDatabase db;

  public BackupService(Context context, SQLiteDatabase db) {
    this.context = context.getApplicationContext();
    this.db = db;
  }

  /** Writes an authenticated portable snapshot; rejects invalid data or files exceeding 32 MiB. */
  public synchronized void exportTo(OutputStream output, char[] password) throws Exception {
    recoverPendingPreferences();
    JSONObject root = new JSONObject();
    db.beginTransaction();
    try {
      root.put("version", 1).put("schemaVersion", db.getVersion())
          .put("createdAt", System.currentTimeMillis());
      JSONObject tables = new JSONObject();
      for (String table : TABLES) {
        JSONArray rows = new JSONArray();
        try (Cursor cursor = db.query(table, null, null, null, null, null, null)) {
          while (cursor.moveToNext()) {
            if (rows.length() >= MAX_ROWS) throw invalid("Too many rows");
            JSONObject row = new JSONObject();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
              Object value;
              switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_NULL: value = JSONObject.NULL; break;
                case Cursor.FIELD_TYPE_INTEGER: value = cursor.getLong(i); break;
                case Cursor.FIELD_TYPE_STRING: value = cursor.getString(i); break;
                default: throw invalid("Unsupported database value");
              }
              row.put(cursor.getColumnName(i), value);
            }
            rows.put(row);
          }
        }
        tables.put(table, rows);
      }
      root.put("tables", tables);
      JSONObject prefs = new JSONObject();
      for (String name : PREFS) {
        JSONObject entries = new JSONObject();
        for (Map.Entry<String, ?> entry : preferences(name).getAll().entrySet()) {
          if (allowedKey(name, entry.getKey())) entries.put(entry.getKey(), encodePreference(entry.getValue()));
        }
        prefs.put(name, entries);
      }
      root.put("prefs", prefs);
      db.setTransactionSuccessful();
    } finally { db.endTransaction(); }
    byte[] plaintext = root.toString().getBytes(StandardCharsets.UTF_8);
    try {
      validate(root);
      BackupCrypto.encrypt(plaintext, password, output);
      output.flush();
    } finally { Arrays.fill(plaintext, (byte) 0); }
  }

  /** Authenticates and validates a current-schema snapshot without changing database or preferences. */
  public synchronized Preview inspect(InputStream input, char[] password) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int count;
    while ((count = input.read(buffer)) != -1) {
      if (bytes.size() + count > BackupCrypto.MAX_FILE_BYTES) throw new BackupException(BackupException.Reason.LIMIT, "Backup exceeds 32 MiB limit");
      bytes.write(buffer, 0, count);
    }
    byte[] plaintext = BackupCrypto.decrypt(bytes.toByteArray(), password);
    try {
      JSONObject root = new JSONObject(new String(plaintext, StandardCharsets.UTF_8));
      validate(root);
      return new Preview(this, root);
    } catch (BackupException error) {
      throw error;
    } catch (org.json.JSONException | IllegalArgumentException | java.time.DateTimeException error) {
      throw new BackupException(BackupException.Reason.INVALID_BACKUP, "Invalid backup content", error);
    } finally { Arrays.fill(plaintext, (byte) 0); }
  }

  /** Replaces data atomically, then replays preferences; PREFERENCES_PENDING means data committed. */
  public synchronized void restore(Preview preview) throws Exception {
    if (preview == null || preview.owner != this) throw invalid("Invalid backup preview");
    synchronized (preview) {
      JSONObject root = preview.root;
      if (root == null) throw invalid("Backup preview is closed");
      validate(root);
      recoverPendingPreferences();
      db.beginTransaction();
      try {
        db.execSQL("PRAGMA defer_foreign_keys=ON");
        for (int i = TABLES.length - 1; i >= 0; i--) db.delete(TABLES[i], null, null);
        JSONObject tables = root.getJSONObject("tables");
        for (String table : TABLES) {
          Map<String, Column> columns = columns(table);
          JSONArray rows = tables.getJSONArray(table);
          for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            ContentValues values = new ContentValues();
            // Only identifiers obtained from our local schema can enter SQL.
            for (String column : columns.keySet()) {
              Object value = row.get(column);
              if (value == JSONObject.NULL) values.putNull(column);
              else if (value instanceof String) values.put(column, (String) value);
              else values.put(column, ((Number) value).longValue());
            }
            db.insertOrThrow(table, null, values);
          }
        }
        try (Cursor check = db.rawQuery("PRAGMA foreign_key_check", null)) {
          if (check.moveToFirst()) throw invalid("Backup has broken references");
        }
        try (Cursor check = db.rawQuery("PRAGMA integrity_check", null)) {
          if (!check.moveToFirst() || !"ok".equals(check.getString(0))) throw invalid("Database integrity check failed");
        }
        db.execSQL("CREATE TABLE IF NOT EXISTS " + JOURNAL
            + " (name TEXT PRIMARY KEY, payload TEXT NOT NULL)");
        db.delete(JOURNAL, null, null);
        for (String name : PREFS) {
          ContentValues values = new ContentValues();
          values.put("name", name);
          values.put("payload", root.getJSONObject("prefs").getJSONObject(name).toString());
          db.insertOrThrow(JOURNAL, null, values);
        }
        db.setTransactionSuccessful();
      } finally { db.endTransaction(); }
      try { recoverPendingPreferences(); }
      catch (Exception error) {
        throw new BackupException(BackupException.Reason.PREFERENCES_PENDING, "Preferences recovery is pending", error);
      }
    }
  }

  /** Replays the same target state after a crash; retain every journal row until all commits succeed. */
  public synchronized void recoverPendingPreferences() throws Exception {
    try (Cursor exists = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
        new String[] {JOURNAL})) {
      if (!exists.moveToFirst()) return;
    }
    Map<String, JSONObject> pending = new LinkedHashMap<>();
    try (Cursor cursor = db.query(JOURNAL, new String[] {"name", "payload"}, null, null, null, null, null)) {
      while (cursor.moveToNext()) {
        String name = cursor.getString(0);
        if (!Arrays.asList(PREFS).contains(name)) throw invalid("Invalid preference recovery journal");
        JSONObject entries = new JSONObject(cursor.getString(1));
        validatePreferences(name, entries);
        pending.put(name, entries);
      }
    }
    for (Map.Entry<String, JSONObject> entry : pending.entrySet()) {
      SharedPreferences prefs = preferences(entry.getKey());
      SharedPreferences.Editor editor = prefs.edit().clear();
      // Device consent and any future nonportable keys retain their local values.
      for (Map.Entry<String, ?> local : prefs.getAll().entrySet()) {
        if (!allowedKey(entry.getKey(), local.getKey())) putPreference(editor, local.getKey(), encodePreference(local.getValue()));
      }
      Iterator<String> keys = entry.getValue().keys();
      while (keys.hasNext()) {
        String key = keys.next();
        putPreference(editor, key, entry.getValue().getJSONObject(key));
      }
      if (!editor.commit()) throw new IOException("Could not persist restored preferences; recovery will retry");
    }
    if (!pending.isEmpty()) db.delete(JOURNAL, null, null);
  }

  public static final class Preview implements AutoCloseable {
    private final BackupService owner;
    private JSONObject root;
    private final int transactions, wallets, templates;
    private Preview(BackupService owner, JSONObject root) throws Exception {
      this.owner = owner; this.root = root;
      JSONObject tables = root.getJSONObject("tables");
      transactions = tables.getJSONArray("transactions").length();
      wallets = tables.getJSONArray("wallets").length();
      templates = tables.getJSONArray("transaction_templates").length();
    }
    public int getTransactionCount() { return transactions; }
    public int getWalletCount() { return wallets; }
    public int getTemplateCount() { return templates; }
    // Java String values cannot be wiped; release all references. Decrypted byte buffers are wiped at parse.
    @Override public synchronized void close() { root = null; }
  }

  private void validate(JSONObject root) throws Exception {
    exactKeys(root, new HashSet<>(Arrays.asList("version", "schemaVersion", "createdAt", "tables", "prefs")));
    if (integer(root.get("version")) != 1 || integer(root.get("schemaVersion")) != db.getVersion())
      throw new BackupException(BackupException.Reason.INCOMPATIBLE, "Backup schema is incompatible");
    if (integer(root.get("createdAt")) < 0) throw invalid("Invalid backup date");
    JSONObject tables = root.getJSONObject("tables");
    exactKeys(tables, new HashSet<>(Arrays.asList(TABLES)));
    Map<String, Set<Long>> ids = new LinkedHashMap<>();
    for (String table : TABLES) {
      Map<String, Column> columns = columns(table);
      if (columns.isEmpty()) throw invalid("Database schema is incomplete");
      JSONArray rows = tables.getJSONArray(table);
      if (rows.length() > MAX_ROWS) throw invalid("Too many rows");
      Set<Long> seen = new HashSet<>(); ids.put(table, seen);
      for (int i = 0; i < rows.length(); i++) {
        JSONObject row = rows.getJSONObject(i);
        exactKeys(row, columns.keySet());
        for (Map.Entry<String, Column> entry : columns.entrySet()) {
          Object value = row.get(entry.getKey());
          Column column = entry.getValue();
          if (value == JSONObject.NULL) {
            if (column.required) throw invalid("Missing required " + table + "." + entry.getKey());
          } else if ("INTEGER".equals(column.type)) integer(value);
          else if (!"TEXT".equals(column.type) || !(value instanceof String)) throw invalid("Invalid column type");
        }
        if (row.has("id")) {
          long id = integer(row.get("id"));
          if (id <= 0 || !seen.add(id)) throw invalid("Invalid or duplicate ID");
        }
        validateDomain(table, row);
      }
    }
    Set<String> occurrenceKeys = new HashSet<>();
    for (String table : TABLES) {
      JSONArray rows = tables.getJSONArray(table);
      for (int i = 0; i < rows.length(); i++) {
        JSONObject row = rows.getJSONObject(i);
        reference(row, "wallet_id", ids.get("wallets"));
        reference(row, "source_wallet_id", ids.get("wallets"));
        reference(row, "destination_wallet_id", ids.get("wallets"));
        reference(row, "category_id", ids.get("categories"));
        reference(row, "transfer_id", ids.get("transfers"));
        reference(row, "template_id", ids.get("transaction_templates"));
        reference(row, "transaction_id", ids.get("transactions"));
        if ("scheduled_occurrences".equals(table)
            && !occurrenceKeys.add(row.getLong("template_id") + ":" + row.getString("due_date")))
          throw invalid("Duplicate scheduled occurrence");
      }
    }
    validateTransferPairs(tables);
    JSONObject prefs = root.getJSONObject("prefs");
    exactKeys(prefs, new HashSet<>(Arrays.asList(PREFS)));
    for (String name : PREFS) validatePreferences(name, prefs.getJSONObject(name));
  }

  private static void validateDomain(String table, JSONObject row) throws Exception {
    for (String key : new String[] {"is_default", "cash_flow_activity_overridden", "is_scheduled"}) {
      if (row.has(key) && (row.getLong(key) < 0 || row.getLong(key) > 1)) throw invalid("Invalid boolean field");
    }
    if (row.has("amount_minor") && row.getLong("amount_minor") < 0) throw invalid("Negative amount");
    if (row.has("usage_count") && row.getLong("usage_count") < 0) throw invalid("Negative usage count");
    if (row.has("type")) TransactionType.valueOf(row.getString("type"));
    if (row.has("type_filter") && !Arrays.asList("BOTH", "EXPENSE", "INCOME").contains(row.getString("type_filter")))
      throw invalid("Invalid category type");
    if (row.has("cash_flow_activity") && !Arrays.asList("OPERATING", "INVESTING", "FINANCING", "UNCLASSIFIED")
        .contains(row.getString("cash_flow_activity"))) throw invalid("Invalid cash flow activity");
    if ("transfers".equals(table) && (row.getLong("amount_minor") <= 0
        || row.getLong("source_wallet_id") == row.getLong("destination_wallet_id"))) throw invalid("Invalid transfer");
    if ("transactions".equals(table)) {
      if (row.getLong("amount_minor") <= 0) throw invalid("Transaction amount must be positive");
      TransactionType type = TransactionType.valueOf(row.getString("type"));
      if (type.isRegular() && row.isNull("category_id")) throw invalid("Missing transaction category");
      if (type.isTransfer() == row.isNull("transfer_id")) throw invalid("Invalid transfer link");
    }
    if ("transaction_templates".equals(table)) {
      RecurringFrequency frequency = RecurringFrequency.valueOf(row.getString("frequency"));
      long day = row.getLong("due_day"), month = row.getLong("due_month");
      if (day < 0 || day > 31 || month < 0 || month > 12) throw invalid("Invalid schedule date");
      if (row.getLong("is_scheduled") == 1 && (frequency == RecurringFrequency.NONE
          || (frequency == RecurringFrequency.WEEKLY && (day < 1 || day > 7))
          || ((frequency == RecurringFrequency.MONTHLY || frequency == RecurringFrequency.YEARLY) && day < 1)))
        throw invalid("Invalid scheduled template");
    }
    if ("scheduled_occurrences".equals(table)) {
      String date = row.getString("due_date"), status = row.getString("status");
      if (!LocalDate.parse(date).toString().equals(date)
          || !("RECORDED".equals(status) || "SKIPPED".equals(status))
          || ("RECORDED".equals(status) == row.isNull("transaction_id"))) throw invalid("Invalid occurrence");
    }
  }

  private static void validateTransferPairs(JSONObject tables) throws Exception {
    Map<Long, JSONObject> transfers = new LinkedHashMap<>();
    Map<Long, Set<TransactionType>> types = new LinkedHashMap<>();
    JSONArray metadata = tables.getJSONArray("transfers");
    for (int i = 0; i < metadata.length(); i++) {
      JSONObject transfer = metadata.getJSONObject(i);
      transfers.put(transfer.getLong("id"), transfer);
      types.put(transfer.getLong("id"), new HashSet<>());
    }
    JSONArray entries = tables.getJSONArray("transactions");
    for (int i = 0; i < entries.length(); i++) {
      JSONObject entry = entries.getJSONObject(i);
      if (entry.isNull("transfer_id")) continue;
      long id = entry.getLong("transfer_id");
      JSONObject transfer = transfers.get(id);
      TransactionType type = TransactionType.valueOf(entry.getString("type"));
      if (transfer == null || !types.get(id).add(type)
          || entry.getLong("amount_minor") != transfer.getLong("amount_minor")
          || entry.getLong("occurred_at") != transfer.getLong("occurred_at")
          || entry.getLong("wallet_id") != transfer.getLong(type == TransactionType.TRANSFER_OUT
              ? "source_wallet_id" : "destination_wallet_id")) throw invalid("Invalid transfer pair");
    }
    for (Set<TransactionType> pair : types.values()) {
      if (pair.size() != 2 || !pair.contains(TransactionType.TRANSFER_OUT)
          || !pair.contains(TransactionType.TRANSFER_IN)) throw invalid("Incomplete transfer pair");
    }
  }

  private Map<String, Column> columns(String table) {
    Map<String, Column> result = new LinkedHashMap<>();
    try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
      while (cursor.moveToNext()) result.put(cursor.getString(1),
          new Column(cursor.getString(2), cursor.getInt(3) != 0 || cursor.getInt(5) != 0));
    }
    return result;
  }
  private static final class Column {
    final String type; final boolean required;
    Column(String type, boolean required) { this.type = type; this.required = required; }
  }
  private static void reference(JSONObject row, String key, Set<Long> ids) throws Exception {
    if (row.has(key) && !row.isNull(key) && !ids.contains(integer(row.get(key)))) throw invalid("Broken " + key + " reference");
  }
  private static long integer(Object value) throws IOException {
    if (!(value instanceof Integer || value instanceof Long)) throw invalid("Expected integer");
    return ((Number) value).longValue();
  }
  private static void exactKeys(JSONObject object, Set<String> expected) throws IOException {
    Set<String> actual = new HashSet<>();
    Iterator<String> keys = object.keys(); while (keys.hasNext()) actual.add(keys.next());
    if (!actual.equals(expected)) throw invalid("Unexpected backup fields");
  }
  private SharedPreferences preferences(String name) { return context.getSharedPreferences(name, Context.MODE_PRIVATE); }
  private static boolean allowedKey(String name, String key) {
    return "finan_defaults".equals(name) || ("finan_prefs".equals(name) && DISPLAY_KEYS.contains(key));
  }
  private static JSONObject encodePreference(Object value) throws Exception {
    JSONObject result = new JSONObject();
    String type;
    if (value instanceof String) type = "string";
    else if (value instanceof Boolean) type = "boolean";
    else if (value instanceof Integer) type = "int";
    else if (value instanceof Long) type = "long";
    else if (value instanceof Float) type = "float";
    else if (value instanceof Set) {
      type = "strings";
      JSONArray array = new JSONArray();
      for (Object member : (Set<?>) value) { if (!(member instanceof String)) throw invalid("Invalid preference set"); array.put(member); }
      value = array;
    } else throw invalid("Unsupported preference value");
    return result.put("type", type).put("value", value);
  }
  private static void validatePreferences(String name, JSONObject entries) throws Exception {
    Iterator<String> keys = entries.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      if (!allowedKey(name, key)) throw invalid("Nonportable preference");
      JSONObject entry = entries.getJSONObject(key);
      exactKeys(entry, new HashSet<>(Arrays.asList("type", "value")));
      Object value = entry.get("value");
      switch (entry.getString("type")) {
        case "string": if (!(value instanceof String)) throw invalid("Invalid string preference"); break;
        case "boolean": if (!(value instanceof Boolean)) throw invalid("Invalid boolean preference"); break;
        case "int": long number = integer(value); if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw invalid("Invalid int preference"); break;
        case "long": integer(value); break;
        case "float": if (!(value instanceof Number) || !Float.isFinite(((Number) value).floatValue())) throw invalid("Invalid float preference"); break;
        case "strings":
          if (!(value instanceof JSONArray)) throw invalid("Invalid preference set");
          JSONArray array = (JSONArray) value;
          for (int i = 0; i < array.length(); i++) if (!(array.get(i) instanceof String)) throw invalid("Invalid preference set member");
          break;
        default: throw invalid("Unknown preference type");
      }
      String expected = null;
      if ("finan_prefs".equals(name)) expected = key.equals("cutoff_day") || key.equals("dashboard_display_mode") ? "int" : "boolean";
      else if (key.equals("last_wallet_id")) expected = "long";
      else if (key.equals("draft_json") || key.equals("amount_shortcuts") || key.startsWith("edit_draft_")) expected = "string";
      if (expected != null && !expected.equals(entry.getString("type"))) throw invalid("Wrong preference type");
      // PayrollCycleResolver uses -1 for the last business day of the month.
      if (key.equals("cutoff_day") && integer(value) != -1 && (integer(value) < 1 || integer(value) > 31)) throw invalid("Invalid payroll cutoff");
      if (key.equals("dashboard_display_mode") && (integer(value) < 0 || integer(value) > 2)) throw invalid("Invalid display mode");
    }
  }
  private static void putPreference(SharedPreferences.Editor editor, String key, JSONObject entry) throws Exception {
    switch (entry.getString("type")) {
      case "string": editor.putString(key, entry.getString("value")); break;
      case "boolean": editor.putBoolean(key, entry.getBoolean("value")); break;
      case "int": editor.putInt(key, entry.getInt("value")); break;
      case "long": editor.putLong(key, entry.getLong("value")); break;
      case "float": editor.putFloat(key, (float) entry.getDouble("value")); break;
      case "strings":
        Set<String> values = new HashSet<>(); JSONArray array = entry.getJSONArray("value");
        for (int i = 0; i < array.length(); i++) values.add(array.getString(i));
        editor.putStringSet(key, values); break;
      default: throw invalid("Unsupported preference type");
    }
  }
  /** A safe failure category; PREFERENCES_PENDING denotes an already committed database restore. */
  public static final class BackupException extends IOException {
    /** Distinguishes invalid input, compatibility, capacity and post-commit recovery failures. */
    public enum Reason { INVALID_BACKUP, INCOMPATIBLE, LIMIT, PREFERENCES_PENDING }
    private final Reason reason;
    BackupException(Reason reason, String message) { super(message); this.reason = reason; }
    BackupException(Reason reason, String message, Throwable cause) { super(message, cause); this.reason = reason; }
    /** Returns the failure category without exposing any backup content. */
    public Reason getReason() { return reason; }
  }
  private static IOException invalid(String message) {
    return new BackupException(BackupException.Reason.INVALID_BACKUP, message);
  }
}
