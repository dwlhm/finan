package com.dwlhm.finan.data.entity;

public final class Tag {

  private final long id;
  private final String name;
  private final int usageCount;
  private final Long lastUsedAt;

  public Tag(long id, String name, int usageCount, Long lastUsedAt) {
    this.id = id;
    this.name = name;
    this.usageCount = usageCount;
    this.lastUsedAt = lastUsedAt;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getUsageCount() {
    return usageCount;
  }

  public Long getLastUsedAt() {
    return lastUsedAt;
  }
}
