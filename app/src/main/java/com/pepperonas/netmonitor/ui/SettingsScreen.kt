package com.pepperonas.netmonitor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pepperonas.netmonitor.R

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val updateInterval by viewModel.updateInterval.collectAsStateWithLifecycle()
    val notificationStyle by viewModel.notificationStyle.collectAsStateWithLifecycle()
    val autoStart by viewModel.autoStart.collectAsStateWithLifecycle()
    val graphWindow by viewModel.graphWindow.collectAsStateWithLifecycle()
    val dataBudget by viewModel.dataBudget.collectAsStateWithLifecycle()
    val budgetWarningPercent by viewModel.budgetWarningPercent.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionLabel(stringResource(R.string.settings_appearance)) }

        item {
            SettingsCard {
                SettingsOptionRow(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.settings_theme),
                    subtitle = when (theme) {
                        "light" -> stringResource(R.string.theme_light)
                        "dark" -> stringResource(R.string.theme_dark)
                        else -> stringResource(R.string.theme_system)
                    },
                    onClick = {
                        viewModel.setTheme(when (theme) {
                            "system" -> "light"; "light" -> "dark"; else -> "system"
                        })
                    }
                )
            }
        }

        item { SectionLabel(stringResource(R.string.settings_monitoring)) }

        item {
            SettingsCard {
                SettingsOptionRow(
                    icon = Icons.Default.Speed,
                    title = stringResource(R.string.settings_update_rate),
                    subtitle = when (updateInterval) {
                        500 -> "0,5s"
                        2000 -> "2s"
                        else -> "1s"
                    },
                    onClick = {
                        viewModel.setUpdateInterval(when (updateInterval) {
                            500 -> 1000; 1000 -> 2000; else -> 500
                        })
                    }
                )
                Spacer(Modifier.height(4.dp))
                SettingsSwitchRow(
                    icon = Icons.Default.PlayArrow,
                    title = stringResource(R.string.settings_auto_start),
                    subtitle = stringResource(R.string.settings_auto_start_desc),
                    checked = autoStart,
                    onCheckedChange = { viewModel.setAutoStart(it) }
                )
            }
        }

        item { SectionLabel(stringResource(R.string.settings_notifications)) }

        item {
            SettingsCard {
                SettingsOptionRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_notification_style),
                    subtitle = when (notificationStyle) {
                        "download" -> stringResource(R.string.notification_download_only)
                        "upload" -> stringResource(R.string.notification_upload_only)
                        else -> stringResource(R.string.notification_both)
                    },
                    onClick = {
                        viewModel.setNotificationStyle(when (notificationStyle) {
                            "both" -> "download"; "download" -> "upload"; else -> "both"
                        })
                    }
                )
            }
        }

        item { SectionLabel(stringResource(R.string.settings_data_budget)) }

        item {
            SettingsCard {
                SettingsOptionRow(
                    icon = Icons.Default.DataUsage,
                    title = stringResource(R.string.settings_budget_limit),
                    subtitle = if (dataBudget <= 0) stringResource(R.string.budget_disabled)
                    else {
                        val gb = dataBudget / (1024.0 * 1024.0 * 1024.0)
                        String.format(stringResource(R.string.budget_gb_format), String.format("%.0f", gb))
                    },
                    onClick = {
                        // Cycle through: off -> 1GB -> 2GB -> 5GB -> 10GB -> 20GB -> 50GB -> off
                        val gb = 1024L * 1024L * 1024L
                        viewModel.setDataBudget(when (dataBudget) {
                            0L -> 1L * gb
                            1L * gb -> 2L * gb
                            2L * gb -> 5L * gb
                            5L * gb -> 10L * gb
                            10L * gb -> 20L * gb
                            20L * gb -> 50L * gb
                            else -> 0L
                        })
                    }
                )
                if (dataBudget > 0) {
                    Spacer(Modifier.height(4.dp))
                    SettingsOptionRow(
                        icon = Icons.Default.Warning,
                        title = stringResource(R.string.settings_budget_warning),
                        subtitle = "$budgetWarningPercent%",
                        onClick = {
                            viewModel.setBudgetWarningPercent(when (budgetWarningPercent) {
                                60 -> 70; 70 -> 80; 80 -> 90; 90 -> 95; else -> 60
                            })
                        }
                    )
                }
            }
        }

        item { SectionLabel(stringResource(R.string.settings_graph)) }

        item {
            SettingsCard {
                SettingsOptionRow(
                    icon = Icons.Default.AutoGraph,
                    title = stringResource(R.string.settings_time_window),
                    subtitle = when (graphWindow) {
                        30 -> String.format(stringResource(R.string.seconds_format), 30)
                        120 -> String.format(stringResource(R.string.minutes_format), 2)
                        300 -> String.format(stringResource(R.string.minutes_format), 5)
                        else -> String.format(stringResource(R.string.seconds_format), 60)
                    },
                    onClick = {
                        viewModel.setGraphWindow(when (graphWindow) {
                            30 -> 60; 60 -> 120; 120 -> 300; else -> 30
                        })
                    }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SettingsOptionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
