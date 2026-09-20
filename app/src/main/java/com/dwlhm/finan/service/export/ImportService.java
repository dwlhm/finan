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
import com.dwlhm.finan.domain.model.Transfer;

import java.io.BufferedReader;
import java.io.PushbackReader;
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

  private static final int MAX_RECORD_CHARS = 1024 * 1024;
  private static final int LEGACY_VERSION = 3;
  private static final List<String> SECTIONS = Arrays.asList(
      WALLET_SECTION, CATEGORY_SECTION, TRANSFER_SECTION, TRANSACTION_SECTION);
  private static final String[] HEADERS = {
    "id,name,currency_code,is_default,opening_balance_minor,icon",
    "id,name,icon,type_filter,sort_order,cash_flow_activity",
    "id,source_wallet_id,destination_wallet_id,amount_minor,occurred_at,note",
    "id,amount_minor,type,wallet_id,category_id,occurred_at,note,transfer_id,cash_flow_activity,cash_flow_activity_overridden"
  };

  public ImportResult importFrom(InputStream in) throws IOException {
    try (PushbackReader reader = new PushbackReader(
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT))), 1)) {
      String version = readCsvRecord(reader);
      if (!(VERSION_PREFIX + LEGACY_VERSION).equals(version)
          && !(VERSION_PREFIX + ExportService.CSV_FORMAT_VERSION).equals(version)) {
        return ImportResult.failure("Invalid or unsupported CSV version");
      }
      db.beginTransaction();
      try {
        Map<Long, Long> wallets = new HashMap<>();
        Map<Long, Long> categories = new HashMap<>();
        Map<Long, Long> transfers = new HashMap<>();
        Set<Long> transactionIds = new HashSet<>();
        int[] counts = new int[SECTIONS.size()];
        int section = -1;
        List<String> headers = null;
        String record;
        while ((record = readCsvRecord(reader)) != null) {
          if (record.isEmpty()) continue;
          int next = SECTIONS.indexOf(record);
          if (next >= 0) {
            require(next == section + 1, "Missing, repeated or out-of-order section");
            section = next;
            String header = readCsvRecord(reader);
            require(header != null, "Missing section header");
            headers = parseCsvLine(header);
            List<String> expected = parseCsvLine(HEADERS[section]);
            require(headers.size() == expected.size()
                && new HashSet<>(headers).equals(new HashSet<>(expected)), "Invalid section header");
            continue;
          }
          require(section >= 0 && headers != null, "Row outside a section");
          List<String> fields = parseCsvLine(record);
          require(fields.size() == headers.size(), "Invalid row field count");
          Map<String, String> row = new HashMap<>();
          for (int i = 0; i < headers.size(); i++) row.put(headers.get(i), fields.get(i));
          long oldId = positive(row.get("id"));
          switch (section) {
            case 0: {
              require(!wallets.containsKey(oldId), "Duplicate wallet ID");
              String name = nonempty(row.get("name"));
              String currency = nonempty(row.get("currency_code"));
              boolean isDefault = booleanValue(row.get("is_default"));
              long opening = Long.parseLong(row.get("opening_balance_minor"));
              com.dwlhm.finan.data.entity.Wallet existing = walletDao.findByName(name);
              long id = existing == null
                  ? walletDao.insert(name, currency, isDefault, opening,
                      System.currentTimeMillis(), nullable(row.get("icon"))) : existing.getId();
              require(id > 0, "Wallet insert failed");
              wallets.put(oldId, id);
              if (existing == null) counts[section]++;
              break;
            }
            case 1: {
              require(!categories.containsKey(oldId), "Duplicate category ID");
              String name = nonempty(row.get("name"));
              String type = row.get("type_filter");
              require(Arrays.asList("INCOME", "EXPENSE", "BOTH").contains(type), "Invalid category type");
              int order = Integer.parseInt(row.get("sort_order"));
              String activity = CashFlowActivity.valueOf(row.get("cash_flow_activity")).name();
              com.dwlhm.finan.data.entity.Category existing = categoryDao.findByNameIgnoreCase(name);
              long id = existing == null
                  ? categoryDao.insert(name, nullable(row.get("icon")), type, order, 0, null, activity)
                  : existing.getId();
              require(id > 0, "Category insert failed");
              categories.put(oldId, id);
              if (existing == null) counts[section]++;
              break;
            }
            case 2: {
              require(!transfers.containsKey(oldId), "Duplicate transfer ID");
              long source = lookupId(wallets, row.get("source_wallet_id"));
              long destination = lookupId(wallets, row.get("destination_wallet_id"));
              require(source != destination, "Transfer wallets must differ");
              long id = transferDao.insert(source, destination, positive(row.get("amount_minor")),
                  Long.parseLong(row.get("occurred_at")), nullable(row.get("note")), System.currentTimeMillis());
              require(id > 0, "Transfer insert failed");
              transfers.put(oldId, id);
              counts[section]++;
              break;
            }
            case 3: {
              require(transactionIds.add(oldId), "Duplicate transaction ID");
              TransactionType type = TransactionType.valueOf(row.get("type"));
              long category = Long.parseLong(row.get("category_id"));
              require(category >= 0, "Invalid category ID");
              Transaction transaction = new Transaction(0L, positive(row.get("amount_minor")), type,
                  lookupId(wallets, row.get("wallet_id")),
                  category == 0 ? 0 : lookupId(categories, row.get("category_id")),
                  Long.parseLong(row.get("occurred_at")), nullable(row.get("note")));
              String transfer = row.get("transfer_id");
              require(type.isTransfer() == !transfer.isEmpty(), "Invalid transfer relationship");
              if (!transfer.isEmpty()) transaction.setTransferId(lookupId(transfers, transfer));
              transaction.setCashFlowActivity(CashFlowActivity.valueOf(row.get("cash_flow_activity")));
              transaction.setCashFlowActivityOverridden(booleanValue(row.get("cash_flow_activity_overridden")));
              require(transactionGateway.insert(transaction) > 0, "Transaction insert failed");
              counts[section]++;
              break;
            }
          }
        }
        require(section == SECTIONS.size() - 1, "Incomplete CSV sections");
        for (long id : transfers.values()) validateTransfer(id);
        for (com.dwlhm.finan.data.entity.Wallet wallet : walletDao.findAll()) {
          balanceService.recalculate(wallet.getId());
        }
        db.setTransactionSuccessful();
        return ImportResult.success(counts[0], counts[1], counts[2], counts[3]);
      } catch (IllegalArgumentException | android.database.SQLException e) {
        return ImportResult.failure("Invalid CSV: " + e.getMessage());
      } finally {
        db.endTransaction();
      }
    }
  }

  private void validateTransfer(long id) {
    Transfer transfer = transferDao.findById(id);
    List<Transaction> entries = transactionGateway.findByTransferId(id);
    require(entries.size() == 2, "Transfer must have two entries");
    Set<TransactionType> types = new HashSet<>();
    for (Transaction entry : entries) {
      types.add(entry.getType());
      require(entry.getAmountMinor() == transfer.getAmountMinor()
          && entry.getOccurredAt() == transfer.getOccurredAt()
          && entry.getWalletId() == (entry.getType() == TransactionType.TRANSFER_OUT
              ? transfer.getSourceWalletId() : transfer.getDestinationWalletId()),
          "Transfer entry does not match transfer");
    }
    require(types.contains(TransactionType.TRANSFER_IN)
        && types.contains(TransactionType.TRANSFER_OUT), "Invalid transfer pair");
  }

  private static String nullable(String value) { return value.isEmpty() ? null : value; }

  private static String nonempty(String value) {
    require(!value.trim().isEmpty(), "Required value is empty");
    return value;
  }

  private static boolean booleanValue(String value) {
    require("0".equals(value) || "1".equals(value), "Invalid boolean");
    return "1".equals(value);
  }

  private static long positive(String value) {
    long number = Long.parseLong(value);
    require(number > 0, "Expected positive number");
    return number;
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }

  private static long lookupId(Map<Long, Long> ids, String value) {
    Long mapped = ids.get(positive(value));
    require(mapped != null, "Unknown referenced ID");
    return mapped;
  }

  static String readCsvRecord(PushbackReader reader) throws IOException {
    StringBuilder record = new StringBuilder();
    boolean quoted = false;
    int value;
    while ((value = reader.read()) != -1) {
      char c = (char) value;
      if (c == '"') {
        if (quoted) {
          int next = reader.read();
          if (next == '"') {
            record.append("\"\"");
            if (record.length() > MAX_RECORD_CHARS) throw new IOException("CSV record too large");
            continue;
          }
          quoted = false;
          if (next != -1) reader.unread(next);
        } else {
          quoted = true;
        }
      }
      if (!quoted && (c == '\n' || c == '\r')) {
        if (c == '\r') {
          int next = reader.read();
          if (next != -1 && next != '\n') reader.unread(next);
        }
        return record.toString();
      }
      record.append(c);
      if (record.length() > MAX_RECORD_CHARS) throw new IOException("CSV record too large");
    }
    if (quoted) throw new IOException("Unterminated quoted CSV field");
    return record.length() == 0 ? null : record.toString();
  }

  static List<String> parseCsvLine(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean quoted = false;
    boolean closed = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (quoted) {
        if (c == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            quoted = false;
            closed = true;
          }
        } else current.append(c);
      } else if (c == ',') {
        fields.add(current.toString());
        current.setLength(0);
        closed = false;
      } else if (c == '"') {
        require(current.length() == 0 && !closed, "Malformed CSV quote");
        quoted = true;
      } else {
        require(!closed && c != '\r' && c != '\n', "Malformed CSV field");
        current.append(c);
      }
    }
    require(!quoted, "Unterminated CSV quote");
    fields.add(current.toString());
    return fields;
  }
}
