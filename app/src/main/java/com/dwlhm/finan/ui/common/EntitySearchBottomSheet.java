package com.dwlhm.finan.ui.common;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.util.search.FuzzySearch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EntitySearchBottomSheet<T> extends BottomSheetDialog {

  public interface ItemMapper<T> {
    String getName(T item);
    long getId(T item);
  }

  public interface Listener<T> {
    void onItemSelected(T item);
  }

  private final List<T> allItems;
  private final ItemMapper<T> mapper;
  private final Listener<T> listener;
  private final Long selectedId;
  private final FuzzySearch.Index<T> searchIndex;
  private final String actionText;
  private final Runnable actionListener;
  @Nullable private final Predicate<T> isDefaultCheck;

  private SearchListAdapter listAdapter;

  public EntitySearchBottomSheet(
      @NonNull Context context,
      @NonNull List<T> items,
      @Nullable Long selectedId,
      @NonNull ItemMapper<T> mapper,
      @NonNull Listener<T> listener) {
    this(context, items, selectedId, mapper, listener, null, null, null);
  }

  public EntitySearchBottomSheet(
      @NonNull Context context,
      @NonNull List<T> items,
      @Nullable Long selectedId,
      @NonNull ItemMapper<T> mapper,
      @NonNull Listener<T> listener,
      @Nullable String actionText,
      @Nullable Runnable actionListener) {
    this(context, items, selectedId, mapper, listener, actionText, actionListener, null);
  }

  public EntitySearchBottomSheet(
      @NonNull Context context,
      @NonNull List<T> items,
      @Nullable Long selectedId,
      @NonNull ItemMapper<T> mapper,
      @NonNull Listener<T> listener,
      @Nullable String actionText,
      @Nullable Runnable actionListener,
      @Nullable Predicate<T> isDefaultCheck) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.allItems = items;
    this.mapper = mapper;
    this.listener = listener;
    this.selectedId = selectedId;
    this.searchIndex = buildSearchIndex(items, mapper);
    this.actionText = actionText;
    this.actionListener = actionListener;
    this.isDefaultCheck = isDefaultCheck;
  }

  private FuzzySearch.Index<T> buildSearchIndex(List<T> items, ItemMapper<T> mapper) {
    return FuzzySearch.index(items, mapper::getName);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_search_bottom_sheet);

    Window window = getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      WindowManager.LayoutParams params = window.getAttributes();
      params.width = WindowManager.LayoutParams.MATCH_PARENT;
      params.height = WindowManager.LayoutParams.WRAP_CONTENT;
      params.gravity = Gravity.BOTTOM;
      window.setAttributes(params);
      window.setWindowAnimations(android.R.style.Animation_InputMethod);
    }

    EditText searchInput = findViewById(R.id.search_input);
    ListView listView = findViewById(R.id.search_list);

    if (listView != null) {
      if (actionText != null && actionListener != null) {
        View footerView = LayoutInflater.from(getContext()).inflate(R.layout.item_category_search_action, listView, false);
        TextView labelView = footerView.findViewById(R.id.category_action_label);
        if (labelView != null) {
          labelView.setText(actionText);
        }
        footerView.setOnClickListener(v -> {
          dismiss();
          actionListener.run();
        });
        listView.addFooterView(footerView);
      }

      listAdapter = new SearchListAdapter();
      listView.setAdapter(listAdapter);
      listAdapter.setItems(allItems);

      listView.setOnItemClickListener((parent, view, position, id) -> {
        if (position < listAdapter.getCount()) {
          T item = listAdapter.getItem(position);
          listener.onItemSelected(item);
          dismiss();
        }
      });
    }

    if (searchInput != null) {
      searchInput.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
          performSearch(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {}
      });
      BottomSheetHelper.makeDraggable(this);
    }
  }

  private void performSearch(String query) {
    if (query.isEmpty()) {
      listAdapter.setItems(allItems);
      return;
    }
    List<T> results = searchIndex.matching(query);
    listAdapter.setItems(results);
  }

  private class SearchListAdapter extends BaseAdapter {
    private final List<T> displayItems = new ArrayList<>();

    void setItems(List<T> newItems) {
      displayItems.clear();
      displayItems.addAll(newItems);
      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      return displayItems.size();
    }

    @Override
    public T getItem(int position) {
      return displayItems.get(position);
    }

    @Override
    public long getItemId(int position) {
      return mapper.getId(getItem(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_entity_search, parent, false);
      }
      
      T item = getItem(position);
      TextView nameView = convertView.findViewById(R.id.item_name);
      TextView defaultBadge = convertView.findViewById(R.id.item_default_badge);
      ImageView checkView = convertView.findViewById(R.id.item_check);
      
      nameView.setText(mapper.getName(item));
      
      boolean isDefault = isDefaultCheck != null && isDefaultCheck.test(item);
      defaultBadge.setVisibility(isDefault ? View.VISIBLE : View.GONE);
      
      boolean isSelected = selectedId != null && selectedId == mapper.getId(item);
      checkView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
      if (isSelected) {
          nameView.setTextColor(getContext().getColor(R.color.finan_primary));
      } else {
          nameView.setTextColor(getContext().getColor(R.color.finan_text_primary));
      }
      
      return convertView;
    }
  }
}
