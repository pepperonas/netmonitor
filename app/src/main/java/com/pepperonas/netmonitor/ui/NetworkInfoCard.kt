package com.pepperonas.netmonitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pepperonas.netmonitor.R
import com.pepperonas.netmonitor.util.NetworkDetails

@Composable
fun NetworkInfoCard(details: NetworkDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header mit Icon und Verbindungstyp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (details.connectionIcon) {
                        "wifi" -> Icons.Default.Wifi
                        "mobile" -> Icons.Default.CellTower
                        "ethernet" -> Icons.Default.Lan
                        "vpn" -> Icons.Default.VpnKey
                        else -> Icons.Default.SignalWifiOff
                    },
                    contentDescription = stringResource(R.string.connection),
                    modifier = Modifier.size(28.dp),
                    tint = if (details.connectionIcon == "none")
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        details.connectionType,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (details.isVpn) {
                        Text(
                            stringResource(R.string.vpn_active),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // WiFi-Details
            if (details.connectionIcon == "wifi") {
                details.wifiSsid?.let { ssid ->
                    InfoRow(stringResource(R.string.network_name), ssid)
                }
                details.wifiSignalLevel?.let { level ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.signal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = level / 4f,
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(6.dp),
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${details.wifiSignalStrength} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                details.wifiLinkSpeed?.let { speed ->
                    InfoRow(stringResource(R.string.link_speed), "$speed Mbps")
                }
            }

            // Mobile Details
            if (details.connectionIcon == "mobile") {
                details.mobileCarrier?.let { carrier ->
                    InfoRow(stringResource(R.string.carrier), carrier)
                }
                details.mobileNetworkType?.let { type ->
                    InfoRow(stringResource(R.string.network_type), type)
                }
            }

            // IP-Adresse (immer anzeigen wenn verfügbar)
            details.localIp?.let { ip ->
                InfoRow(stringResource(R.string.ip_address), ip)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
