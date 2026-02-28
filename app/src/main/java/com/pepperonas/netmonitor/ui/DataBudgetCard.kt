package com.pepperonas.netmonitor.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pepperonas.netmonitor.R
import com.pepperonas.netmonitor.util.TrafficMonitor

@Composable
fun DataBudgetCard(
    usedBytes: Long,
    budgetBytes: Long,
    warningPercent: Int
) {
    if (budgetBytes <= 0) return

    val usagePercent = (usedBytes.toFloat() / budgetBytes).coerceIn(0f, 1f)
    val usagePercentInt = (usagePercent * 100).toInt()
    val isWarning = usagePercentInt >= warningPercent
    val isExceeded = usedBytes >= budgetBytes

    val progressColor by animateColorAsState(
        targetValue = when {
            isExceeded -> MaterialTheme.colorScheme.error
            isWarning -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        label = "budgetColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExceeded)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.data_budget),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$usagePercentInt%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }
            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = usagePercent,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    TrafficMonitor.formatBytes(usedBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    " / ${TrafficMonitor.formatBytes(budgetBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                val remaining = (budgetBytes - usedBytes).coerceAtLeast(0)
                Text(
                    stringResource(R.string.budget_remaining, TrafficMonitor.formatBytes(remaining)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isExceeded) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExceeded) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.budget_exceeded),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (isWarning) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.budget_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
