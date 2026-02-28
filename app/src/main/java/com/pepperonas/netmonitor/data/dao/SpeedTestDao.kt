package com.pepperonas.netmonitor.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pepperonas.netmonitor.data.entity.SpeedTestResult
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestDao {

    @Insert
    suspend fun insert(result: SpeedTestResult)

    @Query("SELECT * FROM speed_test_results ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentResults(limit: Int = 20): Flow<List<SpeedTestResult>>

    @Query("SELECT * FROM speed_test_results ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastResult(): SpeedTestResult?

    @Query("DELETE FROM speed_test_results WHERE id NOT IN (SELECT id FROM speed_test_results ORDER BY timestamp DESC LIMIT 50)")
    suspend fun cleanupOldResults()
}
