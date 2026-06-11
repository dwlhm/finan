package com.dwlhm.finan.service.tag;

import com.dwlhm.finan.data.dao.TagGateway;

import java.util.List;

public class TagUsageService {

  private final TagGateway tagGateway;

  public TagUsageService(TagGateway tagGateway) {
    this.tagGateway = tagGateway;
  }

  public void bumpUsageForTags(List<Long> tagIds) {
    if (tagIds == null) {
      return;
    }
    for (Long tagId : tagIds) {
      if (tagId != null && tagId > 0L) {
        tagGateway.bumpUsage(tagId);
      }
    }
  }
}
