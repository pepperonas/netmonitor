package com.pepperonas.netmonitor.ui

import android.app.Application
import android.content.pm.PackageManager
import android.net.TrafficStats
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pepperonas.netmonitor.NetMonitorApplication
import com.pepperonas.netmonitor.data.SettingsStore
import com.pepperonas.netmonitor.data.TrafficRepository
import com.pepperonas.netmonitor.data.entity.DailyTrafficSummary
import com.pepperonas.netmonitor.data.entity.SpeedSample
import com.pepperonas.netmonitor.data.entity.SpeedTestResult
import com.pepperonas.netmonitor.model.AppTrafficInfo
import com.pepperonas.netmonitor.service.NetworkMonitorService
import com.pepperonas.netmonitor.util.NetworkDetails
import com.pepperonas.netmonitor.util.NetworkInfoProvider
import com.pepperonas.netmonitor.util.SpeedTestEngine
import com.pepperonas.netmonitor.util.TrafficMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val trafficMonitor = TrafficMonitor()
    private val repository: TrafficRepository =
        (application as NetMonitorApplication).repository
    private val settingsStore: SettingsStore =
        (application as NetMonitorApplication).settingsStore

    private val speedTestEngine = SpeedTestEngine()
    private val speedTestDao = (application as NetMonitorApplication).database.speedTestDao()

    private val _speed = MutableStateFlow(TrafficMonitor.Speed(0, 0))
    val speed: StateFlow<TrafficMonitor.Speed> = _speed

    private val _appTraffic = MutableStateFlow<List<AppTrafficInfo>>(emptyList())
    val appTraffic: StateFlow<List<AppTrafficInfo>> = _appTraffic

    private val _networkDetails = MutableStateFlow(NetworkInfoProvider.getDetails(application))
    val networkDetails: StateFlow<NetworkDetails> = _networkDetails

    val isServiceRunning: StateFlow<Boolean> = NetworkMonitorService.isRunning

    /** Letzte 60 Sekunden Speed-Samples für den Live-Graph */
    val recentSamples: StateFlow<List<SpeedSample>> = repository
        .getRecentSamples(60_000L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val theme: StateFlow<String> = settingsStore.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val updateInterval: StateFlow<Int> = settingsStore.updateInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1000)

    val notificationStyle: StateFlow<String> = settingsStore.notificationStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "both")

    val autoStart: StateFlow<Boolean> = settingsStore.autoStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val graphWindow: StateFlow<Int> = settingsStore.graphWindow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    val dataBudget: StateFlow<Long> = settingsStore.dataBudget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val budgetWarningPercent: StateFlow<Int> = settingsStore.budgetWarningPercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 80)

    /** Current month's total usage in bytes */
    val monthlyUsage: StateFlow<Long> = run {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val firstOfMonth = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
        repository.getMonthlyUsage(firstOfMonth)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    }

    // Speed Test
    val speedTestProgress: StateFlow<SpeedTestEngine.Progress> = speedTestEngine.progress
    val speedTestResults: StateFlow<List<SpeedTestResult>> = speedTestDao.getRecentResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Letzte 30 Tage Summaries für Statistik-Screen */
    val recentDays: StateFlow<List<DailyTrafficSummary>> = repository
        .getRecentDays(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var speedJob: Job? = null

    fun startSpeedUpdates() {
        speedJob?.cancel()
        speedJob = viewModelScope.launch {
            while (isActive) {
                _speed.value = trafficMonitor.sample()
                _networkDetails.value = NetworkInfoProvider.getDetails(getApplication())
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

    fun setTheme(value: String) {
        viewModelScope.launch { settingsStore.setTheme(value) }
    }

    fun setUpdateInterval(value: Int) {
        viewModelScope.launch { settingsStore.setUpdateInterval(value) }
    }

    fun setNotificationStyle(value: String) {
        viewModelScope.launch { settingsStore.setNotificationStyle(value) }
    }

    fun setAutoStart(value: Boolean) {
        viewModelScope.launch { settingsStore.setAutoStart(value) }
    }

    fun setGraphWindow(value: Int) {
        viewModelScope.launch { settingsStore.setGraphWindow(value) }
    }

    suspend fun getHourlyData(): List<SpeedSample> {
        val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        return repository.getHourlyAverages(since)
    }

    suspend fun getTodaySummary(): DailyTrafficSummary? {
        return repository.getTodaySummary()
    }

    fun setDataBudget(value: Long) {
        viewModelScope.launch { settingsStore.setDataBudget(value) }
    }

    fun setBudgetWarningPercent(value: Int) {
        viewModelScope.launch { settingsStore.setBudgetWarningPercent(value) }
    }

    fun startSpeedTest() {
        viewModelScope.launch(Dispatchers.IO) {
            speedTestEngine.reset()
            val result = speedTestEngine.runTest()
            val connectionType = NetworkInfoProvider.getDetails(getApplication()).connectionType
            speedTestDao.insert(
                SpeedTestResult(
                    timestamp = System.currentTimeMillis(),
                    downloadBytesPerSec = result.downloadBytesPerSec,
                    uploadBytesPerSec = result.uploadBytesPerSec,
                    latencyMs = result.latencyMs,
                    connectionType = connectionType,
                    serverUrl = result.serverUrl
                )
            )
            speedTestDao.cleanupOldResults()
        }
    }

    fun cleanupOldData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanup()
        }
    }
}
