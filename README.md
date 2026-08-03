# Finan

Capture-first financial tracker for Android (production v1).

## Stack

- Java 17, Android Views, AppCompatActivity
- SQLite + SharedPreferences
- minSdk 30, targetSdk 35, compileSdk 37
- `applicationId`: `com.dwlhm.finan`

## Navigation & UI Philosophy

- **Main Menu**: Consists of **Catat** (Capture), **Buku Kas** (Monthly Dashboard), and **Pengaturan** (Settings).
- **Floating Liquid Glass Navigation**:
  - Translucent glass capsule with glossy rim reflection stroke and subtle drop shadow (`FloatingBottomNavView`).
  - **Side-by-Side Expanding Active Pill**: Inactive items show icons only; active item expands horizontally to display **[ Icon + Label ]** side-by-side.
  - **Auto Hide & Show on Scroll**: Powered by `CoordinatorLayout` + `HideBottomViewOnScrollBehavior` over `NestedScrollView` containers.
  - **Dual-Layer Glass Architecture**: Layer 1 handles frosted glass backdrop effects, while Layer 2 renders foreground icons and labels with 100% crispness.
  - **Full Light & Dark Mode Support**: Auto-adapts color palettes and contrast across system themes according to WCAG 2.1 AA guidelines.

## Structure

```text
app/src/main/java/com/dwlhm/finan/
  ui/
    capture/            # Layar 'Catat' (Quick input & financial keypad)
    dashboard/          # Layar 'Buku Kas' (Monthly dashboard & transactions)
    settings/           # Layar 'Pengaturan' (Wallets, categories, export/import)
    components/         # Reusable UI components (FloatingBottomNavView, Keypad, etc.)
    common/             # Shared fragments, bottom sheets, & navigators
  domain/model|rule/
  data/db|dao|migration|prefs|entity/
  service/
  util/
app/src/test/java/      # Unit tests
app/src/androidTest/java/ # Migration & DAO integration tests
```

## Build

Requires **JDK 17+** (Android Studio JBR) and Android SDK.

```bash
JAVA_HOME="/Users/dwlhm/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```

Command options:

```bash
./gradlew test
./gradlew assembleDebug
./gradlew lintDebug
./gradlew bundleRelease
./gradlew connectedDebugAndroidTest
```

## Features (v1)

- **Capture-First Home**: Quick amount entry → category select → instant local save.
- **Floating Liquid Glass Navigation Bar**: Expanding side-by-side active pill with auto hide/show on scroll.
- **Buku Kas & History**: Monthly financial overview, daily activities, and category breakdown.
- **Pengaturan & Customization**: Manage wallets, manage categories, and CSV data export/import via SAF.
- **Light & Dark Mode**: Native support with automated contrast adaptation across system themes.
- **Indonesian UI (`values/strings.xml`)**, ready for `values-en/`.

## Docs

- `docs/concept.md` — Product concept & core philosophy
- `docs/design-principal.md` — UI/UX & Liquid Glass design principles
- `docs/technical-principal.md` — Architecture & engineering principles
- `docs/adr/` — Architecture Decision Records
