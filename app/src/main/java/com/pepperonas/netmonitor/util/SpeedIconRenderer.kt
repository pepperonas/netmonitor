package com.pepperonas.netmonitor.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Renders dynamic notification small icons showing current network speed.
 *
 * Single-line layout: just the number, vertically centered, maximum size.
 * No unit text — the format itself encodes the unit:
 *   "42"  (integer, no comma) = KB/s
 *   "4,2" (with comma)        = MB/s
 */
object SpeedIconRenderer {

    private const val SIZE = 96
    private const val TARGET_WIDTH = 90f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 3f
    }

    fun createDownloadBitmap(value: String, unit: String): Bitmap = createBitmap(value)

    fun createUploadBitmap(value: String, unit: String): Bitmap = createBitmap(value)

    private fun createBitmap(value: String): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        val cx = SIZE / 2f

        paint.textSize = autoFitSize(paint, value, TARGET_WIDTH, 88f)
        val metrics = paint.fontMetrics
        val baseline = (SIZE - metrics.ascent - metrics.descent) / 2f

        canvas.drawText(value, cx, baseline, paint)

        return bitmap
    }

    private fun autoFitSize(paint: Paint, text: String, maxWidth: Float, startSize: Float): Float {
        var size = startSize
        paint.textSize = size
        while (paint.measureText(text) > maxWidth && size > 28f) {
            size -= 2f
            paint.textSize = size
        }
        return size
    }
}
