package com.pepperonas.netmonitor.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_traffic_summaries")
data class DailyTrafficSummary(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val peakDownload: Long,
    val peakUpload: Long,
    val sampleCount: Int
)
