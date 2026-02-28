package com.pepperonas.netmonitor.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pepperonas.netmonitor.data.entity.SpeedSample
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedSampleDao {

    @Insert
    suspend fun insert(sample: SpeedSample)

    @Query("SELECT * FROM speed_samples WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getSamplesSince(since: Long): Flow<List<SpeedSample>>

    @Query("SELECT * FROM speed_samples WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getSamplesSinceOnce(since: Long): List<SpeedSample>

    @Query("SELECT MAX(rxBytesPerSec) FROM speed_samples WHERE timestamp >= :since")
    suspend fun getPeakDownloadSince(since: Long): Long?

    @Query("SELECT MAX(txBytesPerSec) FROM speed_samples WHERE timestamp >= :since")
    suspend fun getPeakUploadSince(since: Long): Long?

    @Query("SELECT COUNT(*) FROM speed_samples")
    suspend fun getCount(): Int

    @Query("""
        SELECT
            (timestamp / 3600000) * 3600000 AS timestamp,
            0 AS id,
            AVG(rxBytesPerSec) AS rxBytesPerSec,
            AVG(txBytesPerSec) AS txBytesPerSec,
            '' AS connectionType
        FROM speed_samples
        WHERE timestamp >= :since
        GROUP BY timestamp / 3600000
        ORDER BY timestamp ASC
    """)
    suspend fun getHourlyAverages(since: Long): List<SpeedSample>

    @Query("""
        SELECT
            (timestamp / 3600000) * 3600000 AS timestamp,
            0 AS id,
            MAX(rxBytesPerSec) AS rxBytesPerSec,
            MAX(txBytesPerSec) AS txBytesPerSec,
            '' AS connectionType
        FROM speed_samples
        WHERE timestamp >= :since
        GROUP BY timestamp / 3600000
        ORDER BY timestamp ASC
    """)
    suspend fun getHourlyPeaks(since: Long): List<SpeedSample>

    @Query("DELETE FROM speed_samples WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
