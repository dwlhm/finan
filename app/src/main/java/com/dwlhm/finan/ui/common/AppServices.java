package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.CategoryGateway;
import com.dwlhm.finan.data.dao.MerchantDao;
import com.dwlhm.finan.data.dao.MerchantGateway;
import com.dwlhm.finan.data.dao.SqliteCategoryDao;
import com.dwlhm.finan.data.dao.SqliteMerchantDao;
import com.dwlhm.finan.data.dao.SqliteTagDao;
import com.dwlhm.finan.data.dao.SqliteTransactionDao;
import com.dwlhm.finan.data.dao.SqliteWalletBalanceDao;
import com.dwlhm.finan.data.dao.SummaryDao;
import com.dwlhm.finan.data.dao.TagDao;
import com.dwlhm.finan.data.dao.TagGateway;
import com.dwlhm.finan.data.dao.TransactionDao;
import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.data.dao.TransactionTagDao;
import com.dwlhm.finan.data.dao.TransferDao;
import com.dwlhm.finan.data.dao.WalletBalanceDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.prefs.DefaultsStore;
import com.dwlhm.finan.service.balance.BalanceService;
import com.dwlhm.finan.service.balance.AdjustmentService;
import com.dwlhm.finan.service.category.CategoryUsageService;
import com.dwlhm.finan.service.category.CategoryClassificationService;
import com.dwlhm.finan.service.export.ExportService;
import com.dwlhm.finan.service.merchant.MerchantUsageService;
import com.dwlhm.finan.service.summary.SummaryService;
import com.dwlhm.finan.service.tag.TagUsageService;
import com.dwlhm.finan.service.transaction.TransactionService;
import com.dwlhm.finan.service.transfer.TransferService;
import com.dwlhm.finan.util.date.SystemTimeProvider;

import java.time.ZoneId;

public final class AppServices {

  public final FinanDatabaseHelper databaseHelper;
  public final TransactionService transactionService;
  public final AdjustmentService adjustmentService;
  public final TransferService transferService;
  public final SummaryService summaryService;
  public final ExportService exportService;
  public final TransactionGateway transactionGateway;
  public final CategoryDao categoryDao;
  public final CategoryClassificationService categoryClassificationService;
  public final TagDao tagDao;
  public final MerchantDao merchantDao;
  public final WalletDao walletDao;
  public final DefaultsStore defaultsStore;
  public final DbWorker dbWorker;

  private AppServices(
      FinanDatabaseHelper databaseHelper,
      TransactionService transactionService,
      AdjustmentService adjustmentService,
      TransferService transferService,
      SummaryService summaryService,
      ExportService exportService,
      TransactionGateway transactionGateway,
      CategoryDao categoryDao,
      CategoryClassificationService categoryClassificationService,
      TagDao tagDao,
      MerchantDao merchantDao,
      WalletDao walletDao,
      DefaultsStore defaultsStore,
      DbWorker dbWorker) {
    this.databaseHelper = databaseHelper;
    this.transactionService = transactionService;
    this.adjustmentService = adjustmentService;
    this.transferService = transferService;
    this.summaryService = summaryService;
    this.exportService = exportService;
    this.transactionGateway = transactionGateway;
    this.categoryDao = categoryDao;
    this.categoryClassificationService = categoryClassificationService;
    this.tagDao = tagDao;
    this.merchantDao = merchantDao;
    this.walletDao = walletDao;
    this.defaultsStore = defaultsStore;
    this.dbWorker = dbWorker;
  }

  public static AppServices create(Context context) {
    FinanDatabaseHelper databaseHelper = new FinanDatabaseHelper(context);
    SQLiteDatabase db = databaseHelper.getWritableDatabase();

    TransactionDao transactionTable = new TransactionDao(db);
    TransactionTagDao transactionTagTable = new TransactionTagDao(db);
    CategoryDao categoryTable = new CategoryDao(db);
    TagDao tagTable = new TagDao(db);
    MerchantDao merchantTable = new MerchantDao(db);
    WalletDao walletTable = new WalletDao(db);
    TransferDao transferTable = new TransferDao(db);
    SummaryDao summaryDao = new SummaryDao(db);

    TransactionGateway transactionGateway =
        new SqliteTransactionDao(transactionTable, transactionTagTable);
    CategoryGateway categoryGateway = new SqliteCategoryDao(categoryTable);
    TagGateway tagGateway = new SqliteTagDao(tagTable);
    MerchantGateway merchantGateway = new SqliteMerchantDao(merchantTable);
    WalletBalanceDao walletBalanceDao = new SqliteWalletBalanceDao(walletTable);

    BalanceService balanceService = new BalanceService(transactionGateway, walletBalanceDao);
    CategoryUsageService categoryUsageService = new CategoryUsageService(categoryGateway);
    TagUsageService tagUsageService = new TagUsageService(tagGateway);
    MerchantUsageService merchantUsageService = new MerchantUsageService(merchantGateway);
    SystemTimeProvider timeProvider = new SystemTimeProvider();
    TransactionService transactionService =
        new TransactionService(
            db,
            transactionGateway,
            balanceService,
            categoryUsageService,
            tagUsageService,
            merchantUsageService,
            categoryTable,
            timeProvider);
    AdjustmentService adjustmentService =
        new AdjustmentService(
            db, transactionGateway, walletBalanceDao, balanceService, timeProvider);
    TransferService transferService =
        new TransferService(
            db,
            transferTable,
            transactionGateway,
            walletTable,
            balanceService,
            timeProvider);
    SummaryService summaryService =
        new SummaryService(
            summaryDao,
            categoryTable,
            walletTable,
            timeProvider,
            ZoneId.systemDefault());
    CategoryClassificationService categoryClassificationService =
        new CategoryClassificationService(db, categoryTable, transactionTable);

    return new AppServices(
        databaseHelper,
        transactionService,
        adjustmentService,
        transferService,
        summaryService,
        new ExportService(),
        transactionGateway,
        categoryTable,
        categoryClassificationService,
        tagTable,
        merchantTable,
        walletTable,
        new DefaultsStore(context),
        new DbWorker());
  }
}
