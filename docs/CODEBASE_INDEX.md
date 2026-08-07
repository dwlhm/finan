# Project Finan - Codebase Architecture & Index Map

Document persistent index map of the `finan` codebase. This document serves as a offline-accessible structural blueprint for developers, AI assistants, and CLI tools.

---

## 1. System Overview & Tech Stack

* **Platform:** Native Android (minSdk: 26, targetSdk: 34)
* **Language:** Java (Baseline: Java 8+)
* **UI Framework:** Native Android Views (XML layouts, ViewBinding/findViewById, Material Design Components)
* **Database:** Native SQLite via `SQLiteOpenHelper` (`FinanDatabaseHelper`) with explicit manual migration runner (`MigrationRunner`, v1–v11).
* **Architecture Pattern:** Clean 4-Tier Manual Architecture (`ui` -> `service` -> `data` -> `domain`), Manual DI via `AppServices` / `ServicesProvider`.
* **Core Philosophy:** Minimal runtime, zero unnecessary external dependencies, local-first instant save, high input responsiveness (critical path < 50ms).

---

## 2. Directory & Package Structure Map

```txt
com.dwlhm.finan/
├── FinanApplication.java          # App Entry point & Service Initialization
├── domain/                        # Pure Data Models & Validation Rules
│   ├── model/                     # Transaction, Wallet, Category, MonthlySummary, etc.
│   └── rule/                      # BalanceRules, ValidationRules
├── data/                          # SQLite DAOs, Migration Runners, Preferences
│   ├── dao/                       # TransactionDao, WalletDao, CategoryDao, SummaryDao, TransferDao
│   ├── db/                        # FinanDatabaseHelper (SQLiteOpenHelper)
│   ├── migration/                 # Migration001 to Migration011
│   ├── entity/                    # Transaction, Wallet, Category database entities
│   └── prefs/                     # DefaultsStore, TransactionFormDraft
├── service/                       # Business Logic & Transaction Services
│   ├── transaction/               # TransactionService, TransactionSearchResolver, TransactionTemplateService
│   ├── wallet/                    # WalletService
│   ├── summary/                   # SummaryService, CashFlowReportService
│   ├── balance/                   # BalanceService, AdjustmentService
│   ├── category/                  # CategoryUsageService, CategoryClassificationService
│   ├── transfer/                  # TransferService
│   └── export/                    # ExportService, ImportService
├── ui/                            # View Layer (Activities, Fragments, BottomSheets, Dialogs)
│   ├── MainActivity.java          # Root Container Activity (Single Activity + Bottom Nav)
│   ├── capture/                   # Quick Amount Input & Transaction Creation
│   ├── dashboard/                 # Overview Dashboard & Monthly Summaries
│   ├── history/                   # Transaction History List & Date Filtering
│   ├── summary/                   # Cashflow Reports & Category Breakdown
│   ├── wallet/                    # Wallet List, Cards & Balance Adjustments
│   ├── category/                  # Category List & Management
│   ├── settings/                  # App Settings & Template Management
│   ├── search/                    # Search Transactions Activity
│   ├── components/                # Custom Views (FinancialKeypadView, DonutChartView, FloatingBottomNavView)
│   └── common/                    # Shared Dialogs, BottomSheets & Helpers
└── util/                          # Pure Utilities (Date, Money, Math, Search)
    ├── date/                      # DateRange, MonthRange, PayrollCycleResolver
    ├── money/                     # MoneyFormatter, MoneyParser
    └── search/                    # FuzzySearch
```

---

## 3. Database & Migration Map (SQLite)

Database File: `finan.db` | Current Version: `11`

* **`Migration001Initial`**: Initial schema (transactions, wallets, categories).
* **`Migration002TransactionIndexes`**: Speed up queries via date/category/wallet indexes.
* **`Migration003TagMerchantEntities`**: Initial merchant/tag fields.
* **`Migration004WalletOperations`**: Initial balance adjustments.
* **`Migration005CashFlowClassification`**: Income/Expense/Transfer activity tracking.
* **`Migration006CategoryIcon`**: Custom category icon avatars & emojis.
* **`Migration007WalletIcon`**: Custom wallet icon styling.
* **`Migration008RemoveTagsMerchants`**: Clean up unused tag/merchant tables for performance.
* **`Migration009CategoryDefault`**: Default category seeding.
* **`Migration010NoOp`**: Schema alignment check.
* **`Migration011TransactionTemplate`**: Fast transaction template support.

---

## 4. UI Components Inventory

### Bottom Sheets (`BottomSheetDialog` / `DraggableBottomSheetLayout`)
* **`WalletOverviewBottomSheet`**: Displays wallet details & quick actions.
* **`WalletEditBottomSheet`**: Edit/Create wallet attributes.
* **`CategoryOverviewBottomSheet`**: Category details & expense breakdown.
* **`SummaryDateRangeBottomSheet`**: Custom date range selector for summary.
* **`SummaryFilterBottomSheet`**: Multi-criteria summary filter.
* **`HistoryDateRangeBottomSheet`**: Date filtering for transaction history.
* **`HistoryFilterBottomSheet`**: Category/wallet filter for transaction history.
* **`WeeklyDetailBottomSheetDialog`**: Weekly cashflow breakdown.
* **`MonthlyDetailBottomSheetDialog`**: Monthly cashflow breakdown.
* **`EmojiPickerBottomSheet`**: Custom emoji selector for categories/wallets.
* **`DateTimeBottomSheet`**: Custom date-time picker sheet.
* **`EntitySearchBottomSheet`**: Search bottom sheet for categories/entities.
* **`ExportDateRangeBottomSheet`**: Date range selector for data export.

### Dialogs & Custom Views
* **`TransactionDetailDialog`**: View/Edit specific transaction.
* **`CategoryEditorDialog`**: Create or edit categories.
* **`WalletInputDialog`**: Quick wallet selection/input.
* **`AmountShortcutDialog`**: Quick amount shortcuts.
* **`FinancialAdviceDialog`**: Automated financial advice insights.
* **`FinancialKeypadView`**: Custom lightweight numeric keypad with inline calculator.
* **`FloatingBottomNavView`**: Custom elevated floating navigation bar.

---

## 5. Architectural Contracts & Performance Principles

1. **Critical Path Optimization:** Quick capture screen (`CaptureFragment` + `FinancialKeypadView`) must open in < 50ms with zero network or blocking DB queries.
2. **Local-First Writes:** All transaction inserts commit to SQLite first (`TransactionDao`), updating UI immediately via callbacks/listeners.
3. **No Heavy Framework Overhead:** Manual dependency wiring (`AppServices.getInstance()`) is used instead of Dagger/Hilt to minimize startup latency and APK size.
4. **View Recycling:** Custom `InfiniteScrollListView` / `RecyclerView` adapters with `StickyHeaderItemDecoration` are used for smooth 60fps scrolling.
