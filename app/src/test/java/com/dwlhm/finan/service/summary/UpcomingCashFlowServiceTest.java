package com.dwlhm.finan.service.summary;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.SummaryDao;
import com.dwlhm.finan.data.dao.TransactionTemplateDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.domain.model.ForwardCashFlowSummary;
import com.dwlhm.finan.domain.model.RecurringFrequency;
import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.domain.model.UpcomingObligation;
import com.dwlhm.finan.util.date.TimeProvider;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UpcomingCashFlowServiceTest {

  private FakeTransactionTemplateDao templateDao;
  private FakeWalletDao walletDao;
  private FakeCategoryDao categoryDao;
  private FakeSummaryDao summaryDao;
  private FakeTimeProvider timeProvider;
  private ZoneId zoneId;
  private UpcomingCashFlowService service;

  private final LocalDate startDate = LocalDate.of(2026, 8, 1);
  private final LocalDate endDate = LocalDate.of(2026, 8, 31);

  @Before
  public void setUp() {
    templateDao = new FakeTransactionTemplateDao();
    walletDao = new FakeWalletDao();
    categoryDao = new FakeCategoryDao();
    summaryDao = new FakeSummaryDao();
    zoneId = ZoneId.of("UTC");
    
    // Set fixed time: 2026-08-01 00:00:00 UTC
    long fixedEpochMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
    timeProvider = new FakeTimeProvider(fixedEpochMillis);

    service = new UpcomingCashFlowService(templateDao, walletDao, categoryDao, summaryDao, timeProvider, zoneId);

    // Setup default wallets & categories
    Wallet w1 = new Wallet(1L, "Rekening Utama", "IDR", true, 5_000_000_00L, 5_000_000_00L, fixedEpochMillis, 0, "🏦");
    Wallet w2 = new Wallet(2L, "E-Wallet", "IDR", false, 1_000_000_00L, 1_000_000_00L, fixedEpochMillis, 0, "📱");
    walletDao.addWallet(w1);
    walletDao.addWallet(w2);

    summaryDao.setWalletBalance(1L, 5_000_000_00L); // 5,000,000 IDR (in minor units)
    summaryDao.setWalletBalance(2L, 1_000_000_00L); // 1,000,000 IDR

    Category c1 = new Category(1L, "Gaji", "💰", "INCOME", 1, 0, null, CashFlowActivity.OPERATING.name(), false);
    Category c2 = new Category(2L, "Internet", "🌐", "EXPENSE", 2, 0, null, CashFlowActivity.OPERATING.name(), false);
    Category c3 = new Category(3L, "Listrik", "⚡", "EXPENSE", 3, 0, null, CashFlowActivity.OPERATING.name(), false);
    categoryDao.addCategory(c1);
    categoryDao.addCategory(c2);
    categoryDao.addCategory(c3);
  }

  @Test
  public void testHappyPath_multipleScheduledBillsAndIncomes() {
    // Current total balance = 6_000_000_00L
    // Income: Gaji on 25th -> 10_000_000_00L
    TransactionTemplate tSalary =
        new TransactionTemplate(
            1L, "Gaji Kantor", TransactionType.INCOME, 10_000_000_00L, 1L, 1L, null, "Gaji", "💰", 1,
            RecurringFrequency.MONTHLY, 25, true, 0L);

    // Expense: Internet on 15th -> 350_000_00L
    TransactionTemplate tInternet =
        new TransactionTemplate(
            2L, "WiFi Indihome", TransactionType.EXPENSE, 350_000_00L, 2L, 1L, null, "WiFi", "🌐", 2,
            RecurringFrequency.MONTHLY, 15, true, 0L);

    // Expense: Listrik on 20th -> 500_000_00L
    TransactionTemplate tListrik =
        new TransactionTemplate(
            3L, "Listrik PLN", TransactionType.EXPENSE, 500_000_00L, 3L, 2L, null, "PLN", "⚡", 3,
            RecurringFrequency.MONTHLY, 20, true, 0L);

    templateDao.addScheduledTemplate(tSalary);
    templateDao.addScheduledTemplate(tInternet);
    templateDao.addScheduledTemplate(tListrik);

    ForwardCashFlowSummary summary = service.calculateForwardSummary(startDate, endDate, null);

    assertNotNull(summary);
    assertEquals(6_000_000_00L, summary.getCurrentActualBalanceMinor());
    assertEquals(10_000_000_00L, summary.getScheduledInflowMinor());
    assertEquals(850_000_00L, summary.getScheduledOutflowMinor()); // 350k + 500k

    // remainingAfterPlansMinor = current - outflow = 6_000_000 - 850_000 = 5_150_000
    assertEquals(5_150_000_00L, summary.getRemainingAfterPlansMinor());

    // projectedEndBalanceMinor = current + inflow - outflow = 6M + 10M - 850k = 15_150_000
    assertEquals(15_150_000_00L, summary.getProjectedEndBalanceMinor());

    assertEquals(3, summary.getUpcomingObligations().size());
  }

  @Test
  public void testBoundary_zeroScheduledTemplates() {
    ForwardCashFlowSummary summary = service.calculateForwardSummary(startDate, endDate, null);

    assertNotNull(summary);
    assertEquals(6_000_000_00L, summary.getCurrentActualBalanceMinor());
    assertEquals(0L, summary.getScheduledInflowMinor());
    assertEquals(0L, summary.getScheduledOutflowMinor());
    assertEquals(6_000_000_00L, summary.getRemainingAfterPlansMinor());
    assertEquals(6_000_000_00L, summary.getProjectedEndBalanceMinor());
    assertTrue(summary.getUpcomingObligations().isEmpty());
  }

  @Test
  public void testBoundary_templateAlreadyRecordedInCurrentCycle_isExcluded() {
    long cycleStartMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli();

    // Template 1: Internet on 15th, already recorded on Aug 5th (within Aug 1 - Aug 31)
    long recordedTime = startDate.plusDays(4).atStartOfDay(zoneId).toInstant().toEpochMilli();
    TransactionTemplate tInternet =
        new TransactionTemplate(
            2L, "WiFi Indihome", TransactionType.EXPENSE, 350_000_00L, 2L, 1L, null, "WiFi", "🌐", 2,
            RecurringFrequency.MONTHLY, 15, true, recordedTime);

    // Template 2: Listrik on 20th, not yet recorded in this cycle (recorded in previous month July 20th)
    long previousMonthRecorded = startDate.minusDays(12).atStartOfDay(zoneId).toInstant().toEpochMilli();
    TransactionTemplate tListrik =
        new TransactionTemplate(
            3L, "Listrik PLN", TransactionType.EXPENSE, 500_000_00L, 3L, 2L, null, "PLN", "⚡", 3,
            RecurringFrequency.MONTHLY, 20, true, previousMonthRecorded);

    templateDao.addScheduledTemplate(tInternet);
    templateDao.addScheduledTemplate(tListrik);

    ForwardCashFlowSummary summary = service.calculateForwardSummary(startDate, endDate, null);

    assertNotNull(summary);
    // Internet is excluded because it's already recorded in this cycle
    assertEquals(500_000_00L, summary.getScheduledOutflowMinor());
    assertEquals(1, summary.getUpcomingObligations().size());
    assertEquals("Listrik PLN", summary.getUpcomingObligations().get(0).getName());
  }

  @Test
  public void testSorting_obligationsAreSortedByDueDateAscending() {
    TransactionTemplate tLate =
        new TransactionTemplate(
            1L, "Tagihan Akhir Bulan", TransactionType.EXPENSE, 100_000_00L, 3L, 1L, null, null, "⚡", 1,
            RecurringFrequency.MONTHLY, 28, true, 0L);

    TransactionTemplate tEarly =
        new TransactionTemplate(
            2L, "Tagihan Awal Bulan", TransactionType.EXPENSE, 200_000_00L, 2L, 1L, null, null, "🌐", 2,
            RecurringFrequency.MONTHLY, 5, true, 0L);

    TransactionTemplate tMid =
        new TransactionTemplate(
            3L, "Tagihan Tengah Bulan", TransactionType.EXPENSE, 300_000_00L, 2L, 1L, null, null, "🍱", 3,
            RecurringFrequency.MONTHLY, 15, true, 0L);

    // Add out of order
    templateDao.addScheduledTemplate(tLate);
    templateDao.addScheduledTemplate(tEarly);
    templateDao.addScheduledTemplate(tMid);

    ForwardCashFlowSummary summary = service.calculateForwardSummary(startDate, endDate, null);

    List<UpcomingObligation> obligations = summary.getUpcomingObligations();
    assertEquals(3, obligations.size());
    assertEquals("Tagihan Awal Bulan", obligations.get(0).getName());
    assertEquals(5, obligations.get(0).getDueDay());
    assertEquals("Tagihan Tengah Bulan", obligations.get(1).getName());
    assertEquals(15, obligations.get(1).getDueDay());
    assertEquals("Tagihan Akhir Bulan", obligations.get(2).getName());
    assertEquals(28, obligations.get(2).getDueDay());
  }

  // --- Fake Dao Implementations ---

  private static class FakeTransactionTemplateDao extends TransactionTemplateDao {
    private final List<TransactionTemplate> scheduled = new ArrayList<>();

    public void addScheduledTemplate(TransactionTemplate template) {
      scheduled.add(template);
    }

    @Override
    public List<TransactionTemplate> findScheduledTemplates() {
      return new ArrayList<>(scheduled);
    }
  }

  private static class FakeWalletDao extends WalletDao {
    private final Map<Long, Wallet> wallets = new HashMap<>();

    public void addWallet(Wallet wallet) {
      wallets.put(wallet.getId(), wallet);
    }

    @Override
    public List<Wallet> findAll() {
      return new ArrayList<>(wallets.values());
    }

    @Override
    public Wallet findById(long id) {
      return wallets.get(id);
    }
  }

  private static class FakeCategoryDao extends CategoryDao {
    private final Map<Long, Category> categories = new HashMap<>();

    public void addCategory(Category category) {
      categories.put(category.getId(), category);
    }

    @Override
    public Category findById(long id) {
      return categories.get(id);
    }
  }

  private static class FakeSummaryDao extends SummaryDao {
    private final Map<Long, Long> balances = new HashMap<>();

    public void setWalletBalance(long walletId, long balanceMinor) {
      balances.put(walletId, balanceMinor);
    }

    @Override
    public long walletBalanceBefore(long walletId, long endExclusive) {
      return balances.getOrDefault(walletId, 0L);
    }
  }

  private static class FakeTimeProvider implements TimeProvider {
    private final long currentTimeMillis;

    public FakeTimeProvider(long currentTimeMillis) {
      this.currentTimeMillis = currentTimeMillis;
    }

    @Override
    public long currentTimeMillis() {
      return currentTimeMillis;
    }
  }
}
