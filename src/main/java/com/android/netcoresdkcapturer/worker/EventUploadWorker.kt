package com.android.netcoresdkcapturer.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.netcoresdkcapturer.database.CapturedEvent
import com.android.netcoresdkcapturer.database.EventDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            val success = uploadEvents(events, webhookUrl)

            if (success) {
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

                Log.e("EventUpload", "Upload failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("EventUpload", "Upload error: ${e.message}", e)
            Result.retry()
        }
    }

    private fun uploadEvents(events: List<CapturedEvent>, endpoint: String): Boolean {
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

            responseCode == 200
        } catch (e: Exception) {
            Log.e("EventUpload", "Network error: ${e.message}", e)
            false
        }
    }
}
