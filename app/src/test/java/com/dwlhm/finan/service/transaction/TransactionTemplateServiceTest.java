package com.dwlhm.finan.service.transaction;

import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionTemplateServiceTest {

  @Test
  public void isValidTemplate_validatesCorrectly() {
    TransactionTemplate valid = new TransactionTemplate(
        1L, "Kopi", TransactionType.EXPENSE, 1500000L, 1L, 1L, null, "Note", "☕", 0
    );
    assertTrue(TransactionTemplateService.isValidTemplate(valid));

    TransactionTemplate invalidEmptyName = new TransactionTemplate(
        1L, "   ", TransactionType.EXPENSE, 1500000L, 1L, 1L, null, "Note", "☕", 0
    );
    assertFalse(TransactionTemplateService.isValidTemplate(invalidEmptyName));

    assertFalse(TransactionTemplateService.isValidTemplate(null));
  }

  @Test
  public void createFromInput_usesFallbackNameAndIcon() {
    TransactionTemplate template = TransactionTemplateService.createFromInput(
        "",
        TransactionType.EXPENSE,
        2500000L,
        2L,
        3L,
        null,
        "Makan Pagi",
        null
    );

    assertEquals("Makan Pagi", template.getName());
    assertEquals("⚡", template.getIcon());
    assertEquals(2500000L, template.getAmountMinor());
    assertEquals(Long.valueOf(2L), template.getCategoryId());
    assertEquals(Long.valueOf(3L), template.getWalletId());
  }

  @Test
  public void createFromInput_usesProvidedNameAndIcon() {
    TransactionTemplate template = TransactionTemplateService.createFromInput(
        "Bensin Motor",
        TransactionType.EXPENSE,
        3000000L,
        4L,
        1L,
        null,
        "Pertamax",
        "⛽"
    );

    assertEquals("Bensin Motor", template.getName());
    assertEquals("⛽", template.getIcon());
    assertEquals(3000000L, template.getAmountMinor());
    assertEquals("Pertamax", template.getNote());
  }
}
