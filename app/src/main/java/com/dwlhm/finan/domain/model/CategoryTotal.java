package com.dwlhm.finan.domain.model;

public final class CategoryTotal {

  private final long categoryId;
  private final String categoryName;
  private final long totalMinor;
  private final boolean isIncome;

  public CategoryTotal(long categoryId, String categoryName, long totalMinor, boolean isIncome) {
    this.categoryId = categoryId;
    this.categoryName = categoryName;
    this.totalMinor = totalMinor;
    this.isIncome = isIncome;
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

  public boolean isIncome() {
    return isIncome;
  }
}
