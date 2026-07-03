package com.dwlhm.finan.service.export;

import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.Transfer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportService {

  public static final int CSV_FORMAT_VERSION = 4;
  private static final String VERSION_HEADER = "FINAN_CSV_VERSION," + CSV_FORMAT_VERSION;
  private static final String WALLET_SECTION = "WALLETS";
  private static final String WALLET_HEADER =
      "id,name,currency_code,is_default,opening_balance_minor,icon";
  private static final String CATEGORY_SECTION = "CATEGORIES";
  private static final String CATEGORY_HEADER =
      "id,name,icon,type_filter,sort_order,cash_flow_activity";
  private static final String TRANSFER_SECTION = "TRANSFERS";
  private static final String TRANSFER_HEADER =
      "id,source_wallet_id,destination_wallet_id,amount_minor,occurred_at,note";
  private static final String TRANSACTION_SECTION = "TRANSACTIONS";
  private static final String TRANSACTION_HEADER =
      "id,amount_minor,type,wallet_id,category_id,occurred_at,note,transfer_id,cash_flow_activity,cash_flow_activity_overridden";

  public void exportTo(OutputStream out, TransactionGateway transactionGateway) throws IOException {
    exportTo(out, List.of(), List.of(), List.of(), null, null, transactionGateway);
  }

  public void exportTo(
      OutputStream out, List<Wallet> wallets, TransactionGateway transactionGateway)
      throws IOException {
    exportTo(out, wallets, List.of(), List.of(), null, null, transactionGateway);
  }

  public void exportTo(
      OutputStream out,
      List<Wallet> wallets,
      List<Category> categories,
      List<Transfer> transfers,
      TransactionGateway transactionGateway)
      throws IOException {
    exportTo(out, wallets, categories, transfers, null, null, transactionGateway);
  }

  public void exportTo(
      OutputStream out,
      List<Wallet> wallets,
      List<Category> categories,
      List<Transfer> transfers,
      Long startDate,
      Long endDate,
      TransactionGateway transactionGateway)
      throws IOException {
    BufferedOutputStream buffered =
        out instanceof BufferedOutputStream
            ? (BufferedOutputStream) out
            : new BufferedOutputStream(out);
    writeLine(buffered, VERSION_HEADER);
    writeWallets(buffered, wallets);
    writeCategories(buffered, categories);
    writeTransfers(buffered, transfers);
    writeLine(buffered, TRANSACTION_SECTION);
    writeLine(buffered, TRANSACTION_HEADER);
    try {
      transactionGateway.forEachTransaction(startDate, endDate,
          transaction -> {
            try {
              writeTransactionRow(buffered, transaction);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    } catch (UncheckedIOException e) {
      IOException cause = e.getCause();
      if (cause != null) throw cause;
      throw e;
    }
    buffered.flush();
  }

  public String toCsv(List<Transaction> transactions) {
    StringBuilder csv = new StringBuilder();
    csv.append(VERSION_HEADER).append('\n');
    csv.append(TRANSACTION_SECTION).append('\n');
    csv.append(TRANSACTION_HEADER).append('\n');
    for (Transaction transaction : transactions) {
      appendTransactionRow(csv, transaction);
    }
    return csv.toString();
  }

  private static void writeWallets(OutputStream out, List<Wallet> wallets) throws IOException {
    writeLine(out, WALLET_SECTION);
    writeLine(out, WALLET_HEADER);
    for (Wallet wallet : wallets) {
      StringBuilder row = new StringBuilder();
      appendWalletRow(row, wallet);
      out.write(row.toString().getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void writeCategories(OutputStream out, List<Category> categories)
      throws IOException {
    writeLine(out, CATEGORY_SECTION);
    writeLine(out, CATEGORY_HEADER);
    for (Category category : categories) {
      StringBuilder row = new StringBuilder();
      appendCategoryRow(row, category);
      out.write(row.toString().getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void writeTransfers(OutputStream out, List<Transfer> transfers)
      throws IOException {
    writeLine(out, TRANSFER_SECTION);
    writeLine(out, TRANSFER_HEADER);
    for (Transfer transfer : transfers) {
      StringBuilder row = new StringBuilder();
      appendTransferRow(row, transfer);
      out.write(row.toString().getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void appendWalletRow(StringBuilder csv, Wallet wallet) {
    csv.append(wallet.getId()).append(',');
    csv.append(escapeCsv(wallet.getName())).append(',');
    csv.append(escapeCsv(wallet.getCurrencyCode())).append(',');
    csv.append(wallet.isDefault() ? 1 : 0).append(',');
    csv.append(wallet.getOpeningBalanceMinor()).append(',');
    csv.append(escapeCsv(wallet.getIcon())).append('\n');
  }

  private static void appendCategoryRow(StringBuilder csv, Category category) {
    csv.append(category.getId()).append(',');
    csv.append(escapeCsv(category.getName())).append(',');
    csv.append(escapeCsv(category.getIcon())).append(',');
    csv.append(escapeCsv(category.getTypeFilter())).append(',');
    csv.append(category.getSortOrder()).append(',');
    csv.append(escapeCsv(category.getCashFlowActivity())).append('\n');
  }

  private static void appendTransferRow(StringBuilder csv, Transfer transfer) {
    csv.append(transfer.getId()).append(',');
    csv.append(transfer.getSourceWalletId()).append(',');
    csv.append(transfer.getDestinationWalletId()).append(',');
    csv.append(transfer.getAmountMinor()).append(',');
    csv.append(transfer.getOccurredAt()).append(',');
    csv.append(escapeCsv(transfer.getNote())).append('\n');
  }

  private static void writeTransactionRow(OutputStream out, Transaction transaction)
      throws IOException {
    StringBuilder row = new StringBuilder();
    appendTransactionRow(row, transaction);
    out.write(row.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void appendTransactionRow(StringBuilder csv, Transaction transaction) {
    csv.append(transaction.getId()).append(',');
    csv.append(transaction.getAmountMinor()).append(',');
    csv.append(transaction.getType().name()).append(',');
    csv.append(transaction.getWalletId()).append(',');
    csv.append(transaction.getCategoryId()).append(',');
    csv.append(transaction.getOccurredAt()).append(',');
    csv.append(escapeCsv(transaction.getNote())).append(',');
    Long transferId = transaction.getTransferId();
    csv.append(transferId == null ? "" : transferId).append(',');
    csv.append(transaction.getCashFlowActivity().name()).append(',');
    csv.append(transaction.isCashFlowActivityOverridden() ? 1 : 0).append('\n');
  }

  private static void writeLine(OutputStream out, String line) throws IOException {
    out.write(line.getBytes(StandardCharsets.UTF_8));
    out.write('\n');
  }

  private static String escapeCsv(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
