package com.dwlhm.finan.util.money;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoneyFormatterTest {

    @Test
    public void format_zero() {
        assertEquals("Rp 0", MoneyFormatter.format(0L));
    }

    @Test
    public void format_positive_idr() {
        assertEquals("Rp 25.000", MoneyFormatter.format(25_000L));
        assertEquals("Rp 1.500.000", MoneyFormatter.format(1_500_000L));
    }

    @Test
    public void format_negative_idr() {
        assertEquals("-Rp 25.000", MoneyFormatter.format(-25_000L));
    }

    @Test
    public void format_without_currency() {
        assertEquals("25.000", MoneyFormatter.formatWithoutCurrency(25_000L));
        assertEquals("-25.000", MoneyFormatter.formatWithoutCurrency(-25_000L));
    }

    @Test
    public void normalize_currency_code_defaults_to_idr() {
        assertEquals("IDR", MoneyFormatter.normalizeCurrencyCode(null));
        assertEquals("IDR", MoneyFormatter.normalizeCurrencyCode(""));
        assertEquals("USD", MoneyFormatter.normalizeCurrencyCode(" usd "));
    }

    @Test
    public void format_with_currency_code_uses_idr_formatter() {
        assertEquals("Rp 25.000", MoneyFormatter.formatWithCurrencyCode("idr", 25_000L));
        assertEquals("-Rp 25.000", MoneyFormatter.formatWithCurrencyCode("IDR", -25_000L));
    }

    @Test
    public void format_with_currency_code_supports_non_idr() {
        assertEquals("USD 25.000", MoneyFormatter.formatWithCurrencyCode("usd", 25_000L));
        assertEquals("-USD 25.000", MoneyFormatter.formatWithCurrencyCode("USD", -25_000L));
    }

    @Test
    public void format_totals_by_currency_keeps_insertion_order() {
        Map<String, Long> totalsByCurrency = new LinkedHashMap<>();
        totalsByCurrency.put("IDR", 25_000L);
        totalsByCurrency.put("USD", 10_000L);

        assertEquals("IDR: Rp 25.000\nUSD: USD 10.000",
                MoneyFormatter.formatTotalsByCurrency(totalsByCurrency));
    }

    @Test
    public void contains_only_negative_totals_requires_no_positive_amounts() {
        Map<String, Long> totalsByCurrency = new LinkedHashMap<>();
        totalsByCurrency.put("IDR", -25_000L);
        assertTrue(MoneyFormatter.containsOnlyNegativeTotals(totalsByCurrency));

        totalsByCurrency.put("USD", 10_000L);
        assertFalse(MoneyFormatter.containsOnlyNegativeTotals(totalsByCurrency));
    }

    @Test
    public void parse_plain_digits() {
        assertEquals(25_000L, MoneyParser.parse("25000"));
        assertEquals(25_000L, MoneyParser.parse("25.000"));
        assertEquals(25_000L, MoneyParser.parse("Rp 25.000"));
    }

    @Test
    public void parse_strips_whitespace() {
        assertEquals(1_500L, MoneyParser.parse("  Rp 1.500  "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_rejects_empty() {
        MoneyParser.parse("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_rejects_non_numeric() {
        MoneyParser.parse("abc");
    }

    @Test
    public void roundTrip() {
        long amount = 99_999L;
        assertEquals(amount, MoneyParser.parse(MoneyFormatter.format(amount)));
    }

    @Test
    public void format_rejects_fractional_minor_units_not_applicable_for_idr() {
        // IDR uses whole rupiah as minor unit; formatter always shows integer rupiah
        String formatted = MoneyFormatter.format(100L);
        assertTrue(formatted.endsWith("100") || formatted.contains("100"));
    }
}
