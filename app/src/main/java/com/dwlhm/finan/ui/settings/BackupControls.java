package com.dwlhm.finan.ui.settings;

import android.content.Intent;
import android.content.Context;
import com.dwlhm.finan.R;
import android.net.Uri;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.dwlhm.finan.service.backup.BackupService;
import com.dwlhm.finan.ui.MainActivity;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.ServicesProvider;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/** Passwords and decrypted previews are kept only for the current operation. */
final class BackupControls {
  private final Fragment fragment;
  private final ActivityResultLauncher<String> export;
  private final ActivityResultLauncher<String[]> restore;
  private boolean busy;
  private boolean closed = true;
  private long generation;
  private Uri queuedUri;
  private boolean queuedRestore;
  private BackupService.Preview pending;
  private AlertDialog dialog;

  BackupControls(Fragment fragment) {
    this.fragment = fragment;
    export = fragment.registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"),
        uri -> receiveUri(uri, false));
    restore = fragment.registerForActivityResult(new ActivityResultContracts.OpenDocument(),
        uri -> receiveUri(uri, true));
  }

  void attach(LinearLayout container) {
    closed = false;
    generation++;
    busy = false;
    container.post(() -> {
      if (ready() && queuedUri != null) {
        Uri uri = queuedUri;
        boolean restoring = queuedRestore;
        queuedUri = null;
        askPassword(uri, restoring);
      }
    });
    addButton(container, fragment.getString(R.string.backup_create), () -> export.launch("finan-backup.finan"));
    addButton(container, fragment.getString(R.string.backup_restore), () -> restore.launch(new String[]{"application/octet-stream", "*/*"}));
  }

  private void addButton(LinearLayout container, String text, Runnable action) {
    MaterialButton button = new MaterialButton(container.getContext());
    button.setText(text);
    float density = container.getResources().getDisplayMetrics().density;
    button.setCornerRadius((int) (10 * density));
    button.setBackgroundColor(androidx.core.content.ContextCompat.getColor(container.getContext(), R.color.finan_chip_bg));
    button.setTextColor(androidx.core.content.ContextCompat.getColor(container.getContext(), R.color.finan_text_primary));
    button.setStrokeWidth(0);
    button.setElevation(0);
    button.setStateListAnimator(null);
    button.setMinHeight((int) (42 * density));
    button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
    button.setOnClickListener(v -> { if (!busy) action.run(); });
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT);
    int marginH = (int) (16 * density);
    int marginV = (int) (4 * density);
    params.setMargins(marginH, marginV, marginH, marginV);
    container.addView(button, params);
  }

  private void receiveUri(Uri uri, boolean restoring) {
    if (uri == null) return;
    if (ready()) askPassword(uri, restoring);
    else { queuedUri = uri; queuedRestore = restoring; }
  }

  private boolean ready() { return !closed && fragment.isAdded() && fragment.getView() != null; }
  private boolean current(long operation) { return operation == generation && ready(); }

  private void askPassword(Uri uri, boolean restoring) {
    if (busy || !ready()) return;
    final long operation = generation;
    LinearLayout form = new LinearLayout(fragment.requireContext());
    form.setOrientation(LinearLayout.VERTICAL);
    int padding = (int) (24 * fragment.getResources().getDisplayMetrics().density);
    form.setPadding(padding, 0, padding, 0);
    EditText password = passwordField(fragment.getString(R.string.backup_password));
    EditText confirm = passwordField(fragment.getString(R.string.backup_password_confirm));
    form.addView(password);
    if (!restoring) form.addView(confirm);
    dialog = new MaterialAlertDialogBuilder(fragment.requireContext())
        .setTitle(restoring ? R.string.backup_open : R.string.backup_protect)
        .setMessage(restoring ? R.string.backup_password_inspect : R.string.backup_password_guidance)
        .setView(form).setNegativeButton(R.string.backup_cancel, null).setPositiveButton(R.string.backup_continue, null).create();
    final AlertDialog passwordDialog = dialog;
    passwordDialog.setOnDismissListener(d -> { password.setText(""); confirm.setText(""); });
    dialog.setOnShowListener(d -> {
      passwordDialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
      passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
        if (!current(operation)) return;
        char[] secret = new char[password.length()];
        password.getText().getChars(0, password.length(), secret, 0);
        if (secret.length == 0 || (!restoring && secret.length < 12)) {
          password.setError(fragment.getString(restoring ? R.string.backup_password_required : R.string.backup_password_short));
          Arrays.fill(secret, '\0'); return;
        }
        char[] confirmation = new char[confirm.length()];
        confirm.getText().getChars(0, confirm.length(), confirmation, 0);
        boolean matches = restoring || Arrays.equals(secret, confirmation);
        Arrays.fill(confirmation, '\0');
        if (!matches) {
          confirm.setError(fragment.getString(R.string.backup_password_mismatch)); Arrays.fill(secret, '\0'); return;
        }
        passwordDialog.dismiss();
        runFileOperation(uri, secret, restoring);
      });
    });
    dialog.show();
  }

  private EditText passwordField(String hint) {
    EditText field = new EditText(fragment.requireContext());
    field.setHint(hint);
    field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
    field.setSaveEnabled(false);
    field.setSingleLine(true);
    return field;
  }

  private void progress(String title) {
    busy = true;
    dialog = new MaterialAlertDialogBuilder(fragment.requireContext()).setTitle(title)
        .setMessage(R.string.backup_processing).setCancelable(false).create();
    dialog.show();
  }

  private void runFileOperation(Uri uri, char[] secret, boolean restoring) {
    final long operation = generation;
    AppServices services = ServicesProvider.get(fragment.requireContext());
    android.content.Context context = fragment.requireContext().getApplicationContext();
    android.content.ContentResolver resolver = context.getContentResolver();
    BackupService backup = new BackupService(context, services.databaseHelper.getWritableDatabase());
    progress(fragment.getString(restoring ? R.string.backup_inspecting : R.string.backup_creating));
    services.dbWorker.compute(() -> {
      try {
        if (restoring) {
          try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new java.io.IOException();
            return new FileResult(true, backup.inspect(in, secret), null);
          }
        }
        try (OutputStream out = resolver.openOutputStream(uri, "wt")) {
          if (out == null) throw new java.io.IOException();
          backup.exportTo(out, secret);
        }
        return new FileResult(true, null, null);
      } catch (Exception e) { return new FileResult(false, null, e); }
      finally { Arrays.fill(secret, '\0'); }
    }, result -> {
      if (!current(operation)) {
        if (result.preview != null) result.preview.close();
        return;
      }
      busy = false;
      if (dialog != null) dialog.dismiss();
      if (result.preview != null) {
        pending = result.preview;
        confirmRestore(backup, services);
      } else {
        Toast.makeText(context, result.success ? R.string.backup_export_success
            : errorMessage(result.error, false), Toast.LENGTH_LONG).show();
      }
    });
  }

  private void confirmRestore(BackupService backup, AppServices services) {
    busy = true;
    final long operation = generation;
    BackupService.Preview preview = pending;
    dialog = new MaterialAlertDialogBuilder(fragment.requireContext()).setTitle(R.string.backup_replace_title)
        .setMessage(fragment.getString(R.string.backup_replace_summary, preview.getWalletCount(), preview.getTransactionCount(), preview.getTemplateCount()))
        .setNegativeButton(R.string.backup_cancel, null)
        .setPositiveButton(R.string.backup_replace, (d, w) -> {
          if (!current(operation)) { preview.close(); return; }
          pending = null;
          android.content.Context context = fragment.requireContext().getApplicationContext();
          progress(fragment.getString(R.string.backup_restoring));
          services.dbWorker.compute(() -> {
            try {
              backup.restore(preview);
              refreshWidgets(context);
              return new RestoreResult(true, null);
            }
            catch (Exception e) {
              if (preferencesPending(e)) {
                com.dwlhm.finan.service.backup.BackupRecoveryGate.requireRecovery(context);
                refreshWidgets(context);
              }
              return new RestoreResult(false, e);
            }
            finally { preview.close(); }
          }, result -> {
            if (!current(operation)) return;
            if (dialog != null) dialog.dismiss();
            if (preferencesPending(result.error)) {
              busy = true;
              return;
            }
            busy = false;
            if (result.success) {
              fragment.startActivity(new Intent(context, MainActivity.class)
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            } else Toast.makeText(context, errorMessage(result.error, true), Toast.LENGTH_LONG).show();
          });
        }).create();
    dialog.setOnDismissListener(d -> {
      if (pending == preview) { disposePreview(); if (current(operation)) busy = false; }
    });
    dialog.show();
  }

  private void disposePreview() { if (pending != null) { pending.close(); pending = null; } }
  private static void refreshWidgets(Context context) {
    context.getSharedPreferences("finan_widget_state", 0).edit().clear().apply();
    context.sendBroadcast(new Intent("com.dwlhm.finan.ACTION_DATA_CHANGED").setPackage(context.getPackageName()));
  }
  void close() {
    closed = true;
    generation++;
    disposePreview();
    if (dialog != null) { dialog.dismiss(); dialog = null; }
  }

  private static boolean preferencesPending(Exception error) {
    return error instanceof BackupService.BackupException
        && ((BackupService.BackupException) error).getReason()
            == BackupService.BackupException.Reason.PREFERENCES_PENDING;
  }

  private static int errorMessage(Exception error, boolean restoring) {
    if (preferencesPending(error)) return R.string.backup_preferences_pending;
    if (restoring) return R.string.backup_restore_not_applied;
    if (error instanceof java.security.GeneralSecurityException) return R.string.backup_auth_error;
    if (error instanceof BackupService.BackupException) {
      BackupService.BackupException.Reason reason = ((BackupService.BackupException) error).getReason();
      return reason == BackupService.BackupException.Reason.INVALID_BACKUP
          ? R.string.backup_auth_error : R.string.backup_compatibility_error;
    }
    if (error instanceof java.io.StreamCorruptedException) return R.string.backup_compatibility_error;
    if (error instanceof java.io.IOException || error instanceof SecurityException) return R.string.backup_file_error;
    return R.string.backup_auth_error;
  }

  private static final class FileResult {
    final boolean success;
    final BackupService.Preview preview;
    final Exception error;
    FileResult(boolean success, BackupService.Preview preview, Exception error) {
      this.success = success; this.preview = preview; this.error = error;
    }
  }
  private static final class RestoreResult {
    final boolean success;
    final Exception error;
    RestoreResult(boolean success, Exception error) { this.success = success; this.error = error; }
  }
}
