package com.dwlhm.finan.util.date;

public final class SystemTimeProvider implements TimeProvider {

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
