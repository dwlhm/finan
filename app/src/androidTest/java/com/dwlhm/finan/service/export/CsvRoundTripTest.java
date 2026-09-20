package com.dwlhm.finan.service.export;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.dwlhm.finan.data.dao.*;
import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.domain.model.*;
import com.dwlhm.finan.service.balance.BalanceService;
import com.dwlhm.finan.service.transfer.TransferService;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CsvRoundTripTest {
  private SQLiteDatabase db;
  private FinanDatabaseHelper helper;
  private WalletDao wallets;
  private CategoryDao categories;
  private TransferDao transfers;
  private TransactionGateway transactions;
  private BalanceService balance;
  private ImportService importer;

  @Before public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    // An isolated in-memory SQLite database, migrated by the production helper.
    db = SQLiteDatabase.create(null);
    helper = new FinanDatabaseHelper(context);
    helper.onConfigure(db);
    helper.onCreate(db);
    wallets = new WalletDao(db);
    categories = new CategoryDao(db);
    transfers = new TransferDao(db);
    transactions = new SqliteTransactionDao(new TransactionDao(db));
    balance = new BalanceService(transactions, new SqliteWalletBalanceDao(wallets));
    importer = new ImportService(db, wallets, categories, transfers, transactions, balance);
  }

  @After public void tearDown() { db.close(); helper.close(); }

  private String export() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new ExportService().exportTo(out, wallets.findAll(), categories.findAllOrdered(),
        transfers.findAll(), transactions);
    return out.toString(StandardCharsets.UTF_8.name());
  }

  private ImportService.ImportResult restore(String csv) throws Exception {
    return importer.importFrom(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
  }

  private void clearLedger() {
    db.delete("transactions", null, null);
    db.delete("transfers", null, null);
    db.delete("categories", null, null);
    db.delete("wallets", null, null);
  }

  @Test public void completeRoundTrip_preservesFieldsLinksAndBalances() throws Exception {
    clearLedger();
    long source = wallets.insert("Savings", "IDR", false, 1000, 1, "S");
    long destination = wallets.insert("Cash", "IDR", false, 200, 1, "C");
    long category = categories.insert("Custom", "I", "BOTH", 17, 0, null, "INVESTING");
    String note = "line one\nTRANSACTIONS, \"quoted\"\rline three";
    Transaction income = new Transaction(0, 300, TransactionType.INCOME, source, category, 10, note);
    income.setCashFlowActivity(CashFlowActivity.INVESTING);
    income.setCashFlowActivityOverridden(true);
    transactions.insert(income);
    transactions.insert(new Transaction(0, 50, TransactionType.EXPENSE, source, category, 11, "food"));
    new TransferService(db, transfers, transactions, wallets, balance, () -> 12L)
        .create(source, destination, 100, 12, note);
    String csv = export();
    clearLedger();
    ImportService.ImportResult result = restore(csv);
    assertTrue(result.getError(), result.isSuccess());
    assertEquals(2, result.getWalletsImported());
    assertEquals(1, result.getCategoriesImported());
    assertEquals(1, result.getTransfersImported());
    assertEquals(4, result.getTransactionsImported());
    long newSource = wallets.findByName("Savings").getId();
    assertNotEquals(source, newSource);
    assertEquals(1000, wallets.findByName("Savings").getOpeningBalanceMinor());
    assertEquals(1150, balance.recalculate(newSource));
    assertEquals(300, balance.recalculate(wallets.findByName("Cash").getId()));
    assertEquals(17, categories.findByNameIgnoreCase("Custom").getSortOrder());
    Transfer transfer = transfers.findAll().get(0);
    assertEquals(2, transactions.findByTransferId(transfer.getId()).size());
    assertEquals(note, transfer.getNote());
    Transaction restored = transactions.findAll().stream()
        .filter(t -> t.getType() == TransactionType.INCOME).findFirst().get();
    assertEquals(note, restored.getNote());
    assertEquals(CashFlowActivity.INVESTING, restored.getCashFlowActivity());
    assertTrue(restored.isCashFlowActivityOverridden());
    assertTrue(restore(csv).isSuccess());
    assertEquals(8, transactions.findAll().size());
    assertEquals(2, transfers.findAll().size());
  }

  @Test public void emptySectionsAndLegacyVersionAreValid() throws Exception {
    clearLedger();
    String csv = export();
    assertTrue(restore(csv).isSuccess());
    assertEquals(0, restore(csv.replace("FINAN_CSV_VERSION,4", "FINAN_CSV_VERSION,3")).getTotalImported());
    assertTrue(wallets.findAll().isEmpty());
  }

  @Test public void invalidRowsHeadersVersionsAndReferencesRollback() throws Exception {
    clearLedger();
    long wallet = wallets.insert("Existing", "IDR", false, 90, 1, "E");
    long category = categories.insert("Category", "C", "BOTH", 1, 0, null, "OPERATING");
    transactions.insert(new Transaction(0, 10, TransactionType.INCOME, wallet, category, 10, "note"));
    String csv = export().replace("Existing", "Imported");
    for (String invalid : new String[] {
        csv.replace("FINAN_CSV_VERSION,4", "FINAN_CSV_VERSION,999"),
        csv.replace("cash_flow_activity_overridden", "wrong_header"),
        csv.replace(",INCOME," + wallet + ",", ",INCOME,999999,"),
        csv.replace(",INCOME,", ",INVALID,"),
        csv.substring(0, csv.indexOf("TRANSACTIONS")),
        csv + "malformed,row\n"
    }) {
      assertFalse(restore(invalid).isSuccess());
      assertNull(wallets.findByName("Imported"));
      assertEquals(1, transactions.findAll().size());
    }
    try {
      restore(csv + "\"unterminated");
      fail("Expected truncated quote rejection");
    } catch (IOException expected) {
      assertNull(wallets.findByName("Imported"));
      assertEquals(1, transactions.findAll().size());
    }
  }

  @Test public void missingOrMismatchedTransferPairsRollback() throws Exception {
    clearLedger();
    long source = wallets.insert("Source", "IDR", false, 1000, 1);
    long destination = wallets.insert("Destination", "IDR", false, 0, 1);
    new TransferService(db, transfers, transactions, wallets, balance, () -> 1L)
        .create(source, destination, 100, 10, "pair");
    String csv = export();
    clearLedger();
    assertFalse(restore(csv.replace(",TRANSFER_IN,", ",TRANSFER_OUT,")).isSuccess());
    assertTrue(wallets.findAll().isEmpty());
    assertTrue(transfers.findAll().isEmpty());
    String missingPair = csv.replaceAll("(?m)^.*TRANSFER_IN.*\\n", "");
    assertFalse(restore(missingPair).isSuccess());
    assertTrue(transfers.findAll().isEmpty());
    String missingReference = csv.replace(",pair,", ",pair,999999");
    assertFalse(restore(missingReference).isSuccess());
    assertTrue(transactions.findAll().isEmpty());
  }
  @Test public void rangeExportOnlyIncludesTransfersWithinRange() throws Exception {
    clearLedger();
    long source = wallets.insert("Source", "IDR", false, 1000, 1);
    long destination = wallets.insert("Destination", "IDR", false, 0, 1);
    TransferService service = new TransferService(db, transfers, transactions, wallets, balance, () -> 1L);
    service.create(source, destination, 100, 10, "outside");
    service.create(source, destination, 200, 20, "inside");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new ExportService().exportTo(out, wallets.findAll(), categories.findAllOrdered(),
        transfers.findAll(), 20L, 30L, transactions);
    String csv = out.toString(StandardCharsets.UTF_8.name());
    assertFalse(csv.contains("outside"));
    assertTrue(csv.contains("inside"));
    clearLedger();
    assertTrue(restore(csv).isSuccess());
    assertEquals(1, transfers.findAll().size());
    assertEquals(2, transactions.findAll().size());
  }
}
