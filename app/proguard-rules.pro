# Hisaab Diary release rules.
# Room, Hilt, WorkManager, Compose and kotlinx.serialization ship their own consumer rules.

# Type-safe navigation routes and the backup meta use kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.hisaab.diary.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.hisaab.diary.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Enum names are stored in the database and settings (GAVE/GOT, theme, currency).
-keepclassmembers enum com.hisaab.diary.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Workers are created by WorkManager with reflection.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
