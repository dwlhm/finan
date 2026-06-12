package com.dwlhm.finan.service.transaction;

import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Merchant;
import com.dwlhm.finan.data.entity.Tag;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.HistorySearch;
import com.dwlhm.finan.util.search.FuzzySearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToLongFunction;
import java.util.regex.Pattern;

public final class TransactionSearchResolver {

  private static final Pattern DIGITS = Pattern.compile("\\d+");

  private final FuzzySearch.Index<Wallet> wallets;
  private final FuzzySearch.Index<Category> categories;
  private final FuzzySearch.Index<Merchant> merchants;
  private final FuzzySearch.Index<Tag> tags;

  public TransactionSearchResolver(
      List<Wallet> wallets,
      List<Category> categories,
      List<Merchant> merchants,
      List<Tag> tags) {
    this.wallets = FuzzySearch.index(wallets, Wallet::getName);
    this.categories = FuzzySearch.index(categories, Category::getName);
    this.merchants = FuzzySearch.index(merchants, Merchant::getName);
    this.tags = FuzzySearch.index(tags, Tag::getName);
  }

  public HistorySearch resolve(String query) {
    String text = query == null ? "" : query.trim();
    if (text.isEmpty()) {
      return HistorySearch.empty();
    }
    return new HistorySearch(
        text,
        parseAmount(text),
        matchingIds(wallets, text, Wallet::getId),
        matchingIds(categories, text, Category::getId),
        matchingIds(merchants, text, Merchant::getId),
        matchingIds(tags, text, Tag::getId));
  }

  private static <T> List<Long> matchingIds(
      FuzzySearch.Index<T> index, String query, ToLongFunction<T> idOf) {
    List<T> matches = index.matching(query);
    List<Long> ids = new ArrayList<>(matches.size());
    for (T match : matches) {
      ids.add(idOf.applyAsLong(match));
    }
    return ids;
  }

  private static Long parseAmount(String query) {
    String normalized =
        query
            .toLowerCase(Locale.ROOT)
            .replace("idr", "")
            .replace("rp", "")
            .replace(".", "")
            .replace(",", "")
            .replace(" ", "");
    if (normalized.isEmpty() || !DIGITS.matcher(normalized).matches()) {
      return null;
    }
    try {
      return Long.parseLong(normalized);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
