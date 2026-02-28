package com.pepperonas.netmonitor.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pepperonas.netmonitor.data.entity.DailyTrafficSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTrafficDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: DailyTrafficSummary)

    @Query("SELECT * FROM daily_traffic_summaries ORDER BY date DESC LIMIT :limit")
    fun getRecentDays(limit: Int): Flow<List<DailyTrafficSummary>>

    @Query("SELECT * FROM daily_traffic_summaries WHERE date >= :since ORDER BY date ASC")
    fun getDaysSince(since: String): Flow<List<DailyTrafficSummary>>

    @Query("SELECT * FROM daily_traffic_summaries WHERE date = :date")
    suspend fun getDay(date: String): DailyTrafficSummary?

    @Query("SELECT COALESCE(SUM(totalRxBytes + totalTxBytes), 0) FROM daily_traffic_summaries WHERE date >= :since")
    fun getMonthlyUsage(since: String): Flow<Long>

    @Query("DELETE FROM daily_traffic_summaries WHERE date < :before")
    suspend fun deleteOlderThan(before: String)
}
