package com.dwlhm.finan.data.migration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Migration013Test {

  @Test
  public void version_isThirteen() {
    Migration013Indexes migration = new Migration013Indexes();
    assertEquals(13, migration.getVersion());
  }

  @Test
  public void migrationInstance_isNotNull() {
    Migration013Indexes migration = new Migration013Indexes();
    assertNotNull(migration);
  }

  @Test
  public void statements_areIdempotent() {
    assertTrue(Migration013Indexes.SQL_IDX_TRANSACTIONS_CAT_OCCURRED
        .contains("IF NOT EXISTS"));
    assertTrue(Migration013Indexes.SQL_IDX_TRANSACTIONS_WALLET_CAT_OCCURRED
        .contains("IF NOT EXISTS"));
  }

  @Test
  public void statements_targetExpectedIndexes() {
    assertTrue(Migration013Indexes.SQL_IDX_TRANSACTIONS_CAT_OCCURRED
        .contains(Migration013Indexes.IDX_TRANSACTIONS_CAT_OCCURRED));
    assertTrue(Migration013Indexes.SQL_IDX_TRANSACTIONS_WALLET_CAT_OCCURRED
        .contains(Migration013Indexes.IDX_TRANSACTIONS_WALLET_CAT_OCCURRED));
  }
}
