package com.android.netcoresdkcapturer.upload

import android.content.Context
import androidx.work.*
import com.android.netcoresdkcapturer.worker.EventUploadWorker
import java.util.concurrent.TimeUnit

class UploadScheduler(private val context: Context) {

    fun schedulePeriodicUpload(webhookUrl: String) {
        // Respect a persistent flag set by EventUploadWorker when the server indicates the app is no longer available
        val prefs = context.getSharedPreferences("com.android.netcoresdkcapturer.prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("uploads_disabled_${webhookUrl.hashCode()}", false)) {
            android.util.Log.w("UploadScheduler", "Uploads disabled for webhook; skipping periodic scheduling")
            return
        }
        val uploadWork = PeriodicWorkRequestBuilder<EventUploadWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                Data.Builder()
                    .putString("webhook_url", webhookUrl)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "event_upload_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            uploadWork
        )
    }

    fun scheduleImmediateUpload(webhookUrl: String) {
        // Respect persistent disable flag
        val prefs = context.getSharedPreferences("com.android.netcoresdkcapturer.prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("uploads_disabled_${webhookUrl.hashCode()}", false)) {
            android.util.Log.w("UploadScheduler", "Uploads disabled for webhook; skipping immediate upload")
            return
        }
        val uploadWork = OneTimeWorkRequestBuilder<EventUploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                Data.Builder()
                    .putString("webhook_url", webhookUrl)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueue(uploadWork)
    }
}
