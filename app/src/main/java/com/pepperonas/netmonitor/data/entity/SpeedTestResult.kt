package com.pepperonas.netmonitor.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_test_results")
data class SpeedTestResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val downloadBytesPerSec: Long,
    val uploadBytesPerSec: Long,
    val latencyMs: Long,
    val connectionType: String,
    val serverUrl: String
)
