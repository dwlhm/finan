package com.dwlhm.finan.ui.category;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.DebouncedTextWatcher;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ScreenHeaderView;
import com.dwlhm.finan.ui.common.ScreenNavigator;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.common.UiComponentStyles;
import com.dwlhm.finan.util.search.FuzzySearch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CategoryListFragment extends ScreenFragment {

  public static final String TAG = "category_list";
  public static final String ARG_CLASSIFICATION_FILTER = "classification_filter";
  private static final long SEARCH_DEBOUNCE_MS = 200L;
  private static final String SEARCH_STATE = "category_search";
  private static final String PENDING_EXPANDED_STATE = "category_pending_expanded";
  private static final String OTHER_EXPANDED_STATE = "category_other_expanded";
  private static final String CLASSIFICATION_FILTER_STATE = "category_classification_filter";

  private AppServices services;
  private CategoryAdapter adapter;
  private List<Category> categories = List.of();
  private FuzzySearch.Index<Category> allIndex = FuzzySearch.index(List.of(), Category::getName);
  private ListView listView;
  private View loadingView;
  private View emptyView;
  private TextView introView;
  private TextView emptyTitle;
  private TextView emptyHint;
  private Button emptyAction;
  private EditText searchInput;
  private ImageButton searchClear;
  private DebouncedTextWatcher searchWatcher;
  private String selectedTypeFilter = "ALL";
  private String query = "";
  private int reloadGeneration;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    services = ServicesProvider.get(requireContext());
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_category_list;
  }

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      query = savedInstanceState.getString(SEARCH_STATE, "");
    }
    listView = view.findViewById(R.id.category_list);
    loadingView = view.findViewById(R.id.category_loading);
    emptyView = view.findViewById(R.id.category_empty_view);
    emptyTitle = view.findViewById(R.id.category_empty_title);
    emptyHint = view.findViewById(R.id.category_empty_hint);
    emptyAction = view.findViewById(R.id.category_empty_action);
    searchInput = view.findViewById(R.id.category_search);
    searchClear = view.findViewById(R.id.category_search_clear);
    searchInput.setText(query);
    searchInput.setSelection(searchInput.length());

    // Apply system navigation bar insets so bottom list items are never covered
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
      int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
      int extraPaddingBottom = (int) (108 * getResources().getDisplayMetrics().density + 0.5f);
      if (listView != null) {
        listView.setPadding(
            listView.getPaddingLeft(),
            listView.getPaddingTop(),
            listView.getPaddingRight(),
            extraPaddingBottom + bottomInset);
      }
      return insets;
    });

    MaterialButtonToggleGroup filterToggleGroup = view.findViewById(R.id.category_filter_toggle_group);
    if (filterToggleGroup != null) {
      int initialCheckedId = switch (selectedTypeFilter) {
        case "EXPENSE" -> R.id.category_filter_expense;
        case "INCOME" -> R.id.category_filter_income;
        default -> R.id.category_filter_all;
      };
      filterToggleGroup.check(initialCheckedId);

      filterToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
        if (isChecked) {
          if (checkedId == R.id.category_filter_all) {
            selectedTypeFilter = "ALL";
          } else if (checkedId == R.id.category_filter_expense) {
            selectedTypeFilter = "EXPENSE";
          } else if (checkedId == R.id.category_filter_income) {
            selectedTypeFilter = "INCOME";
          }
          render();
        }
      });
    }

    ScreenHeaderView header = view.findViewById(R.id.category_header);
    header.setOnBackClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    header.setOnActionClickListener(v -> openCategoryCreator());

    adapter = new CategoryAdapter(requireContext());
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(
        (parent, row, position, id) -> {
          Category category = adapter.categoryAt(position);
          if (category != null) {
            loadEditor(category);
          }
        });
    searchClear.setOnClickListener(v -> searchInput.setText(""));
    searchWatcher =
        new DebouncedTextWatcher(
            SEARCH_DEBOUNCE_MS,
            value -> {
              query = value.trim();
              render();
            });
    searchInput.addTextChangedListener(searchWatcher);
  }
  @Override
  public void onResume() {
    super.onResume();
    reload();
  }

  @Override
  public void onDestroyView() {
    reloadGeneration++;
    if (searchWatcher != null) {
      searchWatcher.cancel();
    }
    super.onDestroyView();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putString(
        SEARCH_STATE, searchInput == null ? query : searchInput.getText().toString().trim());
    super.onSaveInstanceState(outState);
  }

  private void reload() {
    int generation = ++reloadGeneration;
    loadingView.setVisibility(View.VISIBLE);
    emptyView.setVisibility(View.GONE);
    listView.setVisibility(View.GONE);
    services.dbWorker.compute(
        () -> services.categoryDao.findAllForManage(),
        result -> {
          if (!isAdded() || generation != reloadGeneration) {
            return;
          }
          loadingView.setVisibility(View.GONE);
          if (result == null) {
            showError();
            return;
          }
          categories = result;
          allIndex = FuzzySearch.index(categories, Category::getName);
          render();
        });
  }

  private void render() {
    if (adapter == null || loadingView.getVisibility() == View.VISIBLE) {
      return;
    }
    List<Category> visible = query.isEmpty() ? categories : allIndex.matching(query);
    List<Category> filtered = new ArrayList<>();
    for (Category c : visible) {
      if ("EXPENSE".equals(selectedTypeFilter)) {
        if ("EXPENSE".equals(c.getTypeFilter()) || "BOTH".equals(c.getTypeFilter())) {
          filtered.add(c);
        }
      } else if ("INCOME".equals(selectedTypeFilter)) {
        if ("INCOME".equals(c.getTypeFilter()) || "BOTH".equals(c.getTypeFilter())) {
          filtered.add(c);
        }
      } else {
        filtered.add(c);
      }
    }
    filtered.sort((a, b) -> {
      if (a.isDefault() != b.isDefault()) {
        return a.isDefault() ? -1 : 1;
      }
      return a.getName().compareToIgnoreCase(b.getName());
    });
    searchClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
    boolean empty = categories.isEmpty() || (!query.isEmpty() && filtered.isEmpty());
    listView.setVisibility(empty ? View.GONE : View.VISIBLE);
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    if (empty) {
      bindEmptyState();
    } else {
      adapter.setItems(filtered);
    }
  }
  private void bindEmptyState() {
    if (categories.isEmpty()) {
      emptyTitle.setText(R.string.category_empty);
      emptyHint.setText(R.string.category_empty_hint);
      emptyAction.setText(R.string.category_empty_action);
      emptyAction.setOnClickListener(v -> openCategoryCreator());
    } else {
      emptyTitle.setText(R.string.category_search_empty);
      emptyHint.setText(R.string.category_search_empty_hint);
      emptyAction.setText(R.string.category_search_empty_action);
      emptyAction.setOnClickListener(v -> openCategoryCreator());
    }
  }

  private void showError() {
    listView.setVisibility(View.GONE);
    emptyView.setVisibility(View.VISIBLE);
    emptyTitle.setText(R.string.category_load_error);
    emptyHint.setText("");
    emptyAction.setText(R.string.category_retry);
    emptyAction.setOnClickListener(v -> reload());
  }

  private void loadEditor(Category category) {
    services.dbWorker.compute(
        () -> services.categoryDao.countTransactions(category.getId()),
        count -> {
          if (isAdded() && count != null) {
            new CategoryEditorDialog(
                requireContext(),
                services,
                category,
                count,
                saved -> reload(),
                () -> {
                  if (requireActivity() instanceof ScreenNavigator) {
                    ((ScreenNavigator) requireActivity())
                        .openHistoryForCategory(category.getId());
                  }
                }
            );
          }
        });
  }

  private void openCategoryCreator() {
    new CategoryEditorDialog(
        requireContext(),
        services,
        null,
        0,
        saved -> reload(),
        null,
        query.trim(),
        null
    );
  }

  private static List<Category> unclassified(List<Category> source) {
    List<Category> result = new ArrayList<>();
    for (Category category : source) {
      if (isUnclassified(category)) {
        result.add(category);
      }
    }
    return result;
  }

  static boolean isUnclassified(Category category) {
    return CashFlowActivity.UNCLASSIFIED.name().equals(category.getCashFlowActivity());
  }

  private enum Section {
    PENDING,
    OTHER
  }

  private static final class ListEntry {
    private final String section;
    private final Section sectionType;
    private final boolean expanded;
    private final boolean filters;
    private final Category category;

    private ListEntry(
        String section,
        Section sectionType,
        boolean expanded,
        boolean filters,
        Category category) {
      this.section = section;
      this.sectionType = sectionType;
      this.expanded = expanded;
      this.filters = filters;
      this.category = category;
    }

    private static ListEntry section(String title, Section section, boolean expanded) {
      return new ListEntry(title, section, expanded, false, null);
    }

    private static ListEntry filters() {
      return new ListEntry(null, null, false, true, null);
    }

    private static ListEntry category(Category category) {
      return new ListEntry(null, null, false, false, category);
    }
  }

  private final class CategoryAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private List<Category> items = new ArrayList<>();

    CategoryAdapter(Context context) {
      this.context = context;
      this.inflater = LayoutInflater.from(context);
    }

    void setItems(List<Category> items) {
      this.items = items != null ? items : new ArrayList<>();
      notifyDataSetChanged();
    }

    Category categoryAt(int position) {
      return (position >= 0 && position < items.size()) ? items.get(position) : null;
    }

    @Override
    public int getCount() {
      return items.size();
    }

    @Override
    public Category getItem(int position) {
      return items.get(position);
    }

    @Override
    public long getItemId(int position) {
      Category category = getItem(position);
      return category != null ? category.getId() : position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.item_category, parent, false);
      }
      TextView name = convertView.findViewById(R.id.item_category_name);
      TextView metadata = convertView.findViewById(R.id.item_category_type);
      ImageView icon = convertView.findViewById(R.id.item_category_icon);
      TextView emoji = convertView.findViewById(R.id.item_category_emoji);
      TextView defaultBadge = convertView.findViewById(R.id.item_category_default_badge);

      Category category = getItem(position);

      if (category.getIcon() != null && !category.getIcon().trim().isEmpty()) {
        icon.setBackground(null);
        icon.setImageDrawable(null);
        emoji.setVisibility(View.VISIBLE);
        emoji.setText(category.getIcon().trim());
      } else {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFFEEEEEE);
        bg.setCornerRadius(22f * context.getResources().getDisplayMetrics().density);
        icon.setBackground(bg);
        icon.setImageResource(R.drawable.ic_summary_filter);
        emoji.setVisibility(View.GONE);
      }

      String type = typeLabel(context, category.getTypeFilter());
      String activity = activityLabel(context, category.getCashFlowActivity());
      name.setText(category.getName());
      metadata.setText(context.getString(R.string.category_metadata, type, activity));
      metadata.setTextColor(
          ContextCompat.getColor(
              context,
              isUnclassified(category)
                  ? R.color.finan_warm_accent
                  : R.color.finan_text_secondary));
      defaultBadge.setVisibility(category.isDefault() ? View.VISIBLE : View.GONE);
      convertView.setBackgroundResource(category.isDefault()
          ? R.drawable.bg_card_wallet_default
          : R.drawable.bg_card);
      return convertView;
    }

    private static String typeLabel(Context context, String type) {
      if ("INCOME".equals(type)) {
        return context.getString(R.string.category_type_income);
      }
      return context.getString(
          "BOTH".equals(type) ? R.string.category_type_both_long : R.string.category_type_expense);
    }

    private static String activityLabel(Context context, String activity) {
      if (activity == null) return "";
      try {
        CashFlowActivity value = CashFlowActivity.valueOf(activity);
        return switch (value) {
            case OPERATING -> context.getString(R.string.category_activity_operating_short);
            case INVESTING -> context.getString(R.string.category_activity_investing_short);
            case FINANCING -> context.getString(R.string.category_activity_financing_short);
            default -> context.getString(R.string.category_activity_unclassified_short);
        };
      } catch (Exception e) {
        return "";
      }
    }
  }
}
