package com.dwlhm.finan.service.transaction;

import static org.junit.Assert.assertEquals;

import com.dwlhm.finan.data.entity.Merchant;
import com.dwlhm.finan.domain.model.HistorySearch;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TransactionSearchResolverTest {

  @Test
  public void resolve_findsTypoCandidateAndParsesFormattedAmount() {
    Merchant starbucks = new Merchant(7L, "Starbucks", 1, null);
    TransactionSearchResolver resolver =
        new TransactionSearchResolver(
            List.of(), List.of(), List.of(starbucks), List.of());

    HistorySearch typo = resolver.resolve("starbuks");
    HistorySearch amount = resolver.resolve("Rp 25.000");

    assertEquals(List.of(7L), typo.merchantIds());
    assertEquals(Long.valueOf(25_000L), amount.amountMinor());
  }

  @Test
  public void resolve_keepsAllMatchingEntityIds() {
    List<Merchant> merchants = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      merchants.add(new Merchant(i, "Merchant Kopi " + i, 0, null));
    }

    HistorySearch result =
        new TransactionSearchResolver(List.of(), List.of(), merchants, List.of()).resolve("kopi");

    assertEquals(50, result.merchantIds().size());
  }
}
