package com.dwlhm.finan.domain.model;

public class Tag {

  private long id;
  private String name;
  private int usageCount;

  public Tag(long id, String name, int usageCount) {
    this.id = id;
    this.name = name;
    this.usageCount = usageCount;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getUsageCount() {
    return usageCount;
  }

  public void setUsageCount(int usageCount) {
    this.usageCount = usageCount;
  }
}
