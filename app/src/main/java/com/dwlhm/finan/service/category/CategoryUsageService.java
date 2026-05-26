package com.dwlhm.finan.service.category;

import com.dwlhm.finan.data.dao.CategoryGateway;
import com.dwlhm.finan.domain.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CategoryUsageService {

    private final CategoryGateway categoryDao;

    public CategoryUsageService(CategoryGateway categoryDao) {
        this.categoryDao = categoryDao;
    }

    public void bumpUsage(long categoryId) {
        categoryDao.bumpUsage(categoryId);
    }

    public List<Category> sortByUsage(List<Category> categories) {
        List<Category> sorted = new ArrayList<>(categories);
        Collections.sort(sorted, new Comparator<Category>() {
            @Override
            public int compare(Category left, Category right) {
                int byUsage = Integer.compare(right.getUsageCount(), left.getUsageCount());
                if (byUsage != 0) {
                    return byUsage;
                }
                return left.getName().compareToIgnoreCase(right.getName());
            }
        });
        return sorted;
    }

    public List<Category> findAllSortedByUsage() {
        return sortByUsage(categoryDao.findAll());
    }
}
