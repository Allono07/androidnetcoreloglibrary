# NetcoreSDKCapturer ProGuard Rules

# Keep public API
-keep public class com.android.netcoresdkcapturer.SdkLogCaptor {
    public *;
}

-keep public class com.android.netcoresdkcapturer.LogPayloadCallback {
    public *;
}

# Keep companion object methods (for Java interop)
-keepclassmembers class com.android.netcoresdkcapturer.SdkLogCaptor$Companion {
    public *;
}

# Keep Room database classes
-keep class com.android.netcoresdkcapturer.database.** { *; }

# Keep WorkManager worker
-keep class com.android.netcoresdkcapturer.worker.EventUploadWorker { *; }

# Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Room
-keepclassmembers,allowobfuscation class * {
  @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.impl.WorkManagerInitializer

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
