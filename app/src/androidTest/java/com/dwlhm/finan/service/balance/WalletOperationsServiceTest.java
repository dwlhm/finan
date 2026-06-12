package com.dwlhm.finan.service.balance;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.dao.SqliteTransactionDao;
import com.dwlhm.finan.data.dao.SqliteWalletBalanceDao;
import com.dwlhm.finan.data.dao.SummaryDao;
import com.dwlhm.finan.data.dao.TransactionDao;
import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.data.dao.TransactionTagDao;
import com.dwlhm.finan.data.dao.TransferDao;
import com.dwlhm.finan.data.dao.WalletBalanceDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.domain.model.HistoryQuery;
import com.dwlhm.finan.domain.model.HistorySearch;
import com.dwlhm.finan.domain.model.HistoryTotals;
import com.dwlhm.finan.domain.model.Transfer;
import com.dwlhm.finan.service.transfer.TransferService;
import com.dwlhm.finan.util.date.TimeProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WalletOperationsServiceTest {

  private FinanDatabaseHelper helper;
  private TransactionGateway transactions;
  private WalletBalanceDao balances;
  private AdjustmentService adjustments;
  private TransferService transfers;
  private SummaryDao summary;
  private long sourceId;
  private long destinationId;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
    helper = new FinanDatabaseHelper(context);
    SQLiteDatabase db = helper.getWritableDatabase();
    WalletDao wallets = new WalletDao(db);
    transactions =
        new SqliteTransactionDao(new TransactionDao(db), new TransactionTagDao(db));
    balances = new SqliteWalletBalanceDao(wallets);
    BalanceService balanceService = new BalanceService(transactions, balances);
    TimeProvider time = () -> 1_700_000_000_000L;
    adjustments = new AdjustmentService(db, transactions, balances, balanceService, time);
    transfers =
        new TransferService(
            db, new TransferDao(db), transactions, wallets, balanceService, time);
    summary = new SummaryDao(db);
    sourceId = wallets.insert("Bank", "IDR", false, 1_000_000L, 1L);
    destinationId = wallets.insert("Tunai", "IDR", false, 100_000L, 1L);
  }

  @After
  public void tearDown() {
    helper.close();
  }

  @Test
  public void adjustmentAndTransfer_preserveLedgerRules() {
    long adjustmentId = adjustments.adjustTo(sourceId, 900_000L, 0L, null);
    assertEquals(900_000L, balances.getCachedBalance(sourceId));

    long transferId = transfers.create(sourceId, destinationId, 200_000L, 0L, null);
    assertEquals(700_000L, balances.getCachedBalance(sourceId));
    assertEquals(300_000L, balances.getCachedBalance(destinationId));
    assertEquals(2, transactions.findByTransferId(transferId).size());
    assertNoIncomeOrExpense();

    transfers.edit(
        new Transfer(
            transferId, destinationId, sourceId, 50_000L, 1_700_000_100_000L, "balik"));
    assertEquals(950_000L, balances.getCachedBalance(sourceId));
    assertEquals(50_000L, balances.getCachedBalance(destinationId));
    assertNoIncomeOrExpense();

    transfers.delete(transferId);
    assertEquals(900_000L, balances.getCachedBalance(sourceId));
    assertEquals(100_000L, balances.getCachedBalance(destinationId));

    adjustments.delete(adjustmentId);
    assertEquals(1_000_000L, balances.getCachedBalance(sourceId));
  }

  private void assertNoIncomeOrExpense() {
    HistoryTotals totals =
        transactions.findHistoryTotals(
            new HistoryQuery(
                null, null, null, null, null, false, HistorySearch.empty()));
    assertEquals(0L, totals.getIncomeMinor());
    assertEquals(0L, totals.getExpenseMinor());
    assertEquals(
        0L,
        summary.sumByTypeBetween(
            "INCOME", Long.MIN_VALUE, Long.MAX_VALUE));
    assertEquals(
        0L,
        summary.sumByTypeBetween(
            "EXPENSE", Long.MIN_VALUE, Long.MAX_VALUE));
  }
}
