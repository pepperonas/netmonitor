package com.pepperonas.netmonitor.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import android.os.Build
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pepperonas.netmonitor.R
import com.pepperonas.netmonitor.model.AppTrafficInfo
import com.pepperonas.netmonitor.util.TrafficMonitor

@Composable
fun LiveScreen(
    viewModel: MainViewModel,
    onToggleService: (currentlyRunning: Boolean) -> Unit
) {
    val speed by viewModel.speed.collectAsStateWithLifecycle()
    val isRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val recentSamples by viewModel.recentSamples.collectAsStateWithLifecycle()
    val graphWindow by viewModel.graphWindow.collectAsStateWithLifecycle()
    val networkDetails by viewModel.networkDetails.collectAsStateWithLifecycle()
    val dataBudget by viewModel.dataBudget.collectAsStateWithLifecycle()
    val budgetWarningPercent by viewModel.budgetWarningPercent.collectAsStateWithLifecycle()
    val monthlyUsage by viewModel.monthlyUsage.collectAsStateWithLifecycle()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            item { NotificationPermissionBanner() }
        }
        item { SpeedCard(speed, isRunning) }
        if (dataBudget > 0) {
            item {
                DataBudgetCard(
                    usedBytes = monthlyUsage,
                    budgetBytes = dataBudget,
                    warningPercent = budgetWarningPercent
                )
            }
        }
        item { NetworkInfoCard(details = networkDetails) }
        item { SpeedGraph(samples = recentSamples, windowSeconds = graphWindow) }
        item {
            ServiceToggleButton(
                isRunning = isRunning,
                onToggle = { onToggleService(isRunning) }
            )
        }
        item {
            Spacer(Modifier.height(4.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(4.dp))
            AboutSection(versionName)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AppsScreen(viewModel: MainViewModel) {
    val appTraffic by viewModel.appTraffic.collectAsStateWithLifecycle()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { SectionHeader(stringResource(R.string.app_traffic_title)) }

        if (!hasUsageAccess) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.usage_access_required),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.grant_permission))
                        }
                    }
                }
            }
        }

        if (appTraffic.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.loading_app_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        items(appTraffic, key = { it.uid }) { info -> AppTrafficRow(info) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun NotificationPermissionBanner() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.notification_permission_required),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}

@Composable
private fun ServiceToggleButton(isRunning: Boolean, onToggle: () -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        label = "buttonColor"
    )

    Button(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(if (isRunning) R.string.monitoring_stop else R.string.monitoring_start),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SpeedCard(speed: TrafficMonitor.Speed, isRunning: Boolean) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0.4f,
        label = "speedAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp).alpha(contentAlpha)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedColumn(
                    label = stringResource(R.string.download),
                    value = TrafficMonitor.formatSpeed(speed.rxBytesPerSec),
                    icon = Icons.Default.ArrowDownward,
                    color = MaterialTheme.colorScheme.primary
                )
                SpeedColumn(
                    label = stringResource(R.string.upload),
                    value = TrafficMonitor.formatSpeed(speed.txBytesPerSec),
                    icon = Icons.Default.ArrowUpward,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (!isRunning) {
                Text(
                    stringResource(R.string.monitoring_inactive),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SpeedColumn(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppTrafficRow(info: AppTrafficInfo) {
    val context = LocalContext.current
    val icon = remember(info.packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(info.packageName)
            val bmp = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            Canvas(bmp).let { canvas ->
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
            bmp.asImageBitmap()
        } catch (_: Exception) { null }
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = info.appName, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(info.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("\u2193 ${TrafficMonitor.formatBytes(info.rxBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text("\u2191 ${TrafficMonitor.formatBytes(info.txBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Text(TrafficMonitor.formatBytes(info.rxBytes + info.txBytes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AboutSection(versionName: String) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.about_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AboutRow(icon = Icons.Default.Person, label = stringResource(R.string.about_developer), value = "Martin Pfeffer")
            AboutRow(
                icon = Icons.Default.Language,
                label = stringResource(R.string.about_website),
                value = "celox.io",
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://celox.io"))) }
            )
            AboutRow(
                icon = Icons.Default.Code,
                label = stringResource(R.string.about_source_code),
                value = "GitHub",
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/pepperonas/netmonitor"))) }
            )
            Text("NetMonitor v$versionName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = if (onClick != null) Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(vertical = 2.dp)
        else Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
