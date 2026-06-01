package com.dwlhm.finan.ui.common.infinitescroll;

import androidx.annotation.Nullable;

import com.dwlhm.finan.domain.model.PageResult;

/** Loads pages for cursor-based infinite scroll ({@code null} cursor = first page). */
public interface PageLoader<T, C> {

  PageResult<T, C> loadPage(@Nullable C cursor);
}
