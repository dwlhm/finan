package com.dwlhm.finan.ui.summary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dwlhm.finan.domain.model.MonthlySummary;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;

public class FinancialAdviceDialogTest {

    @Test
    public void testDecodeHtmlEntities() {
        String input = "&amp;lt;b&amp;gt;Catat&amp;lt;/b&amp;gt;&amp;lt;br/&amp;gt;";
        String decoded = FinancialAdviceDialog.decodeHtml(input);
        assertEquals("<b>Catat</b><br/>", decoded);

        String singleEncoded = "&lt;b&gt;Pemasukan&lt;/b&gt;";
        assertEquals("<b>Pemasukan</b>", FinancialAdviceDialog.decodeHtml(singleEncoded));

        String quotes = "&amp;quot;Halo&amp;quot;";
        assertEquals("\"Halo\"", FinancialAdviceDialog.decodeHtml(quotes));
    }

    @Test
    public void testExtractRawAdvicePartsWithTipsKeuangan() {
        String message = "Masih awal periode, yuk pantau terus pengeluaranmu.&amp;lt;br/&amp;gt;&amp;lt;br/&amp;gt;&amp;lt;b&amp;gt;Tips Keuangan:&amp;lt;/b&amp;gt;&amp;lt;br/&amp;gt;Gunakan fitur &amp;lt;b&amp;gt;Ringkasan&amp;lt;/b&amp;gt; secara berkala.";
        FinancialAdviceDialog.RawAdviceParts parts = FinancialAdviceDialog.extractRawAdviceParts(message);

        assertEquals("Masih awal periode, yuk pantau terus pengeluaranmu.", parts.mainMessageHtml);
        assertEquals("Tips Keuangan", parts.tipsTitle);
        assertEquals("Gunakan fitur <b>Ringkasan</b> secara berkala.", parts.tipsContentHtml);
    }

    @Test
    public void testExtractRawAdvicePartsWithTipsAplikasi() {
        String message = "Catat pemasukan pertamamu.<br/><br/><b>Tips Aplikasi:</b><br/>Mulai catat transaksi harian.";
        FinancialAdviceDialog.RawAdviceParts parts = FinancialAdviceDialog.extractRawAdviceParts(message);

        assertEquals("Catat pemasukan pertamamu.", parts.mainMessageHtml);
        assertEquals("Tips Aplikasi", parts.tipsTitle);
        assertEquals("Mulai catat transaksi harian.", parts.tipsContentHtml);
    }

    @Test
    public void testExtractRawAdvicePartsWithTipsAndSorotanPengeluaran() {
        String message = "Sudah 1/3 jalan.<br/><br/><b>Tips Keuangan:</b><br/>Tetap disiplin.<br/><br/><b>Sorotan Pengeluaran:</b><br/>• Kategori Makanan naik 20%.";
        FinancialAdviceDialog.RawAdviceParts parts = FinancialAdviceDialog.extractRawAdviceParts(message);

        assertEquals("Sudah 1/3 jalan.<br/><br/><b>Sorotan Pengeluaran:</b><br/>• Kategori Makanan naik 20%.", parts.mainMessageHtml);
        assertEquals("Tips Keuangan", parts.tipsTitle);
        assertEquals("Tetap disiplin.", parts.tipsContentHtml);
    }

    @Test
    public void testExtractRawAdvicePartsWithoutTips() {
        String message = "Pengeluaran melebihi pemasukan periode ini.";
        FinancialAdviceDialog.RawAdviceParts parts = FinancialAdviceDialog.extractRawAdviceParts(message);

        assertEquals("Pengeluaran melebihi pemasukan periode ini.", parts.mainMessageHtml);
        assertNull(parts.tipsTitle);
        assertNull(parts.tipsContentHtml);
    }

    @Test
    public void testCalculatePaceControlled() {
        // July 2026: 31 days. Day 15 -> ~48.38% time passed.
        // Income 1,000,000, Expense 300,000 -> 30% expense ratio <= 48.38% -> controlled.
        MonthlySummary summary = new MonthlySummary(2026, 7, 300000L, 1000000L, Collections.emptyList(), Collections.emptyList());
        LocalDate referenceDate = LocalDate.of(2026, 7, 15);

        FinancialAdviceDialog.PaceCalculation pace = FinancialAdviceDialog.calculatePace(summary, referenceDate);

        assertEquals(15, pace.elapsedDays);
        assertEquals(31, pace.totalDays);
        assertTrue(pace.hasTransactions);
        assertEquals(30.0f, pace.expensePercent, 0.01f);
        assertTrue(pace.isControlled);
    }

    @Test
    public void testCalculatePaceOverpace() {
        // July 2026: 31 days. Day 10 -> ~32.25% time passed.
        // Income 1,000,000, Expense 600,000 -> 60% expense ratio > 32.25% -> overpace.
        MonthlySummary summary = new MonthlySummary(2026, 7, 600000L, 1000000L, Collections.emptyList(), Collections.emptyList());
        LocalDate referenceDate = LocalDate.of(2026, 7, 10);

        FinancialAdviceDialog.PaceCalculation pace = FinancialAdviceDialog.calculatePace(summary, referenceDate);

        assertEquals(10, pace.elapsedDays);
        assertEquals(31, pace.totalDays);
        assertTrue(pace.hasTransactions);
        assertEquals(60.0f, pace.expensePercent, 0.01f);
        assertFalse(pace.isControlled);
    }

    @Test
    public void testCalculatePaceNoTransactions() {
        MonthlySummary summary = new MonthlySummary(2026, 7, 0L, 0L, Collections.emptyList(), Collections.emptyList());
        LocalDate referenceDate = LocalDate.of(2026, 7, 5);

        FinancialAdviceDialog.PaceCalculation pace = FinancialAdviceDialog.calculatePace(summary, referenceDate);

        assertEquals(5, pace.elapsedDays);
        assertEquals(31, pace.totalDays);
        assertFalse(pace.hasTransactions);
    }
}
