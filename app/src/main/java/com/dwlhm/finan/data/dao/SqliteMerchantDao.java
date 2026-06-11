package com.dwlhm.finan.data.dao;

import java.util.ArrayList;
import java.util.List;

public final class SqliteMerchantDao implements MerchantGateway {

  private final MerchantDao table;

  public SqliteMerchantDao(MerchantDao table) {
    this.table = table;
  }

  @Override
  public void bumpUsage(long merchantId) {
    table.incrementUsage(merchantId, System.currentTimeMillis());
  }

  @Override
  public List<com.dwlhm.finan.domain.model.Merchant> findAll() {
    List<com.dwlhm.finan.domain.model.Merchant> result = new ArrayList<>();
    for (com.dwlhm.finan.data.entity.Merchant entity : table.findAllOrderByUsage()) {
      result.add(toDomain(entity));
    }
    return result;
  }

  private static com.dwlhm.finan.domain.model.Merchant toDomain(
      com.dwlhm.finan.data.entity.Merchant entity) {
    return new com.dwlhm.finan.domain.model.Merchant(
        entity.getId(), entity.getName(), entity.getUsageCount());
  }
}
