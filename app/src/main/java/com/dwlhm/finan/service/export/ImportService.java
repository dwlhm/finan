package com.dwlhm.finan.service.export;

import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.data.dao.TransferDao;
import com.dwlhm.finan.data.dao.WalletDao;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.balance.BalanceService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportService {

  private static final String VERSION_PREFIX = "FINAN_CSV_VERSION,";
  private static final String WALLET_SECTION = "WALLETS";
  private static final String CATEGORY_SECTION = "CATEGORIES";
  private static final String TRANSFER_SECTION = "TRANSFERS";
  private static final String TRANSACTION_SECTION = "TRANSACTIONS";
  private static final Set<String> SECTION_NAMES =
      new HashSet<>(
          Arrays.asList(
              WALLET_SECTION,
              CATEGORY_SECTION,
              TRANSFER_SECTION,
              TRANSACTION_SECTION));

  private final SQLiteDatabase db;
  private final WalletDao walletDao;
  private final CategoryDao categoryDao;
  private final TransferDao transferDao;
  private final TransactionGateway transactionGateway;
  private final BalanceService balanceService;

  public ImportService(
      SQLiteDatabase db,
      WalletDao walletDao,
      CategoryDao categoryDao,
      TransferDao transferDao,
      TransactionGateway transactionGateway,
      BalanceService balanceService) {
    this.db = db;
    this.walletDao = walletDao;
    this.categoryDao = categoryDao;
    this.transferDao = transferDao;
    this.transactionGateway = transactionGateway;
    this.balanceService = balanceService;
  }

  public static final class ImportResult {
    private int walletsImported;
    private int categoriesImported;
    private int transfersImported;
    private int transactionsImported;
    private String error;

    private ImportResult() {}

    public int getWalletsImported() {
      return walletsImported;
    }

    public int getCategoriesImported() {
      return categoriesImported;
    }

    public int getTransfersImported() {
      return transfersImported;
    }

    public int getTransactionsImported() {
      return transactionsImported;
    }

    public String getError() {
      return error;
    }

    public boolean isSuccess() {
      return error == null;
    }

    public int getTotalImported() {
      return walletsImported
          + categoriesImported
          + transfersImported
          + transactionsImported;
    }

    public static ImportResult success(
        int wallets, int categories, int transfers, int transactions) {
      ImportResult r = new ImportResult();
      r.walletsImported = wallets;
      r.categoriesImported = categories;
      r.transfersImported = transfers;
      r.transactionsImported = transactions;
      return r;
    }

    public static ImportResult failure(String error) {
      ImportResult r = new ImportResult();
      r.error = error;
      return r;
    }
  }

  public ImportResult importFrom(InputStream in) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String firstLine = reader.readLine();
      if (firstLine == null || !firstLine.startsWith(VERSION_PREFIX)) {
        return ImportResult.failure("Invalid file format: missing version header");
      }
      int version = parseVersion(firstLine);
      if (version < 3) {
        return ImportResult.failure(
            "Unsupported CSV version " + version + ". Minimum supported version is 3.");
      }

      db.beginTransaction();
      try {
        Map<Long, Long> walletIdMap = new HashMap<>();
        Map<Long, Long> categoryIdMap = new HashMap<>();

        int wallets = 0;
        int categories = 0;
        int transfers = 0;
        int transactions = 0;

        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty()) {
            continue;
          }
          String section = parseSectionHeader(line);
          if (section == null) {
            continue;
          }
          String headerLine = reader.readLine();
          if (headerLine == null || headerLine.trim().isEmpty()) {
            continue;
          }
          List<String> headers = parseCsvLine(headerLine.trim());
          switch (section) {
            case WALLET_SECTION:
              wallets += parseWallets(reader, headers, walletIdMap);
              break;
            case CATEGORY_SECTION:
              categories += parseCategories(reader, headers, categoryIdMap);
              break;
            case TRANSFER_SECTION:
              transfers +=
                  parseTransfers(reader, headers, walletIdMap);
              break;
            case TRANSACTION_SECTION:
              transactions +=
                  parseTransactions(reader, headers, walletIdMap, categoryIdMap);
              break;
          }
        }

        recalculateAllWalletBalances();

        db.setTransactionSuccessful();
        return ImportResult.success(
            wallets, categories, transfers, transactions);
      } finally {
        db.endTransaction();
      }
    }
  }

  private int parseVersion(String headerLine) {
    try {
      return Integer.parseInt(headerLine.substring(VERSION_PREFIX.length()).trim());
    } catch (NumberFormatException | IndexOutOfBoundsException e) {
      return -1;
    }
  }

  private String parseSectionHeader(String line) {
    String trimmed = line.trim();
    if (SECTION_NAMES.contains(trimmed)) {
      return trimmed;
    }
    return null;
  }

  private int parseWallets(
      BufferedReader reader, List<String> headers, Map<Long, Long> idMap) throws IOException {
    int walletIconIndex = headers.indexOf("icon");
    int count = 0;
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.isEmpty()) continue;
      if (SECTION_NAMES.contains(line)) break;
      List<String> fields = parseCsvLine(line);
      if (fields.size() < 5) {
        continue;
      }
      long oldId = Long.parseLong(fields.get(0));
      String name = fields.get(1);
      String currencyCode = fields.get(2);
      boolean isDefault = "1".equals(fields.get(3));
      long openingBalanceMinor = Long.parseLong(fields.get(4));
      String icon = walletIconIndex >= 0 && walletIconIndex < fields.size()
          ? fields.get(walletIconIndex) : null;
      if (icon != null && icon.isEmpty()) {
        icon = null;
      }

      com.dwlhm.finan.data.entity.Wallet existing = walletDao.findByName(name);
      long newId;
      if (existing != null) {
        newId = existing.getId();
      } else {
        long now = System.currentTimeMillis();
        newId =
            walletDao.insert(name, currencyCode, isDefault, openingBalanceMinor, now, icon);
        count++;
      }
      idMap.put(oldId, newId);
    }
    return count;
  }

  private int parseCategories(
      BufferedReader reader, List<String> headers, Map<Long, Long> idMap) throws IOException {
    int iconIndex = headers.indexOf("icon");
    int typeFilterIndex = headers.indexOf("type_filter");
    int sortOrderIndex = headers.indexOf("sort_order");
    int cashFlowIndex = headers.indexOf("cash_flow_activity");
    int count = 0;
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.isEmpty()) continue;
      if (SECTION_NAMES.contains(line)) break;
      List<String> fields = parseCsvLine(line);
      if (fields.size() < 2) continue;
      long oldId = Long.parseLong(fields.get(0));
      String name = fields.get(1);
      String icon = iconIndex >= 0 && iconIndex < fields.size() ? fields.get(iconIndex) : null;
      if (icon != null && icon.isEmpty()) {
        icon = null;
      }
      String typeFilter =
          typeFilterIndex >= 0 && typeFilterIndex < fields.size() ? fields.get(typeFilterIndex)
              : "BOTH";
      int sortOrder =
          sortOrderIndex >= 0 && sortOrderIndex < fields.size()
              ? Integer.parseInt(fields.get(sortOrderIndex))
              : 0;
      String cashFlowActivity =
          cashFlowIndex >= 0 && cashFlowIndex < fields.size() ? fields.get(cashFlowIndex)
              : CashFlowActivity.UNCLASSIFIED.name();

      com.dwlhm.finan.data.entity.Category existing = categoryDao.findByNameIgnoreCase(name);
      long newId;
      if (existing != null) {
        newId = existing.getId();
      } else {
        newId =
            categoryDao.insert(name, icon, typeFilter, sortOrder, 0, null, cashFlowActivity);
        count++;
      }
      idMap.put(oldId, newId);
    }
    return count;
  }

  private int parseTransfers(
      BufferedReader reader, List<String> headers, Map<Long, Long> walletIdMap)
      throws IOException {
    int sourceIndex = headers.indexOf("source_wallet_id");
    int destIndex = headers.indexOf("destination_wallet_id");
    int amountIndex = headers.indexOf("amount_minor");
    int occurredAtIndex = headers.indexOf("occurred_at");
    int noteIndex = headers.indexOf("note");
    int count = 0;
    long now = System.currentTimeMillis();
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.isEmpty()) continue;
      if (SECTION_NAMES.contains(line)) break;
      List<String> fields = parseCsvLine(line);
      if (fields.size() < 5) continue;
      long sourceWalletId = lookupId(walletIdMap, fields.get(sourceIndex >= 0 ? sourceIndex : 1));
      long destWalletId = lookupId(walletIdMap, fields.get(destIndex >= 0 ? destIndex : 2));
      long amountMinor = Long.parseLong(fields.get(amountIndex >= 0 ? amountIndex : 3));
      long occurredAt = Long.parseLong(fields.get(occurredAtIndex >= 0 ? occurredAtIndex : 4));
      String note = noteIndex >= 0 && noteIndex < fields.size() ? fields.get(noteIndex) : null;
      if (note != null && note.isEmpty()) note = null;

      transferDao.insert(sourceWalletId, destWalletId, amountMinor, occurredAt, note, now);
      count++;
    }
    return count;
  }

  private int parseTransactions(
      BufferedReader reader,
      List<String> headers,
      Map<Long, Long> walletIdMap,
      Map<Long, Long> categoryIdMap)
      throws IOException {
    int amountIndex = headers.indexOf("amount_minor");
    int typeIndex = headers.indexOf("type");
    int walletIndex = headers.indexOf("wallet_id");
    int categoryIndex = headers.indexOf("category_id");
    int occurredAtIndex = headers.indexOf("occurred_at");
    int noteIndex = headers.indexOf("note");
    int transferIdIndex = headers.indexOf("transfer_id");
    int cashFlowIndex = headers.indexOf("cash_flow_activity");
    int cashFlowOverriddenIndex = headers.indexOf("cash_flow_activity_overridden");
    int count = 0;
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.isEmpty()) continue;
      if (SECTION_NAMES.contains(line)) break;
      List<String> fields = parseCsvLine(line);
      if (fields.size() < 7) continue;

      long amountMinor = Long.parseLong(fields.get(amountIndex >= 0 ? amountIndex : 1));
      TransactionType type = TransactionType.valueOf(fields.get(typeIndex >= 0 ? typeIndex : 2));
      long walletId = lookupId(walletIdMap, fields.get(walletIndex >= 0 ? walletIndex : 3));
      long categoryId = safeParseLong(
          fields.get(categoryIndex >= 0 && categoryIndex < fields.size() ? categoryIndex : 4), 0L);
      if (categoryId > 0L) {
        categoryId = lookupId(categoryIdMap, String.valueOf(categoryId));
      }
      long occurredAt = Long.parseLong(
          fields.get(occurredAtIndex >= 0 ? occurredAtIndex : 5));
      String note = noteIndex >= 0 && noteIndex < fields.size() ? fields.get(noteIndex) : null;
      if (note != null && note.isEmpty()) {
        note = null;
      }

      CashFlowActivity cashFlowActivity = CashFlowActivity.UNCLASSIFIED;
      if (cashFlowIndex >= 0 && cashFlowIndex < fields.size()) {
        String val = fields.get(cashFlowIndex);
        if (!val.isEmpty()) {
          try {
            cashFlowActivity = CashFlowActivity.valueOf(val);
          } catch (IllegalArgumentException ignored) {
          }
        }
      }
      boolean cashFlowOverridden = false;
      if (cashFlowOverriddenIndex >= 0 && cashFlowOverriddenIndex < fields.size()) {
        cashFlowOverridden = "1".equals(fields.get(cashFlowOverriddenIndex));
      }
      Transaction transaction =
          new Transaction(0L, amountMinor, type, walletId, categoryId, occurredAt, note);
      transaction.setCashFlowActivity(cashFlowActivity);
      transaction.setCashFlowActivityOverridden(cashFlowOverridden);

      long newTrxId = transactionGateway.insert(transaction);
      if (newTrxId > 0L) {
        count++;
      }
    }
    return count;
  }

  private void recalculateAllWalletBalances() {
    List<com.dwlhm.finan.data.entity.Wallet> allWallets = walletDao.findAll();
    for (com.dwlhm.finan.data.entity.Wallet w : allWallets) {
      balanceService.recalculate(w.getId());
    }
  }

  private static long lookupId(Map<Long, Long> idMap, String strId) {
    try {
      long oldId = Long.parseLong(strId.trim());
      Long mapped = idMap.get(oldId);
      return mapped != null ? mapped : oldId;
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private static long safeParseLong(String value, long defaultValue) {
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static long nowOrDefault(int index, List<String> fields, long defaultVal) {
    if (index >= 0 && index < fields.size()) {
      String val = fields.get(index);
      if (!val.isEmpty()) {
        try {
          return Long.parseLong(val);
        } catch (NumberFormatException e) {
          return defaultVal;
        }
      }
    }
    return defaultVal;
  }

  static List<String> parseCsvLine(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          current.append(c);
        }
      } else {
        if (c == '"') {
          inQuotes = true;
        } else if (c == ',') {
          fields.add(current.toString());
          current = new StringBuilder();
        } else {
          current.append(c);
        }
      }
    }
    fields.add(current.toString());
    return fields;
  }
}
