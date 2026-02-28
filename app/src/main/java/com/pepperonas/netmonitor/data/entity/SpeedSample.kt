package com.pepperonas.netmonitor.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_samples")
data class SpeedSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
    val connectionType: String
)
