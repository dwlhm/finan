package com.dwlhm.finan.ui.common;

import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Merchant;
import com.dwlhm.finan.data.entity.Tag;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;

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

  public static Map<Long, Tag> indexTags(List<Tag> tags) {
    if (tags == null || tags.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, Tag> indexed = new HashMap<>(tags.size());
    for (Tag tag : tags) {
      indexed.put(tag.getId(), tag);
    }
    return indexed;
  }

  public static Map<Long, Merchant> indexMerchants(List<Merchant> merchants) {
    if (merchants == null || merchants.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, Merchant> indexed = new HashMap<>(merchants.size());
    for (Merchant merchant : merchants) {
      indexed.put(merchant.getId(), merchant);
    }
    return indexed;
  }

  public static Map<Long, Tag> tagLookupForTransactions(
      List<Tag> allTags, List<Transaction> transactions, TagResolver resolver) {
    Map<Long, Tag> lookup = new HashMap<>(indexTags(allTags));
    for (Transaction transaction : transactions) {
      for (Long tagId : transaction.getTagIds()) {
        if (tagId == null || tagId <= 0L || lookup.containsKey(tagId)) {
          continue;
        }
        Tag tag = resolver.findById(tagId);
        if (tag != null) {
          lookup.put(tagId, tag);
        }
      }
    }
    return lookup;
  }

  public static Map<Long, Merchant> merchantLookupForTransactions(
      List<Merchant> allMerchants, List<Transaction> transactions, MerchantResolver resolver) {
    Map<Long, Merchant> lookup = new HashMap<>(indexMerchants(allMerchants));
    for (Transaction transaction : transactions) {
      Long merchantId = transaction.getMerchantId();
      if (merchantId == null || merchantId <= 0L || lookup.containsKey(merchantId)) {
        continue;
      }
      Merchant merchant = resolver.findById(merchantId);
      if (merchant != null) {
        lookup.put(merchantId, merchant);
      }
    }
    return lookup;
  }

  public interface TagResolver {
    Tag findById(long tagId);
  }

  public interface MerchantResolver {
    Merchant findById(long merchantId);
  }
}
