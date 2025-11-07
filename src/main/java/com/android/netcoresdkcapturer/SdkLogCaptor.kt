package com.android.netcoresdkcapturer

import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.netcoresdkcapturer.capture.LogCaptureThread
import com.android.netcoresdkcapturer.database.CapturedEvent
import com.android.netcoresdkcapturer.database.EventDatabase
import com.android.netcoresdkcapturer.lifecycle.AppLifecycleObserver
import com.android.netcoresdkcapturer.upload.UploadScheduler
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * Main SDK class for capturing and uploading logs.
 * 
 * Usage:
 * ```
 * SdkLogCaptor.getInstance(context).initialize(
 *     webhookEndpoint = "https://your-webhook.com/logs",
 *     samplingRate = 1.0f,
 *     onPayloadCaptured = { payload ->
 *         Log.d("MyApp", "Captured: $payload")
 *     }
 * )
 * ```
 */
class SdkLogCaptor private constructor(private val context: Context) {

    private val database = EventDatabase.getInstance(context)
    private val uploadScheduler = UploadScheduler(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var captureThread: LogCaptureThread? = null
    private var webhookUrl: String? = null
    private var onPayloadCaptured: ((JSONObject) -> Unit)? = null

    // Rate limiting
    private var lastFlushTime = 0L
    private val minFlushIntervalMs = 30000L // 30 seconds

    // SDK enabled/disabled state
    @Volatile
    private var isEnabled = true
    private val prefs = context.getSharedPreferences("com.android.netcoresdkcapturer.prefs", Context.MODE_PRIVATE)

    /**
     * Initialize the SDK with configuration.
     * 
     * @param webhookEndpoint The URL where logs will be sent
     * @param samplingRate Percentage of logs to capture (0.0 to 1.0). Default is 1.0 (100%)
     * @param enabled Whether the SDK should be enabled. If false, no logs will be captured or sent. Default is true.
     * @param callback Optional callback invoked when a log is captured
     */
    @JvmOverloads
    fun initialize(
        webhookEndpoint: String,
        samplingRate: Float = 1.0f,
        enabled: Boolean = true,
        callback: LogPayloadCallback? = null
    ) {
        this.isEnabled = enabled
        this.webhookUrl = webhookEndpoint
        this.onPayloadCaptured = callback?.let { cb ->
            { payload -> cb.onPayloadCaptured(payload) }
        }

        if (!enabled) {
            Log.d("SdkLogCaptor", "SDK disabled via initialization flag; no logs will be captured or sent")
            return
        }

        // Check if uploads are permanently disabled for this webhook (e.g., server returned 444)
        if (prefs.getBoolean("uploads_disabled_${webhookEndpoint.hashCode()}", false)) {
            Log.w("SdkLogCaptor", "Uploads permanently disabled for this webhook (server returned 444); SDK will not capture or send logs")
            this.isEnabled = false
            return
        }

        val validatedSamplingRate = samplingRate.coerceIn(0f, 1f)

        startCapturing(validatedSamplingRate)
        setupLifecycleObserver()
        uploadScheduler.schedulePeriodicUpload(webhookEndpoint)

        Log.d("SdkLogCaptor", "Initialized with sampling rate: ${(validatedSamplingRate * 100).toInt()}%")
    }

    private fun setupLifecycleObserver() {
        try {
            val observer = AppLifecycleObserver(
                onAppBackgrounded = {
                    Log.d("SdkLogCaptor", "App backgrounded, flushing logs")
                    flush()
                },
                onAppDestroyed = {
                    shutdown()
                }
            )
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        } catch (e: Exception) {
            Log.e("SdkLogCaptor", "Failed to setup lifecycle observer", e)
        }
    }

    private fun startCapturing(samplingRate: Float) {
        if (!isEnabled) return
        if (captureThread?.isCapturing == true) return

        captureThread = LogCaptureThread(samplingRate) { json ->
            if (!isEnabled) return@LogCaptureThread
            
            // Store in database (non-blocking)
            scope.launch(Dispatchers.IO) {
                database.eventDao().insertEvent(
                    CapturedEvent(payload = json.toString())
                )
            }

            // Callback (if provided)
            onPayloadCaptured?.invoke(json)
        }.apply {
            start()
        }
    }

    /**
     * Flush logs to webhook.
     * Safe to call from UI thread - returns immediately.
     */
    fun flush() {
        if (!isEnabled) {
            Log.d("SdkLogCaptor", "SDK disabled, skipping flush")
            return
        }
        
        // Rate limiting
        val now = System.currentTimeMillis()
        if (now - lastFlushTime < minFlushIntervalMs) {
            Log.d("SdkLogCaptor", "Flush rate limited, skipping")
            return
        }
        lastFlushTime = now

        scope.launch(Dispatchers.IO) {
            val count = database.eventDao().getUnuploadedCount()
            if (count == 0) {
                Log.d("SdkLogCaptor", "No events to flush")
                return@launch
            }

            Log.d("SdkLogCaptor", "Scheduling upload of $count events")
            webhookUrl?.let { uploadScheduler.scheduleImmediateUpload(it) }
        }
    }

    /**
     * Shutdown the SDK and release resources.
     * Called internally when the app is destroyed or can be called manually.
     */
    fun shutdown() {
        captureThread?.stopCapturing()
        captureThread = null
        scope.cancel()
        Log.d("SdkLogCaptor", "SDK shut down")
    }

    /**
     * Internal method called by EventUploadWorker when server returns 444.
     * Stops all SDK activity permanently for the given webhook.
     */
    internal fun disablePermanently(webhookUrl: String) {
        Log.w("SdkLogCaptor", "Disabling SDK permanently for webhook (server returned 444)")
        
        // Stop capturing
        isEnabled = false
        captureThread?.stopCapturing()
        captureThread = null
        
        // Clear all pending events from DB
        scope.launch(Dispatchers.IO) {
            try {
                val deleted = database.eventDao().deleteAllEvents()
                Log.d("SdkLogCaptor", "Deleted $deleted pending events from database")
            } catch (e: Exception) {
                Log.e("SdkLogCaptor", "Failed to delete events: ${e.message}", e)
            }
        }
        
        Log.d("SdkLogCaptor", "SDK disabled permanently; log capture and uploads stopped")
    }

    companion object {
        @Volatile
        private var INSTANCE: SdkLogCaptor? = null

        /**
         * Get the singleton instance of SdkLogCaptor.
         * 
         * @param context Application context
         * @return SdkLogCaptor instance
         */
        @JvmStatic
        fun getInstance(context: Context): SdkLogCaptor {
            return INSTANCE ?: synchronized(this) {
                val instance = SdkLogCaptor(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
