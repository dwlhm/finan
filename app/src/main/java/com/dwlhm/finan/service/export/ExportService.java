package com.dwlhm.finan.service.export;

import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.domain.model.Transaction;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportService {

  public static final int CSV_FORMAT_VERSION = 2;
  private static final String VERSION_HEADER = "FINAN_CSV_VERSION," + CSV_FORMAT_VERSION;
  private static final String COLUMN_HEADER =
      "id,amount_minor,type,wallet_id,category_id,occurred_at,note,merchant_id,tag_ids";

  public void exportTo(OutputStream out, TransactionGateway transactionGateway) throws IOException {
    BufferedOutputStream buffered =
        out instanceof BufferedOutputStream
            ? (BufferedOutputStream) out
            : new BufferedOutputStream(out);
    writeLine(buffered, VERSION_HEADER);
    writeLine(buffered, COLUMN_HEADER);
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
      throw (IOException) e.getCause();
    }
    buffered.flush();
  }

  public String toCsv(List<Transaction> transactions) {
    StringBuilder csv = new StringBuilder();
    csv.append(VERSION_HEADER).append('\n');
    csv.append(COLUMN_HEADER).append('\n');
    for (Transaction transaction : transactions) {
      appendTransactionRow(csv, transaction);
    }
    return csv.toString();
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
    csv.append(escapeCsv(formatTagIds(transaction.getTagIds()))).append('\n');
  }

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
