package com.dwlhm.finan.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class ScreenFragment extends Fragment {

  @Override
  public final View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(getLayoutResId(), container, false);
  }

  @Override
  public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    onViewReady(view, savedInstanceState);
  }

  @LayoutRes
  protected abstract int getLayoutResId();

  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {}
}
