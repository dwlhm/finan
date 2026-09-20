package com.dwlhm.finan.service.summary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.SummaryDao;
import com.dwlhm.finan.data.dao.TransactionDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.domain.model.CashFlowReport;
import com.dwlhm.finan.domain.model.CashFlowReportResult;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.util.date.TimeProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@RunWith(AndroidJUnit4.class)
public class SummaryAggregationParityAndroidTest {

    private static final long DAY_MILLIS = 86_400_000L;
    private static final long BASE_TIME = 1_700_000_000_000L;

    private FinanDatabaseHelper helper;
    private SummaryDao summaryDao;
    private WalletDao walletDao;
    private CategoryDao categoryDao;
    private TransactionDao transactionDao;
    private SummaryService summaryService;
    private CashFlowReportService cashFlowReportService;
    private ZoneId zoneId = ZoneId.systemDefault();

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(FinanDatabaseHelper.DATABASE_NAME);
        helper = new FinanDatabaseHelper(context);
        summaryDao = new SummaryDao(helper.getWritableDatabase());
        walletDao = new WalletDao(helper.getWritableDatabase());
        categoryDao = new CategoryDao(helper.getWritableDatabase());
        transactionDao = new TransactionDao(helper.getWritableDatabase());
        TimeProvider fixedTime = () -> BASE_TIME;
        summaryService = new SummaryService(
            summaryDao, categoryDao, walletDao, fixedTime, zoneId);
        cashFlowReportService = new CashFlowReportService(
            summaryDao, categoryDao, walletDao, fixedTime, zoneId);
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void walletBalancesAt_matchesPerWalletLoop_allTypes() {
        long walletA = seedWallet("Tunai", 100_000L);
        long walletB = seedWallet("Bank", 50_000L);
        seedWallet("Kosong", 7_000L);
        seedTransactionsAllTypes(walletA, walletB);

        List<SummaryDao.WalletBalanceRow> rows =
            summaryDao.walletBalancesAt(BASE_TIME + 10 * DAY_MILLIS, null);

        long expectedA = summaryDao.walletBalanceBefore(walletA, BASE_TIME + 10 * DAY_MILLIS);
        long expectedB = summaryDao.walletBalanceBefore(walletB, BASE_TIME + 10 * DAY_MILLIS);
        long expectedEmpty = summaryDao.walletBalanceBefore(
            findWalletIdByName("Kosong"), BASE_TIME + 10 * DAY_MILLIS);

        assertEquals(3, rows.size());
        assertEquals(expectedA, rows.get(0).balanceMinor);
        assertEquals(expectedB, rows.get(1).balanceMinor);
        assertEquals(expectedEmpty, rows.get(2).balanceMinor);
    }

    @Test
    public void walletBalancesAt_walletIdFilter_matchesSingleWalletQuery() {
        long walletA = seedWallet("Tunai", 100_000L);
        long walletB = seedWallet("Bank", 50_000L);
        seedTransactionsAllTypes(walletA, walletB);
        long endExclusive = BASE_TIME + 10 * DAY_MILLIS;

        List<SummaryDao.WalletBalanceRow> rows =
            summaryDao.walletBalancesAt(endExclusive, walletA);

        assertEquals(1, rows.size());
        assertEquals(walletA, rows.get(0).walletId);
        assertEquals(
            summaryDao.walletBalanceBefore(walletA, endExclusive), rows.get(0).balanceMinor);
        assertTrue(summaryDao.walletBalanceBefore(walletB, endExclusive) != rows.get(0).balanceMinor);
    }

    @Test
    public void cashFlowTotalsPerWallet_summedMatchesPerWalletLoop() {
        long walletA = seedWallet("Tunai", 100_000L);
        long walletB = seedWallet("Bank", 50_000L);
        seedTransactionsAllTypes(walletA, walletB);
        long startInclusive = BASE_TIME;
        long endExclusive = BASE_TIME + 10 * DAY_MILLIS;

        List<SummaryDao.PerWalletCashFlowTotalsRow> rows =
            summaryDao.cashFlowTotalsPerWallet(startInclusive, endExclusive);

        SummaryDao.CashFlowTotalsRow totalsA =
            summaryDao.cashFlowTotalsBetween(startInclusive, endExclusive, walletA);
        SummaryDao.CashFlowTotalsRow totalsB =
            summaryDao.cashFlowTotalsBetween(startInclusive, endExclusive, walletB);

        assertEquals(2, rows.size());
        assertEquals(totalsA.incomeMinor + totalsB.incomeMinor, sumIncome(rows));
        assertEquals(totalsA.expenseMinor + totalsB.expenseMinor, sumExpense(rows));
        assertEquals(totalsA.transferInMinor + totalsB.transferInMinor, sumTransferIn(rows));
        assertEquals(totalsA.transferOutMinor + totalsB.transferOutMinor, sumTransferOut(rows));
        assertEquals(totalsA.adjIncreaseMinor + totalsB.adjIncreaseMinor, sumAdjIncrease(rows));
        assertEquals(totalsA.adjDecreaseMinor + totalsB.adjDecreaseMinor, sumAdjDecrease(rows));
    }

    @Test
    public void loadRange_matchesOldPerWalletComputation() {
        long walletA = seedWallet("Tunai", 100_000L);
        long walletB = seedWallet("Bank", 50_000L);
        seedWallet("Kosong", 7_000L);
        seedTransactionsAllTypes(walletA, walletB);
        long endExclusive = BASE_TIME + 10 * DAY_MILLIS;

        LocalDate start = LocalDate.now(zoneId).minusDays(2);
        LocalDate end = LocalDate.now(zoneId).plusDays(2);
        MonthlySummary summary = summaryService.loadRange(start, end);

        long expectedA = summaryDao.walletBalanceBefore(walletA, endExclusive);
        long expectedB = summaryDao.walletBalanceBefore(walletB, endExclusive);
        long expectedEmpty =
            summaryDao.walletBalanceBefore(findWalletIdByName("Kosong"), endExclusive);

        List<com.dwlhm.finan.domain.model.WalletBalance> balances = summary.getWalletBalances();
        assertEquals(3, balances.size());
        assertEquals(expectedA, balances.get(0).getBalanceMinor());
        assertEquals(expectedB, balances.get(1).getBalanceMinor());
        assertEquals(expectedEmpty, balances.get(2).getBalanceMinor());
    }

    @Test
    public void loadRange_emptyTransactionsTable() {
        seedWallet("Tunai", 100_000L);
        LocalDate start = LocalDate.now(zoneId).minusDays(2);
        LocalDate end = LocalDate.now(zoneId).plusDays(2);

        MonthlySummary summary = summaryService.loadRange(start, end);

        assertEquals(1, summary.getWalletBalances().size());
        assertEquals(100_000L, summary.getWalletBalances().get(0).getBalanceMinor());
        assertEquals(0L, summary.getMonthIncomeMinor());
        assertEquals(0L, summary.getMonthExpenseMinor());
    }

    @Test
    public void buildReport_combinedPath_matchesOldPerWalletComputation() {
        long walletA = seedWallet("Tunai", 100_000L);
        long walletB = seedWallet("Bank", 50_000L);
        seedTransactionsAllTypes(walletA, walletB);
        long startInclusive = BASE_TIME;
        long endExclusive = BASE_TIME + 10 * DAY_MILLIS;

        LocalDate start = LocalDate.now(zoneId).minusDays(2);
        LocalDate end = LocalDate.now(zoneId).plusDays(2);
        CashFlowReportResult result = cashFlowReportService.buildReport(start, end, 25, null);

        assertEquals(1, result.getAllReports().size());
        CashFlowReport report = result.getAllReports().get(0);

        long expectedOpening = summaryDao.walletBalanceBefore(walletA, startInclusive)
            + summaryDao.walletBalanceBefore(walletB, startInclusive);
        long expectedClosing = summaryDao.walletBalanceBefore(walletA, endExclusive)
            + summaryDao.walletBalanceBefore(walletB, endExclusive);
        SummaryDao.CashFlowTotalsRow totalsA =
            summaryDao.cashFlowTotalsBetween(startInclusive, endExclusive, walletA);
        SummaryDao.CashFlowTotalsRow totalsB =
            summaryDao.cashFlowTotalsBetween(startInclusive, endExclusive, walletB);

        assertEquals(expectedOpening, report.getOpeningBalanceMinor());
        assertEquals(expectedClosing, report.getClosingBalanceMinor());
        assertEquals(totalsA.incomeMinor + totalsB.incomeMinor, report.getIncomeMinor());
        assertEquals(totalsA.expenseMinor + totalsB.expenseMinor, report.getExpenseMinor());
        assertEquals(
            totalsA.transferInMinor + totalsB.transferInMinor, report.getTransferInMinor());
        assertEquals(
            totalsA.transferOutMinor + totalsB.transferOutMinor, report.getTransferOutMinor());
    }

    @Test
    public void buildReport_emptyTransactionsTable() {
        seedWallet("Tunai", 100_000L);
        long startInclusive = BASE_TIME;
        long endExclusive = BASE_TIME + 10 * DAY_MILLIS;

        LocalDate start = LocalDate.now(zoneId).minusDays(2);
        LocalDate end = LocalDate.now(zoneId).plusDays(2);
        CashFlowReportResult result = cashFlowReportService.buildReport(start, end, 25, null);

        assertEquals(1, result.getAllReports().size());
        CashFlowReport report = result.getAllReports().get(0);
        assertEquals(
            summaryDao.walletBalanceBefore(findWalletIdByName("Tunai"), startInclusive),
            report.getOpeningBalanceMinor());
        assertEquals(
            summaryDao.walletBalanceBefore(findWalletIdByName("Tunai"), endExclusive),
            report.getClosingBalanceMinor());
    }

    private long seedWallet(String name, long openingBalanceMinor) {
        return walletDao.insert(name, "IDR", false, openingBalanceMinor, BASE_TIME);
    }

    private void seedTransactionsAllTypes(long walletA, long walletB) {
        long categoryId = Objects.requireNonNull(categoryDao.findByName("Makanan")).getId();
        transactionDao.insert(10_000L, "INCOME", walletA, categoryId,
            BASE_TIME + DAY_MILLIS, null, BASE_TIME, BASE_TIME);
        transactionDao.insert(4_000L, "EXPENSE", walletA, categoryId,
            BASE_TIME + 2 * DAY_MILLIS, null, BASE_TIME, BASE_TIME);
        transactionDao.insert(2_000L, "TRANSFER_IN", walletB, 0L,
            BASE_TIME + 3 * DAY_MILLIS, null, BASE_TIME, BASE_TIME);
        transactionDao.insert(3_000L, "TRANSFER_OUT", walletA, 0L,
            BASE_TIME + 4 * DAY_MILLIS, null, BASE_TIME, BASE_TIME);
        transactionDao.insert(1_500L, "ADJUSTMENT_INCREASE", walletB, 0L,
            BASE_TIME + 5 * DAY_MILLIS, null, BASE_TIME, BASE_TIME);
        transactionDao.insert(500L, "ADJUSTMENT_DECREASE", walletA, 0L,
            BASE_TIME + 6 * DAY_MILLIS, null, BASE_TIME, BASE_TIME);
    }

    private long findWalletIdByName(String name) {
        return Objects.requireNonNull(walletDao.findByName(name)).getId();
    }

    private long sumIncome(List<SummaryDao.PerWalletCashFlowTotalsRow> rows) {
        long sum = 0L;
        for (SummaryDao.PerWalletCashFlowTotalsRow row : rows) sum += row.incomeMinor;
        return sum;
    }

    private long sumExpense(List<SummaryDao.PerWalletCashFlowTotalsRow> rows) {
        long sum = 0L;
        for (SummaryDao.PerWalletCashFlowTotalsRow row : rows) sum += row.expenseMinor;
        return sum;
    }

    private long sumTransferIn(List<SummaryDao.PerWalletCashFlowTotalsRow> rows) {
        long sum = 0L;
        for (SummaryDao.PerWalletCashFlowTotalsRow row : rows) sum += row.transferInMinor;
        return sum;
    }

    private long sumTransferOut(List<SummaryDao.PerWalletCashFlowTotalsRow> rows) {
        long sum = 0L;
        for (SummaryDao.PerWalletCashFlowTotalsRow row : rows) sum += row.transferOutMinor;
        return sum;
    }

    private long sumAdjIncrease(List<SummaryDao.PerWalletCashFlowTotalsRow> rows) {
        long sum = 0L;
        for (SummaryDao.PerWalletCashFlowTotalsRow row : rows) sum += row.adjIncreaseMinor;
        return sum;
    }

    private long sumAdjDecrease(List<SummaryDao.PerWalletCashFlowTotalsRow> rows) {
        long sum = 0L;
        for (SummaryDao.PerWalletCashFlowTotalsRow row : rows) sum += row.adjDecreaseMinor;
        return sum;
    }
}
