package com.pepperonas.netmonitor.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.netmonitor.R
import com.pepperonas.netmonitor.data.entity.SpeedSample
import com.pepperonas.netmonitor.util.TrafficMonitor

@Composable
fun SpeedGraph(
    samples: List<SpeedSample>,
    modifier: Modifier = Modifier,
    windowSeconds: Int = 60
) {
    val dlColor = MaterialTheme.colorScheme.primary
    val ulColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    val animationProgress by animateFloatAsState(
        targetValue = if (samples.isNotEmpty()) 1f else 0f,
        animationSpec = tween(600),
        label = "graphReveal"
    )

    // Zeitfenster: jetzt - windowSeconds
    val now = remember(samples.lastOrNull()?.timestamp) {
        samples.lastOrNull()?.timestamp ?: System.currentTimeMillis()
    }
    val windowStart = now - windowSeconds * 1000L

    // Nur Samples im Zeitfenster
    val windowSamples = remember(samples, windowStart) {
        samples.filter { it.timestamp >= windowStart }
    }

    // Max-Wert für Y-Achse (mindestens 10 KB/s, aufgerundet auf schöne Stufen)
    val maxSpeed = remember(windowSamples) {
        val rawMax = windowSamples.maxOfOrNull {
            maxOf(it.rxBytesPerSec, it.txBytesPerSec)
        } ?: 0L
        niceMaxValue(maxOf(rawMax, 10_240L))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.speed_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                LegendDot(color = dlColor, label = stringResource(R.string.graph_dl))
                Spacer(Modifier.width(12.dp))
                LegendDot(color = ulColor, label = stringResource(R.string.graph_ul))
            }

            Spacer(Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val leftPadding = 48f
                val bottomPadding = 24f
                val graphWidth = size.width - leftPadding
                val graphHeight = size.height - bottomPadding

                // Horizontale Gitterlinien
                drawGridLines(
                    maxSpeed = maxSpeed,
                    leftPadding = leftPadding,
                    graphWidth = graphWidth,
                    graphHeight = graphHeight,
                    gridColor = gridColor,
                    labelColor = labelColor,
                    textMeasurer = textMeasurer
                )

                // Zeitachse
                drawTimeAxis(
                    windowSeconds = windowSeconds,
                    leftPadding = leftPadding,
                    graphWidth = graphWidth,
                    graphHeight = graphHeight,
                    labelColor = labelColor,
                    textMeasurer = textMeasurer
                )

                if (windowSamples.size >= 2) {
                    // Download-Linie + Gradient
                    drawSpeedLine(
                        samples = windowSamples,
                        getValue = { it.rxBytesPerSec },
                        maxSpeed = maxSpeed,
                        windowStart = windowStart,
                        windowDuration = windowSeconds * 1000L,
                        leftPadding = leftPadding,
                        graphWidth = graphWidth,
                        graphHeight = graphHeight,
                        lineColor = dlColor,
                        progress = animationProgress
                    )

                    // Upload-Linie + Gradient
                    drawSpeedLine(
                        samples = windowSamples,
                        getValue = { it.txBytesPerSec },
                        maxSpeed = maxSpeed,
                        windowStart = windowStart,
                        windowDuration = windowSeconds * 1000L,
                        leftPadding = leftPadding,
                        graphWidth = graphWidth,
                        graphHeight = graphHeight,
                        lineColor = ulColor,
                        progress = animationProgress
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DrawScope.drawGridLines(
    maxSpeed: Long,
    leftPadding: Float,
    graphWidth: Float,
    graphHeight: Float,
    gridColor: Color,
    labelColor: Color,
    textMeasurer: TextMeasurer
) {
    val steps = 4
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))

    for (i in 0..steps) {
        val y = graphHeight * (1f - i.toFloat() / steps)
        val speedValue = maxSpeed * i / steps

        drawLine(
            color = gridColor,
            start = Offset(leftPadding, y),
            end = Offset(leftPadding + graphWidth, y),
            pathEffect = if (i > 0) dashEffect else null,
            strokeWidth = 1f
        )

        val label = TrafficMonitor.formatSpeed(speedValue)
        val textResult = textMeasurer.measure(
            label,
            style = TextStyle(fontSize = 9.sp, color = labelColor)
        )
        drawText(
            textResult,
            topLeft = Offset(0f, y - textResult.size.height / 2f)
        )
    }
}

private fun DrawScope.drawTimeAxis(
    windowSeconds: Int,
    leftPadding: Float,
    graphWidth: Float,
    graphHeight: Float,
    labelColor: Color,
    textMeasurer: TextMeasurer
) {
    val timeSteps = when {
        windowSeconds <= 60 -> listOf(0, 15, 30, 45, 60)
        windowSeconds <= 300 -> listOf(0, 60, 120, 180, 240, 300)
        else -> listOf(0, 300, 600, 900)
    }

    for (sec in timeSteps) {
        if (sec > windowSeconds) break
        val x = leftPadding + graphWidth * (1f - sec.toFloat() / windowSeconds)
        val label = if (sec == 0) "jetzt" else "${sec}s"
        val textResult = textMeasurer.measure(
            label,
            style = TextStyle(fontSize = 9.sp, color = labelColor)
        )
        drawText(
            textResult,
            topLeft = Offset(x - textResult.size.width / 2f, graphHeight + 4f)
        )
    }
}

private fun DrawScope.drawSpeedLine(
    samples: List<SpeedSample>,
    getValue: (SpeedSample) -> Long,
    maxSpeed: Long,
    windowStart: Long,
    windowDuration: Long,
    leftPadding: Float,
    graphWidth: Float,
    graphHeight: Float,
    lineColor: Color,
    progress: Float
) {
    if (samples.isEmpty() || maxSpeed <= 0) return

    val linePath = Path()
    val fillPath = Path()
    var started = false

    val visibleCount = (samples.size * progress).toInt().coerceAtLeast(1)

    for (i in 0 until visibleCount) {
        val sample = samples[i]
        val x = leftPadding + graphWidth * ((sample.timestamp - windowStart).toFloat() / windowDuration)
        val y = graphHeight * (1f - getValue(sample).toFloat() / maxSpeed)

        if (!started) {
            linePath.moveTo(x, y)
            fillPath.moveTo(x, graphHeight)
            fillPath.lineTo(x, y)
            started = true
        } else {
            linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
    }

    // Gradient-Fill
    if (started) {
        val lastSample = samples[visibleCount - 1]
        val lastX = leftPadding + graphWidth * ((lastSample.timestamp - windowStart).toFloat() / windowDuration)
        fillPath.lineTo(lastX, graphHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f)),
                startY = 0f,
                endY = graphHeight
            )
        )

        // Linie
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2.5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/** Rundet auf eine schöne Obergrenze für die Y-Achse */
private fun niceMaxValue(raw: Long): Long {
    if (raw <= 0) return 10_240L // 10 KB/s minimum

    val magnitude = generateSequence(1L) { it * 10 }
        .first { it >= raw }

    val step = magnitude / 10
    val steps = listOf(1L, 2L, 5L, 10L)

    for (s in steps) {
        val candidate = s * step
        if (candidate >= raw) return candidate
    }
    return magnitude
}
