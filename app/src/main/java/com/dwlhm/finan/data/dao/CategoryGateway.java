package com.dwlhm.finan.data.dao;

import com.dwlhm.finan.domain.model.Category;

import java.util.List;

public interface CategoryGateway {

    void bumpUsage(long categoryId);

    List<Category> findAll();
}
