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
  @Test
  public void recordReader_preservesMultilineAndNextSection() throws Exception {
    java.io.PushbackReader reader = new java.io.PushbackReader(new java.io.StringReader(
        "1,\"first\r\nWALLETS, \"\"quote\"\"\"\r\nCATEGORIES\n"));
    assertEquals(List.of("1", "first\r\nWALLETS, \"quote\""),
        ImportService.parseCsvLine(ImportService.readCsvRecord(reader)));
    assertEquals("CATEGORIES", ImportService.readCsvRecord(reader));
    org.junit.Assert.assertNull(ImportService.readCsvRecord(reader));
  }

  @Test
  public void malformedQuotesAreRejected() {
    for (String line : List.of("a\"b", "\"a\"tail", "\"unfinished")) {
      org.junit.Assert.assertThrows(IllegalArgumentException.class,
          () -> ImportService.parseCsvLine(line));
    }
  }

  @Test
  public void recordReaderRejectsTruncationAndOversize() {
    org.junit.Assert.assertThrows(java.io.IOException.class, () -> ImportService.readCsvRecord(
        new java.io.PushbackReader(new java.io.StringReader("\"unfinished\n"))));
    org.junit.Assert.assertThrows(java.io.IOException.class, () -> ImportService.readCsvRecord(
        new java.io.PushbackReader(new java.io.StringReader("x".repeat(1024 * 1024 + 1)))));
  }
}
