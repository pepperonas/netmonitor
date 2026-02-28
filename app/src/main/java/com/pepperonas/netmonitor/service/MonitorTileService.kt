package com.pepperonas.netmonitor.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.pepperonas.netmonitor.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MonitorTileService : TileService() {

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        scope = CoroutineScope(Dispatchers.Main + Job())
        scope?.launch {
            NetworkMonitorService.isRunning.collectLatest { running ->
                updateTile(running)
            }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val isRunning = NetworkMonitorService.isRunning.value
        if (isRunning) {
            stopService(Intent(this, NetworkMonitorService::class.java))
        } else {
            ContextCompat.startForegroundService(
                this, Intent(this, NetworkMonitorService::class.java)
            )
        }
    }

    private fun updateTile(isRunning: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.subtitle = getString(if (isRunning) R.string.tile_active else R.string.tile_inactive)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = getString(
                if (isRunning) R.string.tile_monitoring_active else R.string.tile_monitoring_inactive
            )
        }
        tile.updateTile()
    }
}
