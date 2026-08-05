# Dompet (Wallet Management) — UI/UX Redesign Specification

## 1. Overview

Redesign the Dompet management screen from a flat `ListView` + minimal summary into a structured, modern layout with a hero balance card, `RecyclerView`-backed wallet list with item animations, a designed empty state, and consistent micro-interactions. The redesign inherits Finan's existing design system tokens (colors, typography, drawables, spacing grid) and extends them only where required — every new token is defined explicitly below.

### Current state

| Aspect | Before |
|---|---|
| List widget | `ListView` with `BaseAdapter` |
| Summary | Plain `LinearLayout` card with section label, count, and default wallet text |
| Item | Name + action buttons, no balance display, no icon container |
| Empty state | Centered hint `TextView` only |
| Animations | None beyond system default |

### Target state

| Aspect | After |
|---|---|
| List widget | `RecyclerView` with `ListAdapter` + `DiffUtil` |
| Summary | Deep-teal gradient hero card with aggregated balance, wallet count pill, default wallet indicator, and two quick-action buttons |
| Item | Icon/emoji avatar, wallet name, currency badge, large formatted balance, default badge, compact action bar |
| Empty state | Illustration area + title + body + primary CTA |
| Animations | ViewPressAnimator on cards & buttons, `DefaultItemAnimator`, staggered load fade |

---

## 2. Design Tokens (Additions to `colors.xml`)

These extend the existing palette. No existing tokens are modified.

```xml
<!-- Dompet hero card -->
<color name="finan_wallet_hero_bg_start">#2F7575</color>   <!-- reuse finan_summary_hero_bg_start -->
<color name="finan_wallet_hero_bg_end">#173F3F</color>     <!-- reuse finan_summary_hero_bg_end -->
<color name="finan_wallet_on_hero">#FFFFFF</color>          <!-- reuse finan_summary_on_hero -->
<color name="finan_wallet_on_hero_muted">#D7E8E6</color>   <!-- reuse finan_summary_on_hero_muted -->
<color name="finan_wallet_hero_divider">#5E8C8A</color>    <!-- reuse finan_summary_hero_divider -->
<color name="finan_wallet_hero_btn_bg">#FFFFFF</color>     <!-- quick-action button bg on hero -->
<color name="finan_wallet_hero_btn_text">#1F4E4E</color>   <!-- quick-action button text -->

<!-- Wallet card icon avatar -->
<color name="finan_wallet_icon_bg">#E6F0EF</color>         <!-- soft teal tint -->
<color name="finan_wallet_icon_text">#2D6A6A</color>       <!-- finan_primary -->

<!-- Empty state -->
<color name="finan_wallet_empty_icon">#C1D1D0</color>      <!-- muted teal for empty illustration -->
```

> **Implementation note:** Where tokens match an existing `finan_summary_*` color, reference the existing color name directly in XML rather than defining a duplicate. The table above shows the mapping for clarity.

---

## 3. Drawable Assets Required

### 3.1 `bg_wallet_hero.xml` — Hero card background

Identical gradient to the Summary hero card, but with a larger corner radius (16dp) to match the wallet management screen's spacing.

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:angle="135"
        android:startColor="@color/finan_summary_hero_bg_start"
        android:endColor="@color/finan_summary_hero_bg_end"
        android:type="linear" />
    <corners android:radius="16dp" />
</shape>
```

### 3.2 `bg_wallet_hero_btn.xml` — Quick-action button on hero

White pill with press state, matching `bg_button_pill.xml` pattern but inverted for dark background.

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="#E0EDED" />
            <corners android:radius="20dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
            <corners android:radius="20dp" />
        </shape>
    </item>
</selector>
```

### 3.3 `bg_wallet_card.xml` — Standard wallet item card

Extends `bg_card.xml` with increased corner radius (16dp) for a more modern feel.

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/finan_surface" />
    <corners android:radius="16dp" />
    <stroke
        android:width="1dp"
        android:color="@color/finan_divider" />
</shape>
```

### 3.4 `bg_wallet_card_default.xml` — Default wallet card (highlighted)

Same as `bg_wallet_card.xml` but with brand-colored 2dp stroke.

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/finan_surface" />
    <corners android:radius="16dp" />
    <stroke
        android:width="2dp"
        android:color="@color/finan_primary" />
</shape>
```

### 3.5 `bg_wallet_icon_avatar.xml` — Wallet icon container

Rounded square background for the wallet icon/emoji.

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#E6F0EF" />
    <corners android:radius="12dp" />
</shape>
```

### 3.6 `bg_wallet_currency_badge.xml` — Currency code pill

Subtle chip for the currency indicator.

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/finan_chip_bg" />
    <corners android:radius="10dp" />
</shape>
```

### 3.7 `bg_wallet_hero_count_pill.xml` — Wallet count badge on hero

Small pill badge with semi-transparent white background.

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#33FFFFFF" />
    <corners android:radius="12dp" />
</shape>
```

### 3.8 `ic_wallet_empty.xml` — Empty state illustration icon

A large (64dp) vector drawable of a wallet outline in `finan_wallet_empty_icon` (#C1D1D0). Use the existing `ic_wallet.xml` as the base shape, scaled to 64×64dp with a lighter tint.

### 3.9 `ic_transfer.xml` — Transfer icon for hero button

A 20dp vector icon showing two opposing horizontal arrows (⇆). Tint: `finan_wallet_hero_btn_text`.

---

## 4. Text Appearances (Additions to `styles.xml`)

```xml
<!-- Hero card: total balance label -->
<style name="Finan.Text.WalletHeroLabel" parent="TextAppearance.Material3.LabelMedium">
    <item name="android:textSize">13sp</item>
    <item name="android:textColor">@color/finan_summary_on_hero_muted</item>
    <item name="android:textStyle">bold</item>
    <item name="android:letterSpacing">0.04</item>
</style>

<!-- Hero card: total balance amount -->
<style name="Finan.Text.WalletHeroAmount" parent="TextAppearance.Material3.HeadlineMedium">
    <item name="android:textSize">28sp</item>
    <item name="android:textColor">@color/finan_summary_on_hero</item>
    <item name="android:textStyle">bold</item>
    <item name="android:fontFamily">sans-serif-medium</item>
</style>

<!-- Hero card: secondary info (count, default) -->
<style name="Finan.Text.WalletHeroSecondary" parent="TextAppearance.Material3.LabelSmall">
    <item name="android:textSize">12sp</item>
    <item name="android:textColor">@color/finan_summary_on_hero_muted</item>
</style>

<!-- Hero card: quick-action button text -->
<style name="Finan.Text.WalletHeroButton" parent="TextAppearance.Material3.LabelLarge">
    <item name="android:textSize">13sp</item>
    <item name="android:textColor">#1F4E4E</item>
    <item name="android:textStyle">bold</item>
    <item name="android:textAllCaps">false</item>
</style>

<!-- Wallet card: name -->
<style name="Finan.Text.WalletName" parent="TextAppearance.Material3.TitleSmall">
    <item name="android:textSize">16sp</item>
    <item name="android:textColor">@color/finan_text_primary</item>
    <item name="android:textStyle">bold</item>
</style>

<!-- Wallet card: balance -->
<style name="Finan.Text.WalletBalance" parent="TextAppearance.Material3.TitleMedium">
    <item name="android:textSize">20sp</item>
    <item name="android:textColor">@color/finan_text_primary</item>
    <item name="android:textStyle">bold</item>
    <item name="android:fontFamily">sans-serif-medium</item>
</style>

<!-- Wallet card: currency label -->
<style name="Finan.Text.WalletCurrency" parent="TextAppearance.Material3.LabelSmall">
    <item name="android:textSize">11sp</item>
    <item name="android:textColor">@color/finan_text_secondary</item>
    <item name="android:textStyle">bold</item>
</style>
```

---

## 5. String Resources (Additions to `strings.xml`)

```xml
<string name="wallet_hero_total_label">Total Saldo Dompet</string>
<string name="wallet_hero_add_action">+ Tambah Dompet</string>
<string name="wallet_hero_transfer_action">Transfer Saldo</string>
<string name="wallet_hero_default_indicator">★ %1$s</string>
<string name="wallet_list_section_label">Daftar Dompet</string>
<string name="wallet_empty_title">Belum ada dompet</string>
<string name="wallet_empty_body">Buat dompet pertama untuk mulai mencatat keuangan</string>
<string name="wallet_empty_action">Tambah Dompet Pertama</string>
<string name="wallet_balance_masked">•••••</string>
<string name="wallet_card_adjust_short">Sesuaikan</string>
```

---

## 6. Layout Architecture

### 6.1 `activity_wallet_list.xml` — Full Page Layout

```
LinearLayout (vertical, match_parent, bg: finan_background)
├── ScreenHeaderView (@+id/wallet_header)
│   titleText="@string/wallet_title"
│   actionIcon=GONE  ← moved to hero card quick-actions
│
├── RecyclerView (@+id/wallet_recycler)
│   layout_width="match_parent"
│   layout_height="0dp"
│   layout_weight="1"
│   clipToPadding="false"
│   paddingTop="0dp"
│   paddingStart="16dp"
│   paddingEnd="16dp"
│   paddingBottom="24dp"
│   overScrollMode="never"
│   │
│   ├── [ViewType 0: Hero Card]  ← first item, always present
│   ├── [ViewType 1: Section Label] ← "Daftar Dompet" label row
│   ├── [ViewType 2: Wallet Card] × N
│   └── [ViewType 3: Empty State] ← only when wallets.size() == 0
│
└── (no separate empty view — handled as RecyclerView item type)
```

#### XML specification:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/finan_background"
    android:orientation="vertical">

    <com.dwlhm.finan.ui.common.ScreenHeaderView
        android:id="@+id/wallet_header"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:titleText="@string/wallet_title" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/wallet_recycler"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:overScrollMode="never"
        android:paddingStart="16dp"
        android:paddingEnd="16dp"
        android:paddingBottom="24dp" />
</LinearLayout>
```

> **Note:** The `ScreenHeaderView` no longer carries `actionIcon` — the "+ Tambah Dompet" CTA moves into the hero card as a more prominent, contextual quick-action.

---

### 6.2 Hero Balance Card — View Type 0

Rendered as the first item in the RecyclerView adapter. Not a separate included layout — inflated from `item_wallet_hero.xml`.

```
item_wallet_hero.xml
─────────────────────────────────────────────
LinearLayout (vertical)
  bg: @drawable/bg_wallet_hero
  padding: 20dp
  marginBottom: 4dp
│
├── TextView (@+id/wallet_hero_label)
│   text: "Total Saldo Dompet"
│   style: Finan.Text.WalletHeroLabel
│
├── TextView (@+id/wallet_hero_total_balance)
│   style: Finan.Text.WalletHeroAmount
│   marginTop: 4dp
│   autoSizeTextType: uniform
│   autoSizeMinTextSize: 20sp
│   autoSizeMaxTextSize: 28sp
│   maxLines: 1
│   ← shows "Rp 1.250.000" or multi-currency lines
│   ← masked mode shows "•••••"
│
├── LinearLayout (horizontal, marginTop: 12dp, gravity: center_vertical)
│   ├── TextView (@+id/wallet_hero_count)
│   │   bg: @drawable/bg_wallet_hero_count_pill
│   │   padding: 4dp/10dp
│   │   style: Finan.Text.WalletHeroSecondary
│   │   ← "3 dompet"
│   │
│   ├── View (divider, 1dp width, 14dp height, marginStart: 10dp)
│   │   bg: @color/finan_summary_hero_divider
│   │
│   └── TextView (@+id/wallet_hero_default)
│       marginStart: 10dp
│       style: Finan.Text.WalletHeroSecondary
│       ← "★ Bank Jago"
│
├── View (divider line, match_parent × 1dp, marginTop: 16dp)
│   bg: @color/finan_summary_hero_divider
│
└── LinearLayout (horizontal, marginTop: 14dp, gravity: center)
    ├── Button (@+id/wallet_hero_add_btn)
    │   bg: @drawable/bg_wallet_hero_btn
    │   style: Finan.Text.WalletHeroButton
    │   height: 40dp
    │   paddingStart/End: 16dp
    │   drawableStart: @drawable/ic_add (tint #1F4E4E, 18dp)
    │   drawablePadding: 6dp
    │   text: "+ Tambah Dompet"
    │   layout_weight: 1
    │
    ├── Space (width: 10dp)
    │
    └── Button (@+id/wallet_hero_transfer_btn)
        bg: @drawable/bg_wallet_hero_btn
        style: Finan.Text.WalletHeroButton
        height: 40dp
        paddingStart/End: 16dp
        drawableStart: @drawable/ic_transfer (tint #1F4E4E, 18dp)
        drawablePadding: 6dp
        text: "Transfer Saldo"
        layout_weight: 1
```

#### Full XML:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_wallet_hero"
    android:orientation="vertical"
    android:padding="20dp"
    android:layout_marginBottom="4dp">

    <TextView
        android:id="@+id/wallet_hero_label"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/wallet_hero_total_label"
        android:textAppearance="@style/Finan.Text.WalletHeroLabel" />

    <TextView
        android:id="@+id/wallet_hero_total_balance"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:autoSizeMaxTextSize="28sp"
        android:autoSizeMinTextSize="20sp"
        android:autoSizeStepGranularity="1sp"
        android:autoSizeTextType="uniform"
        android:includeFontPadding="false"
        android:maxLines="1"
        android:textAppearance="@style/Finan.Text.WalletHeroAmount" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/wallet_hero_count"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_wallet_hero_count_pill"
            android:paddingStart="10dp"
            android:paddingTop="4dp"
            android:paddingEnd="10dp"
            android:paddingBottom="4dp"
            android:textAppearance="@style/Finan.Text.WalletHeroSecondary" />

        <View
            android:layout_width="1dp"
            android:layout_height="14dp"
            android:layout_marginStart="10dp"
            android:background="@color/finan_summary_hero_divider" />

        <TextView
            android:id="@+id/wallet_hero_default"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="10dp"
            android:textAppearance="@style/Finan.Text.WalletHeroSecondary" />
    </LinearLayout>

    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginTop="16dp"
        android:background="@color/finan_summary_hero_divider" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="14dp"
        android:gravity="center"
        android:orientation="horizontal">

        <Button
            android:id="@+id/wallet_hero_add_btn"
            android:layout_width="0dp"
            android:layout_height="40dp"
            android:layout_weight="1"
            android:background="@drawable/bg_wallet_hero_btn"
            android:drawablePadding="6dp"
            android:gravity="center"
            android:paddingStart="16dp"
            android:paddingEnd="16dp"
            android:text="@string/wallet_hero_add_action"
            android:textAppearance="@style/Finan.Text.WalletHeroButton"
            app:drawableStartCompat="@drawable/ic_add"
            app:drawableTint="#1F4E4E" />

        <Space
            android:layout_width="10dp"
            android:layout_height="match_parent" />

        <Button
            android:id="@+id/wallet_hero_transfer_btn"
            android:layout_width="0dp"
            android:layout_height="40dp"
            android:layout_weight="1"
            android:background="@drawable/bg_wallet_hero_btn"
            android:drawablePadding="6dp"
            android:gravity="center"
            android:paddingStart="16dp"
            android:paddingEnd="16dp"
            android:text="@string/wallet_hero_transfer_action"
            android:textAppearance="@style/Finan.Text.WalletHeroButton"
            app:drawableStartCompat="@drawable/ic_transfer"
            app:drawableTint="#1F4E4E" />
    </LinearLayout>
</LinearLayout>
```

---

### 6.3 Section Label — View Type 1

A simple label row between the hero and the wallet list.

```xml
<!-- item_wallet_section_label.xml -->
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/wallet_section_label"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingTop="16dp"
    android:paddingBottom="8dp"
    android:text="@string/wallet_list_section_label"
    android:textAppearance="@style/Finan.Text.SectionLabel" />
```

---

### 6.4 `item_wallet.xml` — Wallet Card — View Type 2

```
item_wallet.xml
───────────────────────────────────────────────
LinearLayout (vertical)
  bg: @drawable/bg_wallet_card  OR  bg_wallet_card_default
  foreground: ?android:attr/selectableItemBackground
  cornerRadius effect via bg drawable
  padding: 16dp
  marginBottom: 10dp
│
├── LinearLayout (horizontal, gravity: center_vertical)
│   │
│   ├── FrameLayout (@+id/item_wallet_icon_container)
│   │   width/height: 44dp
│   │   bg: @drawable/bg_wallet_icon_avatar
│   │   marginEnd: 14dp
│   │   │
│   │   ├── ImageView (@+id/item_wallet_icon_image)
│   │   │   match_parent, padding: 10dp
│   │   │   src: @drawable/ic_wallet (fallback)
│   │   │   tint: @color/finan_primary
│   │   │   visibility: gone when emoji is set
│   │   │
│   │   └── TextView (@+id/item_wallet_icon_emoji)
│   │       match_parent, gravity: center
│   │       textSize: 22sp
│   │       visibility: gone when no emoji
│   │
│   ├── LinearLayout (vertical, weight: 1)
│   │   ├── LinearLayout (horizontal, gravity: center_vertical)
│   │   │   ├── TextView (@+id/item_wallet_name)
│   │   │   │   style: Finan.Text.WalletName
│   │   │   │   ellipsize: end, maxLines: 1
│   │   │   │   weight: 1 (shrinks)
│   │   │   │
│   │   │   └── TextView (@+id/item_wallet_default_badge)
│   │   │       bg: @drawable/bg_chip_selected
│   │   │       text: "Default"
│   │   │       paddingH: 8dp, paddingV: 3dp
│   │   │       textSize: 10sp, bold
│   │   │       textColor: finan_chip_text_selected
│   │   │       visibility: gone (shown only for default wallet)
│   │   │       marginStart: 8dp
│   │   │
│   │   └── TextView (@+id/item_wallet_currency)
│   │       style: Finan.Text.WalletCurrency
│   │       bg: @drawable/bg_wallet_currency_badge
│   │       paddingH: 8dp, paddingV: 2dp
│   │       marginTop: 4dp
│   │       ← "IDR" / "USD"
│   │
│   └── LinearLayout (vertical, gravity: end, marginStart: 12dp)
│       │ ← right-aligned balance area
│       │
│       ├── ImageButton (@+id/item_wallet_edit)
│       │   width/height: 32dp
│       │   bg: ?android:attr/selectableItemBackgroundBorderless
│       │   src: @drawable/ic_edit
│       │   tint: @color/finan_text_hint
│       │
│       └── ImageButton (@+id/item_wallet_delete)
│           width/height: 32dp
│           marginTop: 2dp
│           bg: ?android:attr/selectableItemBackgroundBorderless
│           src: @drawable/ic_delete
│           tint: @color/finan_text_hint
│
├── TextView (@+id/item_wallet_balance)
│   style: Finan.Text.WalletBalance
│   marginTop: 12dp
│   gravity: start
│   ← "Rp 1.250.000" or "•••••"
│
└── LinearLayout (horizontal, marginTop: 12dp, gravity: start|center_vertical)
    │ ← action bar
    │
    ├── Button (@+id/item_wallet_adjust)
    │   style: Finan.Button.Nav
    │   text: "Sesuaikan"
    │   textSize: 13sp
    │   minWidth: 0dp
    │   minHeight: 36dp
    │   paddingStart/End: 12dp
    │
    ├── Button (@+id/item_wallet_transfer)
    │   style: Finan.Button.Nav
    │   text: "Transfer"
    │   textSize: 13sp
    │   marginStart: 6dp
    │   minWidth: 0dp
    │   minHeight: 36dp
    │   paddingStart/End: 12dp
    │
    └── (edit/delete are positioned top-right via the icon buttons above)
```

#### Full XML:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/item_wallet_card"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_wallet_card"
    android:foreground="?android:attr/selectableItemBackground"
    android:orientation="vertical"
    android:padding="16dp"
    android:layout_marginBottom="10dp">

    <!-- Row 1: Icon + Name/Currency + Action icons -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <!-- Icon/Emoji avatar -->
        <FrameLayout
            android:id="@+id/item_wallet_icon_container"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:layout_marginEnd="14dp"
            android:background="@drawable/bg_wallet_icon_avatar">

            <ImageView
                android:id="@+id/item_wallet_icon_image"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:contentDescription="@string/wallet_title"
                android:padding="10dp"
                android:src="@drawable/ic_wallet"
                app:tint="@color/finan_primary" />

            <TextView
                android:id="@+id/item_wallet_icon_emoji"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:textSize="22sp"
                android:visibility="gone" />
        </FrameLayout>

        <!-- Name + Currency column -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <TextView
                    android:id="@+id/item_wallet_name"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:ellipsize="end"
                    android:maxLines="1"
                    android:textAppearance="@style/Finan.Text.WalletName" />

                <TextView
                    android:id="@+id/item_wallet_default_badge"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:background="@drawable/bg_chip_selected"
                    android:paddingStart="8dp"
                    android:paddingTop="3dp"
                    android:paddingEnd="8dp"
                    android:paddingBottom="3dp"
                    android:text="@string/wallet_default_badge"
                    android:textColor="@color/finan_chip_text_selected"
                    android:textSize="10sp"
                    android:textStyle="bold"
                    android:visibility="gone" />
            </LinearLayout>

            <TextView
                android:id="@+id/item_wallet_currency"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:background="@drawable/bg_wallet_currency_badge"
                android:paddingStart="8dp"
                android:paddingTop="2dp"
                android:paddingEnd="8dp"
                android:paddingBottom="2dp"
                android:textAppearance="@style/Finan.Text.WalletCurrency" />
        </LinearLayout>

        <!-- Edit/Delete icon cluster (right side, stacked vertically) -->
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:gravity="center_horizontal"
            android:orientation="vertical">

            <ImageButton
                android:id="@+id/item_wallet_edit"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:background="?android:attr/selectableItemBackgroundBorderless"
                android:contentDescription="@string/wallet_edit_name"
                android:src="@drawable/ic_edit"
                app:tint="@color/finan_text_hint" />

            <ImageButton
                android:id="@+id/item_wallet_delete"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:layout_marginTop="2dp"
                android:background="?android:attr/selectableItemBackgroundBorderless"
                android:contentDescription="@string/transaction_delete_action"
                android:src="@drawable/ic_delete"
                app:tint="@color/finan_text_hint" />
        </LinearLayout>
    </LinearLayout>

    <!-- Row 2: Balance display -->
    <TextView
        android:id="@+id/item_wallet_balance"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:textAppearance="@style/Finan.Text.WalletBalance" />

    <!-- Row 3: Action bar (Adjust + Transfer) -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:gravity="start|center_vertical"
        android:orientation="horizontal">

        <Button
            android:id="@+id/item_wallet_adjust"
            style="@style/Finan.Button.Nav"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:minWidth="0dp"
            android:minHeight="36dp"
            android:paddingStart="12dp"
            android:paddingEnd="12dp"
            android:text="@string/wallet_card_adjust_short"
            android:textSize="13sp" />

        <Button
            android:id="@+id/item_wallet_transfer"
            style="@style/Finan.Button.Nav"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="6dp"
            android:minWidth="0dp"
            android:minHeight="36dp"
            android:paddingStart="12dp"
            android:paddingEnd="12dp"
            android:text="@string/wallet_transfer_action"
            android:textSize="13sp" />
    </LinearLayout>
</LinearLayout>
```

---

### 6.5 Empty State — View Type 3

Shown instead of wallet cards when `wallets.isEmpty()`. Rendered as a RecyclerView item type to keep scroll behavior unified.

```xml
<!-- item_wallet_empty.xml -->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center"
    android:orientation="vertical"
    android:paddingStart="24dp"
    android:paddingTop="48dp"
    android:paddingEnd="24dp"
    android:paddingBottom="48dp">

    <!-- Illustration: large muted wallet icon -->
    <ImageView
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:contentDescription="@string/wallet_empty_title"
        android:src="@drawable/ic_wallet"
        app:tint="@color/finan_wallet_empty_icon" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:gravity="center"
        android:text="@string/wallet_empty_title"
        android:textAppearance="@style/Finan.Text.Title" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:gravity="center"
        android:text="@string/wallet_empty_body"
        android:textAppearance="@style/Finan.Text.Body"
        android:textColor="@color/finan_text_secondary" />

    <Button
        android:id="@+id/wallet_empty_action"
        style="@style/Finan.Button.Primary"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:paddingStart="24dp"
        android:paddingEnd="24dp"
        android:text="@string/wallet_empty_action" />
</LinearLayout>
```

---

## 7. RecyclerView Adapter Architecture

### 7.1 View Types

| Type constant | Value | Layout | Condition |
|---|---|---|---|
| `TYPE_HERO` | 0 | `item_wallet_hero.xml` | Always position 0 |
| `TYPE_SECTION` | 1 | `item_wallet_section_label.xml` | Always position 1 |
| `TYPE_WALLET` | 2 | `item_wallet.xml` | Positions 2..N+1 when wallets.size() > 0 |
| `TYPE_EMPTY` | 3 | `item_wallet_empty.xml` | Position 2 when wallets.size() == 0 |

### 7.2 Item count formula

```java
@Override
public int getItemCount() {
    // hero + section label + max(wallets.size(), 1 for empty)
    return 2 + Math.max(wallets.size(), 1);
}
```

When `wallets.size() == 0`, `getItemCount()` returns 3 (hero, section, empty).

### 7.3 Adapter class: `WalletListAdapter`

Replace the inner `WalletAdapter extends BaseAdapter` with `WalletListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>`.

**ViewHolder classes:**
- `HeroViewHolder` — binds total balance, wallet count, default wallet name, quick-action click listeners.
- `SectionViewHolder` — static label, no binding needed.
- `WalletViewHolder` — binds single wallet data (icon, name, currency, balance, default badge, actions).
- `EmptyViewHolder` — binds the CTA click listener.

**DiffUtil:** Implement `DiffUtil.ItemCallback<Object>` keyed by item type + wallet ID for `TYPE_WALLET`, using `areContentsTheSame()` on wallet fields.

### 7.4 LayoutManager

```java
recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
```

Standard vertical `LinearLayoutManager`. No grid, no staggered layout.

---

## 8. Interaction & Animation Specifications

### 8.1 ViewPressAnimator (existing pattern — extend to new elements)

Apply `ViewPressAnimator.bindScale(view)` to:
- `wallet_hero_add_btn`
- `wallet_hero_transfer_btn`
- `item_wallet_card` (the entire card root)
- `item_wallet_edit`
- `item_wallet_delete`
- `item_wallet_adjust`
- `item_wallet_transfer`
- `wallet_empty_action`

Parameters: unchanged from existing (0.9× scale down over 80ms, 1.0× restore over 120ms).

### 8.2 RecyclerView Item Animations

#### Default add/remove/change

Use the built-in `DefaultItemAnimator` (already the RecyclerView default). This provides:
- **Item added:** Fade-in (250ms)
- **Item removed:** Fade-out (250ms)
- **Item changed:** Cross-fade (250ms)

No custom `ItemAnimator` subclass needed.

#### Delete with undo

When user taps delete and confirms:
1. The wallet card is removed from the adapter list.
2. `notifyItemRemoved(position)` triggers the `DefaultItemAnimator` fade-out.
3. `FinanToast.show(activity, "Dompet \"X\" dihapus", "Urungkan", undoRunnable)` appears.
4. If undone, `notifyItemInserted(position)` re-animates the card in.

#### Staggered load animation (initial page load)

On first `reload()` completing, animate wallet cards with a subtle stagger effect:

```java
// In WalletListAdapter, track whether initial animation has played
private boolean initialAnimationDone = false;

@Override
public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    // ... normal binding ...

    if (!initialAnimationDone && holder instanceof WalletViewHolder) {
        int walletIndex = position - 2; // offset for hero + section
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(24f);
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(walletIndex * 50L)  // 50ms stagger per card
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();
    }
}

// Called after setWallets() completes
void markInitialAnimationDone() {
    initialAnimationDone = true;
}
```

### 8.3 Hero card balance animation

On balance value change (e.g., after adjustment or transfer), animate the total balance text with a brief fade-through:

```java
private void animateBalanceChange(TextView balanceView, String newText) {
    balanceView.animate()
        .alpha(0f)
        .setDuration(100)
        .withEndAction(() -> {
            balanceView.setText(newText);
            balanceView.animate()
                .alpha(1f)
                .setDuration(200)
                .start();
        })
        .start();
}
```

### 8.4 Button ripple / foreground

All interactive elements already use `?android:attr/selectableItemBackground` or `?android:attr/selectableItemBackgroundBorderless` as foreground/background, providing system-level Material ripple. No custom ripple drawables needed.

### 8.5 Transfer button disabled state

When only 1 wallet exists:
- Per-card `item_wallet_transfer` button: `setEnabled(false)`, `setAlpha(0.5f)`
- Hero `wallet_hero_transfer_btn`: `setEnabled(false)`, `setAlpha(0.5f)`

---

## 9. Data Binding Logic

### 9.1 Hero card binding

```java
void bindHero(HeroViewHolder holder, List<Wallet> wallets) {
    boolean masked = BottomSheetHelper.isMasked(context);

    // Total balance computation (grouped by currency)
    Map<String, Long> totalsByCurrency = new LinkedHashMap<>();
    Wallet defaultWallet = null;
    for (Wallet wallet : wallets) {
        String code = MoneyFormatter.normalizeCurrencyCode(wallet.getCurrencyCode());
        totalsByCurrency.merge(code, wallet.getCachedBalanceMinor(), Long::sum);
        if (wallet.isDefault()) defaultWallet = wallet;
    }

    // Format total — if single currency, show formatted amount
    // If multi-currency, show first currency amount (comma-separated overflow)
    if (masked) {
        holder.totalBalance.setText(R.string.wallet_balance_masked);
    } else if (totalsByCurrency.size() == 1) {
        Map.Entry<String, Long> entry = totalsByCurrency.entrySet().iterator().next();
        holder.totalBalance.setText(
            MoneyFormatter.formatWithCurrencyCode(entry.getKey(), entry.getValue()));
    } else {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> entry : totalsByCurrency.entrySet()) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(MoneyFormatter.formatWithCurrencyCode(entry.getKey(), entry.getValue()));
        }
        holder.totalBalance.setText(sb.toString());
    }

    // Count pill
    holder.count.setText(
        context.getResources().getQuantityString(
            R.plurals.wallet_count, wallets.size(), wallets.size()));

    // Default wallet indicator
    if (defaultWallet != null) {
        holder.defaultIndicator.setText(
            context.getString(R.string.wallet_hero_default_indicator, defaultWallet.getName()));
        holder.defaultIndicator.setVisibility(View.VISIBLE);
    } else {
        holder.defaultIndicator.setVisibility(View.GONE);
    }

    // Transfer button state
    holder.transferBtn.setEnabled(wallets.size() > 1);
    holder.transferBtn.setAlpha(wallets.size() > 1 ? 1f : 0.5f);
}
```

### 9.2 Wallet card binding

```java
void bindWallet(WalletViewHolder holder, Wallet wallet, int walletCount) {
    boolean masked = BottomSheetHelper.isMasked(context);

    // Card background — highlight default wallet
    holder.card.setBackgroundResource(
        wallet.isDefault() ? R.drawable.bg_wallet_card_default : R.drawable.bg_wallet_card);

    // Icon: prefer emoji, fallback to ic_wallet
    String icon = wallet.getIcon();
    if (icon != null && !icon.isEmpty()) {
        holder.iconEmoji.setText(icon);
        holder.iconEmoji.setVisibility(View.VISIBLE);
        holder.iconImage.setVisibility(View.GONE);
    } else {
        holder.iconImage.setVisibility(View.VISIBLE);
        holder.iconEmoji.setVisibility(View.GONE);
    }

    // Name
    holder.name.setText(wallet.getName());

    // Default badge
    holder.defaultBadge.setVisibility(wallet.isDefault() ? View.VISIBLE : View.GONE);

    // Currency label
    holder.currency.setText(MoneyFormatter.normalizeCurrencyCode(wallet.getCurrencyCode()));

    // Balance
    if (masked) {
        holder.balance.setText(R.string.wallet_balance_masked);
    } else {
        holder.balance.setText(
            MoneyFormatter.formatWithCurrencyCode(
                wallet.getCurrencyCode(), wallet.getCachedBalanceMinor()));
    }

    // Actions
    ViewPressAnimator.bindScale(holder.card);
    ViewPressAnimator.bindScale(holder.editBtn);
    ViewPressAnimator.bindScale(holder.deleteBtn);
    holder.editBtn.setOnClickListener(v -> listener.onEdit(wallet));
    holder.deleteBtn.setOnClickListener(v -> listener.onDelete(wallet));
    holder.adjustBtn.setOnClickListener(v -> listener.onAdjust(wallet));
    holder.transferBtn.setOnClickListener(v -> listener.onTransfer(wallet));
    holder.transferBtn.setEnabled(walletCount > 1);
    holder.transferBtn.setAlpha(walletCount > 1 ? 1f : 0.5f);
}
```

---

## 10. Interaction Flows

### 10.1 Page load

```
User taps "Dompet" in Settings
  → Fragment inflates activity_wallet_list.xml
  → RecyclerView set up with LinearLayoutManager + WalletListAdapter
  → ScreenHeaderView back button → popBackStack()
  → reload() fetches wallets via services.dbWorker.compute()
  → On result:
      if wallets.isEmpty():
          adapter shows [Hero, Section, Empty]
          Hero total balance shows "Rp 0"
          Hero count shows "0 dompet"
          Hero add button active, transfer button disabled
      else:
          adapter shows [Hero, Section, Wallet₁, Wallet₂, ...]
          Hero bound with aggregate data
          Stagger animation plays for wallet cards
```

### 10.2 Add wallet (from hero)

```
User taps "+ Tambah Dompet" on hero card
  → ViewPressAnimator scale feedback (80ms down, 120ms up)
  → showAddWalletDialog() opens WalletInputDialog bottom sheet
  → On save: reload() → adapter.setWallets() → DiffUtil animates new card in
  → Hero card rebinds with updated totals
```

### 10.3 Transfer (from hero)

```
User taps "Transfer Saldo" on hero card
  → If wallets.size() < 2: FinanToast shows wallet_transfer_need_two
  → Else: showTransferDialog(null) → opens dialog_transfer_bottom_sheet
  → Source defaults to first wallet (no pre-selection since triggered from hero)
  → On complete: reload() → cards update via DiffUtil, hero balance animates
```

### 10.4 Transfer (from card)

```
User taps "Transfer" on a wallet card
  → showTransferDialog(wallet) → source pre-selected to that wallet
  → Flow same as above
```

### 10.5 Adjust balance

```
User taps "Sesuaikan" on a wallet card
  → showAdjustmentDialog(wallet) → opens dialog_adjustment_bottom_sheet
  → On complete: reload() → DiffUtil updates the card, hero balance animates
```

### 10.6 Edit wallet

```
User taps edit (pencil) icon on card
  → ViewPressAnimator scale feedback
  → showEditWalletDialog(wallet) → opens WalletEditBottomSheet
  → On save: reload() → DiffUtil cross-fades updated card
```

### 10.7 Delete wallet

```
User taps delete (trash) icon on card
  → ViewPressAnimator scale feedback
  → showDeleteWalletDialog(wallet) → opens dialog_confirm_delete
  → On confirm:
      Card removed → notifyItemRemoved() → DefaultItemAnimator fade-out
      FinanToast with "Urungkan" action
      If undone: insertUndo() → reload() → notifyItemInserted() → fade-in
```

### 10.8 Balance mask toggle

Masked mode is toggled from Settings (not on this page), but the Dompet page reacts to it on `onResume()`:
- Hero total balance → `"•••••"`
- Each wallet card balance → `"•••••"`
- Adjustment dialog current balance → `"***"` (existing behavior)
- Transfer spinner labels → `"***"` suffix (existing behavior)

---

## 11. Accessibility

### 11.1 Content descriptions

- Hero card total balance: `"Total saldo dompet: <formatted amount>"` or `"Total saldo dompet: disembunyikan"` when masked.
- Wallet count pill: Read naturally by TalkBack from text content.
- Wallet card: The card root has `android:focusable="true"` and a content description assembled from name + balance.
- Edit button: `contentDescription="@string/wallet_edit_name"`
- Delete button: `contentDescription="@string/transaction_delete_action"`
- Adjust button: Text content is self-describing.
- Transfer button: Text content is self-describing. When disabled, TalkBack announces disabled state.

### 11.2 Focus order

TalkBack traversal follows document order:
1. Back button
2. "Dompet" title
3. Hero label → total balance → count → default → add button → transfer button
4. Section label
5. Wallet cards in list order (icon → name/badge → currency → balance → adjust → transfer → edit → delete)

### 11.3 Contrast ratios

| Element | Foreground | Background | Ratio |
|---|---|---|---|
| Hero label | #D7E8E6 on gradient ~#234A4A | — | ≥ 5.2:1 |
| Hero balance | #FFFFFF on gradient ~#234A4A | — | ≥ 12:1 |
| Hero button text | #1F4E4E on #FFFFFF | — | ≥ 9:1 |
| Card name | #1A2830 on #FFFFFF | — | ≥ 15:1 |
| Card balance | #1A2830 on #FFFFFF | — | ≥ 15:1 |
| Action button | #2D6A6A on #FFFFFF | — | ≥ 6:1 |

All pass WCAG AA (4.5:1 for normal text, 3:1 for large text).

---

## 12. Spacing Grid Reference

All spacing values use the app's implicit 4dp base grid:

| Token | Value | Usage |
|---|---|---|
| Page horizontal margin | 16dp | RecyclerView paddingStart/End |
| Hero padding | 20dp | Inner content padding |
| Hero bottom margin | 4dp | Space before section label |
| Section label paddingTop | 16dp | Breathing room after hero |
| Section label paddingBottom | 8dp | Before first card |
| Card padding | 16dp | Inner content padding |
| Card bottom margin | 10dp | Between wallet cards |
| Icon avatar size | 44dp | Matches category list pattern |
| Icon to name gap | 14dp | marginEnd on icon container |
| Balance marginTop | 12dp | After name/currency row |
| Action bar marginTop | 12dp | After balance |
| Action button minHeight | 36dp | Compact touch target |
| Empty state paddingV | 48dp | Generous vertical whitespace |
| Empty icon size | 64dp | Prominent but not overwhelming |
| Empty CTA marginTop | 20dp | After body text |
| Page bottom padding | 24dp | Scroll overrun clearance |

---

## 13. File Manifest

### New files to create

| File | Purpose |
|---|---|
| `res/layout/item_wallet_hero.xml` | Hero balance card layout |
| `res/layout/item_wallet_section_label.xml` | Section divider label |
| `res/layout/item_wallet_empty.xml` | Empty state layout |
| `res/drawable/bg_wallet_hero.xml` | Hero gradient background |
| `res/drawable/bg_wallet_hero_btn.xml` | Quick-action pill button on hero |
| `res/drawable/bg_wallet_card.xml` | Wallet card background (16dp radius) |
| `res/drawable/bg_wallet_card_default.xml` | Default wallet card (brand stroke) |
| `res/drawable/bg_wallet_icon_avatar.xml` | Wallet icon container background |
| `res/drawable/bg_wallet_currency_badge.xml` | Currency code pill |
| `res/drawable/bg_wallet_hero_count_pill.xml` | Count badge on hero |
| `res/drawable/ic_transfer.xml` | Transfer icon vector |

### Files to modify

| File | Changes |
|---|---|
| `res/layout/activity_wallet_list.xml` | Replace `ListView` + summary + empty with `RecyclerView` |
| `res/layout/item_wallet.xml` | Complete redesign per spec §6.4 |
| `res/values/colors.xml` | Add `finan_wallet_empty_icon` (#C1D1D0) |
| `res/values/styles.xml` | Add 7 new text appearance styles |
| `res/values/strings.xml` | Add 8 new string resources |
| `WalletListFragment.java` | Replace `BaseAdapter`/`ListView` with `RecyclerView.Adapter`, new ViewHolder classes, stagger animation, hero binding |

### Files to remove or deprecate

| File | Action |
|---|---|
| `res/drawable/bg_card_wallet_default.xml` | Replaced by `bg_wallet_card_default.xml` — remove after migration |

---

## 14. Visual Reference (ASCII Wireframe)

```
┌─────────────────────────────────┐
│  ← Dompet                      │  ← ScreenHeaderView (no action icon)
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │  Total Saldo Dompet         │ │  ← hero label, muted white
│ │  Rp 3.450.000               │ │  ← hero amount, 28sp bold white
│ │                             │ │
│ │  ╭──────────╮               │ │
│ │  │ 3 dompet │ │ ★ Bank Jago│ │  ← count pill + default indicator
│ │  ╰──────────╯               │ │
│ │  ─────────────────────────  │ │  ← divider
│ │  ╭───────────────╮ ╭──────╮ │ │
│ │  │+ Tambah Dompet│ │Trans…│ │ │  ← white pill buttons
│ │  ╰───────────────╯ ╰──────╯ │ │
│ └─────────────────────────────┘ │
│                                 │
│  Daftar Dompet                  │  ← section label
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ╭────╮                  ✎ 🗑│ │  ← 44dp icon avatar + edit/delete
│ │ │ 🏦 │ Bank Jago  Default  │ │  ← emoji + name + badge
│ │ ╰────╯ IDR                  │ │  ← currency pill
│ │                             │ │
│ │ Rp 2.100.000                │ │  ← 20sp balance
│ │                             │ │
│ │ Sesuaikan   Transfer        │ │  ← nav buttons
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ╭────╮                  ✎ 🗑│ │
│ │ │ 💵 │ Cash Harian          │ │
│ │ ╰────╯ IDR                  │ │
│ │                             │ │
│ │ Rp 350.000                  │ │
│ │                             │ │
│ │ Sesuaikan   Transfer        │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ╭────╮                  ✎ 🗑│ │
│ │ │ 💲 │ Savings USD          │ │
│ │ ╰────╯ USD                  │ │
│ │                             │ │
│ │ $1,000.00                   │ │
│ │                             │ │
│ │ Sesuaikan   Transfer        │ │
│ └─────────────────────────────┘ │
│                   ▒▒ 24dp pad ▒ │
└─────────────────────────────────┘
```

### Empty state wireframe

```
┌─────────────────────────────────┐
│  ← Dompet                      │
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │  Total Saldo Dompet         │ │
│ │  Rp 0                      │ │
│ │  ╭──────────╮               │ │
│ │  │ 0 dompet │               │ │
│ │  ╰──────────╯               │ │
│ │  ─────────────────────────  │ │
│ │  ╭───────────────╮ ╭──────╮ │ │
│ │  │+ Tambah Dompet│ │Trans…│ │ │  ← transfer disabled (0.5 alpha)
│ │  ╰───────────────╯ ╰──────╯ │ │
│ └─────────────────────────────┘ │
│                                 │
│  Daftar Dompet                  │
│                                 │
│                                 │
│          ╭────────╮             │
│          │  🔲    │             │  ← 64dp muted wallet icon
│          ╰────────╯             │
│                                 │
│       Belum ada dompet          │  ← Title style, 22sp
│                                 │
│    Buat dompet pertama untuk    │
│    mulai mencatat keuangan      │  ← Body, secondary color
│                                 │
│    ╭──────────────────────╮     │
│    │  Tambah Dompet Pertama│    │  ← Primary button
│    ╰──────────────────────╯     │
│                                 │
└─────────────────────────────────┘
```
