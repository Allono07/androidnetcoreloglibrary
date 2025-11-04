# Netcore SDK Capturer

Android SDK for capturing and uploading Netcore Smartech event logs to a webhook endpoint.

## Features

- 📊 **Automatic Log Capture** - Captures logs from Netcore Smartech SDK
- 💾 **Local Storage** - Stores logs in Room database
- 🚀 **Background Upload** - Uploads logs via WorkManager
- 🔄 **Auto Retry** - Exponential backoff retry mechanism
- 🎯 **Lifecycle Aware** - Auto-flushes when app goes to background
- 📦 **Obfuscated** - ProGuard/R8 obfuscation for release builds
- ⚙️ **Configurable Sampling** - Control log capture percentage

## Installation

Add the dependency to your `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.android.netcoresdkcapturer:netcore-sdk-capturer:1.0.0")
}
```

## Usage

### Basic Setup

Initialize in your `Application` class:

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize the SDK
        SdkLogCaptor.Companion.getInstance(this).initialize(
            "https://your-webhook.com/logs",  // Webhook URL
            1.0f                                 // 100% sampling rate
        );
    }
}
```

### With Callback (Optional)

```java
SdkLogCaptor.Companion.getInstance(this).initialize(
    "https://your-webhook.com/logs",
    1.0f,
    new LogPayloadCallback() {
        @Override
        public void onPayloadCaptured(JSONObject payload) {
            Log.d("MyApp", "Captured: " + payload.toString());
        }
    }
);
```

### Manual Flush

```java
// Manually flush logs (optional, auto-flushes every 15 min and on background)
SdkLogCaptor.Companion.getInstance(this).flush();
```

## Configuration

### Sampling Rate

Control what percentage of logs to capture:

```java
// Capture 50% of logs
SdkLogCaptor.Companion.getInstance(this).initialize(webhookUrl, 0.5f);

// Capture all logs (default)
SdkLogCaptor.Companion.getInstance(this).initialize(webhookUrl, 1.0f);
```

## How It Works

1. **Capture** - Reads logcat for `SMTEventRecorder` logs
2. **Store** - Saves to local Room database
3. **Upload** - Periodic uploads every 15 minutes via WorkManager
4. **Retry** - Failed uploads retry with exponential backoff
5. **Cleanup** - Deletes uploaded logs older than 7 days

## Requirements

- Android API 24+
- AndroidX dependencies
- Kotlin 1.8+

## ProGuard

ProGuard rules are included automatically. The public API is preserved while internal implementation is obfuscated.

## License

Apache License 2.0

## Author

Allen Thomson
