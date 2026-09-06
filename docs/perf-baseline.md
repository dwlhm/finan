# TG4 Performance Baseline — build hygiene (DDL out of `onConfigure`)

Base: `ab93650`. Change: `CREATE INDEX` DDL moved from
`FinanDatabaseHelper.onConfigure()` to `Migration013Indexes` (v13, idempotent);
`onConfigure` keeps `PRAGMA journal_mode=WAL` + `synchronous=NORMAL` + FK ON.

## 1. Cold-start init path (before/after: unchanged by design)

`AppServices.create()` @ `app/src/main/java/com/dwlhm/finan/ui/common/AppServices.java:99`
calls `databaseHelper.getWritableDatabase()` @ `AppServices.java:101`
**synchronously on the caller thread**. This triggers `onCreate`/`onUpgrade`
(full `MigrationRunner.migrate()` chain, now 1→13) plus `onConfigure`
(PRAGMAs + FK) before any DAO/service is constructed.

- Before: `onConfigure` also executed 2× `CREATE INDEX IF NOT EXISTS` on every
  open (cheap when indexes exist, but DDL on the open path).
- After: `onConfigure` executes zero DDL; index creation happens once inside
  the v12→v13 migration transaction. Steady-state open cost is strictly lower
  (2 fewer `execSQL` round-trips per open).
- No wall-clock device measurement was taken (no benchmark harness in repo);
  no threading change was made (out of TG4 scope: no new threading frameworks,
  no UI flow changes).

## 2. Save latency path — local-first instant save (before/after: unchanged)

`TransactionService.save()` @
`app/src/main/java/com/dwlhm/finan/service/transaction/TransactionService.java:43`
runs one SQLite transaction (`beginTransaction` → insert + `BalanceService`
apply + usage bump → `setTransactionSuccessful` → `endTransaction`)
@ `TransactionService.java:50-63`. Capture UI invokes it off the main thread
via `DbWorker.compute()` (`CaptureFragment.saveTransaction`, `executeShortcut`,
`saveTransfer`); no summary/report query sits on the save path.
`CaptureFragment.onViewReady()` @ `CaptureFragment.java:113` issues no summary
query — `refreshCaptureData()` loads wallets/categories/templates only, via
`dbWorker.compute()` @ `CaptureFragment.java:438`. Dashboard/summary
aggregation stays lazy: `MonthlyDashboardFragment` defers
`cashFlowReportService`/`summaryService` queries unless the summary tab is
active @ `MonthlyDashboardFragment.java:382-397`, and `SummaryFragment`
loads via `dbWorker.compute()` @ `SummaryFragment.java:242`. No code change
was required for this item — verified, not modified.

## 3. APK size (`./gradlew :app:assembleDebug`, `--offline`)

| Build | APK bytes | SHA source |
|---|---|---|
| Before (base `ab93650`) | 15,310,074 | `app-debug.apk` built from clean stash |
| After (TG4: v13 migration) | 15,272,514 | `app-debug.apk` built with TG4 changes |
| Delta | −37,560 (~−0.25%) | DDL-only move; residual delta is zip non-determinism, not a real shrink |

No dependencies added or removed. Local-first instant save preserved.
