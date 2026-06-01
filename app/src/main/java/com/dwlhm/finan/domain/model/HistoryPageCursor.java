package com.dwlhm.finan.domain.model;

/** Keyset cursor for history pagination (occurred_at + id). */
public record HistoryPageCursor(long occurredAt, long id) {}
