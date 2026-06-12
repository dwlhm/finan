package com.dwlhm.finan.util.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class FuzzySearch {

  private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  private FuzzySearch() {}

  public static <T> Index<T> index(List<T> values, Function<T, String> nameOf) {
    return new Index<>(values, nameOf);
  }

  public static final class Index<T> {

    private final List<Entry<T>> entries;

    private Index(List<T> values, Function<T, String> nameOf) {
      if (values == null || values.isEmpty()) {
        entries = List.of();
        return;
      }
      List<Entry<T>> indexed = new ArrayList<>(values.size());
      for (int i = 0; i < values.size(); i++) {
        T value = values.get(i);
        String normalized = normalize(nameOf.apply(value));
        indexed.add(new Entry<>(value, normalized, words(normalized), i));
      }
      entries = List.copyOf(indexed);
    }

    public List<T> matching(String query) {
      String normalizedQuery = normalize(query);
      if (normalizedQuery.isEmpty() || entries.isEmpty()) {
        return List.of();
      }
      String[] queryWords = words(normalizedQuery);
      DistanceWorkspace workspace = new DistanceWorkspace();
      List<Match<T>> matches = new ArrayList<>();
      for (Entry<T> entry : entries) {
        int score =
            score(
                normalizedQuery,
                queryWords,
                entry.normalized(),
                entry.words(),
                workspace);
        if (score >= 0) {
          matches.add(new Match<>(entry.value(), score, entry.order()));
        }
      }
      matches.sort(
          Comparator.comparingInt((Match<T> match) -> match.score())
              .thenComparingInt(Match::order));
      List<T> values = new ArrayList<>(matches.size());
      for (Match<T> match : matches) {
        values.add(match.value());
      }
      return values;
    }
  }

  private static int score(
      String query,
      String[] queryWords,
      String candidate,
      String[] candidateWords,
      DistanceWorkspace workspace) {
    if (query.isEmpty() || candidate.isEmpty()) {
      return -1;
    }
    if (candidate.equals(query)) {
      return 0;
    }
    if (candidate.contains(query)) {
      return 1;
    }
    int wholeTolerance = tolerance(query.length());
    int wholeDistance =
        Math.abs(query.length() - candidate.length()) <= wholeTolerance
            ? workspace.distance(query, candidate, wholeTolerance)
            : Integer.MAX_VALUE;
    if (wholeDistance <= wholeTolerance) {
      return 2 + wholeDistance;
    }

    int score = 2;
    for (String queryWord : queryWords) {
      int tolerance = tolerance(queryWord.length());
      if (tolerance == 0) {
        return -1;
      }
      int best = Integer.MAX_VALUE;
      for (String candidateWord : candidateWords) {
        if (candidateWord.contains(queryWord)) {
          best = 0;
          break;
        }
        if (Math.abs(queryWord.length() - candidateWord.length()) <= tolerance) {
          best = Math.min(best, workspace.distance(queryWord, candidateWord, tolerance));
        }
      }
      if (best > tolerance) {
        return -1;
      }
      score += best;
    }
    return score;
  }

  public static String normalize(String value) {
    if (value == null) {
      return "";
    }
    String normalized =
        DIACRITICS
            .matcher(Normalizer.normalize(value, Normalizer.Form.NFD))
            .replaceAll("")
            .toLowerCase(Locale.ROOT)
            .trim();
    return WHITESPACE.matcher(normalized).replaceAll(" ");
  }

  private static int tolerance(int length) {
    if (length < 4) {
      return 0;
    }
    return length < 8 ? 1 : 2;
  }

  private static String[] words(String value) {
    return value.indexOf(' ') < 0 ? new String[] {value} : value.split(" ");
  }

  private static final class DistanceWorkspace {

    private int[] first = new int[0];
    private int[] second = new int[0];
    private int[] third = new int[0];

    private int distance(String left, String right, int limit) {
      if (Math.abs(left.length() - right.length()) > limit) {
        return limit + 1;
      }
      ensureCapacity(right.length() + 1);
      int unavailable = limit + 1;
      int[] previousPrevious = first;
      int[] previous = second;
      int[] current = third;
      for (int j = 0; j <= right.length(); j++) {
        previous[j] = j <= limit ? j : unavailable;
      }
      for (int i = 1; i <= left.length(); i++) {
        Arrays.fill(current, 0, right.length() + 1, unavailable);
        if (i <= limit) {
          current[0] = i;
        }
        int from = Math.max(1, i - limit);
        int to = Math.min(right.length(), i + limit);
        int rowMinimum = unavailable;
        for (int j = from; j <= to; j++) {
          int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
          int best =
              Math.min(
                  Math.min(previous[j] + 1, current[j - 1] + 1),
                  previous[j - 1] + cost);
          if (i > 1
              && j > 1
              && left.charAt(i - 1) == right.charAt(j - 2)
              && left.charAt(i - 2) == right.charAt(j - 1)) {
            best = Math.min(best, previousPrevious[j - 2] + 1);
          }
          current[j] = Math.min(best, unavailable);
          rowMinimum = Math.min(rowMinimum, current[j]);
        }
        if (rowMinimum > limit) {
          return unavailable;
        }
        int[] swap = previousPrevious;
        previousPrevious = previous;
        previous = current;
        current = swap;
      }
      return previous[right.length()];
    }

    private void ensureCapacity(int capacity) {
      if (first.length >= capacity) {
        return;
      }
      first = new int[capacity];
      second = new int[capacity];
      third = new int[capacity];
    }
  }

  private record Entry<T>(T value, String normalized, String[] words, int order) {}

  private record Match<T>(T value, int score, int order) {}
}
