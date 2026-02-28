package com.pepperonas.netmonitor.ui

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.pepperonas.netmonitor.R
import com.pepperonas.netmonitor.data.entity.DailyTrafficSummary
import com.pepperonas.netmonitor.data.entity.SpeedSample
import com.pepperonas.netmonitor.util.TrafficMonitor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class TimeRange(val label: String) {
    TODAY("today"),
    WEEK("week"),
    MONTH("month")
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: MainViewModel) {
    var selectedRange by remember { mutableStateOf(TimeRange.TODAY) }

    var hourlyData by remember { mutableStateOf<List<SpeedSample>>(emptyList()) }
    val dailyData by viewModel.recentDays.collectAsStateWithLifecycle()
    var todaySummary by remember { mutableStateOf<DailyTrafficSummary?>(null) }

    LaunchedEffect(selectedRange) {
        when (selectedRange) {
            TimeRange.TODAY -> {
                hourlyData = viewModel.getHourlyData()
                todaySummary = viewModel.getTodaySummary()
            }
            else -> {
                todaySummary = null
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Zeitraum-Auswahl
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRange.values().forEach { range ->
                    val chipLabel = when (range) {
                        TimeRange.TODAY -> stringResource(R.string.stats_today)
                        TimeRange.WEEK -> stringResource(R.string.stats_week)
                        TimeRange.MONTH -> stringResource(R.string.stats_month)
                    }
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        label = { Text(chipLabel) }
                    )
                }
            }
        }

        // Zusammenfassung
        item {
            SummaryCard(
                selectedRange = selectedRange,
                todaySummary = todaySummary,
                dailyData = dailyData
            )
        }

        // Chart
        item {
            when (selectedRange) {
                TimeRange.TODAY -> HourlyBarChart(hourlyData)
                TimeRange.WEEK -> DailyBarChart(
                    data = dailyData.takeLast(7),
                    label = stringResource(R.string.last_7_days)
                )
                TimeRange.MONTH -> DailyBarChart(
                    data = dailyData.takeLast(30),
                    label = stringResource(R.string.last_30_days)
                )
            }
        }

        // Peak-Werte
        item {
            PeakCard(
                selectedRange = selectedRange,
                todaySummary = todaySummary,
                dailyData = dailyData
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    selectedRange: TimeRange,
    todaySummary: DailyTrafficSummary?,
    dailyData: List<DailyTrafficSummary>
) {
    val (totalRx, totalTx) = when (selectedRange) {
        TimeRange.TODAY -> Pair(
            todaySummary?.totalRxBytes ?: 0L,
            todaySummary?.totalTxBytes ?: 0L
        )
        TimeRange.WEEK -> {
            val last7 = dailyData.takeLast(7)
            Pair(last7.sumOf { it.totalRxBytes }, last7.sumOf { it.totalTxBytes })
        }
        TimeRange.MONTH -> {
            val last30 = dailyData.takeLast(30)
            Pair(last30.sumOf { it.totalRxBytes }, last30.sumOf { it.totalTxBytes })
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.data_usage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatValue(
                    label = "Download",
                    value = TrafficMonitor.formatBytes(totalRx),
                    color = MaterialTheme.colorScheme.primary
                )
                StatValue(
                    label = "Upload",
                    value = TrafficMonitor.formatBytes(totalTx),
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatValue(
                    label = stringResource(R.string.total),
                    value = TrafficMonitor.formatBytes(totalRx + totalTx),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StatValue(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
private fun PeakCard(
    selectedRange: TimeRange,
    todaySummary: DailyTrafficSummary?,
    dailyData: List<DailyTrafficSummary>
) {
    val (peakDl, peakUl) = when (selectedRange) {
        TimeRange.TODAY -> Pair(
            todaySummary?.peakDownload ?: 0L,
            todaySummary?.peakUpload ?: 0L
        )
        TimeRange.WEEK -> {
            val last7 = dailyData.takeLast(7)
            Pair(
                last7.maxOfOrNull { it.peakDownload } ?: 0L,
                last7.maxOfOrNull { it.peakUpload } ?: 0L
            )
        }
        TimeRange.MONTH -> {
            val last30 = dailyData.takeLast(30)
            Pair(
                last30.maxOfOrNull { it.peakDownload } ?: 0L,
                last30.maxOfOrNull { it.peakUpload } ?: 0L
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.peak_values),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatValue(
                    label = stringResource(R.string.peak_download),
                    value = TrafficMonitor.formatSpeed(peakDl),
                    color = MaterialTheme.colorScheme.primary
                )
                StatValue(
                    label = stringResource(R.string.peak_upload),
                    value = TrafficMonitor.formatSpeed(peakUl),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun HourlyBarChart(data: List<SpeedSample>) {
    val dlColor = MaterialTheme.colorScheme.primary
    val ulColor = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.hourly_average),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_data_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val maxVal = data.maxOf { maxOf(it.rxBytesPerSec, it.txBytesPerSec) }
                        .coerceAtLeast(1024)
                    val barWidth = (size.width - 40f) / (data.size * 2.5f)
                    val bottomPad = 20f
                    val chartHeight = size.height - bottomPad

                    data.forEachIndexed { index, sample ->
                        val x = 40f + index * barWidth * 2.5f

                        // Download bar
                        val dlHeight = (sample.rxBytesPerSec.toFloat() / maxVal) * chartHeight
                        drawRoundRect(
                            color = dlColor,
                            topLeft = Offset(x, chartHeight - dlHeight),
                            size = Size(barWidth, dlHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // Upload bar
                        val ulHeight = (sample.txBytesPerSec.toFloat() / maxVal) * chartHeight
                        drawRoundRect(
                            color = ulColor,
                            topLeft = Offset(x + barWidth + 2f, chartHeight - ulHeight),
                            size = Size(barWidth, ulHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // Stunden-Label
                        val cal = Calendar.getInstance().apply { timeInMillis = sample.timestamp }
                        val hourLabel = "${cal.get(Calendar.HOUR_OF_DAY)}h"
                        val textResult = textMeasurer.measure(
                            hourLabel,
                            style = TextStyle(fontSize = 8.sp, color = labelColor)
                        )
                        drawText(
                            textResult,
                            topLeft = Offset(
                                x + barWidth - textResult.size.width / 2f,
                                chartHeight + 2f
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBarChart(data: List<DailyTrafficSummary>, label: String) {
    val dlColor = MaterialTheme.colorScheme.primary
    val ulColor = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val dateFormat = remember { SimpleDateFormat("dd.", Locale.GERMANY) }
    val parseFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_data_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val maxVal = data.maxOf { it.totalRxBytes + it.totalTxBytes }
                        .coerceAtLeast(1024)
                    val barWidth = (size.width - 40f) / (data.size * 1.5f)
                    val bottomPad = 20f
                    val chartHeight = size.height - bottomPad

                    data.forEachIndexed { index, day ->
                        val x = 40f + index * barWidth * 1.5f
                        val total = day.totalRxBytes + day.totalTxBytes
                        val totalHeight = (total.toFloat() / maxVal) * chartHeight
                        val dlRatio = if (total > 0) day.totalRxBytes.toFloat() / total else 0.5f

                        // Gestapelter Balken: DL unten, UL oben
                        val dlHeight = totalHeight * dlRatio
                        val ulHeight = totalHeight * (1f - dlRatio)

                        drawRoundRect(
                            color = dlColor,
                            topLeft = Offset(x, chartHeight - dlHeight),
                            size = Size(barWidth, dlHeight),
                            cornerRadius = CornerRadius(0f, 0f)
                        )
                        drawRoundRect(
                            color = ulColor,
                            topLeft = Offset(x, chartHeight - totalHeight),
                            size = Size(barWidth, ulHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // Datums-Label (nur jeden 2. oder 5. Tag bei vielen Einträgen)
                        val showLabel = data.size <= 7 || index % (if (data.size > 14) 5 else 2) == 0
                        if (showLabel) {
                            val date = try {
                                parseFormat.parse(day.date)
                            } catch (_: Exception) { null }
                            val dayLabel = date?.let { dateFormat.format(it) } ?: ""
                            val textResult = textMeasurer.measure(
                                dayLabel,
                                style = TextStyle(fontSize = 8.sp, color = labelColor)
                            )
                            drawText(
                                textResult,
                                topLeft = Offset(
                                    x + barWidth / 2f - textResult.size.width / 2f,
                                    chartHeight + 2f
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
