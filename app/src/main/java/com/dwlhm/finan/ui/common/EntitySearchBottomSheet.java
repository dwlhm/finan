package com.dwlhm.finan.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.function.Function;

public class EntitySearchBottomSheet<T> extends Dialog {

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

  private EditText searchInput;
  private SearchListAdapter listAdapter;

  public EntitySearchBottomSheet(
      @NonNull Context context,
      @NonNull List<T> items,
      @Nullable Long selectedId,
      @NonNull ItemMapper<T> mapper,
      @NonNull Listener<T> listener) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.allItems = items;
    this.mapper = mapper;
    this.listener = listener;
    this.selectedId = selectedId;
    this.searchIndex = buildSearchIndex(items, mapper);
  }

  private FuzzySearch.Index<T> buildSearchIndex(List<T> items, ItemMapper<T> mapper) {
    return FuzzySearch.index(items, item -> mapper.getName(item));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_search_bottom_sheet);
    
    WindowManager.LayoutParams params = getWindow().getAttributes();
    params.width = WindowManager.LayoutParams.MATCH_PARENT;
    params.height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.7);
    params.gravity = Gravity.BOTTOM;
    getWindow().setAttributes(params);
    getWindow().setWindowAnimations(android.R.style.Animation_InputMethod);

    searchInput = findViewById(R.id.search_input);
    ListView listView = findViewById(R.id.search_list);

    listAdapter = new SearchListAdapter();
    listView.setAdapter(listAdapter);
    listAdapter.setItems(allItems);

    listView.setOnItemClickListener((parent, view, position, id) -> {
      T item = listAdapter.getItem(position);
      listener.onItemSelected(item);
      dismiss();
    });

    searchInput.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      @Override
      public void afterTextChanged(Editable s) {
        performSearch(s.toString().trim());
      }
    });
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
      ImageView checkView = convertView.findViewById(R.id.item_check);
      
      nameView.setText(mapper.getName(item));
      
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
