package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.domain.model.Tag;

import java.util.List;

public interface TagGateway {

  void bumpUsage(long tagId);

  List<Tag> findAll();
}
