package com.dwlhm.finan.service.backup;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BackupServiceTest {
  private SQLiteDatabase db;
  private Context context;
  private BackupService service;
  private static final char[] PASSWORD = "complete backup test password".toCharArray();
  private Map<String, ?> oldDefaults, oldPrefs;

  @Before public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    oldDefaults = prefs("finan_defaults").getAll(); oldPrefs = prefs("finan_prefs").getAll();
    prefs("finan_defaults").edit().clear().commit(); prefs("finan_prefs").edit().clear().commit();
    db = SQLiteDatabase.create(null);
    db.setForeignKeyConstraintsEnabled(true);
    new FinanDatabaseHelper(context).onCreate(db);
    db.setVersion(FinanDatabaseHelper.DATABASE_VERSION);
    service = new BackupService(context, db);
  }
  @After public void tearDown() {
    db.close(); resetPrefs("finan_defaults", oldDefaults); resetPrefs("finan_prefs", oldPrefs);
  }
  @SuppressWarnings("unchecked") private void resetPrefs(String name, Map<String, ?> values) {
    SharedPreferences.Editor e = prefs(name).edit().clear();
    for (Map.Entry<String, ?> entry : values.entrySet()) {
      Object v = entry.getValue(); String k = entry.getKey();
      if (v instanceof String) e.putString(k, (String)v);
      else if (v instanceof Long) e.putLong(k, (Long)v);
      else if (v instanceof Integer) e.putInt(k, (Integer)v);
      else if (v instanceof Boolean) e.putBoolean(k, (Boolean)v);
      else if (v instanceof Float) e.putFloat(k, (Float)v);
      else e.putStringSet(k, (java.util.Set<String>)v);
    }
    assertTrue(e.commit());
  }
  private SharedPreferences prefs(String name) { return context.getSharedPreferences(name, 0); }
  private byte[] export() throws Exception { ByteArrayOutputStream out = new ByteArrayOutputStream(); service.exportTo(out, PASSWORD); return out.toByteArray(); }
  private BackupService.Preview inspect(byte[] bytes) throws Exception { return service.inspect(new ByteArrayInputStream(bytes), PASSWORD); }
  private long scalar(String sql) { try (Cursor c = db.rawQuery(sql, null)) { assertTrue(c.moveToFirst()); return c.getLong(0); } }
  private void seed() {
    db.execSQL("UPDATE wallets SET opening_balance_minor=123, cached_balance_minor=999 WHERE id=1");
    db.execSQL("INSERT INTO transactions(id,amount_minor,type,wallet_id,category_id,occurred_at,created_at,updated_at) VALUES(91,200,'EXPENSE',1,1,1000,1000,1000)");
    db.execSQL("UPDATE transaction_templates SET wallet_id=1,category_id=1,frequency='YEARLY',due_day=12,due_month=9,is_scheduled=1 WHERE id=1");
    db.execSQL("INSERT INTO scheduled_occurrences VALUES(1,'2026-09-12','RECORDED',91)");
    db.execSQL("INSERT INTO scheduled_occurrences VALUES(1,'2025-09-12','SKIPPED',NULL)");
    prefs("finan_defaults").edit().putLong("last_wallet_id",1).putString("edit_draft_91","{draft}").commit();
    prefs("finan_prefs").edit().putInt("cutoff_day",25).putBoolean("summary_percentage_mode",true).putBoolean("reminder_consent",true).commit();
  }
  @Test public void fullReplacePreservesIdsLinksSchedulesBalancesAndPortablePreferences() throws Exception {
    seed(); byte[] bytes = export();
    db.execSQL("DELETE FROM scheduled_occurrences"); db.execSQL("DELETE FROM transactions");
    db.execSQL("UPDATE wallets SET cached_balance_minor=0");
    prefs("finan_defaults").edit().clear().commit();
    prefs("finan_prefs").edit().putInt("cutoff_day",1).putBoolean("reminder_consent",false).commit();
    try (BackupService.Preview preview = inspect(bytes)) {
      assertEquals(1, preview.getTransactionCount()); assertEquals(1, preview.getWalletCount());
      assertEquals(4, preview.getTemplateCount()); service.restore(preview);
    }
    assertEquals(91, scalar("SELECT id FROM transactions"));
    assertEquals(999, scalar("SELECT cached_balance_minor FROM wallets WHERE id=1"));
    assertEquals(123, scalar("SELECT opening_balance_minor FROM wallets WHERE id=1"));
    assertEquals(9, scalar("SELECT due_month FROM transaction_templates WHERE id=1"));
    assertEquals(2, scalar("SELECT count(*) FROM scheduled_occurrences"));
    assertEquals(91, scalar("SELECT transaction_id FROM scheduled_occurrences WHERE status='RECORDED'"));
    assertEquals("{draft}", prefs("finan_defaults").getString("edit_draft_91",null));
    assertEquals(25, prefs("finan_prefs").getInt("cutoff_day",0));
    assertFalse(prefs("finan_prefs").getBoolean("reminder_consent",true));
    assertEquals(0, scalar("SELECT count(*) FROM backup_pending_preferences"));
  }
  @Test public void cancelTamperAndInvalidReferencesNeverMutate() throws Exception {
    seed(); byte[] bytes = export();
    BackupService.Preview preview = inspect(bytes); preview.close();
    assertThrows(Exception.class, () -> service.restore(preview));
    bytes[bytes.length-1] ^= 1;
    assertThrows(Exception.class, () -> inspect(bytes));
    JSONObject root = new JSONObject(new String(BackupCrypto.decrypt(export(), PASSWORD),StandardCharsets.UTF_8));
    root.getJSONObject("tables").getJSONArray("transactions").getJSONObject(0).put("wallet_id",999);
    byte[] invalid = BackupCrypto.encrypt(root.toString().getBytes(StandardCharsets.UTF_8), PASSWORD);
    assertThrows(Exception.class, () -> inspect(invalid));
    assertEquals(91, scalar("SELECT id FROM transactions"));
  }
  @Test public void insertionFailureRollsBackAndPendingPreferencesReplay() throws Exception {
    seed(); byte[] bytes = export();
    db.execSQL("UPDATE wallets SET cached_balance_minor=555");
    db.execSQL("CREATE TRIGGER reject_restore BEFORE INSERT ON transactions BEGIN SELECT RAISE(ABORT,'test failure'); END");
    try (BackupService.Preview preview = inspect(bytes)) { assertThrows(Exception.class, () -> service.restore(preview)); }
    assertEquals(555, scalar("SELECT cached_balance_minor FROM wallets WHERE id=1"));
    assertEquals(91, scalar("SELECT id FROM transactions"));
    db.execSQL("CREATE TABLE backup_pending_preferences(name TEXT PRIMARY KEY,payload TEXT NOT NULL)");
    db.execSQL("INSERT INTO backup_pending_preferences VALUES(?,?)", new Object[]{"finan_prefs", "{\"cutoff_day\":{\"type\":\"int\",\"value\":17}}"});
    service.recoverPendingPreferences();
    assertEquals(17,prefs("finan_prefs").getInt("cutoff_day",0));
    assertEquals(0, scalar("SELECT count(*) FROM backup_pending_preferences"));
    service.recoverPendingPreferences();
  }

  @Test public void lastBusinessDayCutoffRoundTripsAndInvalidCutoffIsRejected() throws Exception {
    prefs("finan_prefs").edit().putInt("cutoff_day", -1).commit();
    byte[] bytes = export();
    prefs("finan_prefs").edit().putInt("cutoff_day", 8).commit();
    try (BackupService.Preview preview = inspect(bytes)) { service.restore(preview); }
    assertEquals(-1, prefs("finan_prefs").getInt("cutoff_day", 0));
    JSONObject root = new JSONObject(new String(BackupCrypto.decrypt(bytes, PASSWORD), StandardCharsets.UTF_8));
    root.getJSONObject("prefs").getJSONObject("finan_prefs").getJSONObject("cutoff_day").put("value", 0);
    byte[] invalid = BackupCrypto.encrypt(root.toString().getBytes(StandardCharsets.UTF_8), PASSWORD);
    BackupService.BackupException failure = assertThrows(BackupService.BackupException.class, () -> inspect(invalid));
    assertEquals(BackupService.BackupException.Reason.INVALID_BACKUP, failure.getReason());
    assertEquals(-1, prefs("finan_prefs").getInt("cutoff_day", 0));
  }

  @Test public void emptySnapshotWrongPasswordAndSchemaBoundary() throws Exception {
    db.execSQL("DELETE FROM transaction_templates");
    db.execSQL("DELETE FROM categories");
    db.execSQL("DELETE FROM wallets");
    byte[] bytes = export();
    try (BackupService.Preview preview = inspect(bytes)) {
      assertEquals(0, preview.getWalletCount());
      assertEquals(0, preview.getTransactionCount());
      service.restore(preview);
    }
    assertThrows(java.security.GeneralSecurityException.class,
        () -> service.inspect(new ByteArrayInputStream(bytes), "unrelated password".toCharArray()));
    JSONObject root = new JSONObject(new String(BackupCrypto.decrypt(bytes, PASSWORD), StandardCharsets.UTF_8));
    root.put("schemaVersion", db.getVersion() + 1);
    byte[] incompatible = BackupCrypto.encrypt(root.toString().getBytes(StandardCharsets.UTF_8), PASSWORD);
    BackupService.BackupException failure = assertThrows(BackupService.BackupException.class, () -> inspect(incompatible));
    assertEquals(BackupService.BackupException.Reason.INCOMPATIBLE, failure.getReason());
    assertEquals(0, scalar("SELECT count(*) FROM wallets"));
  }

  @Test public void transferPairRoundTripsAndMismatchedAmountIsRejected() throws Exception {
    db.execSQL("INSERT INTO wallets(id,name,is_default,created_at) VALUES(2,'Reserve',0,2000)");
    db.execSQL("INSERT INTO transfers(id,source_wallet_id,destination_wallet_id,amount_minor,occurred_at,created_at,updated_at) VALUES(7,1,2,350,2000,2000,2000)");
    db.execSQL("INSERT INTO transactions(id,amount_minor,type,wallet_id,transfer_id,occurred_at,created_at,updated_at) VALUES(81,350,'TRANSFER_OUT',1,7,2000,2000,2000)");
    db.execSQL("INSERT INTO transactions(id,amount_minor,type,wallet_id,transfer_id,occurred_at,created_at,updated_at) VALUES(82,350,'TRANSFER_IN',2,7,2000,2000,2000)");
    byte[] bytes = export();
    db.execSQL("DELETE FROM transactions"); db.execSQL("DELETE FROM transfers");
    try (BackupService.Preview preview = inspect(bytes)) { service.restore(preview); }
    assertEquals(2, scalar("SELECT count(*) FROM transactions WHERE transfer_id=7"));
    JSONObject root = new JSONObject(new String(BackupCrypto.decrypt(bytes, PASSWORD), StandardCharsets.UTF_8));
    root.getJSONObject("tables").getJSONArray("transactions").getJSONObject(0).put("amount_minor", 351);
    byte[] invalid = BackupCrypto.encrypt(root.toString().getBytes(StandardCharsets.UTF_8), PASSWORD);
    assertThrows(BackupService.BackupException.class, () -> inspect(invalid));
    assertEquals(350, scalar("SELECT amount_minor FROM transactions WHERE id=81"));
  }

  @Test public void preferenceCommitFailureRetainsJournalAndReportsCommittedDatabase() throws Exception {
    seed();
    byte[] bytes = export();
    db.execSQL("UPDATE wallets SET cached_balance_minor=444");
    Context failing = new android.content.ContextWrapper(context) {
      @Override public Context getApplicationContext() { return this; }
      @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        SharedPreferences actual = super.getSharedPreferences(name, mode);
        if (!"finan_prefs".equals(name)) return actual;
        return (SharedPreferences) java.lang.reflect.Proxy.newProxyInstance(
            SharedPreferences.class.getClassLoader(), new Class<?>[] {SharedPreferences.class},
            (proxy, method, args) -> {
              if (!"edit".equals(method.getName())) return method.invoke(actual, args);
              SharedPreferences.Editor editor = actual.edit();
              return java.lang.reflect.Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),
                  new Class<?>[] {SharedPreferences.Editor.class}, (editorProxy, editorMethod, editorArgs) -> {
                    if ("commit".equals(editorMethod.getName())) return false;
                    Object result = editorMethod.invoke(editor, editorArgs);
                    return result instanceof SharedPreferences.Editor ? editorProxy : result;
                  });
            });
      }
    };
    BackupService failingService = new BackupService(failing, db);
    try (BackupService.Preview preview = failingService.inspect(new ByteArrayInputStream(bytes), PASSWORD)) {
      BackupService.BackupException failure = assertThrows(BackupService.BackupException.class,
          () -> failingService.restore(preview));
      assertEquals(BackupService.BackupException.Reason.PREFERENCES_PENDING, failure.getReason());
    }
    assertEquals(999, scalar("SELECT cached_balance_minor FROM wallets WHERE id=1"));
    assertEquals(2, scalar("SELECT count(*) FROM backup_pending_preferences"));
    assertThrows(java.io.IOException.class, failingService::recoverPendingPreferences);
    assertEquals(2, scalar("SELECT count(*) FROM backup_pending_preferences"));
    service.recoverPendingPreferences();
    assertEquals(0, scalar("SELECT count(*) FROM backup_pending_preferences"));
    assertEquals(25, prefs("finan_prefs").getInt("cutoff_day", 0));
  }
}
