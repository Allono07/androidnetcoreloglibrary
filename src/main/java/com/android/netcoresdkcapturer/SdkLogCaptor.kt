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

    /**
     * Initialize the SDK with configuration.
     * 
     * @param webhookEndpoint The URL where logs will be sent
     * @param samplingRate Percentage of logs to capture (0.0 to 1.0). Default is 1.0 (100%)
     * @param callback Optional callback invoked when a log is captured
     */
    @JvmOverloads
    fun initialize(
        webhookEndpoint: String,
        samplingRate: Float = 1.0f,
        callback: LogPayloadCallback? = null
    ) {
        this.webhookUrl = webhookEndpoint
        this.onPayloadCaptured = callback?.let { cb ->
            { payload -> cb.onPayloadCaptured(payload) }
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
        if (captureThread?.isCapturing == true) return

        captureThread = LogCaptureThread(samplingRate) { json ->
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
     */
    fun shutdown() {
        captureThread?.stopCapturing()
        captureThread = null
        scope.cancel()
        Log.d("SdkLogCaptor", "SDK shut down")
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
        fun getInstance(context: Context): SdkLogCaptor {
            return INSTANCE ?: synchronized(this) {
                val instance = SdkLogCaptor(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
