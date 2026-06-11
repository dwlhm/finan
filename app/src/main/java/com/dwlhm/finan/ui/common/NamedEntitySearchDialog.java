package com.dwlhm.finan.ui.common;

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
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.dwlhm.finan.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NamedEntitySearchDialog<T> extends Dialog {

  public interface Listener<T> {
    void onEntityChosen(T entity, boolean created);
  }

  public interface EntityAccess<T> {
    List<T> loadAll();

    @Nullable
    T findByNameIgnoreCase(String name);

    T insertIfAbsent(String name);

    String nameOf(T entity);

    long idOf(T entity);
  }

  private static final int VIEW_ENTITY = 0;
  private static final int VIEW_CREATE_FROM_SEARCH = 1;
  private static final int VIEW_ADD_NEW = 2;

  private final EntityAccess<T> access;
  private final DbWorker dbWorker;
  private final Listener<T> listener;
  private final Set<Long> excludeIds;
  private final int titleRes;
  private final int hintRes;
  private final int createFromSearchRes;
  private final int addNewRes;
  private final int addNewDialogTitleRes;
  private final int nameHintRes;
  private final int createdRes;
  private final int emptyNameRes;
  private final int alreadyExistsRes;

  private final List<T> allEntities = new ArrayList<>();
  private EditText searchInput;
  private SearchListAdapter listAdapter;

  public NamedEntitySearchDialog(
      @NonNull Context context,
      @NonNull EntityAccess<T> access,
      @NonNull DbWorker dbWorker,
      @NonNull Listener<T> listener,
      @StringRes int titleRes,
      @StringRes int hintRes,
      @StringRes int createFromSearchRes,
      @StringRes int addNewRes,
      @StringRes int addNewDialogTitleRes,
      @StringRes int nameHintRes,
      @StringRes int createdRes,
      @StringRes int emptyNameRes,
      @StringRes int alreadyExistsRes,
      @Nullable Set<Long> excludeIds) {
    super(context);
    this.access = access;
    this.dbWorker = dbWorker;
    this.listener = listener;
    this.titleRes = titleRes;
    this.hintRes = hintRes;
    this.createFromSearchRes = createFromSearchRes;
    this.addNewRes = addNewRes;
    this.addNewDialogTitleRes = addNewDialogTitleRes;
    this.nameHintRes = nameHintRes;
    this.createdRes = createdRes;
    this.emptyNameRes = emptyNameRes;
    this.alreadyExistsRes = alreadyExistsRes;
    this.excludeIds = excludeIds == null ? new HashSet<>() : new HashSet<>(excludeIds);
    allEntities.addAll(access.loadAll());
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_named_entity_search);
    if (getWindow() != null) {
      getWindow()
          .setLayout(
              WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    TextView titleView = findViewById(R.id.named_entity_search_title);
    titleView.setText(titleRes);
    searchInput = findViewById(R.id.named_entity_search_input);
    searchInput.setHint(hintRes);
    ListView listView = findViewById(R.id.named_entity_search_list);

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

    listView.setOnItemClickListener((parent, view, position, id) -> listAdapter.onItemClick(position));

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

  private List<T> filteredEntities() {
    String query = currentQuery().toLowerCase(Locale.ROOT);
    List<T> filtered = new ArrayList<>();
    for (T entity : allEntities) {
      if (excludeIds.contains(access.idOf(entity))) {
        continue;
      }
      if (query.isEmpty() || access.nameOf(entity).toLowerCase(Locale.ROOT).contains(query)) {
        filtered.add(entity);
      }
    }
    return filtered;
  }

  private boolean hasExactMatch(String query) {
    if (query.isEmpty()) {
      return false;
    }
    for (T entity : allEntities) {
      if (access.nameOf(entity).equalsIgnoreCase(query)) {
        return true;
      }
    }
    return false;
  }

  private void chooseEntity(T entity, boolean created) {
    listener.onEntityChosen(entity, created);
    dismiss();
  }

  private void createFromSearchQuery() {
    String query = currentQuery();
    if (query.isEmpty()) {
      Toast.makeText(getContext(), emptyNameRes, Toast.LENGTH_SHORT).show();
      return;
    }
    dbWorker.compute(
        () -> {
          T existing = access.findByNameIgnoreCase(query);
          if (existing != null) {
            return new CreateResult(existing, false, true);
          }
          T created = access.insertIfAbsent(query);
          return new CreateResult(created, true, false);
        },
        result -> {
          if (!isShowing() || result == null || result.entity == null) {
            return;
          }
          if (result.alreadyExists) {
            Toast.makeText(getContext(), alreadyExistsRes, Toast.LENGTH_SHORT).show();
            chooseEntity(result.entity, false);
            return;
          }
          if (result.created) {
            allEntities.add(result.entity);
            Toast.makeText(getContext(), createdRes, Toast.LENGTH_SHORT).show();
          }
          chooseEntity(result.entity, result.created);
        });
  }

  private void showAddNewDialog() {
    View dialogView =
        LayoutInflater.from(getContext()).inflate(R.layout.dialog_category_name_input, null);
    EditText nameInput = dialogView.findViewById(R.id.category_name_input);
    nameInput.setHint(nameHintRes);
    String prefill = currentQuery();
    if (!prefill.isEmpty()) {
      nameInput.setText(prefill);
      nameInput.setSelection(prefill.length());
    }

    new AlertDialog.Builder(getContext())
        .setTitle(addNewDialogTitleRes)
        .setView(dialogView)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              String name = nameInput.getText().toString().trim();
              if (name.isEmpty()) {
                Toast.makeText(getContext(), emptyNameRes, Toast.LENGTH_SHORT).show();
                return;
              }
              dbWorker.compute(
                  () -> {
                    T existing = access.findByNameIgnoreCase(name);
                    if (existing != null) {
                      return new CreateResult(existing, false, true);
                    }
                    T created = access.insertIfAbsent(name);
                    return new CreateResult(created, true, false);
                  },
                  result -> {
                    if (!isShowing() || result == null || result.entity == null) {
                      return;
                    }
                    if (result.alreadyExists) {
                      Toast.makeText(getContext(), alreadyExistsRes, Toast.LENGTH_SHORT).show();
                      chooseEntity(result.entity, false);
                      return;
                    }
                    if (result.created) {
                      allEntities.add(result.entity);
                      Toast.makeText(getContext(), createdRes, Toast.LENGTH_SHORT).show();
                    }
                    chooseEntity(result.entity, result.created);
                  });
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private final class CreateResult {
    private final T entity;
    private final boolean created;
    private final boolean alreadyExists;

    private CreateResult(T entity, boolean created, boolean alreadyExists) {
      this.entity = entity;
      this.created = created;
      this.alreadyExists = alreadyExists;
    }
  }

  private final class SearchListAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      int count = filteredEntities().size();
      if (shouldShowCreateFromSearch()) {
        count++;
      }
      count++;
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
      return VIEW_ENTITY;
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
            getContext().getString(createFromSearchRes, currentQuery()));
      }
      if (type == VIEW_ADD_NEW) {
        return bindActionRow(convertView, parent, getContext().getString(addNewRes));
      }
      int entityIndex = shouldShowCreateFromSearch() ? position - 1 : position;
      T entity = filteredEntities().get(entityIndex);
      return bindEntityRow(convertView, parent, entity);
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
      int entityIndex = shouldShowCreateFromSearch() ? position - 1 : position;
      chooseEntity(filteredEntities().get(entityIndex), false);
    }

    private boolean shouldShowCreateFromSearch() {
      String query = currentQuery();
      return !query.isEmpty() && !hasExactMatch(query);
    }

    private View bindEntityRow(View convertView, ViewGroup parent, T entity) {
      TextView label;
      if (convertView == null) {
        label =
            (TextView)
                LayoutInflater.from(getContext())
                    .inflate(R.layout.item_category_search_row, parent, false);
      } else {
        label = (TextView) convertView;
      }
      label.setText(access.nameOf(entity));
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
