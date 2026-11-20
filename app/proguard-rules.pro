# Keep Room Entities, DataStore settings, and DAOs
-keep class com.example.shiftly.data.local.** { *; }
-keep interface com.example.shiftly.data.local.** { *; }

# Keep Backup JSON serialization models
-keep class com.example.shiftly.data.backup.** { *; }

# Keep WorkManager Workers
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class com.example.shiftly.notifications.** { *; }
