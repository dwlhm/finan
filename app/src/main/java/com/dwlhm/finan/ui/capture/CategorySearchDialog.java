package com.dwlhm.finan.ui.capture;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.TransactionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CategorySearchDialog extends Dialog {

  public interface Listener {
    void onCategoryChosen(Category category, boolean created);
  }

  private static final int VIEW_CATEGORY = 0;
  private static final int VIEW_CREATE_FROM_SEARCH = 1;
  private static final int VIEW_ADD_NEW = 2;

  private final CategoryDao categoryDao;
  private final TransactionType transactionType;
  private final List<Category> allCategories;
  private final Listener listener;

  private EditText searchInput;
  private SearchListAdapter listAdapter;

  public CategorySearchDialog(
      @NonNull Context context,
      CategoryDao categoryDao,
      TransactionType transactionType,
      List<Category> allCategories,
      Listener listener) {
    super(context);
    this.categoryDao = categoryDao;
    this.transactionType = transactionType;
    this.allCategories = new ArrayList<>(allCategories);
    this.listener = listener;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_category_search);
    if (getWindow() != null) {
      getWindow().setLayout(
          WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    searchInput = findViewById(R.id.category_search_input);
    ListView listView = findViewById(R.id.category_search_list);

    listAdapter = new SearchListAdapter();
    listView.setAdapter(listAdapter);

    searchInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            listAdapter.notifyDataSetChanged();
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });

    listView.setOnItemClickListener(
        (parent, view, position, id) -> listAdapter.onItemClick(position));

    searchInput.requestFocus();
    searchInput.post(this::showKeyboard);
  }

  private void showKeyboard() {
    InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }
  }

  private String currentQuery() {
    return searchInput.getText().toString().trim();
  }

  private List<Category> filteredCategories() {
    String query = currentQuery().toLowerCase(Locale.ROOT);
    List<Category> filtered = new ArrayList<>();
    for (Category category : allCategories) {
      if (query.isEmpty()
          || category.getName().toLowerCase(Locale.ROOT).contains(query)) {
        filtered.add(category);
      }
    }
    return filtered;
  }

  private boolean hasExactMatch(String query) {
    if (query.isEmpty()) {
      return false;
    }
    for (Category category : allCategories) {
      if (category.getName().equalsIgnoreCase(query)) {
        return true;
      }
    }
    return false;
  }

  private void chooseCategory(Category category, boolean created) {
    listener.onCategoryChosen(category, created);
    dismiss();
  }

  private void createFromSearchQuery() {
    String query = currentQuery();
    if (query.isEmpty()) {
      Toast.makeText(getContext(), R.string.capture_category_name_empty, Toast.LENGTH_SHORT).show();
      return;
    }
    Category existing = categoryDao.findByNameIgnoreCase(query);
    if (existing != null) {
      Toast.makeText(getContext(), R.string.capture_category_already_exists, Toast.LENGTH_SHORT)
          .show();
      chooseCategory(existing, false);
      return;
    }
    Category created =
        categoryDao.insertForTransactionType(query, transactionType.name());
    Toast.makeText(getContext(), R.string.capture_category_created, Toast.LENGTH_SHORT).show();
    chooseCategory(created, true);
  }

  private void showAddNewDialog() {
    View dialogView =
        LayoutInflater.from(getContext()).inflate(R.layout.dialog_category_name_input, null);
    EditText nameInput = dialogView.findViewById(R.id.category_name_input);
    String prefill = currentQuery();
    if (!prefill.isEmpty()) {
      nameInput.setText(prefill);
      nameInput.setSelection(prefill.length());
    }

    new AlertDialog.Builder(getContext())
        .setTitle(R.string.capture_category_add_new_dialog_title)
        .setView(dialogView)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              String name = nameInput.getText().toString().trim();
              if (name.isEmpty()) {
                Toast.makeText(
                        getContext(), R.string.capture_category_name_empty, Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              Category existing = categoryDao.findByNameIgnoreCase(name);
              if (existing != null) {
                Toast.makeText(
                        getContext(), R.string.capture_category_already_exists, Toast.LENGTH_SHORT)
                    .show();
                chooseCategory(existing, false);
                return;
              }
              Category created =
                  categoryDao.insertForTransactionType(name, transactionType.name());
              Toast.makeText(getContext(), R.string.capture_category_created, Toast.LENGTH_SHORT)
                  .show();
              chooseCategory(created, true);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private final class SearchListAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      int count = filteredCategories().size();
      if (shouldShowCreateFromSearch()) {
        count++;
      }
      count++; // add new at end
      return count;
    }

    @Override
    public int getItemViewType(int position) {
      if (shouldShowCreateFromSearch() && position == 0) {
        return VIEW_CREATE_FROM_SEARCH;
      }
      if (position == getCount() - 1) {
        return VIEW_ADD_NEW;
      }
      return VIEW_CATEGORY;
    }

    @Override
    public int getViewTypeCount() {
      return 3;
    }

    @Override
    public Object getItem(int position) {
      return null;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      int type = getItemViewType(position);
      if (type == VIEW_CREATE_FROM_SEARCH) {
        return bindActionRow(
            convertView,
            parent,
            getContext()
                .getString(R.string.capture_category_create_from_search, currentQuery()));
      }
      if (type == VIEW_ADD_NEW) {
        return bindActionRow(
            convertView, parent, getContext().getString(R.string.capture_category_add_new));
      }
      int categoryIndex = shouldShowCreateFromSearch() ? position - 1 : position;
      Category category = filteredCategories().get(categoryIndex);
      return bindCategoryRow(convertView, parent, category);
    }

    void onItemClick(int position) {
      int type = getItemViewType(position);
      if (type == VIEW_CREATE_FROM_SEARCH) {
        createFromSearchQuery();
        return;
      }
      if (type == VIEW_ADD_NEW) {
        showAddNewDialog();
        return;
      }
      int categoryIndex = shouldShowCreateFromSearch() ? position - 1 : position;
      chooseCategory(filteredCategories().get(categoryIndex), false);
    }

    private boolean shouldShowCreateFromSearch() {
      String query = currentQuery();
      return !query.isEmpty() && !hasExactMatch(query);
    }

    private View bindCategoryRow(View convertView, ViewGroup parent, Category category) {
      TextView label;
      if (convertView == null) {
        label =
            (TextView)
                LayoutInflater.from(getContext())
                    .inflate(R.layout.item_category_search_row, parent, false);
      } else {
        label = (TextView) convertView;
      }
      label.setText(category.getName());
      return label;
    }

    private View bindActionRow(View convertView, ViewGroup parent, String text) {
      TextView label;
      if (convertView == null) {
        label =
            (TextView)
                LayoutInflater.from(getContext())
                    .inflate(R.layout.item_category_search_action, parent, false);
      } else {
        label = (TextView) convertView;
      }
      label.setText(text);
      return label;
    }
  }
}
