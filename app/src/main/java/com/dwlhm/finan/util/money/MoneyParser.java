package com.dwlhm.finan.util.money;

public final class MoneyParser {

    private MoneyParser() {
    }

    public static long parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }
        String normalized = trimmed
                .replace("Rp", "")
                .replace("rp", "")
                .replace("IDR", "")
                .replace("idr", "")
                .replace(".", "")
                .replace(",", "")
                .replace(" ", "")
                .trim();
        if (normalized.isEmpty() || !normalized.matches("-?\\d+")) {
            throw new IllegalArgumentException("Invalid amount: " + input);
        }
        return Long.parseLong(normalized);
    }
}
