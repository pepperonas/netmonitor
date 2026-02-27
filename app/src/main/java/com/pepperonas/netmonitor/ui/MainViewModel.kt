package com.pepperonas.netmonitor.ui

import android.app.Application
import android.content.pm.PackageManager
import android.net.TrafficStats
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pepperonas.netmonitor.model.AppTrafficInfo
import com.pepperonas.netmonitor.service.NetworkMonitorService
import com.pepperonas.netmonitor.util.TrafficMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val trafficMonitor = TrafficMonitor()

    private val _speed = MutableStateFlow(TrafficMonitor.Speed(0, 0))
    val speed: StateFlow<TrafficMonitor.Speed> = _speed

    private val _appTraffic = MutableStateFlow<List<AppTrafficInfo>>(emptyList())
    val appTraffic: StateFlow<List<AppTrafficInfo>> = _appTraffic

    val isServiceRunning: StateFlow<Boolean> = NetworkMonitorService.isRunning

    private var speedJob: Job? = null

    fun startSpeedUpdates() {
        speedJob?.cancel()
        speedJob = viewModelScope.launch {
            while (isActive) {
                _speed.value = trafficMonitor.sample()
                delay(1000)
            }
        }
    }

    fun stopSpeedUpdates() {
        speedJob?.cancel()
    }

    fun loadAppTraffic() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val list = apps.mapNotNull { info ->
                val rx = TrafficStats.getUidRxBytes(info.uid)
                val tx = TrafficStats.getUidTxBytes(info.uid)
                if (rx <= 0 && tx <= 0) return@mapNotNull null

                AppTrafficInfo(
                    appName = pm.getApplicationLabel(info).toString(),
                    packageName = info.packageName,
                    uid = info.uid,
                    rxBytes = rx.coerceAtLeast(0),
                    txBytes = tx.coerceAtLeast(0)
                )
            }.sortedByDescending { it.rxBytes + it.txBytes }

            _appTraffic.value = list
        }
    }
}
