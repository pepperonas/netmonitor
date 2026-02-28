package com.pepperonas.netmonitor.data

import com.pepperonas.netmonitor.data.dao.DailyTrafficDao
import com.pepperonas.netmonitor.data.dao.SpeedSampleDao
import com.pepperonas.netmonitor.data.entity.DailyTrafficSummary
import com.pepperonas.netmonitor.data.entity.SpeedSample
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrafficRepository(
    private val speedSampleDao: SpeedSampleDao,
    private val dailyTrafficDao: DailyTrafficDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun recordSample(rxBytesPerSec: Long, txBytesPerSec: Long, connectionType: String) {
        val now = System.currentTimeMillis()
        speedSampleDao.insert(
            SpeedSample(
                timestamp = now,
                rxBytesPerSec = rxBytesPerSec,
                txBytesPerSec = txBytesPerSec,
                connectionType = connectionType
            )
        )
        updateDailySummary(now, rxBytesPerSec, txBytesPerSec)
    }

    private suspend fun updateDailySummary(timestamp: Long, rxPerSec: Long, txPerSec: Long) {
        val date = dateFormat.format(Date(timestamp))
        val existing = dailyTrafficDao.getDay(date)
        if (existing != null) {
            dailyTrafficDao.upsert(
                existing.copy(
                    totalRxBytes = existing.totalRxBytes + rxPerSec,
                    totalTxBytes = existing.totalTxBytes + txPerSec,
                    peakDownload = maxOf(existing.peakDownload, rxPerSec),
                    peakUpload = maxOf(existing.peakUpload, txPerSec),
                    sampleCount = existing.sampleCount + 1
                )
            )
        } else {
            dailyTrafficDao.upsert(
                DailyTrafficSummary(
                    date = date,
                    totalRxBytes = rxPerSec,
                    totalTxBytes = txPerSec,
                    peakDownload = rxPerSec,
                    peakUpload = txPerSec,
                    sampleCount = 1
                )
            )
        }
    }

    fun getRecentSamples(durationMs: Long): Flow<List<SpeedSample>> {
        val since = System.currentTimeMillis() - durationMs
        return speedSampleDao.getSamplesSince(since)
    }

    fun getRecentDays(limit: Int): Flow<List<DailyTrafficSummary>> {
        return dailyTrafficDao.getRecentDays(limit)
    }

    fun getDaysSince(date: String): Flow<List<DailyTrafficSummary>> {
        return dailyTrafficDao.getDaysSince(date)
    }

    suspend fun getHourlyAverages(since: Long): List<SpeedSample> {
        return speedSampleDao.getHourlyAverages(since)
    }

    suspend fun getHourlyPeaks(since: Long): List<SpeedSample> {
        return speedSampleDao.getHourlyPeaks(since)
    }

    suspend fun getTodaySummary(): DailyTrafficSummary? {
        val today = dateFormat.format(Date())
        return dailyTrafficDao.getDay(today)
    }

    fun getMonthlyUsage(sinceDate: String): Flow<Long> {
        return dailyTrafficDao.getMonthlyUsage(sinceDate)
    }

    suspend fun cleanup() {
        // Detail-Samples älter als 7 Tage löschen
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        speedSampleDao.deleteOlderThan(sevenDaysAgo)

        // Tages-Summaries älter als 365 Tage löschen
        val oneYearAgo = dateFormat.format(Date(System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000))
        dailyTrafficDao.deleteOlderThan(oneYearAgo)
    }
}
