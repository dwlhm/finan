package com.dwlhm.finan.ui.common.infinitescroll;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public abstract class InfiniteScrollRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
    extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static final int VIEW_TYPE_LOADING_FOOTER = -1;

  private final List<T> items = new ArrayList<>();
  private final LayoutInflater inflater;
  @LayoutRes private final int loadingFooterLayoutRes;
  private boolean loadingFooterVisible;

  protected InfiniteScrollRecyclerAdapter(
      @NonNull LayoutInflater inflater, @LayoutRes int loadingFooterLayoutRes) {
    this.inflater = inflater;
    this.loadingFooterLayoutRes = loadingFooterLayoutRes;
  }

  public void replaceItems(List<T> items) {
    int previousSize = this.items.size();
    this.items.clear();
    if (items != null) {
      this.items.addAll(items);
    }
    if (previousSize > 0) {
      notifyItemRangeRemoved(0, previousSize);
    }
    if (!this.items.isEmpty()) {
      notifyItemRangeInserted(0, this.items.size());
    }
  }

  public void appendItems(List<T> items) {
    if (items == null || items.isEmpty()) {
      return;
    }
    int start = this.items.size();
    this.items.addAll(items);
    notifyItemRangeInserted(start, items.size());
  }

  public T getItemAt(int position) {
    return items.get(position);
  }

  public int getContentItemCount() {
    return items.size();
  }

  public void setLoadingFooterVisible(boolean visible) {
    if (loadingFooterVisible == visible) {
      return;
    }
    loadingFooterVisible = visible;
    if (visible) {
      notifyItemInserted(items.size());
    } else {
      notifyItemRemoved(items.size());
    }
  }

  public boolean isLoadingFooterVisible() {
    return loadingFooterVisible;
  }

  @Override
  public final int getItemViewType(int position) {
    return position < items.size() ? 0 : VIEW_TYPE_LOADING_FOOTER;
  }

  @Override
  public final int getItemCount() {
    return items.size() + (loadingFooterVisible ? 1 : 0);
  }

  @NonNull
  @Override
  public final RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_LOADING_FOOTER) {
      return new LoadingFooterViewHolder(
          inflater.inflate(loadingFooterLayoutRes, parent, false));
    }
    return onCreateContentViewHolder(parent);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (position < items.size()) {
      onBindContentViewHolder((VH) holder, position, items.get(position));
    }
  }

  @NonNull
  protected abstract VH onCreateContentViewHolder(@NonNull ViewGroup parent);

  protected abstract void onBindContentViewHolder(
      @NonNull VH holder, int position, @NonNull T item);

  private static final class LoadingFooterViewHolder extends RecyclerView.ViewHolder {
    LoadingFooterViewHolder(@NonNull View itemView) {
      super(itemView);
    }
  }
}
