package com.dwlhm.finan.data.dao;

import java.util.ArrayList;
import java.util.List;

public final class SqliteTagDao implements TagGateway {

  private final TagDao table;

  public SqliteTagDao(TagDao table) {
    this.table = table;
  }

  @Override
  public void bumpUsage(long tagId) {
    table.incrementUsage(tagId, System.currentTimeMillis());
  }

  @Override
  public List<com.dwlhm.finan.domain.model.Tag> findAll() {
    List<com.dwlhm.finan.domain.model.Tag> result = new ArrayList<>();
    for (com.dwlhm.finan.data.entity.Tag entity : table.findAllOrderByUsage()) {
      result.add(toDomain(entity));
    }
    return result;
  }

  private static com.dwlhm.finan.domain.model.Tag toDomain(com.dwlhm.finan.data.entity.Tag entity) {
    return new com.dwlhm.finan.domain.model.Tag(
        entity.getId(), entity.getName(), entity.getUsageCount());
  }
}
