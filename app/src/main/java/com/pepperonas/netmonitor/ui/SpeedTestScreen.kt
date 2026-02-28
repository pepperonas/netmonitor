package com.pepperonas.netmonitor.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pepperonas.netmonitor.R
import com.pepperonas.netmonitor.data.entity.SpeedTestResult
import com.pepperonas.netmonitor.util.SpeedTestEngine
import com.pepperonas.netmonitor.util.TrafficMonitor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpeedTestScreen(viewModel: MainViewModel) {
    val progress by viewModel.speedTestProgress.collectAsStateWithLifecycle()
    val results by viewModel.speedTestResults.collectAsStateWithLifecycle()
    val isRunning = progress.phase != SpeedTestEngine.Phase.IDLE
            && progress.phase != SpeedTestEngine.Phase.DONE
            && progress.phase != SpeedTestEngine.Phase.ERROR

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Gauge + current state
        item { SpeedGauge(progress) }

        // Error message
        if (progress.phase == SpeedTestEngine.Phase.ERROR) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        stringResource(R.string.speed_test_error),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Start button
        item {
            Button(
                onClick = { viewModel.startSpeedTest() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isRunning
            ) {
                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(if (isRunning) R.string.speed_test_running else R.string.speed_test_start),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Last result
        if (progress.phase == SpeedTestEngine.Phase.DONE) {
            item {
                results.firstOrNull()?.let { ResultCard(it) }
            }
        }

        // History
        if (results.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.speed_test_history))
            }
            items(results, key = { it.id }) { result ->
                HistoryRow(result)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SpeedGauge(progress: SpeedTestEngine.Progress) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.progressPercent,
        animationSpec = tween(300),
        label = "gaugeProgress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val gaugeColor = when (progress.phase) {
        SpeedTestEngine.Phase.DOWNLOAD -> primaryColor
        SpeedTestEngine.Phase.UPLOAD -> tertiaryColor
        SpeedTestEngine.Phase.LATENCY -> MaterialTheme.colorScheme.secondary
        SpeedTestEngine.Phase.ERROR -> MaterialTheme.colorScheme.error
        else -> primaryColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Track
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = gaugeColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (progress.phase != SpeedTestEngine.Phase.IDLE && progress.currentSpeedBps > 0) {
                        Text(
                            TrafficMonitor.formatSpeed(progress.currentSpeedBps),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "0 B/s",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        when (progress.phase) {
                            SpeedTestEngine.Phase.IDLE -> stringResource(R.string.speed_test_ready)
                            SpeedTestEngine.Phase.LATENCY -> stringResource(R.string.speed_test_latency)
                            SpeedTestEngine.Phase.DOWNLOAD -> stringResource(R.string.speed_test_download)
                            SpeedTestEngine.Phase.UPLOAD -> stringResource(R.string.speed_test_upload)
                            SpeedTestEngine.Phase.DONE -> stringResource(R.string.speed_test_done)
                            SpeedTestEngine.Phase.ERROR -> stringResource(R.string.speed_test_ready)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (progress.phase != SpeedTestEngine.Phase.IDLE && progress.phase != SpeedTestEngine.Phase.DONE) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Composable
private fun ResultCard(result: SpeedTestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.speed_test_result),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultValue(
                    icon = Icons.Default.ArrowDownward,
                    label = stringResource(R.string.download),
                    value = TrafficMonitor.formatSpeed(result.downloadBytesPerSec),
                    color = MaterialTheme.colorScheme.primary
                )
                ResultValue(
                    icon = Icons.Default.ArrowUpward,
                    label = stringResource(R.string.upload),
                    value = TrafficMonitor.formatSpeed(result.uploadBytesPerSec),
                    color = MaterialTheme.colorScheme.tertiary
                )
                ResultValue(
                    icon = Icons.Default.Timer,
                    label = stringResource(R.string.speed_test_ping),
                    value = if (result.latencyMs >= 0) "${result.latencyMs} ms" else "– ms",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun ResultValue(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryRow(result: SpeedTestResult) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date(result.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "\u2193 ${TrafficMonitor.formatSpeed(result.downloadBytesPerSec)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "\u2191 ${TrafficMonitor.formatSpeed(result.uploadBytesPerSec)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Text(
                if (result.latencyMs >= 0) "${result.latencyMs} ms" else "– ms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
