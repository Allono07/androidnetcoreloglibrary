package com.android.netcoresdkcapturer.lifecycle

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver(
    private val onAppBackgrounded: () -> Unit,
    private val onAppDestroyed: () -> Unit
) : LifecycleEventObserver {

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                Log.d("AppLifecycle", "App backgrounded")
                onAppBackgrounded()
            }
            Lifecycle.Event.ON_DESTROY -> {
                Log.d("AppLifecycle", "App destroyed")
                onAppDestroyed()
            }
            else -> {}
        }
    }
}
