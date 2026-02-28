package com.pepperonas.netmonitor.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficMonitorTest {

    @Test
    fun formatSpeedParts_zeroBytes_returnsZeroKB() {
        val result = TrafficMonitor.formatSpeedParts(0)
        assertEquals("0", result.value)
        assertEquals("KB/s", result.unit)
    }

    @Test
    fun formatSpeedParts_smallValue_returnsKB() {
        val result = TrafficMonitor.formatSpeedParts(512)
        assertEquals("1", result.value) // (512 + 512) / 1024 = 1
        assertEquals("KB/s", result.unit)
    }

    @Test
    fun formatSpeedParts_exactOneKB_returnsOneKB() {
        val result = TrafficMonitor.formatSpeedParts(1024)
        assertEquals("1", result.value)
        assertEquals("KB/s", result.unit)
    }

    @Test
    fun formatSpeedParts_largeKBValue_returnsCorrectKB() {
        val result = TrafficMonitor.formatSpeedParts(512 * 1024) // 512 KB/s
        assertEquals("512", result.value)
        assertEquals("KB/s", result.unit)
    }

    @Test
    fun formatSpeedParts_oneMB_returnsMBWithComma() {
        val result = TrafficMonitor.formatSpeedParts(1024 * 1024) // 1 MB/s
        assertEquals("1,0", result.value)
        assertEquals("MB/s", result.unit)
    }

    @Test
    fun formatSpeedParts_fractionalMB_returnsMBWithComma() {
        val result = TrafficMonitor.formatSpeedParts((2.5 * 1024 * 1024).toLong())
        assertEquals("2,5", result.value)
        assertEquals("MB/s", result.unit)
    }

    @Test
    fun formatSpeed_combinesValueAndUnit() {
        val result = TrafficMonitor.formatSpeed(1024)
        assertEquals("1 KB/s", result)
    }

    @Test
    fun formatBytes_zero() {
        assertEquals("0 B", TrafficMonitor.formatBytes(0))
    }

    @Test
    fun formatBytes_bytes() {
        assertEquals("500 B", TrafficMonitor.formatBytes(500))
    }

    @Test
    fun formatBytes_kilobytes() {
        assertEquals("10 KB", TrafficMonitor.formatBytes(10 * 1024))
    }

    @Test
    fun formatBytes_megabytes() {
        val result = TrafficMonitor.formatBytes(5 * 1024 * 1024)
        // Locale-abhängig: "5.0 MB" oder "5,0 MB"
        assert(result == "5.0 MB" || result == "5,0 MB") { "Expected 5.0/5,0 MB but got: $result" }
    }

    @Test
    fun formatBytes_gigabytes() {
        val result = TrafficMonitor.formatBytes(2L * 1024 * 1024 * 1024)
        assert(result == "2.00 GB" || result == "2,00 GB") { "Expected 2.00/2,00 GB but got: $result" }
    }
}
