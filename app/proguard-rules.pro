# Finan — keep SQLite and domain models used via reflection sparingly
-keep class com.dwlhm.finan.data.db.FinanDatabaseHelper { *; }
-keep class com.dwlhm.finan.data.migration.** { *; }

# Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
