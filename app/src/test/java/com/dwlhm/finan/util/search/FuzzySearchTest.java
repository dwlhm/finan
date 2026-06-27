package com.dwlhm.finan.util.search;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FuzzySearchTest {

  @Test
  public void index_handlesMissingAndTransposedCharacters() {
    List<Value> values =
        List.of(
            new Value(1L, "Starbucks"),
            new Value(2L, "Transport"),
            new Value(3L, "Makanan"));
    FuzzySearch.Index<Value> index = FuzzySearch.index(values, Value::name);

    assertEquals(List.of(values.get(0)), index.matching("starbuks"));
    assertEquals(List.of(values.get(1)), index.matching("transprot"));
    assertEquals(List.of(values.get(2)), index.matching("maknan"));
  }

  @Test
  public void index_toleratesTyposInShortNames() {
    List<Value> values = List.of(new Value(1L, "Kopi"));
    FuzzySearch.Index<Value> index = FuzzySearch.index(values, Value::name);

    assertEquals(values, index.matching("kpi"));
    assertEquals(values, index.matching("kopi"));
  }

  @Test
  public void index_matchesMultiWordNamesPerWord() {
    List<Value> values = List.of(new Value(1L, "Bank Jago"));

    assertEquals(values, FuzzySearch.index(values, Value::name).matching("bank jgoo"));
  }

  @Test
  public void index_doesNotDropMatchesAfterTwentyResults() {
    List<Value> values = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      values.add(new Value(i, "Merchant Kopi " + i));
    }

    assertEquals(50, FuzzySearch.index(values, Value::name).matching("kopi").size());
  }

  private record Value(long id, String name) {}
}
