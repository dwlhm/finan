package com.dwlhm.finan.ui.common;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.dwlhm.finan.R;
import com.dwlhm.finan.data.dao.CategoryDao;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.service.category.CategoryClassificationService;
import com.dwlhm.finan.util.search.FuzzySearch;

import java.util.ArrayList;
import java.util.List;

public final class CategorySearchDialog extends BottomSheetDialog {

  public interface Listener {
    void onCategoryChosen(Category category, boolean created);
  }

  private static final int VIEW_CATEGORY = 0;
  private static final int VIEW_CREATE_FROM_SEARCH = 1;
  private static final int VIEW_ADD_NEW = 2;

  private final CategoryDao categoryDao;
  private final CategoryClassificationService categoryClassificationService;
  private final DbWorker dbWorker;
  private final TransactionType transactionType;
  private final List<Category> allCategories;
  private final FuzzySearch.Index<Category> searchIndex;
  private final Listener listener;

  private EditText searchInput;
  private SearchListAdapter listAdapter;

  public CategorySearchDialog(
      @NonNull Context context,
      CategoryDao categoryDao,
      TransactionType transactionType,
      List<Category> allCategories,
      Listener listener) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.categoryDao = categoryDao;
    AppServices services = ServicesProvider.get(context);
    this.categoryClassificationService = services.categoryClassificationService;
    this.dbWorker = services.dbWorker;
    this.transactionType = transactionType;
    this.allCategories = new ArrayList<>(allCategories);
    searchIndex = FuzzySearch.index(this.allCategories, Category::getName);
    this.listener = listener;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_category_search);
    if (getWindow() != null) {
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
    BottomSheetHelper.show(this);

    searchInput = findViewById(R.id.category_search_input);
    ListView listView = findViewById(R.id.category_search_list);

    listAdapter = new SearchListAdapter();
    if (listView != null) {
      listView.setAdapter(listAdapter);
    }

    if (searchInput != null) {
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
      searchInput.requestFocus();
      searchInput.post(this::showKeyboard);
    }
    if (listView != null) {
      listView.setOnItemClickListener(
          (parent, view, position, id) -> listAdapter.onItemClick(position));
    }
  }

  private void showKeyboard() {
    InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.showSoftInput(searchInput, 0);
    }
  }

  private String currentQuery() {
    return searchInput.getText().toString().trim();
  }

  private List<Category> filteredCategories() {
    String query = currentQuery();
    return query.isEmpty() ? allCategories : searchIndex.matching(query);
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
    dbWorker.compute(
        () -> {
          Category existing = categoryDao.findByNameIgnoreCase(query);
          if (existing != null) {
            return new CategoryCreateResult(existing, false, true);
          }
          Category created = categoryClassificationService.create(query, null, transactionType.name(), CashFlowActivity.UNCLASSIFIED, false);
          return new CategoryCreateResult(created, true, false);
        },
        result -> {
          if (!isShowing() || result == null || result.category == null) {
            return;
          }
          if (result.alreadyExists) {
            Toast.makeText(getContext(), R.string.capture_category_already_exists, Toast.LENGTH_SHORT)
                .show();
            chooseCategory(result.category, false);
            return;
          }
          Toast.makeText(getContext(), R.string.capture_category_created, Toast.LENGTH_SHORT).show();
          chooseCategory(result.category, result.created);
        });
  }

  private void showAddNewDialog() {
    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.Finan_BottomSheetDialog);
    View dialogView =
        LayoutInflater.from(getContext()).inflate(R.layout.dialog_category_name_input, null);
    EditText nameInput = dialogView.findViewById(R.id.category_name_input);
    String prefill = currentQuery();
    if (!prefill.isEmpty()) {
      nameInput.setText(prefill);
      nameInput.setSelection(prefill.length());
    }

    LinearLayout root = new LinearLayout(getContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundResource(R.drawable.bg_bottom_sheet);
    int p20 = UiComponentStyles.dp(getContext(), 20);
    int p16 = UiComponentStyles.dp(getContext(), 16);
    root.setPadding(p20, p16, p20, p20);

    View handle = new View(getContext());
    LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(
        UiComponentStyles.dp(getContext(), 40), UiComponentStyles.dp(getContext(), 4));
    handleLp.gravity = Gravity.CENTER_HORIZONTAL;
    handleLp.bottomMargin = p16;
    handle.setLayoutParams(handleLp);
    handle.setBackgroundResource(R.drawable.bg_bottom_sheet_handle);
    root.addView(handle);

    TextView titleView = new TextView(getContext());
    titleView.setText(R.string.capture_category_add_new_dialog_title);
    titleView.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_text_primary));
    titleView.setTextSize(18f);
    titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
    root.addView(titleView);

    root.addView(dialogView);

    DialogActionsView actionsView = new DialogActionsView(getContext());
    actionsView.setPrimaryText(getContext().getString(android.R.string.ok));
    actionsView.setOnCancelClickListener(v -> dialog.dismiss());
    actionsView.setOnPrimaryClickListener(
        v -> {
          String name = nameInput.getText().toString().trim();
          if (name.isEmpty()) {
            Toast.makeText(
                    getContext(), R.string.capture_category_name_empty, Toast.LENGTH_SHORT)
                .show();
            return;
          }
          dialog.dismiss();
          dbWorker.compute(
              () -> {
                Category existing = categoryDao.findByNameIgnoreCase(name);
                if (existing != null) {
                  return new CategoryCreateResult(existing, false, true);
                }
                Category created = categoryClassificationService.create(name, null, transactionType.name(), CashFlowActivity.UNCLASSIFIED, false);
                return new CategoryCreateResult(created, true, false);
              },
              result -> {
                if (!isShowing() || result == null || result.category == null) {
                  return;
                }
                if (result.alreadyExists) {
                  Toast.makeText(
                          getContext(),
                          R.string.capture_category_already_exists,
                          Toast.LENGTH_SHORT)
                      .show();
                  chooseCategory(result.category, false);
                  return;
                }
                Toast.makeText(getContext(), R.string.capture_category_created, Toast.LENGTH_SHORT)
                    .show();
                chooseCategory(result.category, result.created);
              });
        });
    root.addView(actionsView);

    dialog.setContentView(root);
    BottomSheetHelper.show(dialog);
  }

  private static final class CategoryCreateResult {
    private final Category category;
    private final boolean created;
    private final boolean alreadyExists;

    private CategoryCreateResult(Category category, boolean created, boolean alreadyExists) {
      this.category = category;
      this.created = created;
      this.alreadyExists = alreadyExists;
    }
  }

  private final class SearchListAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      int count = filteredCategories().size();
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
