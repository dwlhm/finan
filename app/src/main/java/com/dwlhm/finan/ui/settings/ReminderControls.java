package com.dwlhm.finan.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.dwlhm.finan.R;
import com.dwlhm.finan.data.prefs.ReminderPreferences;
import com.dwlhm.finan.service.reminder.ReminderScheduler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

/** View-scoped controls; permission results cannot revive abandoned consent. */
public final class ReminderControls {
  private final Fragment fragment;
  private final ActivityResultLauncher<String> permission;
  private MaterialSwitch toggle;
  private TextView status;
  private boolean updating;
  private boolean permissionPending;

  /** Registers the permission result before the fragment reaches STARTED. */
  public ReminderControls(Fragment fragment) {
    this.fragment = fragment;
    permission = fragment.registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
      if (!permissionPending || toggle == null || !fragment.isAdded()) return;
      permissionPending = false;
      ReminderScheduler.setEnabled(fragment.requireContext(), granted);
      refresh();
    });
  }

  /** Builds controls in the dedicated settings container. */
  public void attach(LinearLayout container) {
    android.content.Context context = fragment.requireContext();
    float density = context.getResources().getDisplayMetrics().density;
    int padH = (int) (16 * density);
    int padV = (int) (10 * density);

    // Row containing rose badge, title/status column, and toggle
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
    row.setMinimumHeight((int) (54 * density));
    row.setPadding(padH, padV, padH, padV);

    // Rose squircle badge
    android.widget.FrameLayout badge = new android.widget.FrameLayout(context);
    int badgeSize = (int) (32 * density);
    badge.setLayoutParams(new LinearLayout.LayoutParams(badgeSize, badgeSize));
    badge.setBackgroundResource(R.drawable.bg_badge_reminder);

    android.widget.ImageView icon = new android.widget.ImageView(context);
    int iconSize = (int) (18 * density);
    android.widget.FrameLayout.LayoutParams iconParams =
        new android.widget.FrameLayout.LayoutParams(iconSize, iconSize, android.view.Gravity.CENTER);
    icon.setImageResource(R.drawable.ic_apple_bell);
    icon.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    badge.addView(icon, iconParams);
    row.addView(badge);

    // Title & Status column
    LinearLayout textCol = new LinearLayout(context);
    textCol.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams textParams =
        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    textParams.setMarginStart((int) (12 * density));
    textParams.setMarginEnd((int) (8 * density));
    textCol.setLayoutParams(textParams);

    TextView title = new TextView(context);
    title.setText(R.string.reminder_title);
    title.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.finan_text_dark_primary));
    title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
    title.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_family), android.graphics.Typeface.BOLD);
    textCol.addView(title);

    status = new TextView(context);
    status.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.finan_text_secondary));
    status.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
    LinearLayout.LayoutParams statusParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    statusParams.topMargin = (int) (2 * density);
    textCol.addView(status, statusParams);
    row.addView(textCol);

    // Toggle switch
    toggle = new MaterialSwitch(context);
    row.addView(toggle, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    container.addView(row);

    // System Settings Button (compact tonal styling)
    MaterialButton settings = new MaterialButton(context);
    settings.setText(R.string.reminder_system_settings);
    settings.setCornerRadius((int) (10 * density));
    settings.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.finan_chip_bg));
    settings.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.finan_text_primary));
    settings.setStrokeWidth(0);
    settings.setElevation(0);
    settings.setStateListAnimator(null);
    settings.setMinHeight((int) (40 * density));
    settings.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
    settings.setOnClickListener(view -> fragment.startActivity(
        new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, fragment.requireContext().getPackageName())));

    LinearLayout.LayoutParams btnParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    btnParams.setMargins(padH, (int) (2 * density), padH, (int) (12 * density));
    container.addView(settings, btnParams);

    toggle.setOnCheckedChangeListener((button, checked) -> { if (!updating) onToggle(checked); });
    refresh();
  }

  /** Reconciles externally changed permission and displays the actual scheduling state. */
  public void refresh() {
    if (toggle == null || !fragment.isAdded()) return;
    ReminderScheduler.reconcile(fragment.requireContext());
    updating = true;
    boolean enabled = new ReminderPreferences(fragment.requireContext()).isEnabled();
    toggle.setChecked(enabled);
    toggle.setEnabled(!permissionPending);
    status.setText(enabled ? R.string.reminder_enabled :
        ReminderScheduler.notificationsAllowed(fragment.requireContext())
            ? R.string.reminder_disabled : R.string.reminder_permission_denied);
    updating = false;
  }

  /** Invalidates any permission request associated with a destroyed view. */
  public void detach() { permissionPending = false; toggle = null; status = null; }

  private void onToggle(boolean enabled) {
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && fragment.requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
      permissionPending = true;
      refresh();
      permission.launch(Manifest.permission.POST_NOTIFICATIONS);
      return;
    }
    ReminderScheduler.setEnabled(fragment.requireContext(), enabled);
    refresh();
    if (enabled && !new ReminderPreferences(fragment.requireContext()).isEnabled()
        && ReminderScheduler.notificationsAllowed(fragment.requireContext())) {
      status.setText(R.string.reminder_schedule_failed);
    }
  }
}
