package com.dwlhm.finan.service.export;

import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ExportServiceTest {

    private final ExportService exportService = new ExportService();

    @Test
    public void csv_includes_format_version_header() {
        List<Transaction> transactions = Arrays.asList(
                new Transaction(1L, 25_000L, TransactionType.EXPENSE, 1L, 2L, 1_700_000_000_000L, "lunch")
        );
        String csv = exportService.toCsv(transactions);
        assertTrue(csv.startsWith("FINAN_CSV_VERSION,1"));
    }

    @Test
    public void csv_includes_column_headers() {
        String csv = exportService.toCsv(Arrays.asList());
        assertTrue(csv.contains("id,amount_minor,type,wallet_id,category_id,occurred_at,note"));
    }

    @Test
    public void csv_includes_transaction_row() {
        Transaction t = new Transaction(
                42L, 25_000L, TransactionType.EXPENSE, 1L, 2L, 1_700_000_000_000L, "lunch"
        );
        String csv = exportService.toCsv(Arrays.asList(t));
        assertTrue(csv.contains("42,25000,EXPENSE,1,2,1700000000000,lunch"));
    }

    @Test
    public void csv_escapes_note_with_comma() {
        Transaction t = new Transaction(
                1L, 1000L, TransactionType.INCOME, 1L, 1L, 1L, "a,b"
        );
        String csv = exportService.toCsv(Arrays.asList(t));
        assertTrue(csv.contains("\"a,b\""));
    }
}
