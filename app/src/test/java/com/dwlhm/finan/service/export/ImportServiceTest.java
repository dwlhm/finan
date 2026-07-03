package com.dwlhm.finan.service.export;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ImportServiceTest {

  @Test
  public void parseCsvLine_splits_simple_fields() {
    List<String> fields = ImportService.parseCsvLine("a,b,c");
    assertEquals(List.of("a", "b", "c"), fields);
  }

  @Test
  public void parseCsvLine_handles_quoted_field_with_comma() {
    List<String> fields = ImportService.parseCsvLine("1,\"hello, world\",3");
    assertEquals(List.of("1", "hello, world", "3"), fields);
  }

  @Test
  public void parseCsvLine_handles_escaped_quotes() {
    List<String> fields = ImportService.parseCsvLine("\"say \"\"hi\"\"\",42");
    assertEquals(List.of("say \"hi\"", "42"), fields);
  }

  @Test
  public void parseCsvLine_handles_empty_fields() {
    List<String> fields = ImportService.parseCsvLine("a,,c,");
    assertEquals(List.of("a", "", "c", ""), fields);
  }

  @Test
  public void parseCsvLine_handles_quoted_field_with_newline() {
    List<String> fields = ImportService.parseCsvLine("a,\"b\nc\",d");
    assertEquals(3, fields.size());
    assertEquals("a", fields.get(0));
    assertEquals("b\nc", fields.get(1));
    assertEquals("d", fields.get(2));
  }

  @Test
  public void parseCsvLine_handles_single_field() {
    List<String> fields = ImportService.parseCsvLine("hello");
    assertEquals(List.of("hello"), fields);
  }

  @Test
  public void parseCsvLine_handles_empty_string() {
    List<String> fields = ImportService.parseCsvLine("");
    assertEquals(List.of(""), fields);
  }

  @Test
  public void parseCsvLine_handles_quotes_around_regular_text() {
    List<String> fields = ImportService.parseCsvLine("\"hello\",world");
    assertEquals(List.of("hello", "world"), fields);
  }
}
