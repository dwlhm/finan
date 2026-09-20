package com.dwlhm.finan.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dwlhm.finan.service.privacy.AppLock;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.ui.capture.CaptureFragment;
import com.dwlhm.finan.ui.category.CategoryListFragment;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ScreenNavigator;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.ui.dashboard.DashboardFragment;
import com.dwlhm.finan.ui.settings.SettingsFragment;
import com.dwlhm.finan.ui.settings.TransactionTemplateManagerDialog;
import com.dwlhm.finan.ui.wallet.WalletListFragment;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.dwlhm.finan.util.ui.ViewPressAnimator;

import java.time.LocalDate;

public final class MainActivity extends AppCompatActivity implements ScreenNavigator {

  public static final String EXTRA_NAV_TARGET = "com.dwlhm.finan.EXTRA_NAV_TARGET";
  public static final String EXTRA_OPEN_TEMPLATE_MANAGER = "com.dwlhm.finan.EXTRA_OPEN_TEMPLATE_MANAGER";
  public static final String EXTRA_TEMPLATE_ID = "com.dwlhm.finan.EXTRA_TEMPLATE_ID";
  public static final String EXTRA_OPEN_DUE_REMINDERS = "com.dwlhm.finan.EXTRA_OPEN_DUE_REMINDERS";
  public static final String NAV_TARGET_CAPTURE = "capture";
  public static final String NAV_TARGET_DASHBOARD = "dashboard";
  public static final String NAV_TARGET_SETTINGS = "settings";
  public static final String NAV_TARGET_WALLETS = "wallets";
  public static final String NAV_TARGET_CATEGORIES = "categories";

  private static final String KEY_PENDING_INTENT = "pending_nav_intent";
  private Intent pendingNavIntent;
  private boolean pendingReminderOpen;
  private boolean reminderQueryPending;
  private long reminderRequestGeneration;
  private com.dwlhm.finan.ui.dashboard.UpcomingDetailBottomSheet reminderDialog;

  private static final String KEY_SELECTED_SCREEN = "selected_screen";
  private static final String BACK_STACK_CATEGORY = "category";
  private static final String BACK_STACK_WALLET = "wallet";

  private View whiteSlidingSheet;
  private View bottomBarBukuKas;
  private TextView bottomBarBalanceText;
  private TextView bottomBarTrendText;
  private ImageView bottomBarTrendIcon;
  private View bottomBarTrendPill;
  private ImageView bottomBarBtnPlus;
  private ImageView bottomBarBtnSettings;
  private View settingsOverlayContainer;
  private View captureContainer;
  // Navigation back stack — bottom = root, top = current screen
  private final java.util.ArrayDeque<Screen> navStack = new java.util.ArrayDeque<>();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    whiteSlidingSheet = findViewById(R.id.white_sliding_sheet);
    bottomBarBukuKas = findViewById(R.id.bottom_bar_buku_kas);
    bottomBarBalanceText = findViewById(R.id.bottom_bar_balance);
    bottomBarTrendText = findViewById(R.id.bottom_bar_trend);
    bottomBarTrendIcon = findViewById(R.id.bottom_bar_trend_icon);
    bottomBarTrendPill = findViewById(R.id.bottom_bar_trend_pill);
    bottomBarBtnPlus = findViewById(R.id.bottom_bar_btn_plus);
    bottomBarBtnSettings = findViewById(R.id.bottom_bar_btn_settings);
    settingsOverlayContainer = findViewById(R.id.settings_overlay_container);
    captureContainer = findViewById(R.id.capture_container);

    if (bottomBarBtnPlus != null) {
      ViewPressAnimator.bindSpringScale(bottomBarBtnPlus);
      bottomBarBtnPlus.setOnClickListener(v -> navigateTo(Screen.CAPTURE));
    }

    if (bottomBarBtnSettings != null) {
      ViewPressAnimator.bindSpringScale(bottomBarBtnSettings);
      bottomBarBtnSettings.setOnClickListener(v -> navigateTo(Screen.SETTINGS));
    }

    AppServices services = ServicesProvider.get(this);
    services.dbWorker.compute(() -> {
      try {
        LocalDate today = LocalDate.now();
        LocalDate startCurrent = today.withDayOfMonth(1);
        LocalDate endCurrent = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate startPrev = startCurrent.minusMonths(1);
        LocalDate endPrev = startPrev.withDayOfMonth(startPrev.lengthOfMonth());

        MonthlySummary currentSummary = services.summaryService.loadRange(startCurrent, endCurrent, null, null);
        MonthlySummary prevSummary = services.summaryService.loadRange(startPrev, endPrev, null, null);

        long currentNet = currentSummary.getMonthIncomeMinor() - currentSummary.getMonthExpenseMinor();
        long prevNet = prevSummary.getMonthIncomeMinor() - prevSummary.getMonthExpenseMinor();

        int percentageTrend = 0;
        if (prevNet != 0L) {
          percentageTrend = (int) Math.round(((double) (currentNet - prevNet) / Math.abs(prevNet)) * 100);
        } else if (currentNet > 0) {
          percentageTrend = 100;
        } else if (currentNet < 0) {
          percentageTrend = -100;
        }
        return new long[]{currentNet, percentageTrend};
      } catch (Exception e) {
        return null;
      }
    }, result -> {
      if (result != null && !isFinishing() && !isDestroyed()) {
        updateBottomBarSummary(result[0], (int) result[1]);
      }
    });

    FragmentManager fm = getSupportFragmentManager();
    CaptureFragment captureFragment = (CaptureFragment) fm.findFragmentById(R.id.capture_container);
    if (captureFragment == null) {
      captureFragment = new CaptureFragment();
      fm.beginTransaction().replace(R.id.capture_container, captureFragment, "capture").commit();
    }
    bindCaptureNavigationListener(captureFragment);

    DashboardFragment dashboardFragment = (DashboardFragment) fm.findFragmentById(R.id.dashboard_container);
    if (dashboardFragment == null) {
      dashboardFragment = new DashboardFragment();
      fm.beginTransaction().replace(R.id.dashboard_container, dashboardFragment, "dashboard").commit();
    }

    Screen initialScreen = savedInstanceState != null
        ? Screen.fromTag(savedInstanceState.getString(KEY_SELECTED_SCREEN), Screen.CAPTURE)
        : Screen.CAPTURE;
    navStack.push(initialScreen);
    applyImmediateState(initialScreen);

    handleNavIntent(savedInstanceState != null && savedInstanceState.containsKey(KEY_PENDING_INTENT)
        ? savedInstanceState.getParcelable(KEY_PENDING_INTENT) : getIntent());
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putString(KEY_SELECTED_SCREEN, currentScreen().tag);
    if (pendingNavIntent != null) outState.putParcelable(KEY_PENDING_INTENT, pendingNavIntent);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    reminderRequestGeneration++;
    reminderQueryPending = false;
    setIntent(intent);
    handleNavIntent(intent);
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    // AndroidX publishes RESUMED after this callback returns on current Android versions.
    getWindow().getDecorView().post(() -> {
      if (!isDestroyed() && !isFinishing()) handleNavIntent(pendingNavIntent);
    });
  }

  private void handleNavIntent(@Nullable Intent intent) {
    if (intent == null) return;
    pendingNavIntent = intent;
    pendingReminderOpen = intent.getBooleanExtra(EXTRA_OPEN_DUE_REMINDERS, false);
    AppLock.afterUnlock(this, () -> consumeNavIntent(intent));
  }

  private void consumeNavIntent(Intent intent) {
    if (intent != pendingNavIntent || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)
        || getSupportFragmentManager().isStateSaved()) return;
    openPendingReminder();
    if (intent.getBooleanExtra(EXTRA_OPEN_TEMPLATE_MANAGER, false)) {
      intent.removeExtra(EXTRA_OPEN_TEMPLATE_MANAGER);
      new TransactionTemplateManagerDialog(this, ServicesProvider.get(this), null).show();
    }
    if (intent.hasExtra(EXTRA_TEMPLATE_ID)) {
      long templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, 0L);
      if (templateId > 0) {
        navigateTo(Screen.CAPTURE);
        getSupportFragmentManager().executePendingTransactions();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(Screen.CAPTURE.tag);
        if (fragment instanceof CaptureFragment && fragment.getView() != null) {
          intent.removeExtra(EXTRA_TEMPLATE_ID);
          ((CaptureFragment) fragment).executeTemplateById(templateId);
        }
      } else {
        intent.removeExtra(EXTRA_TEMPLATE_ID);
      }
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
        navigateTo(Screen.CAPTURE);
        break;
      case NAV_TARGET_DASHBOARD:
        navigateTo(Screen.DASHBOARD);
        break;
      case NAV_TARGET_SETTINGS:
        navigateTo(Screen.SETTINGS);
        break;
      case NAV_TARGET_WALLETS:
        openWallets();
        break;
      case NAV_TARGET_CATEGORIES:
        openCategories();
        break;
    }
  }

  private void openPendingReminder() {
    if (!pendingReminderOpen || reminderQueryPending || isFinishing() || isDestroyed()
        || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)
        || getSupportFragmentManager().isStateSaved()) return;
    long generation = reminderRequestGeneration;
    AppLock.afterUnlock(this, () -> {
      if (generation != reminderRequestGeneration || !pendingReminderOpen || reminderQueryPending
          || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)
          || getSupportFragmentManager().isStateSaved()) return;
      reminderQueryPending = true;
      java.time.ZoneId zone = java.time.ZoneId.systemDefault();
      java.time.LocalDate today = java.time.LocalDate.now(zone);
      com.dwlhm.finan.ui.common.AppServices services = ServicesProvider.get(this);
      services.dbWorker.compute(() -> {
        try {
          return new ReminderQueryResult(services.createUpcomingCashFlowService(zone)
              .calculateForwardSummary(today, today, null), null);
        } catch (Exception error) {
          return new ReminderQueryResult(null, error);
        }
      }, result -> {
        if (generation != reminderRequestGeneration || isDestroyed() || isFinishing()) return;
        reminderQueryPending = false;
        AppLock.afterUnlock(this, () -> {
          if (generation != reminderRequestGeneration || !pendingReminderOpen
              || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)
              || getSupportFragmentManager().isStateSaved()) return;
          if (!zone.equals(java.time.ZoneId.systemDefault()) || !today.equals(java.time.LocalDate.now(zone))) {
            openPendingReminder();
            return;
          }
          pendingReminderOpen = false;
          if (pendingNavIntent != null) pendingNavIntent.removeExtra(EXTRA_OPEN_DUE_REMINDERS);
          if (result.error != null) {
            android.widget.Toast.makeText(this, R.string.reminder_query_failed,
                android.widget.Toast.LENGTH_LONG).show();
            return;
          }
          if (reminderDialog != null) reminderDialog.dismiss();
          reminderDialog = new com.dwlhm.finan.ui.dashboard.UpcomingDetailBottomSheet(this,
              services, result.summary, com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.MASKED, null);
          reminderDialog.show();
        });
      });
    });
  }

  @Override protected void onPause() {
    reminderRequestGeneration++;
    reminderQueryPending = false;
    super.onPause();
  }

  @Override protected void onDestroy() {
    reminderRequestGeneration++;
    if (reminderDialog != null) {
      reminderDialog.dismiss();
      reminderDialog = null;
    }
    super.onDestroy();
  }

  private static final class ReminderQueryResult {
    final com.dwlhm.finan.domain.model.ForwardCashFlowSummary summary;
    final Exception error;
    ReminderQueryResult(com.dwlhm.finan.domain.model.ForwardCashFlowSummary summary, Exception error) {
      this.summary = summary;
      this.error = error;
    }
  }

  @Override
  public void openCapture() {
    navigateTo(Screen.CAPTURE);
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
    android.content.Intent intent = new android.content.Intent(this, com.dwlhm.finan.ui.search.SearchTransactionActivity.class);
    intent.putExtra("filter_category_id", categoryId);
    startActivity(intent);
  }

  @Override
  public void openHistoryWithFilter(String type, long startDate, long endDate, Long walletId, Long categoryId) {
    Intent intent = new Intent(this, com.dwlhm.finan.ui.search.SearchTransactionActivity.class);
    if (categoryId != null) intent.putExtra("filter_category_id", categoryId.longValue());
    if (walletId != null) intent.putExtra("filter_wallet_id", walletId.longValue());
    intent.putExtra("filter_type", type);
    intent.putExtra("filter_start", startDate);
    intent.putExtra("filter_end", endDate);
    startActivity(intent);
  }

  @Override
  public void openHistoryForActivity(String activity) {
    navigateTo(Screen.DASHBOARD);
  }

  private void bindCaptureNavigationListener(CaptureFragment fragment) {
    if (fragment != null) {
      fragment.setOnCaptureNavigationListener(new CaptureFragment.OnCaptureNavigationListener() {
        @Override
        public void onNavigateToBukuKas() {
          navigateTo(Screen.DASHBOARD);
        }

        @Override
        public void onNavigateToSettings() {
          navigateTo(Screen.SETTINGS);
        }
      });
    }
  }

  private Screen currentScreen() {
    return navStack.isEmpty() ? Screen.CAPTURE : navStack.peek();
  }

  private boolean isSettingsOpen() {
    return currentScreen() == Screen.SETTINGS;
  }

  private void navigateTo(Screen screen) {
    Screen from = currentScreen();
    if (from == screen) return;

    // If destination already exists in the stack, pop back to it (avoids duplicates).
    // Otherwise push it on top — preserves back history.
    if (navStack.contains(screen)) {
      while (navStack.peek() != screen) {
        navStack.pop();
      }
    } else {
      navStack.push(screen);
    }

    applyTransition(from, screen);
  }

  private void navigateBack() {
    if (navStack.size() <= 1) {
      super.onBackPressed();
      return;
    }
    Screen from = navStack.pop();
    Screen to = navStack.peek();
    applyTransition(from, to);
  }

  private void applyTransition(Screen from, Screen to) {
    NavigationTransition transition = NavigationTransition.resolve(from, to);
    cancelNavigationAnimations();
    prepareDestination(from, to, transition);
    if (transition.bottomBarMotion() == BottomBarMotion.FADE_IN
        || (transition.overlayMotion() == OverlayMotion.NONE
            && transition.bottomBarMotion() == BottomBarMotion.NONE)) {
      configureBottomBarFor(to);
    }
    applyOverlayMotion(from, to, transition.overlayMotion());
    applyBottomBarMotion(transition.bottomBarMotion(), to);
  }

  private void cancelNavigationAnimations() {
    if (whiteSlidingSheet != null) whiteSlidingSheet.animate().cancel();
    if (settingsOverlayContainer != null) settingsOverlayContainer.animate().cancel();
    if (captureContainer != null) captureContainer.animate().cancel();
    if (bottomBarBukuKas != null) bottomBarBukuKas.animate().cancel();
  }

  private void prepareDestination(Screen from, Screen to, NavigationTransition transition) {
    if (to == Screen.CAPTURE) {
      if (captureContainer != null) captureContainer.setVisibility(View.VISIBLE);
      if (bottomBarBukuKas != null && transition.bottomBarMotion() != BottomBarMotion.FADE_OUT) {
        bottomBarBukuKas.setVisibility(View.GONE);
      }
      if (from == Screen.SETTINGS) {
        if (whiteSlidingSheet != null) {
          whiteSlidingSheet.setVisibility(View.GONE);
          whiteSlidingSheet.setTranslationY(-sheetHeight(whiteSlidingSheet));
          whiteSlidingSheet.setAlpha(1f);
        }
      } else if (from == Screen.DASHBOARD) {
        if (settingsOverlayContainer != null) {
          settingsOverlayContainer.setVisibility(View.GONE);
          settingsOverlayContainer.setTranslationY(-sheetHeight(settingsOverlayContainer));
        }
        if (whiteSlidingSheet != null) {
          whiteSlidingSheet.setAlpha(1f);
        }
      } else {
        if (whiteSlidingSheet != null) {
          whiteSlidingSheet.setVisibility(View.GONE);
          whiteSlidingSheet.setTranslationY(-sheetHeight(whiteSlidingSheet));
          whiteSlidingSheet.setAlpha(1f);
        }
        if (settingsOverlayContainer != null) {
          settingsOverlayContainer.setVisibility(View.GONE);
          settingsOverlayContainer.setTranslationY(-sheetHeight(settingsOverlayContainer));
        }
      }
      return;
    }

    if (captureContainer != null) captureContainer.setVisibility(View.INVISIBLE);
    if (bottomBarBukuKas != null) {
      bottomBarBukuKas.setVisibility(View.VISIBLE);
      bottomBarBukuKas.setAlpha(transition.bottomBarMotion() == BottomBarMotion.FADE_IN ? 0f : 1f);
    }

    if (to == Screen.DASHBOARD) {
      if (settingsOverlayContainer != null) {
        settingsOverlayContainer.setVisibility(View.GONE);
        settingsOverlayContainer.setTranslationY(-sheetHeight(settingsOverlayContainer));
      }
      if (whiteSlidingSheet != null) {
        whiteSlidingSheet.setVisibility(View.VISIBLE);
        whiteSlidingSheet.setAlpha(transition.overlayMotion() == OverlayMotion.FADE_IN ? 0f : 1f);
        if (transition.overlayMotion() == OverlayMotion.SLIDE_DOWN) {
          whiteSlidingSheet.setTranslationY(-sheetHeight(whiteSlidingSheet));
        } else {
          whiteSlidingSheet.setTranslationY(0f);
        }
      }
    } else {
      FragmentManager fm = getSupportFragmentManager();
      Fragment existing = fm.findFragmentById(R.id.settings_container);
      if (existing == null || !(existing instanceof SettingsFragment)) {
        fm.beginTransaction().replace(R.id.settings_container, new SettingsFragment(), "settings").commit();
      }
      if (whiteSlidingSheet != null) {
        whiteSlidingSheet.setVisibility(from == Screen.DASHBOARD ? View.VISIBLE : View.GONE);
        whiteSlidingSheet.setAlpha(1f);
        whiteSlidingSheet.setTranslationY(0f);
      }
      if (settingsOverlayContainer != null) {
        settingsOverlayContainer.setVisibility(View.VISIBLE);
        settingsOverlayContainer.setAlpha(transition.overlayMotion() == OverlayMotion.FADE_IN ? 0f : 1f);
        settingsOverlayContainer.setTranslationY(
            transition.overlayMotion() == OverlayMotion.SLIDE_DOWN
                ? -sheetHeight(settingsOverlayContainer) : 0f);
      }
    }
  }

  private int sheetHeight(View view) {
    int height = view.getHeight();
    return height == 0 ? getResources().getDisplayMetrics().heightPixels : height;
  }

  private void applyOverlayMotion(Screen from, Screen to, OverlayMotion motion) {
    View target = motion == OverlayMotion.SLIDE_UP && from == Screen.SETTINGS
        ? settingsOverlayContainer
        : (to == Screen.SETTINGS ? settingsOverlayContainer : whiteSlidingSheet);
    if (motion == OverlayMotion.NONE || target == null) {
      if (to == Screen.DASHBOARD && whiteSlidingSheet != null) whiteSlidingSheet.setAlpha(1f);
      return;
    }
    if (motion == OverlayMotion.SLIDE_DOWN) {
      target.animate().translationY(0f).setDuration(320)
          .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
          .withEndAction(() -> {
            if (to == Screen.DASHBOARD && captureContainer != null) captureContainer.setVisibility(View.GONE);
          }).start();
    } else if (motion == OverlayMotion.SLIDE_UP) {
      target.animate().translationY(-sheetHeight(target)).setDuration(320)
          .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
          .withEndAction(() -> {
            target.setVisibility(View.GONE);
            target.setTranslationY(-sheetHeight(target));
            if (to == Screen.CAPTURE) {
              if (whiteSlidingSheet != null) {
                whiteSlidingSheet.setVisibility(View.GONE);
                whiteSlidingSheet.setTranslationY(-sheetHeight(whiteSlidingSheet));
                whiteSlidingSheet.setAlpha(1f);
              }
              if (settingsOverlayContainer != null) {
                settingsOverlayContainer.setVisibility(View.GONE);
                settingsOverlayContainer.setTranslationY(-sheetHeight(settingsOverlayContainer));
              }
            }
          }).start();
    } else if (motion == OverlayMotion.FADE_IN) {
      target.animate().alpha(1f).setDuration(220)
          .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
          .setListener(null)
          .withEndAction(() -> {
            configureBottomBarFor(to);
            if (to == Screen.SETTINGS) {
              if (whiteSlidingSheet != null) {
                whiteSlidingSheet.setVisibility(View.GONE);
                whiteSlidingSheet.setTranslationY(-sheetHeight(whiteSlidingSheet));
              }
            }
          })
          .start();
      if (from == Screen.SETTINGS && settingsOverlayContainer != null) {
        settingsOverlayContainer.setVisibility(View.GONE);
      }
    }
  }

  private void applyBottomBarMotion(BottomBarMotion motion, Screen destination) {
    if (bottomBarBukuKas == null) return;
    if (motion == BottomBarMotion.FADE_IN) {
      bottomBarBukuKas.setVisibility(View.VISIBLE);
      bottomBarBukuKas.animate().alpha(1f).setDuration(220)
          .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
          .setListener(null).start();
    } else if (motion == BottomBarMotion.FADE_OUT) {
      bottomBarBukuKas.setVisibility(View.VISIBLE);
      bottomBarBukuKas.animate().alpha(0f).setDuration(220)
          .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
          .withEndAction(() -> {
            bottomBarBukuKas.setVisibility(View.GONE);
            configureBottomBarFor(destination);
          }).start();
    } else {
      bottomBarBukuKas.setAlpha(1f);
      bottomBarBukuKas.setVisibility(View.VISIBLE);
    }
  }

  private void configureBottomBarFor(Screen screen) {
    if (bottomBarBtnSettings == null) return;
    if (screen == Screen.SETTINGS) {
      bottomBarBtnSettings.setImageResource(R.drawable.ic_nav_bukukas_3d);
      bottomBarBtnSettings.setContentDescription(getString(R.string.nav_dashboard));
      bottomBarBtnSettings.setOnClickListener(v -> navigateTo(Screen.DASHBOARD));
    } else {
      bottomBarBtnSettings.setImageResource(R.drawable.ic_nav_settings_3d);
      bottomBarBtnSettings.setContentDescription(getString(R.string.nav_settings));
      bottomBarBtnSettings.setOnClickListener(v -> navigateTo(Screen.SETTINGS));
    }
  }

  private void dismissSettingsOverlay() {
    if (settingsOverlayContainer == null) return;
    settingsOverlayContainer.animate().cancel();
    settingsOverlayContainer.setVisibility(View.GONE);
  }

  private void applyImmediateState(Screen screen) {
    if (screen == Screen.CAPTURE) {
      if (captureContainer != null) captureContainer.setVisibility(View.VISIBLE);
      if (bottomBarBukuKas != null) bottomBarBukuKas.setVisibility(View.GONE);
      if (whiteSlidingSheet != null) {
        whiteSlidingSheet.setVisibility(View.INVISIBLE);
        whiteSlidingSheet.post(() -> {
          if (whiteSlidingSheet == null) return;
          int h = whiteSlidingSheet.getHeight();
          if (h == 0) h = getResources().getDisplayMetrics().heightPixels;
          whiteSlidingSheet.setTranslationY(-h);
          whiteSlidingSheet.setVisibility(View.GONE);
        });
      }
      if (bottomBarBtnSettings != null) {
        configureBottomBarFor(Screen.CAPTURE);
      }
    } else if (screen == Screen.DASHBOARD) {
      if (captureContainer != null) captureContainer.setVisibility(View.GONE);
      if (bottomBarBukuKas != null) bottomBarBukuKas.setVisibility(View.VISIBLE);
      if (whiteSlidingSheet != null) {
        whiteSlidingSheet.setVisibility(View.VISIBLE);
        whiteSlidingSheet.setTranslationY(0);
      }
      if (bottomBarBtnSettings != null) {
        configureBottomBarFor(Screen.DASHBOARD);
      }
    } else { // SETTINGS — unlikely on cold start but handle it
      applyImmediateState(Screen.CAPTURE);
      navigateTo(Screen.SETTINGS);
    }
  }

  public void updateBottomBarSummary(long balanceMinor, int percentageTrend) {
    if (bottomBarBalanceText != null) {
      bottomBarBalanceText.setText(MoneyFormatter.format(balanceMinor));
    }
    if (bottomBarTrendIcon != null && bottomBarTrendText != null) {
      if (percentageTrend > 0) {
        bottomBarTrendIcon.setImageResource(R.drawable.ic_trend_up);
        bottomBarTrendIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#A3F5CF")));
        bottomBarTrendText.setTextColor(Color.parseColor("#A3F5CF"));
        bottomBarTrendText.setText("+" + percentageTrend + "%");
      } else if (percentageTrend < 0) {
        bottomBarTrendIcon.setImageResource(R.drawable.ic_trend_down);
        bottomBarTrendIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFB4AB")));
        bottomBarTrendText.setTextColor(Color.parseColor("#FFB4AB"));
        bottomBarTrendText.setText(percentageTrend + "%");
      } else {
        bottomBarTrendIcon.setImageResource(R.drawable.ic_trend_neutral);
        bottomBarTrendIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D7E8E6")));
        bottomBarTrendText.setTextColor(Color.parseColor("#D7E8E6"));
        bottomBarTrendText.setText("0%");
      }
    }
  }

  @Override
  public void onBackPressed() {
    if (isSettingsOpen()) {
      FragmentManager fm = getSupportFragmentManager();
      if (fm.getBackStackEntryCount() > 0) {
        fm.popBackStack();
        return;
      }
    }
    navigateBack();
  }

  private void openSettingsChild(Fragment fragment, String tag, String backStackName) {
    if (!isSettingsOpen()) {
      navigateTo(Screen.SETTINGS);
    } else if (settingsOverlayContainer != null) {
      settingsOverlayContainer.setVisibility(View.VISIBLE);
    }
    FragmentManager fragmentManager = getSupportFragmentManager();

    Fragment existing = fragmentManager.findFragmentByTag(tag);
    if (existing != null && existing.isVisible()) {
      return;
    }

    fragmentManager
        .beginTransaction()
        .setReorderingAllowed(true)
        .setCustomAnimations(
            R.anim.screen_enter,
            R.anim.screen_exit,
            R.anim.screen_pop_enter,
            R.anim.screen_pop_exit)
        .replace(R.id.settings_container, fragment, tag)
        .addToBackStack(backStackName)
        .commit();
  }

  enum OverlayMotion {
    NONE,
    SLIDE_DOWN,
    SLIDE_UP,
    FADE_IN
  }

  enum BottomBarMotion {
    NONE,
    FADE_IN,
    FADE_OUT
  }

  static final class NavigationTransition {
    private final OverlayMotion overlayMotion;
    private final BottomBarMotion bottomBarMotion;

    private NavigationTransition(OverlayMotion overlayMotion, BottomBarMotion bottomBarMotion) {
      this.overlayMotion = overlayMotion;
      this.bottomBarMotion = bottomBarMotion;
    }

    OverlayMotion overlayMotion() {
      return overlayMotion;
    }

    BottomBarMotion bottomBarMotion() {
      return bottomBarMotion;
    }

    static NavigationTransition resolve(Screen from, Screen to) {
      if (from == null || to == null || from == to) {
        return new NavigationTransition(OverlayMotion.NONE, BottomBarMotion.NONE);
      }
      if (from == Screen.CAPTURE && (to == Screen.DASHBOARD || to == Screen.SETTINGS)) {
        return new NavigationTransition(OverlayMotion.SLIDE_DOWN, BottomBarMotion.FADE_IN);
      }
      if ((from == Screen.DASHBOARD || from == Screen.SETTINGS) && to == Screen.CAPTURE) {
        return new NavigationTransition(OverlayMotion.SLIDE_UP, BottomBarMotion.FADE_OUT);
      }
      if ((from == Screen.DASHBOARD && to == Screen.SETTINGS)
          || (from == Screen.SETTINGS && to == Screen.DASHBOARD)) {
        return new NavigationTransition(OverlayMotion.FADE_IN, BottomBarMotion.NONE);
      }
      return new NavigationTransition(OverlayMotion.NONE, BottomBarMotion.NONE);
    }
  }

  enum Screen {
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
