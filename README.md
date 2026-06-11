# Finan

Capture-first financial tracker for Android (production v1).

## Stack

- Java 17, Android Views, Activity
- SQLite + SharedPreferences
- minSdk 30, targetSdk 35
- `applicationId`: `com.dwlhm.finan`

## Structure

```text
app/src/main/java/com/dwlhm/finan/
  ui/<screen>/          # satu folder per layar
  domain/model|rule/
  data/db|dao|migration|prefs|entity/
  service/
  util/
app/src/test/java/      # unit tests (TDD)
app/src/androidTest/java/ # migration & DAO tests
```

## Build

Requires **JDK 17+** (Android Studio JBR) and Android SDK.

1. Copy `local.properties.example` → `local.properties` and set `sdk.dir` (default macOS: `~/Library/Android/sdk`).
2. Point `JAVA_HOME` to Android Studio JBR, e.g.  
   `export JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
3. Sync Gradle (Android Studio: **Sync Project**, or run `./gradlew` below).

```bash
./gradlew test
./gradlew assembleDebug
./gradlew bundleRelease
./gradlew connectedDebugAndroidTest
```

## Features (v1)

- Home capture: amount → category → save (local-first)
- History, wallets, categories
- Monthly summary (lazy-loaded, not on cold start)
- Export CSV only (Settings → SAF)
- Indonesian UI (`values/strings.xml`), ready for `values-en/`

## Docs

- `docs/concept.md` — product concept
- `docs/adr/` — architecture decisions
