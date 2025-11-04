## Consumer ProGuard rules for Netcore SDK Capturer
## These rules are packaged into the AAR and applied to consuming apps.

## Keep the public SDK entry points and callback so R8 in consumer apps doesn't
## strip or rename them (prevents "Cannot access ... SdkLogCaptor.a" style errors).
-keep class com.android.netcoresdkcapturer.SdkLogCaptor {
	public *;
}

-keep class com.android.netcoresdkcapturer.LogPayloadCallback {
	public *;
}

## Keep companion object methods used from Java
-keepclassmembers class com.android.netcoresdkcapturer.SdkLogCaptor$Companion {
	public *;
}

## Keep Room DB and Worker classes (consumer may use default rules but keep here as well)
-keep class com.android.netcoresdkcapturer.database.** { *; }
-keep class com.android.netcoresdkcapturer.worker.EventUploadWorker { *; }
