package com.android.netcoresdkcapturer.capture

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class LogCaptureThread(
    private val samplingRate: Float,
    private val onLogCaptured: (JSONObject) -> Unit
) : Thread() {

    @Volatile
    var isCapturing = false
        private set

    init {
        priority = MIN_PRIORITY // Low priority to not impact app performance
    }

    override fun run() {
        isCapturing = true
        try {
            // Clear logcat buffer first (optional, for cleaner capture)
            Runtime.getRuntime().exec("logcat -c")
            sleep(100)

            // Capture logs from SMTEventRecorder
            val process = Runtime.getRuntime().exec("logcat SMTEventRecorder:I *:S")
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (isCapturing) {
                line = reader.readLine() ?: break

                if (line.contains("Event Payload:")) {
                    processLogLine(line)
                }
            }
        } catch (e: Exception) {
            Log.e("LogCaptureThread", "Error reading logcat", e)
        }
    }

    private fun processLogLine(logLine: String) {
        try {
            // Apply sampling
            if (samplingRate < 1.0f && Math.random() > samplingRate) {
                return // Skip this event
            }

            val jsonStartIndex = logLine.indexOf("Event Payload:") + "Event Payload:".length
            val jsonString = logLine.substring(jsonStartIndex).trim()
            val json = JSONObject(jsonString)

            onLogCaptured(json)

        } catch (e: Exception) {
            Log.e("LogCaptureThread", "Failed to parse payload: $logLine", e)
        }
    }

    fun stopCapturing() {
        isCapturing = false
        interrupt()
    }
}
