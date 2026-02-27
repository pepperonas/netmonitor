package com.pepperonas.netmonitor.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Renders dynamic notification small icons showing current network speed.
 * Uses ALPHA_8 bitmaps (alpha-mask only) as required by Android notification small icons.
 * Paint objects are reused across calls for GC efficiency at 2Hz update rate.
 */
object SpeedIconRenderer {

    private const val ICON_SIZE = 96

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = 20f
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = 28f
    }

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = 18f
    }

    private val combinedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = 22f
    }

    fun createDownloadBitmap(value: String, unit: String): Bitmap =
        createDirectionalBitmap("\u2193", value, unit)

    fun createUploadBitmap(value: String, unit: String): Bitmap =
        createDirectionalBitmap("\u2191", value, unit)

    fun createCombinedBitmap(downValue: String, upValue: String, unit: String): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        val quarterX = (ICON_SIZE / 4).toFloat()
        val threeQuarterX = (ICON_SIZE * 3 / 4).toFloat()
        val centerX = (ICON_SIZE / 2).toFloat()

        canvas.drawText("\u2193$downValue", quarterX, 36f, combinedPaint)
        canvas.drawText("\u2191$upValue", threeQuarterX, 36f, combinedPaint)
        canvas.drawText(unit, centerX, 70f, unitPaint)

        return bitmap
    }

    private fun createDirectionalBitmap(arrow: String, value: String, unit: String): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        val centerX = (ICON_SIZE / 2).toFloat()

        canvas.drawText(arrow, centerX, 20f, arrowPaint)
        canvas.drawText(value, centerX, 54f, valuePaint)
        canvas.drawText(unit, centerX, 80f, unitPaint)

        return bitmap
    }
}
