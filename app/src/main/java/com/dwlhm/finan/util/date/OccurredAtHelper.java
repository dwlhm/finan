package com.dwlhm.finan.util.date;

public final class OccurredAtHelper {

    private OccurredAtHelper() {
    }

    public static long resolve(long occurredAt, TimeProvider timeProvider) {
        if (occurredAt > 0L) {
            return occurredAt;
        }
        return timeProvider.currentTimeMillis();
    }
}
