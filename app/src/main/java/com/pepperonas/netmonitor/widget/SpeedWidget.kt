package com.pepperonas.netmonitor.widget

import android.content.Context
import android.net.TrafficStats
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.pepperonas.netmonitor.MainActivity
import com.pepperonas.netmonitor.NetMonitorApplication
import com.pepperonas.netmonitor.util.TrafficMonitor

class SpeedWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes()

        // Read latest speed sample from database
        var dlSpeed = 0L
        var ulSpeed = 0L
        try {
            val app = context.applicationContext as NetMonitorApplication
            val latest = app.database.speedSampleDao().getLatest()
            if (latest != null) {
                // Only show speed if sample is recent (< 5 seconds old)
                val age = System.currentTimeMillis() - latest.timestamp
                if (age < 5000) {
                    dlSpeed = latest.rxBytesPerSec
                    ulSpeed = latest.txBytesPerSec
                }
            }
        } catch (_: Exception) {
            // Database not available, show 0
        }

        provideContent {
            GlanceTheme {
                SpeedWidgetContent(
                    downloadSpeed = TrafficMonitor.formatSpeed(dlSpeed),
                    uploadSpeed = TrafficMonitor.formatSpeed(ulSpeed),
                    totalRx = TrafficMonitor.formatBytes(totalRx),
                    totalTx = TrafficMonitor.formatBytes(totalTx)
                )
            }
        }
    }
}

@Composable
private fun SpeedWidgetContent(
    downloadSpeed: String,
    uploadSpeed: String,
    totalRx: String,
    totalTx: String
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NetMonitor",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.primary
            )
        )

        Spacer(GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Download
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\u2193",
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.primary)
                )
                Text(
                    text = downloadSpeed,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )
            }

            Spacer(GlanceModifier.width(24.dp))

            // Upload
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\u2191",
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.tertiary)
                )
                Text(
                    text = uploadSpeed,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )
            }
        }

        Spacer(GlanceModifier.height(6.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\u2193 $totalRx",
                style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
            Spacer(GlanceModifier.width(12.dp))
            Text(
                text = "\u2191 $totalTx",
                style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        }
    }
}

class SpeedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpeedWidget()
}
