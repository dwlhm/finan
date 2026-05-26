package com.dwlhm.finan.domain.model;

public final class CategoryTotal {

  private final long categoryId;
  private final String categoryName;
  private final long totalMinor;

  public CategoryTotal(long categoryId, String categoryName, long totalMinor) {
    this.categoryId = categoryId;
    this.categoryName = categoryName;
    this.totalMinor = totalMinor;
  }

  public long getCategoryId() {
    return categoryId;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public long getTotalMinor() {
    return totalMinor;
  }
}
