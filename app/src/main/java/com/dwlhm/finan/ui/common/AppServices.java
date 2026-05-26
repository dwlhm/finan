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
import com.dwlhm.finan.data.dao.WalletBalanceDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.data.db.FinanDatabaseHelper;
import com.dwlhm.finan.data.prefs.DefaultsStore;
import com.dwlhm.finan.service.balance.BalanceService;
import com.dwlhm.finan.service.category.CategoryUsageService;
import com.dwlhm.finan.service.export.ExportService;
import com.dwlhm.finan.service.summary.SummaryService;
import com.dwlhm.finan.service.transaction.TransactionService;
import com.dwlhm.finan.util.date.SystemTimeProvider;

import java.time.ZoneId;

public final class AppServices {

  public final FinanDatabaseHelper databaseHelper;
  public final TransactionService transactionService;
  public final SummaryService summaryService;
  public final ExportService exportService;
  public final TransactionGateway transactionGateway;
  public final CategoryDao categoryDao;
  public final WalletDao walletDao;
  public final DefaultsStore defaultsStore;

  private AppServices(
      FinanDatabaseHelper databaseHelper,
      TransactionService transactionService,
      SummaryService summaryService,
      ExportService exportService,
      TransactionGateway transactionGateway,
      CategoryDao categoryDao,
      WalletDao walletDao,
      DefaultsStore defaultsStore) {
    this.databaseHelper = databaseHelper;
    this.transactionService = transactionService;
    this.summaryService = summaryService;
    this.exportService = exportService;
    this.transactionGateway = transactionGateway;
    this.categoryDao = categoryDao;
    this.walletDao = walletDao;
    this.defaultsStore = defaultsStore;
  }

  public static AppServices create(Context context) {
    FinanDatabaseHelper databaseHelper = new FinanDatabaseHelper(context);
    SQLiteDatabase db = databaseHelper.getWritableDatabase();

    TransactionDao transactionTable = new TransactionDao(db);
    CategoryDao categoryTable = new CategoryDao(db);
    WalletDao walletTable = new WalletDao(db);
    SummaryDao summaryDao = new SummaryDao(db);

    TransactionGateway transactionGateway = new SqliteTransactionDao(transactionTable);
    CategoryGateway categoryGateway = new SqliteCategoryDao(categoryTable);
    WalletBalanceDao walletBalanceDao = new SqliteWalletBalanceDao(walletTable);

    BalanceService balanceService = new BalanceService(transactionGateway, walletBalanceDao);
    CategoryUsageService categoryUsageService = new CategoryUsageService(categoryGateway);
    SystemTimeProvider timeProvider = new SystemTimeProvider();
    TransactionService transactionService =
        new TransactionService(
            transactionGateway, balanceService, categoryUsageService, timeProvider);
    SummaryService summaryService =
        new SummaryService(
            summaryDao,
            categoryTable,
            walletTable,
            timeProvider,
            ZoneId.systemDefault());

    return new AppServices(
        databaseHelper,
        transactionService,
        summaryService,
        new ExportService(),
        transactionGateway,
        categoryTable,
        walletTable,
        new DefaultsStore(context));
  }
}
