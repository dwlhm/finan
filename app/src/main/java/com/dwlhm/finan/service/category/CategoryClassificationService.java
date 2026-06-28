package com.dwlhm.finan.service.category;

import android.database.sqlite.SQLiteDatabase;

import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.dao.TransactionDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.CashFlowActivity;

public final class CategoryClassificationService {

  private final SQLiteDatabase db;
  private final CategoryDao categories;
  private final TransactionDao transactions;

  private static final String[] RANDOM_EMOJIS = {
    "🍔", "🚗", "💼", "🛒", "🎉", "✈️", "🏠", "🎁", "☕", "🎮", "💡", "🩺", "📚", "👕", "🐶"
  };

  private static String getRandomEmoji() {
    java.security.SecureRandom random = new java.security.SecureRandom();
    return RANDOM_EMOJIS[random.nextInt(RANDOM_EMOJIS.length)];
  }

  public CategoryClassificationService(
      SQLiteDatabase db, CategoryDao categories, TransactionDao transactions) {
    this.db = db;
    this.categories = categories;
    this.transactions = transactions;
  }

  public Category create(String name, String icon, String typeFilter, CashFlowActivity activity) {
    String normalizedName = requireName(name, 0L);
    String finalIcon = (icon == null || icon.trim().isEmpty()) ? getRandomEmoji() : icon.trim();
    db.beginTransaction();
    try {
      long id =
          categories.insert(
              normalizedName,
              finalIcon,
              requireType(typeFilter),
              categories.nextSortOrder(),
              0,
              null,
              requireActivity(activity).name());
      if (id <= 0L) {
        throw new IllegalStateException("Failed to create category");
      }
      Category category = categories.findById(id);
      db.setTransactionSuccessful();
      return category;
    } finally {
      db.endTransaction();
    }
  }

  public Category update(
      long id,
      String name,
      String icon,
      String typeFilter,
      CashFlowActivity activity,
      boolean includeHistory) {
    Category existing = categories.findById(id);
    if (existing == null) {
      throw new IllegalArgumentException("Category not found");
    }
    String normalizedName = requireName(name, id);
    String finalIcon = (icon == null || icon.trim().isEmpty()) ? getRandomEmoji() : icon.trim();
    CashFlowActivity normalizedActivity = requireActivity(activity);
    db.beginTransaction();
    try {
      if (!categories.update(id, normalizedName, finalIcon, requireType(typeFilter), normalizedActivity.name())) {
        throw new IllegalStateException("Failed to update category");
      }
      if (includeHistory && !existing.getCashFlowActivity().equals(normalizedActivity.name())) {
        transactions.updateCashFlowActivityForCategory(id, normalizedActivity.name());
      }
      Category category = categories.findById(id);
      db.setTransactionSuccessful();
      return category;
    } finally {
      db.endTransaction();
    }
  }

  private String requireName(String name, long currentId) {
    String normalized = name == null ? "" : name.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Category name is empty");
    }
    Category duplicate = categories.findByNameIgnoreCase(normalized);
    if (duplicate != null && duplicate.getId() != currentId) {
      throw new IllegalArgumentException("Category name already exists");
    }
    return normalized;
  }

  private static String requireType(String typeFilter) {
    if (!"EXPENSE".equals(typeFilter)
        && !"INCOME".equals(typeFilter)
        && !"BOTH".equals(typeFilter)) {
      throw new IllegalArgumentException("Invalid category type");
    }
    return typeFilter;
  }

  private static CashFlowActivity requireActivity(CashFlowActivity activity) {
    return activity == null ? CashFlowActivity.UNCLASSIFIED : activity;
  }
}
