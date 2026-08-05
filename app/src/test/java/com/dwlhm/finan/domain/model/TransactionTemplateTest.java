package com.dwlhm.finan.domain.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TransactionTemplateTest {

  @Test
  public void createsTemplateWithDefaults() {
    TransactionTemplate template = new TransactionTemplate(
        1L,
        "Kopi Pagi",
        TransactionType.EXPENSE,
        1800000L,
        2L,
        3L,
        null,
        "Kopi Janji Jiwa",
        "☕",
        1
    );

    assertEquals(1L, template.getId());
    assertEquals("Kopi Pagi", template.getName());
    assertEquals(TransactionType.EXPENSE, template.getType());
    assertEquals(1800000L, template.getAmountMinor());
    assertEquals(Long.valueOf(2L), template.getCategoryId());
    assertEquals(Long.valueOf(3L), template.getWalletId());
    assertNull(template.getDestinationWalletId());
    assertEquals("Kopi Janji Jiwa", template.getNote());
    assertEquals("☕", template.getIcon());
    assertEquals(1, template.getSortOrder());
  }

  @Test
  public void fallbackIconWhenEmpty() {
    TransactionTemplate template = new TransactionTemplate(
        "Bensin",
        TransactionType.EXPENSE,
        3000000L,
        null,
        null,
        null,
        null,
        ""
    );

    assertEquals("⚡", template.getIcon());
  }

  @Test
  public void supportsSetters() {
    TransactionTemplate template = new TransactionTemplate(
        "Test",
        TransactionType.INCOME,
        5000000L,
        null,
        null,
        null,
        null,
        "⚡"
    );

    template.setName("Gaji");
    template.setAmountMinor(1000000000L);
    template.setType(TransactionType.INCOME);
    template.setCategoryId(5L);
    template.setWalletId(1L);
    template.setNote("Gaji Bulanan");
    template.setIcon("💰");
    template.setSortOrder(10);

    assertEquals("Gaji", template.getName());
    assertEquals(1000000000L, template.getAmountMinor());
    assertEquals(TransactionType.INCOME, template.getType());
    assertEquals(Long.valueOf(5L), template.getCategoryId());
    assertEquals(Long.valueOf(1L), template.getWalletId());
    assertEquals("Gaji Bulanan", template.getNote());
    assertEquals("💰", template.getIcon());
    assertEquals(10, template.getSortOrder());
  }
}
