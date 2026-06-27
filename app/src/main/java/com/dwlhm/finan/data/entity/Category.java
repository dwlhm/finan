package com.dwlhm.finan.data.entity;

public final class Category {

  private final long id;
  private final String name;
  private final String typeFilter;
  private final int sortOrder;
  private final int usageCount;
  private final Long lastUsedAt;
  private final String cashFlowActivity;

  public Category(
      long id,
      String name,
      String typeFilter,
      int sortOrder,
      int usageCount,
      Long lastUsedAt,
      String cashFlowActivity) {
    this.id = id;
    this.name = name;
    this.typeFilter = typeFilter;
    this.sortOrder = sortOrder;
    this.usageCount = usageCount;
    this.lastUsedAt = lastUsedAt;
    this.cashFlowActivity = cashFlowActivity;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getTypeFilter() {
    return typeFilter;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public int getUsageCount() {
    return usageCount;
  }

  public Long getLastUsedAt() {
    return lastUsedAt;
  }

  public String getCashFlowActivity() {
    return cashFlowActivity;
  }
}
