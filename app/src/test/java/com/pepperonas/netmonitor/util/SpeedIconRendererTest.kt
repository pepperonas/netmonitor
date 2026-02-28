package com.pepperonas.netmonitor.util

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * SpeedIconRenderer tests.
 * Note: createBitmap tests require Android instrumentation (Bitmap needs native libs).
 * These tests verify the formatting logic that feeds into the renderer.
 */
@RunWith(JUnit4::class)
class SpeedIconRendererTest {

    @Test
    fun speedFormat_zeroKB_showsZero() {
        val parts = TrafficMonitor.formatSpeedParts(0)
        assertEquals("0", parts.value)
    }

    @Test
    fun speedFormat_belowOneMB_showsKBNoDecimal() {
        val parts = TrafficMonitor.formatSpeedParts(100 * 1024)
        assertEquals("100", parts.value)
        // Kein Komma = KB/s (Icon-Konvention)
        assert(!parts.value.contains(","))
    }

    @Test
    fun speedFormat_aboveOneMB_showsDecimalWithComma() {
        val parts = TrafficMonitor.formatSpeedParts(1500 * 1024)
        // 1500 KB = 1.5 MB -> "1,5"
        assert(parts.value.contains(","))
        assertEquals("MB/s", parts.unit)
    }

    @Test
    fun speedFormat_veryHighSpeed_showsCorrectMB() {
        val parts = TrafficMonitor.formatSpeedParts(100L * 1024 * 1024) // 100 MB/s
        assertEquals("100,0", parts.value)
        assertEquals("MB/s", parts.unit)
    }
}
