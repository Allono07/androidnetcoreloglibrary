package com.android.netcoresdkcapturer.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.netcoresdkcapturer.database.CapturedEvent
import com.android.netcoresdkcapturer.database.EventDatabase
import com.android.netcoresdkcapturer.SdkLogCaptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.work.WorkManager
import android.content.SharedPreferences
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class EventUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = EventDatabase.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val webhookUrl = inputData.getString("webhook_url")
        if (webhookUrl == null) {
            Log.e("EventUpload", "Webhook URL not provided")
            return@withContext Result.failure()
        }

        try {
            val events = database.eventDao().getUnuploadedEvents(50)
            if (events.isEmpty()) {
                Log.d("EventUpload", "No events to upload")
                return@withContext Result.success()
            }

            Log.d("EventUpload", "Uploading ${events.size} events")

            // Upload (plain text, no gzip) so backends that expect text can parse directly
            val responseCode = uploadEvents(events, webhookUrl)

            if (responseCode == 444) {
                Log.w("EventUpload", "Server returned 444 — app no longer available on server; disabling SDK permanently")

                // Delete all pending events from the database (don't save anything)
                val deletedCount = database.eventDao().deleteAllEvents()
                Log.d("EventUpload", "Deleted $deletedCount pending events from database")

                // Set a persistent flag to prevent future uploads for this webhook
                val prefs = applicationContext.getSharedPreferences("com.android.netcoresdkcapturer.prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("uploads_disabled_${webhookUrl.hashCode()}", true).apply()

                // Stop capturing and cancel periodic work via SdkLogCaptor
                try {
                    SdkLogCaptor.getInstance(applicationContext).disablePermanently(webhookUrl)
                } catch (e: Exception) {
                    Log.w("EventUpload", "Failed to disable SdkLogCaptor: ${e.message}")
                }

                // Cancel periodic upload work (best-effort)
                try {
                    WorkManager.getInstance(applicationContext).cancelUniqueWork("event_upload_periodic")
                } catch (e: Exception) {
                    Log.w("EventUpload", "Failed to cancel periodic work: ${e.message}")
                }

                Log.d("EventUpload", "SDK disabled permanently; all events deleted and uploads stopped")
                return@withContext Result.success()
            }

            if (responseCode in 200..299) {
                database.eventDao().markAsUploaded(events.map { it.id })

                // Cleanup old uploaded events (older than 7 days)
                val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                database.eventDao().deleteOldEvents(cutoffTime)

                Log.d("EventUpload", "Successfully uploaded ${events.size} events")
                Result.success()
            } else {
                // Increment retry count
                database.eventDao().incrementRetryCount(events.map { it.id })

                // Delete events that have failed too many times
                database.eventDao().deleteFailedEvents(maxRetries = 5)

                Log.e("EventUpload", "Upload failed, will retry (response code/status false)")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("EventUpload", "Upload error: ${e.message}", e)
            Result.retry()
        }
    }

    private fun uploadEvents(events: List<CapturedEvent>, endpoint: String): Int {
        return try {
            val payloadText = buildString {
                for (event in events) {
                    append("Event Payload: ").append(event.payload).append("\n\n")
                }
            }

            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            // Send plain text so servers that expect the "Event Payload: {...}" format can parse it directly
            conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.doOutput = true

            // Write uncompressed payload
            conn.outputStream.use { outputStream ->
                outputStream.write(payloadText.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }

            val responseCode = conn.responseCode
            conn.disconnect()

            responseCode
        } catch (e: Exception) {
            Log.e("EventUpload", "Network error: ${e.message}", e)
            -1
        }
    }
}
