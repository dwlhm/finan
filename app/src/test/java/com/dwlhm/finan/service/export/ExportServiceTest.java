package com.dwlhm.finan.service.export;

import com.dwlhm.finan.data.dao.TransactionGateway;
import com.dwlhm.finan.domain.model.HistoryPageCursor;
import com.dwlhm.finan.domain.model.HistoryQuery;
import com.dwlhm.finan.domain.model.HistoryTotals;
import com.dwlhm.finan.domain.model.PageResult;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

public class ExportServiceTest {

    private final ExportService exportService = new ExportService();

    @Test
    public void csv_includes_format_version_header() {
        List<Transaction> transactions = List.of(sampleTransaction());
        String csv = exportService.toCsv(transactions);
        assertTrue("Version header must be present", csv.startsWith("FINAN_CSV_VERSION,4"));
    }

    @Test
    public void csv_includes_section_headers() {
        String csv = exportService.toCsv(List.of());
        assertTrue(csv.contains("TRANSACTIONS"));
    }

    @Test
    public void csv_includes_column_headers() {
        String csv = exportService.toCsv(List.of());
        assertTrue(
            csv.contains(
                "id,amount_minor,type,wallet_id,category_id,occurred_at,note,transfer_id,cash_flow_activity,cash_flow_activity_overridden"));
    }

    @Test
    public void csv_includes_transaction_row() {
        Transaction t = sampleTransaction();
        String csv = exportService.toCsv(List.of(t));
        assertTrue(csv.contains("42,25000,EXPENSE,1,2,1700000000000,lunch,,UNCLASSIFIED,0"));
    }

    @Test
    public void csv_escapes_note_with_comma() {
        Transaction t = new Transaction(1L, 1000L, TransactionType.INCOME, 1L, 1L, 1L, "a,b");
        String csv = exportService.toCsv(List.of(t));
        assertTrue(csv.contains("\"a,b\""));
    }

    @Test
    public void exportTo_streams_transactions_without_loading_list() throws Exception {
        Transaction t = sampleTransaction();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportTo(out, singleTransactionGateway(t));
        String csv = out.toString(StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("FINAN_CSV_VERSION,4"));
        assertTrue(csv.contains("42,25000,EXPENSE,1,2,1700000000000,lunch,,"));
    }

    private static Transaction sampleTransaction() {
        return new Transaction(
            42L, 25_000L, TransactionType.EXPENSE, 1L, 2L, 1_700_000_000_000L, "lunch");
    }

    private static TransactionGateway singleTransactionGateway(Transaction transaction) {
        return new TransactionGateway() {
            @Override
            public long insert(Transaction value) {
                return 0L;
            }

            @Override
            public void update(Transaction value) {}

            @Override
            public void delete(long transactionId) {}

            @Override
            public Transaction findById(long transactionId) {
                return null;
            }

            @Override
            public List<Transaction> findRecent(int limit) {
                return List.of();
            }

            @Override
            public PageResult<Transaction, HistoryPageCursor> findHistoryPage(
                HistoryQuery query, HistoryPageCursor cursor, int limit) {
                return new PageResult<>(List.of(), false, null);
            }

            @Override
            public HistoryTotals findHistoryTotals(HistoryQuery query) {
                return new HistoryTotals(0, 0L, 0L);
            }

            @Override
            public List<Transaction> findByWalletId(long walletId) {
                return List.of();
            }

            @Override
            public List<Transaction> findByTransferId(long transferId) {
                return List.of();
            }

            @Override
            public void forEachTransaction(Consumer<Transaction> consumer) {
                consumer.accept(transaction);
            }

            @Override
            public List<Transaction> findAll() {
                return List.of(transaction);
            }
        };
    }
}
