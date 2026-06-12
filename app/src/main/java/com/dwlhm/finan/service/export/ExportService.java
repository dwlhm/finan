package com.dwlhm.finan.service.export;

import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportService {

  public static final int CSV_FORMAT_VERSION = 3;
  private static final String VERSION_HEADER = "FINAN_CSV_VERSION," + CSV_FORMAT_VERSION;
  private static final String WALLET_SECTION = "WALLETS";
  private static final String WALLET_HEADER =
      "id,name,currency_code,is_default,opening_balance_minor";
  private static final String TRANSACTION_SECTION = "TRANSACTIONS";
  private static final String TRANSACTION_HEADER =
      "id,amount_minor,type,wallet_id,category_id,occurred_at,note,merchant_id,tag_ids,transfer_id";

  public void exportTo(OutputStream out, TransactionGateway transactionGateway) throws IOException {
    exportTo(out, List.of(), transactionGateway);
  }

  public void exportTo(
      OutputStream out, List<Wallet> wallets, TransactionGateway transactionGateway)
      throws IOException {
    BufferedOutputStream buffered =
        out instanceof BufferedOutputStream
            ? (BufferedOutputStream) out
            : new BufferedOutputStream(out);
    writeLine(buffered, VERSION_HEADER);
    writeWallets(buffered, wallets);
    writeLine(buffered, TRANSACTION_SECTION);
    writeLine(buffered, TRANSACTION_HEADER);
    try {
      transactionGateway.forEachTransaction(
          transaction -> {
            try {
              writeTransactionRow(buffered, transaction);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    } catch (UncheckedIOException e) {
      IOException cause = e.getCause();
      if (cause != null) {
        throw cause;
      }
      throw e;
    }
    buffered.flush();
  }

  public String toCsv(List<Transaction> transactions) {
    StringBuilder csv = new StringBuilder();
    csv.append(VERSION_HEADER).append('\n');
    appendWallets(csv, List.of());
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

  private static void appendWallets(StringBuilder csv, List<Wallet> wallets) {
    csv.append(WALLET_SECTION).append('\n');
    csv.append(WALLET_HEADER).append('\n');
    for (Wallet wallet : wallets) {
      appendWalletRow(csv, wallet);
    }
  }

  private static void appendWalletRow(StringBuilder csv, Wallet wallet) {
    csv.append(wallet.getId()).append(',');
    csv.append(escapeCsv(wallet.getName())).append(',');
    csv.append(escapeCsv(wallet.getCurrencyCode())).append(',');
    csv.append(wallet.isDefault() ? 1 : 0).append(',');
    csv.append(wallet.getOpeningBalanceMinor()).append('\n');
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
    Long merchantId = transaction.getMerchantId();
    csv.append(merchantId == null ? "" : merchantId).append(',');
    csv.append(escapeCsv(formatTagIds(transaction.getTagIds()))).append(',');
    Long transferId = transaction.getTransferId();
    csv.append(transferId == null ? "" : transferId).append('\n');
  }

  @SuppressWarnings("SizeReplaceableByIsEmpty")
  private static String formatTagIds(List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    for (Long tagId : tagIds) {
      if (tagId == null || tagId <= 0L) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append(';');
      }
      builder.append(tagId);
    }
    return builder.toString();
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
