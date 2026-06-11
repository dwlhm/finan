package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.dao.TagDao;
import com.dwlhm.finan.data.entity.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TagSelectionController {

  private final Context context;
  private final TagDao tagDao;
  private final DbWorker dbWorker;
  private final LinearLayout chipContainer;
  private final Button addButton;

  private final Map<Long, Tag> selectedById = new LinkedHashMap<>();

  public TagSelectionController(
      @NonNull Context context,
      @NonNull TagDao tagDao,
      @NonNull DbWorker dbWorker,
      @NonNull LinearLayout chipContainer,
      @NonNull Button addButton) {
    this.context = context;
    this.tagDao = tagDao;
    this.dbWorker = dbWorker;
    this.chipContainer = chipContainer;
    this.addButton = addButton;
    addButton.setOnClickListener(v -> openSearchDialog());
  }

  public void setSelectedTags(@NonNull List<Tag> tags) {
    selectedById.clear();
    for (Tag tag : tags) {
      selectedById.put(tag.getId(), tag);
    }
    renderChips();
  }

  public void setSelectedTagIds(@NonNull List<Long> tagIds) {
    selectedById.clear();
    for (Long tagId : tagIds) {
      if (tagId == null || tagId <= 0L) {
        continue;
      }
      Tag tag = tagDao.findById(tagId);
      if (tag != null) {
        selectedById.put(tag.getId(), tag);
      }
    }
    renderChips();
  }

  @NonNull
  public List<Long> getSelectedTagIds() {
    return new ArrayList<>(selectedById.keySet());
  }

  public void clear() {
    selectedById.clear();
    renderChips();
  }

  private void openSearchDialog() {
    Set<Long> exclude = new HashSet<>(selectedById.keySet());
    NamedEntitySearchDialog<Tag> dialog =
        new NamedEntitySearchDialog<>(
            context,
            new NamedEntitySearchDialog.EntityAccess<Tag>() {
              @Override
              public List<Tag> loadAll() {
                return tagDao.findAllOrderByUsage();
              }

              @Override
              public Tag findByNameIgnoreCase(String name) {
                return tagDao.findByNameIgnoreCase(name);
              }

              @Override
              public Tag insertIfAbsent(String name) {
                return tagDao.insertIfAbsent(name);
              }

              @Override
              public String nameOf(Tag entity) {
                return entity.getName();
              }

              @Override
              public long idOf(Tag entity) {
                return entity.getId();
              }
            },
            dbWorker,
            (tag, created) -> {
              selectedById.put(tag.getId(), tag);
              renderChips();
            },
            R.string.capture_tag_search_title,
            R.string.capture_tag_search_hint,
            R.string.capture_tag_create_from_search,
            R.string.capture_tag_add_new,
            R.string.capture_tag_add_new_dialog_title,
            R.string.capture_tag_name_hint,
            R.string.capture_tag_created,
            R.string.capture_tag_name_empty,
            R.string.capture_tag_already_exists,
            exclude);
    dialog.show();
  }

  private void renderChips() {
    chipContainer.removeAllViews();
    for (Tag tag : selectedById.values()) {
      Button chip = new Button(context, null, android.R.attr.borderlessButtonStyle);
      chip.setText(tag.getName() + "  ×");
      UiComponentStyles.prepareChip(chip);
      chip.setMinHeight(UiComponentStyles.dp(context, 32));
      chip.setTextSize(12f);
      UiComponentStyles.setChipSelected(context, chip, true, R.drawable.bg_chip);
      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      params.setMarginEnd(UiComponentStyles.dp(context, 6));
      chip.setLayoutParams(params);
      chip.setOnClickListener(v -> removeTag(tag.getId()));
      chipContainer.addView(chip);
    }
    chipContainer.setVisibility(selectedById.isEmpty() ? View.GONE : View.VISIBLE);
  }

  private void removeTag(long tagId) {
    selectedById.remove(tagId);
    renderChips();
  }
}
