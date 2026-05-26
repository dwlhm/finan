package com.dwlhm.finan.data.dao;

import java.util.ArrayList;
import java.util.List;

public final class SqliteCategoryDao implements CategoryGateway {

  private final CategoryDao table;

  public SqliteCategoryDao(CategoryDao table) {
    this.table = table;
  }

  @Override
  public void bumpUsage(long categoryId) {
    table.incrementUsage(categoryId, System.currentTimeMillis());
  }

  @Override
  public List<com.dwlhm.finan.domain.model.Category> findAll() {
    List<com.dwlhm.finan.domain.model.Category> result = new ArrayList<>();
    for (com.dwlhm.finan.data.entity.Category entity : table.findAllOrdered()) {
      result.add(toDomain(entity));
    }
    return result;
  }

  private static com.dwlhm.finan.domain.model.Category toDomain(
      com.dwlhm.finan.data.entity.Category entity) {
    return new com.dwlhm.finan.domain.model.Category(
        entity.getId(), entity.getName(), entity.getUsageCount());
  }
}
