package com.android.netcoresdkcapturer.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_events")
data class CapturedEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val uploaded: Boolean = false
)
