package com.pepperonas.netmonitor.util

import android.net.TrafficStats

class TrafficMonitor {
    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()
    private var lastNanos = System.nanoTime()

    data class Speed(val rxBytesPerSec: Long, val txBytesPerSec: Long)

    fun sample(): Speed {
        val now = System.nanoTime()
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()

        val elapsedNanos = (now - lastNanos).coerceAtLeast(1)
        val rxDelta = (currentRx - lastRxBytes).coerceAtLeast(0)
        val txDelta = (currentTx - lastTxBytes).coerceAtLeast(0)

        val rxPerSec = rxDelta * 1_000_000_000L / elapsedNanos
        val txPerSec = txDelta * 1_000_000_000L / elapsedNanos

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastNanos = now

        return Speed(rxPerSec, txPerSec)
    }

    data class FormattedSpeed(val value: String, val unit: String)

    companion object {
        fun formatSpeedParts(bytesPerSec: Long): FormattedSpeed = when {
            bytesPerSec < 1_024 -> FormattedSpeed("$bytesPerSec", "B/s")
            bytesPerSec < 1_024 * 1_024 -> FormattedSpeed("${bytesPerSec / 1_024}", "KB/s")
            else -> FormattedSpeed(
                String.format("%.1f", bytesPerSec / (1_024.0 * 1_024.0)), "MB/s"
            )
        }

        fun formatSpeed(bytesPerSec: Long): String {
            val parts = formatSpeedParts(bytesPerSec)
            return "${parts.value} ${parts.unit}"
        }

        fun formatBytes(bytes: Long): String = when {
            bytes < 1_024 -> "$bytes B"
            bytes < 1_024 * 1_024 -> "${bytes / 1_024} KB"
            bytes < 1_024L * 1_024 * 1_024 -> String.format("%.1f MB", bytes / (1_024.0 * 1_024.0))
            else -> String.format("%.2f GB", bytes / (1_024.0 * 1_024.0 * 1_024.0))
        }
    }
}
