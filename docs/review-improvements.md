# Review improvements

Implementation evidence below is relative to the repository root. These are localized production graphs of the approved work, not a replacement for its approval record. Final build, lint, unit, and Android verification passed on 2026-09-11; no remaining verified approved-graph drift was identified.

## Encrypted backup and recovery

Production:

```ts
BackupControls.askPassword()
  → BackupControls.runFileOperation()
    → [export] BackupService.exportTo()
    → [restore preview] BackupService.inspect()
      → [explicit replacement confirmation]
        → BackupControls.confirmRestore()
          → BackupService.restore()
            → BackupService.recoverPendingPreferences()
              → [post-commit replay failure]
                → BackupRecoveryGate.requireRecovery()
                  → [blocking retry] retry()
```

Evidence (paths relative to `app/src/main/java/com/dwlhm/finan/`):

- `BackupControls.askPassword(uri, restoring) @ ui/settings/BackupControls.java:79`
- `BackupControls.runFileOperation(uri, secret, restoring) @ ui/settings/BackupControls.java:137`
- `BackupService.exportTo(output, password) @ service/backup/BackupService.java:46`
- `BackupService.inspect(input, password) @ service/backup/BackupService.java:96`
- `BackupControls.confirmRestore(backup, services) @ ui/settings/BackupControls.java:176`
- `BackupService.restore(preview) @ service/backup/BackupService.java:117`
- `BackupService.recoverPendingPreferences() @ service/backup/BackupService.java:170`
- `BackupRecoveryGate.requireRecovery(context) @ service/backup/BackupRecoveryGate.java:41`
- `BackupRecoveryGate.retry() @ service/backup/BackupRecoveryGate.java:67`

Backups accept only the current schema and are limited to 32 MiB. New backup passwords require at least 12 characters. Authentication and validation precede replacement. A committed database with pending preferences remains covered until replay succeeds; reminders are suppressed during this recovery. Preview and password ownership follow the screen operation lifecycle. Device lock and reminder consent are excluded from both manual preference backup and Android cloud/device transfer backup.

## CSV exchange

Production:

```ts
SettingsFragment.writeCsvExportAsync()
  → ExportService.exportTo()
SettingsFragment.readCsvImportAsync()
  → ImportService.importFrom()
    → [validated file and references] atomic append
    → [invalid input] rollback / failure result
```

Evidence (paths relative to `app/src/main/java/com/dwlhm/finan/`):

- `SettingsFragment.writeCsvExportAsync(destination) @ ui/settings/SettingsFragment.java:362`
- `ExportService.exportTo(...) @ service/export/ExportService.java:53`
- `SettingsFragment.readCsvImportAsync(source) @ ui/settings/SettingsFragment.java:400`
- `ImportService.importFrom(input) @ service/export/ImportService.java:126`

CSV remains unencrypted exchange, and importing appends records. It is distinct from replacement backup. Quoted multiline fields and transfer records travel through the structured parser; version and malformed data are rejected at the input boundary.

## Scheduled obligations

Production:

```ts
TransactionTemplateManagerDialog.showEditDialog()
  → TransactionTemplateEditorDialog.saveShortcut()
UpcomingCashFlowService.calculateForwardSummary()
  → UpcomingCashFlowService.findOccurrences()
    → TransactionTemplateDao.isOccurrenceHandled()
      → [unhandled]
        → UpcomingDetailBottomSheet.handleObligation()
          → [explicit Catat/Lewati]
            → TransactionTemplateDao.markOccurrence()
```

Evidence (paths relative to `app/src/main/java/com/dwlhm/finan/`):

- `TransactionTemplateManagerDialog.showEditDialog(template) @ ui/settings/TransactionTemplateManagerDialog.java:118`
- `TransactionTemplateEditorDialog.saveShortcut(...) @ ui/settings/TransactionTemplateEditorDialog.java:280`
- `UpcomingCashFlowService.calculateForwardSummary(from, to, wallet) @ service/summary/UpcomingCashFlowService.java:90`
- `UpcomingCashFlowService.findOccurrences(template, from, to) @ service/summary/UpcomingCashFlowService.java:211`
- `TransactionTemplateDao.isOccurrenceHandled(templateId, date) @ data/dao/TransactionTemplateDao.java:154`
- `UpcomingDetailBottomSheet.handleObligation(obligation, skip) @ ui/dashboard/UpcomingDetailBottomSheet.java:202`
- `TransactionTemplateDao.markOccurrence(templateId, date, status, transactionId) @ data/dao/TransactionTemplateDao.java:170`

An occurrence is identified by template and local due date. Handled occurrences leave the forecast. Annual schedules require a month; legacy annual entries without a month must be edited before projection. Month-end days are clamped to the target month's length. Forecasting itself writes no transactions. The dashboard shows a 30-day forecast (today through today + 29 days), at `MonthlyDashboardFragment @ app/src/main/java/com/dwlhm/finan/ui/dashboard/MonthlyDashboardFragment.java:396`.

## Capture and category history

Production:

```ts
CaptureFragment.onViewReady()
  → [tap / accessibility click] saveTransaction(true)
  → [completed hold, once] saveTransaction(false)
  → [canceled / moved-off hold] hideHoldState(); no save
saveTransaction(clearAfterSave)
  → [current live view owns completion] save result / failure
CaptureFragment.showUndoState()
  → Save remains visible
  → [explicit undo] CaptureFragment.performUndo()
    → [delete succeeds; same draft] restoreDraft()
    → [delete fails] visible failure; preserve input
MainActivity.openHistoryForCategory(categoryId)
  → SearchTransactionActivity.onCreate(savedInstanceState)
    → [category intent] reload(refreshSources)
      → category-filtered history
```

Evidence (paths relative to `app/src/main/java/com/dwlhm/finan/`):

- `CaptureFragment.onViewReady(View, Bundle) @ ui/capture/CaptureFragment.java:118`
- `onViewReady: save click listener @ ui/capture/CaptureFragment.java:273`
- `onAnimationEnd(Animator) @ ui/capture/CaptureFragment.java:302`
- `onTouch(View, MotionEvent) @ ui/capture/CaptureFragment.java:315`
- `CaptureFragment.hideHoldState() @ ui/capture/CaptureFragment.java:1364`
- `CaptureFragment.saveTransaction(clearAfterSave) @ ui/capture/CaptureFragment.java:1209`

A completed hold saves once and retains the draft. Cancellation
or movement outside the button cancels an unfinished hold without
saving. Accessibility click invokes the save action. Save remains
visible while undo is offered.
Undo restores a saved draft only when no newer edit or save owns
the form. Category history opens search with the category ID.

Evidence (paths relative to `app/src/main/java/com/dwlhm/finan/`):

- `showUndoState() @ ui/capture/CaptureFragment.java:1337`
- `performUndo() @ ui/capture/CaptureFragment.java:1390`
- `restoreDraft(PendingSaveUndo) @ ui/capture/CaptureFragment.java:1474`
- `openHistoryForCategory(long) @ ui/MainActivity.java:254`
- `onCreate(Bundle) @ ui/search/SearchTransactionActivity.java:99`
- `reload(boolean) @ ui/search/SearchTransactionActivity.java:233`

## Financial advice

Production:

```ts
FinancialAdvisor.calculateAdviceDetails()
  → [no income or expense] NO_DATA
  → [expense > income] OVERSPENDING variants
  → [actual expense / actual income] spending percentage
    → [high spending] HIGH_SPENDING using actual percentage
    → [other ratios] period-stage or savings advice
```

Evidence (paths relative to `app/src/main/java/com/dwlhm/finan/`):

- `FinancialAdvisor.calculateAdviceDetails(summary, previous, previousPrevious, dayProgress) @ ui/summary/FinancialAdvisor.java:61`

The high-spending percentage is not multiplied by elapsed-period progress. Regression tests cover the same actual ratio early and halfway through the period, zero income, and end-of-period savings.

## Privacy

Production:

```ts
AppLock.setEnabled()
  → AppLock.authenticate()
    → [authenticated] device-local opt-in
AppLock.afterUnlock()
  → [locked] lifecycle-owned cover / authentication
  → [unlocked, resumed] deferred action
AppLock.privateWidgetViews()
  → generic private widget surface
```

Evidence (paths relative to `app/src/main/java/com/dwlhm/finan/`):

- `AppLock.setEnabled(activity, value, done, failure) @ service/privacy/AppLock.java:80`
- `AppLock.authenticate(activity, success, failure) @ service/privacy/AppLock.java:103`
- `AppLock.afterUnlock(activity, action) @ service/privacy/AppLock.java:70`
- `AppLock.privateWidgetViews(context, widgetId) @ service/privacy/AppLock.java:90`

App lock has a 60-second background grace period (`AppLock.LOCK_TIMEOUT_MS @ app/src/main/java/com/dwlhm/finan/service/privacy/AppLock.java:29`). It protects UI access with device authentication; it does not encrypt the local SQLite database. Widget privacy is device-local and enforced while app lock is enabled. Deferred navigation must remain tied to the resumed activity and current request.

## Due-date reminders

Production:

```ts
ReminderControls.onToggle()
  → [explicit opt-in, API33+] notification permission
  → ReminderScheduler.reconcile()
    → [off / denied / schedule failure] no job or notification
    → [allowed] one persisted hourly Android job
      → DueReminderJobService.onStartJob()
        → [recovery gate open] current date/zone forecast
          → ReminderPolicy.shouldNotify()
            → [current consent/date/zone; unhandled due]
              → [forward day] generic notification
              → MainActivity.openPendingReminder()
                → AppLock.afterUnlock(activity, action)
                  → [current resumed request]
                  → masked UpcomingDetailBottomSheet
                    → [Catat/Lewati] handleObligation()
```

Evidence (paths relative to `app/src/main/java/com/dwlhm/finan/`):

- `ReminderControls.onToggle(enabled) @ ui/settings/ReminderControls.java:72`
- `ReminderScheduler.reconcile(context) @ service/reminder/ReminderScheduler.java:47`
- `DueReminderJobService.onStartJob(params) @ service/reminder/DueReminderJobService.java:15`
- `ReminderPolicy.shouldNotify(...) @ service/reminder/ReminderPolicy.java:11`
- `MainActivity.openPendingReminder() @ ui/MainActivity.java:161`

Reminders default off, are inexact, and can be delayed by Android. Notifications contain no financial names, counts, or amounts. A successful delivery advances a forward-only local-day marker. Empty queries cancel stale notifications; errors retry on a future periodic run. Stopped, stale, or recovery-blocked results cannot publish. Turning consent off and back on invalidates an older pending query. Notification taps never record a transaction automatically.

## Verification gate

The parent verified the final source on 2026-09-11 with:

```sh
JAVA_HOME='/Users/dwlhm/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --offline
```

Reported terminal output and process result:

```text
Finished 67 tests
BUILD SUCCESSFUL in 1m 12s
Exit code: 0
```

XML reports contain 150 unit tests and 67 Android tests, each with
zero failures, errors, or skipped tests. Lint reports zero errors
and 303 warnings. The recovery test's earlier retry-button timing
failure and cleanup leak were fixed with bounded accessibility
waiting and real-retry cleanup before this successful run.

No remaining verified approved-graph drift was identified.
Real-device biometric or credential authentication has not been
exercised; session logic and widget runtime checks do not establish
hardware authentication.
