package com.android.netcoresdkcapturer

import org.json.JSONObject

/**
 * Java-friendly callback interface for log payload capture events.
 */
fun interface LogPayloadCallback {
    fun onPayloadCaptured(payload: JSONObject)
}
