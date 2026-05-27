package com.dwlhm.finan.util.money;

import java.util.Locale;
import java.util.Map;

public final class MoneyFormatter {

    public static final String DEFAULT_CURRENCY_CODE = "IDR";

    private MoneyFormatter() {
    }

    public static String format(long amountMinor) {
        boolean negative = amountMinor < 0;
        long absolute = negative ? -amountMinor : amountMinor;
        String digits = formatGrouped(absolute);
        if (negative) {
            return "-Rp " + digits;
        }
        return "Rp " + digits;
    }

    public static String formatWithoutCurrency(long amountMinor) {
        boolean negative = amountMinor < 0;
        long absolute = negative ? -amountMinor : amountMinor;
        String digits = formatGrouped(absolute);
        return negative ? "-" + digits : digits;
    }

    public static String formatWithCurrencyCode(String currencyCode, long amountMinor) {
        String normalizedCurrencyCode = normalizeCurrencyCode(currencyCode);
        if (DEFAULT_CURRENCY_CODE.equals(normalizedCurrencyCode)) {
            return format(amountMinor);
        }
        boolean negative = amountMinor < 0;
        long absolute = negative ? -amountMinor : amountMinor;
        String digits = formatGrouped(absolute);
        return (negative ? "-" : "") + normalizedCurrencyCode + " " + digits;
    }

    public static String formatTotalsByCurrency(Map<String, Long> totalsByCurrency) {
        StringBuilder totals = new StringBuilder();
        if (totalsByCurrency == null) {
            return totals.toString();
        }
        for (Map.Entry<String, Long> entry : totalsByCurrency.entrySet()) {
            if (totals.length() > 0) {
                totals.append('\n');
            }
            String currencyCode = normalizeCurrencyCode(entry.getKey());
            if (totalsByCurrency.size() > 1) {
                totals.append(currencyCode).append(": ");
            }
            Long amountMinor = entry.getValue();
            long safeAmountMinor = amountMinor == null ? 0L : amountMinor;
            totals.append(formatWithCurrencyCode(currencyCode, safeAmountMinor));
        }
        return totals.toString();
    }

    public static boolean containsOnlyNegativeTotals(Map<String, Long> totalsByCurrency) {
        boolean hasNegative = false;
        if (totalsByCurrency == null) {
            return false;
        }
        for (Long total : totalsByCurrency.values()) {
            if (total != null && total < 0) {
                hasNegative = true;
            } else if (total != null && total > 0) {
                return false;
            }
        }
        return hasNegative;
    }

    public static String normalizeCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return DEFAULT_CURRENCY_CODE;
        }
        return currencyCode.trim().toUpperCase(Locale.US);
    }

    static String formatGrouped(long amount) {
        String raw = Long.toString(amount);
        StringBuilder grouped = new StringBuilder();
        int len = raw.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                grouped.append('.');
            }
            grouped.append(raw.charAt(i));
        }
        return grouped.toString();
    }
}
