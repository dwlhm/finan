package com.dwlhm.finan.ui.category;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.CashFlowActivity;
import com.dwlhm.finan.ui.category.CategoryEditorDialog;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.DebouncedTextWatcher;
import com.dwlhm.finan.ui.common.DialogActionsView;
import com.dwlhm.finan.ui.common.LabeledEditTextView;
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
  private boolean pendingExpanded = true;
  private boolean otherExpanded = true;
  private CashFlowActivity classificationFilter;
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
    query = savedInstanceState == null ? "" : savedInstanceState.getString(SEARCH_STATE, "");
    if (savedInstanceState != null) {
      pendingExpanded = savedInstanceState.getBoolean(PENDING_EXPANDED_STATE, true);
      otherExpanded = savedInstanceState.getBoolean(OTHER_EXPANDED_STATE, true);
      String filter = savedInstanceState.getString(CLASSIFICATION_FILTER_STATE);
      classificationFilter = filter == null ? null : CashFlowActivity.valueOf(filter);
    }
    listView = view.findViewById(R.id.category_list);
    loadingView = view.findViewById(R.id.category_loading);
    emptyView = view.findViewById(R.id.category_empty);
    introView = view.findViewById(R.id.category_intro);
    emptyTitle = view.findViewById(R.id.category_empty_title);
    emptyHint = view.findViewById(R.id.category_empty_hint);
    emptyAction = view.findViewById(R.id.category_empty_action);
    searchInput = view.findViewById(R.id.category_search);
    searchClear = view.findViewById(R.id.category_search_clear);
    searchInput.setText(query);
    searchInput.setSelection(searchInput.length());
    ScreenHeaderView header = view.findViewById(R.id.category_header);
    header.setOnBackClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    header.setOnActionClickListener(v -> openCategoryCreator());

    adapter =
        new CategoryAdapter(
            requireContext(),
            this::toggleSection,
            filter -> {
              classificationFilter = filter;
              render();
            });
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
    outState.putBoolean(PENDING_EXPANDED_STATE, pendingExpanded);
    outState.putBoolean(OTHER_EXPANDED_STATE, otherExpanded);
    if (classificationFilter != null) {
      outState.putString(CLASSIFICATION_FILTER_STATE, classificationFilter.name());
    }
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

  private void toggleSection(Section section) {
    if (section == Section.PENDING) {
      pendingExpanded = !pendingExpanded;
    } else {
      otherExpanded = !otherExpanded;
    }
    render();
  }

  private void render() {
    if (adapter == null || loadingView.getVisibility() == View.VISIBLE) {
      return;
    }
    List<Category> visible = query.isEmpty() ? categories : allIndex.matching(query);
    List<Category> pending = unclassified(visible);
    List<Category> classified = classified(visible, classificationFilter);
    boolean noResults = pending.isEmpty() && classified.isEmpty();
    adapter.setClassificationFilter(classificationFilter);
    adapter.setItems(entries(visible));
    searchClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
    introView.setVisibility(query.isEmpty() ? View.VISIBLE : View.GONE);
    boolean empty = categories.isEmpty() || (!query.isEmpty() && noResults);
    listView.setVisibility(empty ? View.GONE : View.VISIBLE);
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    if (empty) {
      bindEmptyState();
    }
  }

  private List<ListEntry> entries(List<Category> visible) {
    List<ListEntry> result = new ArrayList<>();
    List<Category> pending = unclassified(visible);
    List<Category> classified = classified(visible, classificationFilter);
    boolean searching = !query.isEmpty();
    if (!pending.isEmpty()) {
      result.add(
          ListEntry.section(
              getString(R.string.category_section_unclassified, pending.size()),
              Section.PENDING,
              searching || pendingExpanded));
      if (searching || pendingExpanded) {
        for (Category category : pending) {
          result.add(ListEntry.category(category));
        }
      }
    }
    if (hasClassified(categories)) {
      result.add(
          ListEntry.section(
              getString(R.string.category_section_other, classified.size()),
              Section.OTHER,
              searching || otherExpanded));
      if (searching || otherExpanded) {
        result.add(ListEntry.filters());
        for (Category category : classified) {
          result.add(ListEntry.category(category));
        }
      }
    }
    return result;
  }

  private static List<Category> classified(
      List<Category> source, @Nullable CashFlowActivity filter) {
    List<Category> result = new ArrayList<>();
    for (Category category : source) {
      if (!isUnclassified(category)
          && (filter == null || filter.name().equals(category.getCashFlowActivity()))) {
        result.add(category);
      }
    }
    return result;
  }

  private static boolean hasClassified(List<Category> source) {
    for (Category category : source) {
      if (!isUnclassified(category)) {
        return true;
      }
    }
    return false;
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

  private static final class CategoryAdapter extends BaseAdapter {
    private static final int TYPE_SECTION = 0;
    private static final int TYPE_FILTERS = 1;
    private static final int TYPE_CATEGORY = 2;
    private final Context context;
    private final LayoutInflater inflater;
    private final Consumer<Section> sectionListener;
    private final Consumer<CashFlowActivity> filterListener;
    private List<ListEntry> items = List.of();
    private CashFlowActivity classificationFilter;

    private CategoryAdapter(
        Context context,
        Consumer<Section> sectionListener,
        Consumer<CashFlowActivity> filterListener) {
      this.context = context;
      this.sectionListener = sectionListener;
      this.filterListener = filterListener;
      inflater = LayoutInflater.from(context);
    }

    private void setClassificationFilter(@Nullable CashFlowActivity filter) {
      classificationFilter = filter;
    }

    private void setItems(List<ListEntry> items) {
      this.items = items;
      notifyDataSetChanged();
    }

    private Category categoryAt(int position) {
      return items.get(position).category;
    }

    @Override
    public int getCount() {
      return items.size();
    }

    @Override
    public ListEntry getItem(int position) {
      return items.get(position);
    }

    @Override
    public long getItemId(int position) {
      Category category = categoryAt(position);
      return category == null ? -position - 1L : category.getId();
    }

    @Override
    public int getItemViewType(int position) {
      ListEntry item = getItem(position);
      if (item.category != null) {
        return TYPE_CATEGORY;
      }
      return item.filters ? TYPE_FILTERS : TYPE_SECTION;
    }

    @Override
    public int getViewTypeCount() {
      return 3;
    }

    @Override
    public boolean isEnabled(int position) {
      return categoryAt(position) != null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ListEntry item = getItem(position);
      if (item.sectionType != null) {
        if (convertView == null) {
          convertView = inflater.inflate(R.layout.item_category_section, parent, false);
        }
        TextView title = convertView.findViewById(R.id.item_category_section);
        ImageView indicator =
            convertView.findViewById(R.id.item_category_section_indicator);
        title.setText(item.section);
        indicator.setImageResource(
            item.expanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        convertView.setSelected(item.expanded);
        convertView.setOnClickListener(v -> sectionListener.accept(item.sectionType));
        return convertView;
      }
      if (item.filters) {
        if (convertView == null) {
          convertView = inflater.inflate(R.layout.item_category_filters, parent, false);
        }
        bindFilter(convertView.findViewById(R.id.category_classification_all), null);
        bindFilter(
            convertView.findViewById(R.id.category_classification_operating),
            CashFlowActivity.OPERATING);
        bindFilter(
            convertView.findViewById(R.id.category_classification_investing),
            CashFlowActivity.INVESTING);
        bindFilter(
            convertView.findViewById(R.id.category_classification_financing),
            CashFlowActivity.FINANCING);
        return convertView;
      }
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.item_category, parent, false);
      }
      TextView name = convertView.findViewById(R.id.item_category_name);
      TextView metadata = convertView.findViewById(R.id.item_category_type);
      ImageView icon = convertView.findViewById(R.id.item_category_icon);
      TextView emoji = convertView.findViewById(R.id.item_category_emoji);
      
      Category category = item.category;
      
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
      convertView.setContentDescription(
          category.getName() + ". " + type + ". " + activity + ".");
      return convertView;
    }

    private void bindFilter(Button button, @Nullable CashFlowActivity activity) {
      UiComponentStyles.prepareChip(button);
      UiComponentStyles.setChipSelected(
          context, button, classificationFilter == activity, R.drawable.bg_chip_selected);
      button.setGravity(android.view.Gravity.CENTER_VERTICAL);
      button.setOnClickListener(v -> filterListener.accept(activity));
    }

    private static String typeLabel(Context context, String type) {
      if ("INCOME".equals(type)) {
        return context.getString(R.string.category_type_income);
      }
      return context.getString(
          "BOTH".equals(type) ? R.string.category_type_both_long : R.string.category_type_expense);
    }

    private static String activityLabel(Context context, String activity) {
      CashFlowActivity value = CashFlowActivity.valueOf(activity);
      switch (value) {
        case OPERATING:
          return context.getString(R.string.category_activity_operating_short);
        case INVESTING:
          return context.getString(R.string.category_activity_investing_short);
        case FINANCING:
          return context.getString(R.string.category_activity_financing_short);
        default:
          return context.getString(R.string.category_activity_unclassified_short);
      }
    }
  }
}
