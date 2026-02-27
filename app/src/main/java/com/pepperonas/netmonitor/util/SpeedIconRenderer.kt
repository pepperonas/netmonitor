package com.pepperonas.netmonitor.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Renders dynamic notification small icons showing current network speed.
 *
 * Maximizes readability in ~24dp status bar icons by:
 * - Auto-scaling value text to fill the full bitmap width
 * - Using condensed bold typeface (narrower glyphs = bigger textSize)
 * - Heavy FILL_AND_STROKE for maximum glyph thickness
 * - Single-letter unit (M/K/B) to save vertical space for the number
 */
object SpeedIconRenderer {

    private const val SIZE = 96
    private const val TARGET_WIDTH = 88f // 4px padding each side

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 3.5f
    }

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2f
        textSize = 32f
    }

    fun createDownloadBitmap(value: String, unit: String): Bitmap =
        createBitmap(value, unitLetter(unit))

    fun createUploadBitmap(value: String, unit: String): Bitmap =
        createBitmap(value, unitLetter(unit))

    /**
     * Layout (96x96):
     *   4.2      Y≈56  — value, auto-sized up to 76px, condensed bold + 3.5px stroke
     *    M       Y=90  — single letter unit, 32px bold + 2px stroke
     */
    private fun createBitmap(value: String, unit: String): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        val cx = SIZE / 2f

        valuePaint.textSize = autoFitSize(valuePaint, value, TARGET_WIDTH, 76f)
        canvas.drawText(value, cx, 56f, valuePaint)
        canvas.drawText(unit, cx, 90f, unitPaint)

        return bitmap
    }

    /** Shrinks textSize until text fits within maxWidth. */
    private fun autoFitSize(paint: Paint, text: String, maxWidth: Float, startSize: Float): Float {
        var size = startSize
        paint.textSize = size
        while (paint.measureText(text) > maxWidth && size > 24f) {
            size -= 2f
            paint.textSize = size
        }
        return size
    }

    /** "MB/s" → "M", "KB/s" → "K", "B/s" → "B" */
    private fun unitLetter(unit: String): String = when {
        unit.startsWith("MB") -> "M"
        unit.startsWith("KB") -> "K"
        else -> "B"
    }
}
