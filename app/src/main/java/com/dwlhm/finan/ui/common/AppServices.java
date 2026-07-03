package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.CategoryGateway;
import com.dwlhm.finan.data.dao.SqliteCategoryDao;
import com.dwlhm.finan.data.dao.SqliteTransactionDao;
import com.dwlhm.finan.data.dao.SqliteWalletBalanceDao;
import com.dwlhm.finan.data.dao.SummaryDao;
import com.dwlhm.finan.data.dao.TransactionDao;
import com.dwlhm.finan.data.dao.TransactionGateway;
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
import com.dwlhm.finan.service.export.ImportService;
import com.dwlhm.finan.service.summary.SummaryService;
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
  public final ImportService importService;
  public final TransactionGateway transactionGateway;
  public final TransferDao transferDao;
  public final CategoryDao categoryDao;
  public final CategoryClassificationService categoryClassificationService;
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
      ImportService importService,
      TransactionGateway transactionGateway,
      CategoryDao categoryDao,
      CategoryClassificationService categoryClassificationService,
      WalletDao walletDao,
      TransferDao transferDao,
      DefaultsStore defaultsStore,
      DbWorker dbWorker) {
    this.databaseHelper = databaseHelper;
    this.transactionService = transactionService;
    this.adjustmentService = adjustmentService;
    this.transferService = transferService;
    this.summaryService = summaryService;
    this.exportService = exportService;
    this.importService = importService;
    this.transactionGateway = transactionGateway;
    this.categoryDao = categoryDao;
    this.categoryClassificationService = categoryClassificationService;
    this.walletDao = walletDao;
    this.transferDao = transferDao;
    this.defaultsStore = defaultsStore;
    this.dbWorker = dbWorker;
  }

  public static AppServices create(Context context) {
    FinanDatabaseHelper databaseHelper = new FinanDatabaseHelper(context);
    SQLiteDatabase db = databaseHelper.getWritableDatabase();

    TransactionDao transactionTable = new TransactionDao(db);
    CategoryDao categoryTable = new CategoryDao(db);
    WalletDao walletTable = new WalletDao(db);
    TransferDao transferTable = new TransferDao(db);
    SummaryDao summaryDao = new SummaryDao(db);

    TransactionGateway transactionGateway =
        new SqliteTransactionDao(transactionTable);
    CategoryGateway categoryGateway = new SqliteCategoryDao(categoryTable);
    WalletBalanceDao walletBalanceDao = new SqliteWalletBalanceDao(walletTable);

    BalanceService balanceService = new BalanceService(transactionGateway, walletBalanceDao);
    CategoryUsageService categoryUsageService = new CategoryUsageService(categoryGateway);
    SystemTimeProvider timeProvider = new SystemTimeProvider();
    TransactionService transactionService =
        new TransactionService(
            db,
            transactionGateway,
            balanceService,
            categoryUsageService,
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

    ImportService importService =
        new ImportService(
            db, walletTable, categoryTable, transferTable,
            transactionGateway, balanceService);

    return new AppServices(
        databaseHelper,
        transactionService,
        adjustmentService,
        transferService,
        summaryService,
        new ExportService(),
        importService,
        transactionGateway,
        categoryTable,
        categoryClassificationService,
        walletTable,
        transferTable,
        new DefaultsStore(context),
        new DbWorker());
  }
}
