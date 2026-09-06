package com.dwlhm.finan.data.migration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Migration012Test {

  @Test
  public void version_isTwelve() {
    Migration012RecurringSchedule migration = new Migration012RecurringSchedule();
    assertEquals(12, migration.getVersion());
  }

  @Test
  public void migrationInstance_isNotNull() {
    Migration012RecurringSchedule migration = new Migration012RecurringSchedule();
    assertNotNull(migration);
  }
}
