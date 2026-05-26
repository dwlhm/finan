package com.dwlhm.finan.util.money;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
