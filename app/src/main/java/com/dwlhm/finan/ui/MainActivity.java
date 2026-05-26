package com.dwlhm.finan.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.dwlhm.finan.R;
import com.dwlhm.finan.ui.capture.CaptureFragment;
import com.dwlhm.finan.ui.category.CategoryListFragment;
import com.dwlhm.finan.ui.common.ScreenNavigator;
import com.dwlhm.finan.ui.history.HistoryFragment;
import com.dwlhm.finan.ui.settings.SettingsFragment;
import com.dwlhm.finan.ui.summary.SummaryFragment;
import com.dwlhm.finan.ui.wallet.WalletListFragment;

public final class MainActivity extends AppCompatActivity implements ScreenNavigator {

  private static final String KEY_SELECTED_SCREEN = "selected_screen";
  private static final String BACK_STACK_CATEGORY = "category";

  private Screen selectedScreen = Screen.CAPTURE;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupBottomNav();

    getSupportFragmentManager().addOnBackStackChangedListener(this::syncBottomNav);

    if (savedInstanceState != null) {
      selectedScreen =
          Screen.fromTag(savedInstanceState.getString(KEY_SELECTED_SCREEN), Screen.CAPTURE);
      syncBottomNav();
    } else {
      showScreen(selectedScreen, false);
    }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putString(KEY_SELECTED_SCREEN, selectedScreen.tag);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void openCategories() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.executePendingTransactions();

    Fragment existing = fragmentManager.findFragmentByTag(CategoryListFragment.TAG);
    if (existing != null && existing.isVisible()) {
      return;
    }

    Fragment currentTab = fragmentManager.findFragmentByTag(selectedScreen.tag);
    CategoryListFragment categoryFragment = new CategoryListFragment();
    FragmentTransaction transaction =
        fragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                R.anim.screen_enter,
                R.anim.screen_exit,
                R.anim.screen_pop_enter,
                R.anim.screen_pop_exit);

    if (currentTab != null) {
      transaction.hide(currentTab);
      transaction.setMaxLifecycle(currentTab, Lifecycle.State.STARTED);
    }
    transaction.add(R.id.main_content, categoryFragment, CategoryListFragment.TAG);
    transaction.setMaxLifecycle(categoryFragment, Lifecycle.State.RESUMED);
    transaction.setPrimaryNavigationFragment(categoryFragment);
    transaction.addToBackStack(BACK_STACK_CATEGORY);
    transaction.commit();
    syncBottomNav();
  }

  private void setupBottomNav() {
    setNavClick(R.id.nav_capture, Screen.CAPTURE);
    setNavClick(R.id.nav_history, Screen.HISTORY);
    setNavClick(R.id.nav_summary, Screen.SUMMARY);
    setNavClick(R.id.nav_wallet, Screen.WALLET);
    setNavClick(R.id.nav_settings, Screen.SETTINGS);
    syncBottomNav();
  }

  private void setNavClick(@IdRes int buttonId, Screen screen) {
    View item = findViewById(buttonId);
    item.setOnClickListener(v -> showScreen(screen, true));
  }

  private void showScreen(Screen screen, boolean animate) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.executePendingTransactions();

    if (fragmentManager.getBackStackEntryCount() > 0) {
      fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    if (screen == selectedScreen && isVisibleTab(screen)) {
      syncBottomNav();
      return;
    }

    selectedScreen = screen;

    FragmentTransaction transaction =
        fragmentManager.beginTransaction().setReorderingAllowed(true);
    if (animate) {
      transaction.setCustomAnimations(R.anim.screen_enter, R.anim.screen_exit);
    }

    for (Screen tab : Screen.values()) {
      Fragment fragment = fragmentManager.findFragmentByTag(tab.tag);
      if (fragment != null) {
        transaction.hide(fragment);
        transaction.setMaxLifecycle(fragment, Lifecycle.State.STARTED);
      }
    }

    Fragment target = fragmentManager.findFragmentByTag(screen.tag);
    if (target == null) {
      target = screen.createFragment();
      transaction.add(R.id.main_content, target, screen.tag);
    }
    transaction.show(target);
    transaction.setMaxLifecycle(target, Lifecycle.State.RESUMED);
    transaction.setPrimaryNavigationFragment(target);
    transaction.commit();
    syncBottomNav();
  }

  private boolean isVisibleTab(Screen screen) {
    Fragment fragment = getSupportFragmentManager().findFragmentByTag(screen.tag);
    return fragment != null && fragment.isVisible();
  }

  private void syncBottomNav() {
    setSelected(R.id.nav_capture, selectedScreen == Screen.CAPTURE);
    setSelected(R.id.nav_history, selectedScreen == Screen.HISTORY);
    setSelected(R.id.nav_summary, selectedScreen == Screen.SUMMARY);
    setSelected(R.id.nav_wallet, selectedScreen == Screen.WALLET);
    setSelected(R.id.nav_settings, selectedScreen == Screen.SETTINGS);
  }

  private void setSelected(@IdRes int buttonId, boolean selected) {
    View item = findViewById(buttonId);
    item.setSelected(selected);
  }

  private enum Screen {
    CAPTURE("capture"),
    HISTORY("history"),
    SUMMARY("summary"),
    WALLET("wallet"),
    SETTINGS("settings");

    private final String tag;

    Screen(String tag) {
      this.tag = tag;
    }

    private Fragment createFragment() {
      switch (this) {
        case HISTORY:
          return new HistoryFragment();
        case SUMMARY:
          return new SummaryFragment();
        case WALLET:
          return new WalletListFragment();
        case SETTINGS:
          return new SettingsFragment();
        case CAPTURE:
        default:
          return new CaptureFragment();
      }
    }

    private static Screen fromTag(@Nullable String tag, Screen fallback) {
      for (Screen screen : values()) {
        if (screen.tag.equals(tag)) {
          return screen;
        }
      }
      return fallback;
    }
  }
}
