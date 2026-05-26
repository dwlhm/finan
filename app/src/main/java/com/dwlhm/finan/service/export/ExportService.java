package com.dwlhm.finan.service.export;

import com.dwlhm.finan.domain.model.Transaction;

import java.util.List;

public class ExportService {

    public static final int CSV_FORMAT_VERSION = 1;
    private static final String VERSION_HEADER = "FINAN_CSV_VERSION," + CSV_FORMAT_VERSION;
    private static final String COLUMN_HEADER =
            "id,amount_minor,type,wallet_id,category_id,occurred_at,note";

    public String toCsv(List<Transaction> transactions) {
        StringBuilder csv = new StringBuilder();
        csv.append(VERSION_HEADER).append('\n');
        csv.append(COLUMN_HEADER).append('\n');
        for (Transaction t : transactions) {
            csv.append(t.getId()).append(',');
            csv.append(t.getAmountMinor()).append(',');
            csv.append(t.getType().name()).append(',');
            csv.append(t.getWalletId()).append(',');
            csv.append(t.getCategoryId()).append(',');
            csv.append(t.getOccurredAt()).append(',');
            csv.append(escapeCsv(t.getNote())).append('\n');
        }
        return csv.toString();
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
