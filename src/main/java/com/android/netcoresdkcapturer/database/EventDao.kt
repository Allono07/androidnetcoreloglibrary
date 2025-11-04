package com.android.netcoresdkcapturer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EventDao {
    @Insert
    suspend fun insertEvent(event: CapturedEvent)

    @Query("SELECT * FROM captured_events WHERE uploaded = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnuploadedEvents(limit: Int): List<CapturedEvent>

    @Query("UPDATE captured_events SET uploaded = 1 WHERE id IN (:ids)")
    suspend fun markAsUploaded(ids: List<Long>)

    @Query("DELETE FROM captured_events WHERE timestamp < :cutoffTime")
    suspend fun deleteOldEvents(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM captured_events WHERE uploaded = 0")
    suspend fun getUnuploadedCount(): Int

    @Query("UPDATE captured_events SET retryCount = retryCount + 1 WHERE id IN (:ids)")
    suspend fun incrementRetryCount(ids: List<Long>)

    @Query("DELETE FROM captured_events WHERE retryCount > :maxRetries")
    suspend fun deleteFailedEvents(maxRetries: Int)
}
