package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.domain.model.TransactionType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransactionTemplateDaoTest {

  @Test
  public void tableNameMatchesExpected() {
    assertEquals("transaction_templates", TransactionTemplateDao.TABLE_NAME);
  }

  @Test
  public void createsValidTemplateObject() {
    TransactionTemplate template = new TransactionTemplate(
        10L,
        "Gaji",
        TransactionType.INCOME,
        1500000000L,
        null,
        1L,
        null,
        "Gaji Bulanan PT X",
        "💰",
        5
    );

    assertEquals(10L, template.getId());
    assertEquals("Gaji", template.getName());
    assertEquals(TransactionType.INCOME, template.getType());
    assertEquals(1500000000L, template.getAmountMinor());
    assertEquals(Long.valueOf(1L), template.getWalletId());
    assertEquals("Gaji Bulanan PT X", template.getNote());
    assertEquals("💰", template.getIcon());
    assertEquals(5, template.getSortOrder());
  }
}
