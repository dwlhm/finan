package com.dwlhm.finan.ui.category;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BaseActivity;
import com.dwlhm.finan.ui.common.ServicesProvider;

import java.util.ArrayList;
import java.util.List;

public class CategoryListActivity extends BaseActivity {

  private AppServices services;
  private CategoryAdapter adapter;
  private ListView listView;
  private TextView emptyView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    services = ServicesProvider.get(this);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_category_list;
  }

  @Override
  protected void onReady() {
    listView = findViewById(R.id.category_list);
    emptyView = findViewById(R.id.category_empty);
    adapter = new CategoryAdapter(this);
    listView.setAdapter(adapter);
    reload();
  }

  private void reload() {
    adapter.setCategories(services.categoryDao.findAllOrdered());
    boolean empty = adapter.getCount() == 0;
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    listView.setVisibility(empty ? View.GONE : View.VISIBLE);
  }

  private static class CategoryAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private List<Category> categories = new ArrayList<>();

    CategoryAdapter(Context context) {
      this.context = context;
      this.inflater = LayoutInflater.from(context);
    }

    void setCategories(List<Category> categories) {
      this.categories = categories != null ? categories : new ArrayList<>();
      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      return categories.size();
    }

    @Override
    public Category getItem(int position) {
      return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
      return getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.item_category, parent, false);
      }
      TextView name = convertView.findViewById(R.id.item_category_name);
      TextView type = convertView.findViewById(R.id.item_category_type);
      Category category = getItem(position);
      name.setText(category.getName());
      int typeRes;
      if ("INCOME".equals(category.getTypeFilter())) {
        typeRes = R.string.category_type_income;
      } else if ("EXPENSE".equals(category.getTypeFilter())) {
        typeRes = R.string.category_type_expense;
      } else {
        typeRes = R.string.category_type_both;
      }
      type.setText(context.getString(typeRes));
      return convertView;
    }
  }
}
