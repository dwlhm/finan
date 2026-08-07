package com.dwlhm.finan.ui;

import android.content.Intent;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
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
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.components.FloatingBottomNavView;
import com.dwlhm.finan.ui.dashboard.DashboardFragment;
import com.dwlhm.finan.ui.settings.SettingsFragment;
import com.dwlhm.finan.ui.settings.TransactionTemplateManagerDialog;
import com.dwlhm.finan.ui.wallet.WalletListFragment;

public final class MainActivity extends AppCompatActivity implements ScreenNavigator {

  public static final String EXTRA_NAV_TARGET = "com.dwlhm.finan.EXTRA_NAV_TARGET";
  public static final String EXTRA_OPEN_TEMPLATE_MANAGER = "com.dwlhm.finan.EXTRA_OPEN_TEMPLATE_MANAGER";
  public static final String EXTRA_TEMPLATE_ID = "com.dwlhm.finan.EXTRA_TEMPLATE_ID";
  public static final String NAV_TARGET_CAPTURE = "capture";
  public static final String NAV_TARGET_DASHBOARD = "dashboard";
  public static final String NAV_TARGET_SETTINGS = "settings";
  public static final String NAV_TARGET_WALLETS = "wallets";
  public static final String NAV_TARGET_CATEGORIES = "categories";

  private static final String KEY_SELECTED_SCREEN = "selected_screen";
  private static final String BACK_STACK_CATEGORY = "category";
  private static final String BACK_STACK_WALLET = "wallet";

  private FloatingBottomNavView bottomNav;
  private Screen selectedScreen = Screen.CAPTURE;
  private boolean isSyncingBottomNav = false;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    bottomNav = findViewById(R.id.bottom_nav);
    setupBottomNav();

    getSupportFragmentManager().addOnBackStackChangedListener(() -> {
      if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
        syncBottomNav();
      }
    });

    if (savedInstanceState != null) {
      selectedScreen =
          Screen.fromTag(savedInstanceState.getString(KEY_SELECTED_SCREEN), Screen.CAPTURE);
      syncBottomNav();
    } else {
      showScreen(selectedScreen, false);
    }

    handleNavIntent(getIntent());
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putString(KEY_SELECTED_SCREEN, selectedScreen.tag);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleNavIntent(intent);
  }

  private void handleNavIntent(@Nullable Intent intent) {
    if (intent == null) {
      return;
    }
    if (intent.getBooleanExtra(EXTRA_OPEN_TEMPLATE_MANAGER, false)) {
      intent.removeExtra(EXTRA_OPEN_TEMPLATE_MANAGER);
      new TransactionTemplateManagerDialog(this, ServicesProvider.get(this), null).show();
    }
    if (intent.hasExtra(EXTRA_TEMPLATE_ID)) {
      long templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, 0L);
      intent.removeExtra(EXTRA_TEMPLATE_ID);
      showScreen(Screen.CAPTURE, false);
      new Handler(Looper.getMainLooper()).postDelayed(
          () -> {
            Fragment fragment =
                getSupportFragmentManager().findFragmentByTag(Screen.CAPTURE.tag);
            if (fragment instanceof CaptureFragment) {
              ((CaptureFragment) fragment).executeTemplateById(templateId);
            }
          },
          150L);
    }
    if (!intent.hasExtra(EXTRA_NAV_TARGET)) {
      return;
    }
    String target = intent.getStringExtra(EXTRA_NAV_TARGET);
    intent.removeExtra(EXTRA_NAV_TARGET);
    if (target == null) {
      return;
    }
    switch (target) {
      case NAV_TARGET_CAPTURE:
        showScreen(Screen.CAPTURE, false);
        break;
      case NAV_TARGET_DASHBOARD:
        showScreen(Screen.DASHBOARD, false);
        break;
      case NAV_TARGET_SETTINGS:
        showScreen(Screen.SETTINGS, false);
        break;
      case NAV_TARGET_WALLETS:
        openWallets();
        break;
      case NAV_TARGET_CATEGORIES:
        openCategories();
        break;
    }
  }

  @Override
  public void openCategories() {
    openSettingsChild(new CategoryListFragment(), CategoryListFragment.TAG, BACK_STACK_CATEGORY);
  }

  @Override
  public void openCategoriesFiltered(String classificationFilter) {
    Bundle args = new Bundle();
    if (classificationFilter != null) {
      args.putString(CategoryListFragment.ARG_CLASSIFICATION_FILTER, classificationFilter);
    }
    CategoryListFragment fragment = new CategoryListFragment();
    fragment.setArguments(args);
    openSettingsChild(fragment, CategoryListFragment.TAG, BACK_STACK_CATEGORY);
  }

  @Override
  public void openWallets() {
    openSettingsChild(new WalletListFragment(), WalletListFragment.TAG, BACK_STACK_WALLET);
  }

  @Override
  public void openHistoryForCategory(long categoryId) {
    showScreen(Screen.DASHBOARD, false);
  }

  @Override
  public void openHistoryWithFilter(String type, long startDate, long endDate, Long walletId, Long categoryId) {
    showScreen(Screen.DASHBOARD, false);
  }

  @Override
  public void openHistoryForActivity(String activity) {
    showScreen(Screen.DASHBOARD, false);
  }

  private void openSettingsChild(Fragment fragment, String tag, String backStackName) {
    FragmentManager fragmentManager = getSupportFragmentManager();

    Fragment existing = fragmentManager.findFragmentByTag(tag);
    if (existing != null && existing.isVisible()) {
      return;
    }

    Fragment currentTab = fragmentManager.findFragmentByTag(selectedScreen.tag);
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
    transaction.add(R.id.main_content, fragment, tag);
    transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED);
    transaction.setPrimaryNavigationFragment(fragment);
    transaction.addToBackStack(backStackName);
    transaction.commit();
    slideUpNavContainer();
  }

  private void setupBottomNav() {
    bottomNav.setOnItemSelectedListener(itemId -> {
      if (isSyncingBottomNav) {
        return true;
      }
      Screen screen = Screen.fromItemId(itemId);
      if (screen != null && screen != selectedScreen) {
        showScreen(screen, false);
      }
      return true;
    });
    syncBottomNav();
  }

  private void showScreen(Screen screen, boolean animate) {
    FragmentManager fragmentManager = getSupportFragmentManager();

    if (fragmentManager.getBackStackEntryCount() > 0) {
      fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    if (screen == selectedScreen && isVisibleTab(screen)) {
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
      if (fragment != null && !tab.tag.equals(screen.tag)) {
        transaction.hide(fragment);
        transaction.setMaxLifecycle(fragment, Lifecycle.State.STARTED);
      }
    }

    Fragment target = fragmentManager.findFragmentByTag(screen.tag);
    if (target == null) {
      target = screen.createFragment();
      transaction.add(R.id.main_content, target, screen.tag);
    } else {
      transaction.show(target);
    }
    transaction.setMaxLifecycle(target, Lifecycle.State.RESUMED);
    transaction.setPrimaryNavigationFragment(target);
    transaction.commit();

    syncBottomNav();
    slideUpNavContainer();
  }

  private void slideUpNavContainer() {
    if (bottomNav != null && bottomNav.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
      androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params =
          (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) bottomNav.getLayoutParams();
      if (params.getBehavior() instanceof com.google.android.material.behavior.HideBottomViewOnScrollBehavior) {
        ((com.google.android.material.behavior.HideBottomViewOnScrollBehavior<android.view.View>) params.getBehavior()).slideUp(bottomNav);
      }
    }
  }

  private boolean isVisibleTab(Screen screen) {
    Fragment fragment = getSupportFragmentManager().findFragmentByTag(screen.tag);
    return fragment != null && fragment.isVisible();
  }

  private void syncBottomNav() {
    if (bottomNav != null && selectedScreen != null) {
      if (bottomNav.getSelectedItemId() != selectedScreen.itemId) {
        isSyncingBottomNav = true;
        try {
          bottomNav.setSelectedItemId(selectedScreen.itemId);
        } finally {
          isSyncingBottomNav = false;
        }
      }
    }
  }

  private enum Screen {
    CAPTURE(R.id.nav_capture, "capture"),
    DASHBOARD(R.id.nav_dashboard, "dashboard"),
    SETTINGS(R.id.nav_settings, "settings");

    @IdRes private final int itemId;
    private final String tag;

    Screen(@IdRes int itemId, String tag) {
      this.itemId = itemId;
      this.tag = tag;
    }

    private Fragment createFragment() {
      switch (this) {
        case DASHBOARD:
          return new DashboardFragment();
        case SETTINGS:
          return new SettingsFragment();
        case CAPTURE:
        default:
          return new CaptureFragment();
      }
    }

    @Nullable
    private static Screen fromItemId(int itemId) {
      for (Screen screen : values()) {
        if (screen.itemId == itemId) {
          return screen;
        }
      }
      return null;
    }

    private static Screen fromTag(@Nullable String tag, Screen fallback) {
      if (tag == null) {
        return fallback;
      }
      for (Screen screen : values()) {
        if (screen.tag.equals(tag)) {
          return screen;
        }
      }
      return fallback;
    }
  }
}
