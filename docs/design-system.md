# Finan Design System Specification

## 1. System Foundations & Design Philosophy

Finan is built around a single unifying paradigm: **Capture-First Financial Tracking**. Financial friction is the primary reason personal finance apps are abandoned. The Finan Design System prioritizes speed of entry, emotional calm, and absolute clarity over visual clutter or premature complexity.

```
       +-------------------------------------------------------------+
       |                  Capture-First Philosophy                   |
       +-------------------------------------------------------------+
       | Fast-Path: <= 3 Taps  |  Local-First Instant Commit (< 5ms) |
       | Calm Tone (Non-Judge) |  48dp Minimum Touch Target Grid     |
       +-------------------------------------------------------------+
```

### Core Principles

1. **Input Is the Main Screen**
   - Opening the application immediately presents the transaction recording surface.
   - Dashboards, charts, and reports are secondary feedback layers and never block the user from recording expenses or income.

2. **The Fast-Path Target (<= 3 Taps / < 5 Seconds)**
   - The default workflow from launching the application to a completed transaction must satisfy:
     - **Taps:** 2 to 3 taps maximum (Amount $\rightarrow$ Category $\rightarrow$ Save).
     - **Time to Save:** $< 5$ seconds total elapsed time.
     - **Cognitive Load:** Zero mandatory secondary fields (note, tag, merchant, location are strictly optional).

3. **Record First, Complete Later**
   - System defaults (`Today`, `Current Time`, `Default Wallet`, `Expense Type`) eliminate redundant decision-making.
   - Users can save an imperfect record instantly and refine details later via inline editing or quick correction.

4. **Local-First Zero-Latency Architecture**
   - Saving a transaction executes synchronously against the local SQLite database.
   - **No blocking loaders in the critical path.** Network sync, aggregation recalculation, or cloud backup must occur asynchronously off the main user flow.

5. **Calm, Non-Judgmental Tone**
   - Microcopy provides factual, actionable context without emotional reprimand or guilt.
   - Avoid: *"You overspent this month!"* / *"Budget failed."*
   - Prefer: *"Food & Dining increased by 12% this week"* / *"5 transactions recorded today."*

6. **Justify Every UI Element**
   - Every layout item, badge, border, and control must answer: *Does this accelerate entry, prevent error, or provide critical financial context?* If not, it is relegated to a secondary disclosure sheet.

---

## 2. Color System & Semantic Tokens

The Finan palette is engineered around an organic, muted teal core paired with high-contrast neutral surfaces and unambiguous financial status triplets. All colors are defined in `res/values/colors.xml` and mapped across themes.

### 2.1 Brand & Container Tokens

| Token Name | Hex Value | Role & Usage Description |
|---|---|---|
| `finan_primary` | `#2D6A6A` | Deep Teal — Primary brand color, primary button background, active tab indicators, key actions |
| `finan_primary_dark` | `#1F4E4E` | Dark Spruce — Primary pressed state, high-contrast dark accents |
| `finan_accent` | `#5B8A8A` | Medium Sage Teal — Secondary branding, icons, active highlights |
| `colorPrimaryContainer` | `#D0E8E7` | Soft Sage Tint — Material container background for highlighted elements |
| `colorOnPrimaryContainer` | `#052020` | Deep Pine — Foreground text/icon on primary containers |

### 2.2 Surfaces, Backgrounds & Neutrals

| Token Name | Hex Value | Role & Usage Description |
|---|---|---|
| `finan_background` | `#F4F6F8` | Cool Light Gray — Base window background behind cards and sheets |
| `finan_surface` | `#FFFFFF` | Pure White — Card surfaces, bottom sheets, dialog cards, keypad keys |
| `finan_control_bg` | `#F9FBFC` | Off-white Cool Tint — Input field backgrounds, selectable control items |
| `finan_chip_bg` | `#E8ECEF` | Neutral Gray — Inactive filter chips, unselected segment pills |
| `finan_chip_selected` | `#2D6A6A` | Active chip fill color |
| `finan_divider` | `#DDE3E8` | Light Border — Card outlines, control borders, subtle dividers |
| `colorOutline` | `#8A9AA3` | Secondary outline / border accent |

### 2.3 Typography & Contrast Tokens

| Token Name | Hex Value | Contrast Ratio | Usage Description |
|---|---|---|---|
| `finan_text_primary` | `#1A2830` | 13.8:1 on `#FFFFFF` | Primary headers, transaction amounts, active labels, body text |
| `finan_text_secondary` | `#5C6B73` | 5.2:1 on `#FFFFFF` | Section titles, secondary metadata, inactive nav icons, timestamps |
| `finan_text_hint` | `#8A9AA3` | 3.1:1 on `#FFFFFF` | Placeholder text, search hints, disabled helper labels |
| `finan_btn_pill_text` | `#FFFFFF` | 7.8:1 on `#2D6A6A` | Primary button text |
| `finan_btn_disabled_bg` | `#DDE3E8` | — | Disabled button container fill |
| `finan_btn_disabled_text` | `#70808A` | 4.6:1 on `#DDE3E8` | Disabled button label |

### 2.4 Financial Semantic Triplets

Financial state representations follow a strict 3-tier token structure (Solid Accent, 10% Tinted Container Background, and Soft Stroke Outline):

```
+-------------------------------------------------------------------------------+
|                             Semantic Triplets                                 |
+-------------------------------------------------------------------------------+
| Expense / Negative : [ #C45C4A (Solid) ] [ #FFF1EE (Bg) ] [ #EACCC5 (Stroke) ]|
| Income  / Positive : [ #3D8B6E (Solid) ] [ #EEF8F3 (Bg) ] [ #CAE7DA (Stroke) ]|
| Warm    / Warning  : [ #D28B45 (Solid) ] [ #FFF8ED (Bg) ] [ #E9D7BF (Stroke) ]|
+-------------------------------------------------------------------------------+
```

| Semantic Role | Solid Color Token | Container Bg Token | Border Stroke Token |
|---|---|---|---|
| **Expense / Debit / Error** | `finan_expense` / `finan_error`<br>`#C45C4A` (Terracotta Red) | `finan_expense_bg` / `finan_error_bg`<br>`#FFF1EE` (Pale Coral) | `finan_expense_stroke`<br>`#EACCC5` (Muted Rose) |
| **Income / Credit / Success** | `finan_income`<br>`#3D8B6E` (Forest Jade) | `finan_income_bg`<br>`#EEF8F3` (Pale Mint) | `finan_income_stroke`<br>`#CAE7DA` (Muted Mint) |
| **Warm / Transfer / Warning** | `finan_warm_accent`<br>`#D28B45` (Amber Ochre) | `finan_emphasis_bg`<br>`#FFF8ED` (Pale Amber) | `finan_emphasis_stroke`<br>`#E9D7BF` (Muted Amber) |

### 2.5 Hero Gradient & Data Visualization Tokens

| Token Name | Hex Value | Purpose & Visual Target |
|---|---|---|
| `finan_summary_hero_bg_start` | `#2F7575` | Hero gradient start color ($135^\circ$ angle) |
| `finan_summary_hero_bg_end` | `#173F3F` | Hero gradient end color ($135^\circ$ angle) |
| `finan_summary_on_hero` | `#FFFFFF` | High-contrast white text over dark hero gradient |
| `finan_summary_on_hero_muted` | `#D7E8E6` | Muted mint-white secondary text on hero cards |
| `finan_summary_hero_divider` | `#5E8C8A` | Translucent teal divider line within hero cards |
| `finan_summary_net_income` | `#A3F5CF` | Mint Green highlight for positive net balance |
| `finan_summary_net_expense` | `#FFC2C2` | Soft Coral highlight for negative net balance |
| `finan_summary_progress_bg` | `#E9EEF2` | Background track for category donut/progress bars |

---

## 3. Typography Scale & Hierarchy

Finan uses a clean 5-tier typographic scale mapped directly to Material 3 type tokens in `res/values/styles.xml`.

```
Scale Overview:
Tier 1: Display   [ 32sp Bold / 28sp Medium ] -> Transaction Amounts & Hero Totals
Tier 2: Titles    [ 22sp Bold / 20sp Medium / 16sp Bold ] -> Screen Titles & Wallet Names
Tier 3: Body      [ 16sp Regular / 15sp Regular ] -> Form Values & List Descriptions
Tier 4: Labels    [ 14sp Bold / 13sp Medium ] -> Section Labels & Action Buttons
Tier 5: Captions  [ 12sp Bold / 11sp Bold ] -> Badges, Timestamps & Error Messages
```

### 3.1 Typographic Scale Specification

| Tier | Token / Style Name | Size | Weight / Font | Color Token | XML Style Reference |
|---|---|---|---|---|---|
| **Display** | `Finan.Text.Amount` | `32sp` | Bold | `finan_text_primary` | `styles.xml#L14-18` |
| **Display** | `Finan.Text.WalletHeroAmount` | `28sp` | Bold (`sans-serif-medium`) | `finan_summary_on_hero` | `styles.xml#L181-186` |
| **Title L** | `Finan.Text.Title` | `22sp` | Bold | `finan_text_primary` | `styles.xml#L3-7` |
| **Title M** | `Finan.Text.WalletBalance` | `20sp` | Bold (`sans-serif-medium`) | `finan_text_primary` | `styles.xml#L206-211` |
| **Title S** | `Finan.Text.WalletName` | `16sp` | Bold | `finan_text_primary` | `styles.xml#L200-204` |
| **Body L** | `Finan.Text.Body` | `16sp` | Normal | `finan_text_primary` | `styles.xml#L9-12` |
| **Body M** | `Finan.Control.OccurredAtValue` | `15sp` | Normal | `finan_text_primary` | `styles.xml#L74-89` |
| **Label L** | `Finan.Text.SectionLabel` | `14sp` | Bold | `finan_text_secondary` | `styles.xml#L20-24` |
| **Label L** | `Finan.Text.CollapsibleLabel` | `14sp` | Bold | `finan_text_primary` | `styles.xml#L40-44` |
| **Label L** | `Finan.Text.RequiredMarker` | `14sp` | Bold | `finan_error` | `styles.xml#L26-32` |
| **Label M** | `Finan.TabTextAppearance` | `13sp` | Bold (`sans-serif-medium`) | `finan_primary` / `finan_text_secondary` | `styles.xml#L168-173` |
| **Label M** | `Finan.Text.WalletHeroLabel` | `13sp` | Bold | `finan_summary_on_hero_muted` | `styles.xml#L175-179` |
| **Label M** | `Finan.Text.WalletHeroButton` | `13sp` | Bold | `#1F4E4E` | `styles.xml#L193-198` |
| **Caption** | `Finan.Text.FieldError` | `12sp` | Bold | `finan_error` | `styles.xml#L34-38` |
| **Caption** | `Finan.BottomNav.Text` | `12sp` | Bold | `finan_primary` | `styles.xml#L164-167` |
| **Caption** | `Finan.Text.WalletHeroSecondary`| `12sp` | Normal | `finan_summary_on_hero_muted` | `styles.xml#L188-191` |
| **Badge** | `Finan.Text.WalletCurrency` | `11sp` | Bold | `finan_text_secondary` | `styles.xml#L213-217` |

---

## 4. Geometry, Shapes & Spacing

Finan enforces a clean 4-tier corner radius scale and a strict 4dp base spacing grid.

```
       +-------------------------------------------------------------+
       |                    Corner Radius Hierarchy                  |
       +-------------------------------------------------------------+
       | [ 24dp ] Top Sheet Header & Active Nav Pill                 |
       | [ 20dp ] Standard Action Pill Buttons                       |
       | [ 16dp ] Hero & List Cards (bg_wallet_card, bg_summary_hero)|
       | [ 12dp ] Form Controls & Panels (bg_control_surface)        |
       | [ 8-10dp ] Chips, Badges & Keypad Keys                      |
       +-------------------------------------------------------------+
```

### 4.1 Corner Radius Scale

| Radius | Tier Name | Associated Drawables & Components |
|---|---|---|
| **24dp** | Floating Pill & Top Sheet | `Finan.ShapeAppearance.BottomSheet` (top-left & top-right), `FloatingBottomNavView` active pill container |
| **20dp** | Button Pill | `bg_button_pill.xml`, `bg_button_primary.xml`, `bg_wallet_hero_btn.xml` |
| **16dp** | Large Surface / Hero Card | `bg_wallet_hero.xml`, `bg_wallet_card.xml`, `bg_summary_hero.xml` |
| **12dp** | Control & Form Surface | `bg_card.xml`, `bg_control_surface.xml`, `bg_panel_emphasis.xml`, `Finan.Panel.CollapsibleContent` |
| **8–10dp**| Micro Control / Badge / Key | `bg_chip.xml` (8dp), `bg_wallet_currency_badge.xml` (8dp), `bg_calculator_operator.xml` (8dp), `FinancialKeypadView` keys (8dp) |

### 4.2 Spacing Grid (4dp Base)

All margins, paddings, and structural gaps use integer multiples of 4dp:

- **4dp (`grid_1`):** Micro-spacing between inline icons and text, key margins.
- **8dp (`grid_2`):** Tight padding inside chips, operator buttons, and grouped toggle controls.
- **12dp (`grid_3`):** Standard internal padding for edit texts, control rows, and collapsible panels.
- **14–16dp (`grid_4`):** Standard card padding, screen horizontal gutters, list item separation.
- **20–24dp (`grid_5`–`grid_6`):** Section separation, modal header margins, hero card vertical padding.
- **32dp (`grid_8`):** Major content block separation, empty state vertical margins.
- **96dp (`grid_24`):** **Bottom Nav Clearance Padding** (ensures scrolling content is never obstructed by the floating navigation bar).

---

## 5. Component Library & Architectural Patterns

### 5.1 `FinancialKeypadView`
Custom 4x4 arithmetic keypad optimized for single-handed entry.

```
+-------+-------+-------+-------+
|   1   |   2   |   3   |   +   |
+-------+-------+-------+-------+
|   4   |   5   |   6   |   -   |
+-------+-------+-------+-------+
|   7   |   8   |   9   |   *   |
+-------+-------+-------+-------+
|  000  |   0   |  DEL  |   /   |
+-------+-------+-------+-------+
```

- **Layout Grid:** 4 rows $\times$ 4 columns with 4dp internal key gaps.
- **Key Ergonomics:** Standard numeric keys (`1`–`9`, `0`, `000`) on white surface with subtle `#DDE3E8` outline.
- **Operator Column:** Distinct arithmetic operators (`+`, `-`, `×`, `÷`) and backspace (`⌫`) with primary teal accents.
- **Feedback:** Integrates `HapticFeedbackConstants.KEYBOARD_TAP` on down-touch.
- **Controller Binding:** Direct two-way binding with `KeypadAmountManager`.

### 5.2 `CalculatorStripView`
Inline mathematical calculation strip placed immediately above the keypad.
- Shows live intermediate expression (e.g. `15,000 + 4,500 = 19,500`).
- Provides quick operator toggles without shifting focus away from amount entry.
- Auto-evaluates on save or category selection.

### 5.3 `FloatingBottomNavView`
Modern floating translucent navigation bar with auto-hide and breathing space.

```
       +--------------------------------------------------------+
       |   ( [Icon] Catat )        [Icon]        [Icon]         |
       +--------------------------------------------------------+
```

- **Dual-Layer Glass:**
  - *Layer 1 (Backdrop):* Translucent background (`#CCFFFFFF` light mode, `#B31E1E1E` dark mode) with `RenderEffect.createBlurEffect(20f, 20f)` on Android 12+ (API 31+).
  - *Layer 2 (Rim Stroke):* High-precision 1.5dp glossy stroke (`#40000000` light / `#40FFFFFF` dark).
- **Expanding Side-by-Side Active Pill:**
  - Inactive items show icon only (`24dp`, tinted `finan_text_secondary`).
  - Active item expands smoothly via `AutoTransition(180ms)` to show icon + bold label inside a 24dp rounded teal tinted pill (`#E6E0F2F1`).
- **Auto-Hide on Scroll:** Automatically slides down off-screen on downward scroll and reappears on upward scroll.

### 5.4 `DraggableBottomSheetLayout`
Single-layer gesture-driven modal sheet container.
- **Touch Handling:** Uses `ViewConfiguration.getScaledTouchSlop()` and checks `canChildScrollUp()` to smoothly switch between internal list scrolling and sheet dragging.
- **Dismiss Threshold:** Dismisses downwards if dragged past 35% of total sheet height; snaps back with `DecelerateInterpolator` otherwise.
- **Top Corners:** 24dp rounded top radius (`Finan.ShapeAppearance.BottomSheet`).

### 5.5 `LabeledEditTextView`
Standardized compound form control combining label, input field, and visual states.
- Displays `Finan.Text.SectionLabel` above a 48dp minimum-height input box (`bg_control_surface.xml`).
- Supports dynamic typeface transitions (medium weight on non-empty content).
- Built-in `DebouncedTextWatcher` support to eliminate excessive redraws.

### 5.6 `InfiniteScrollListView`
Optimized cursor-driven paginated list wrapper.
- Built on `RecyclerView` with `LinearLayoutManager`.
- Configurable `pageSize` (default: 20 items) and `loadMoreThreshold` (default: 5 items remaining).
- Automated empty state and footer loading progress coordination without UI stutter.

### 5.7 `DonutChartView`
High-performance Canvas-based two-tier financial breakdown visualization.
- Multi-segment rendering for Inflows (Greens) vs Outflows (Terracottas).
- Smooth entry animation using `ValueAnimator` with `DecelerateInterpolator`.
- Dual-line centered net summary text (`Finan.Text.Amount` style).

### 5.8 Custom Pickers
1. **`CustomDatePickerView`:** Custom calendar matrix with quick selection (`Today`, `Yesterday`, custom date cell selection) avoiding standard OS dialog overhead.
2. **`CustomTimePickerView`:** Compact time selection with quick increments (+15m, -15m, Now).
3. **`CustomEmojiKeyboardView`:** Categorized financial emoji matrix for category and wallet iconography.
4. **`DateTimeBottomSheet`:** Unified composite date-time selection sheet keeping users in context.

---

## 6. UI/UX Interaction Rules & Excluded Anti-Patterns

### 6.1 Strict Interaction Standards

1. **48dp Minimum Touch Target**
   - Every clickable button, icon container, keypad cell, and selection chip must provide at least $48\text{dp} \times 48\text{dp}$ of interactive touch area (WCAG 2.1 AA / Material 3 accessibility baseline).
   
2. **Immediate Feedback & Micro-Interactions**
   - Touch interactions on buttons and cards trigger visual scale depression (`ViewPressAnimator` to 97% scale) or ripple states.
   - Haptic ticks accompany keypad actions and category selections.

3. **96dp Bottom Nav Clearance Rule**
   - Any scrolling list or root container residing behind the `FloatingBottomNavView` must include `android:paddingBottom="96dp"` with `android:clipToPadding="false"`.

### 6.2 Excluded Anti-Patterns (Design Prohibitions)

```
       +-------------------------------------------------------------+
       |                  Excluded Anti-Patterns                     |
       +-------------------------------------------------------------+
       | [X] Heavy Black Drop Shadows      [X] Multi-Level Modals    |
       | [X] Blocking Critical-Path Loaders [X] Pre-Recording Wizard  |
       | [X] Shaming / Guilt Microcopy     [X] Touch Targets < 48dp  |
       +-------------------------------------------------------------+
```

1. **No Heavy Drop Shadows**
   - Prohibited: High-opacity black shadows (`elevation > 16dp` with dark ambients).
   - Enforced: Subtle elevation (2dp–8dp) paired with delicate outline strokes (`#DDE3E8` or translucent glass rim).

2. **No Multi-Level Modals (Modal Stacking Trap)**
   - Prohibited: Opening a dialog on top of an existing modal bottom sheet.
   - Enforced: Inline progressive disclosure, inline expandable sections (`CollapsibleController`), or smooth single-level sheet transitions.

3. **No Blocking Loaders in the Fast-Path**
   - Prohibited: Full-screen loading spinners or spinner-blocked save buttons during transaction commits.
   - Enforced: Optimistic immediate local database write with background sync/update.

4. **No Mandatory Wizardry for First Use**
   - Prohibited: Multi-step introductory setup wizards requiring bank account linkage or detailed budget allocations before first recording.
   - Enforced: Instant access with pre-configured default wallet and standard categories.
