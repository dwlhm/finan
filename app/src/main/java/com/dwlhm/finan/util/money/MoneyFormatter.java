package com.dwlhm.finan.util.money;

public final class MoneyFormatter {

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

    private static String formatGrouped(long amount) {
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
