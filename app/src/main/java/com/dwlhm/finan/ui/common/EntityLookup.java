package com.dwlhm.finan.ui.common;

import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityLookup {

  private EntityLookup() {}

  public static Map<Long, Category> indexCategories(List<Category> categories) {
    if (categories == null || categories.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, Category> indexed = new HashMap<>(categories.size());
    for (Category category : categories) {
      indexed.put(category.getId(), category);
    }
    return indexed;
  }

  public static Map<Long, Wallet> indexWallets(List<Wallet> wallets) {
    if (wallets == null || wallets.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, Wallet> indexed = new HashMap<>(wallets.size());
    for (Wallet wallet : wallets) {
      indexed.put(wallet.getId(), wallet);
    }
    return indexed;
  }


}
