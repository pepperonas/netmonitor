package com.pepperonas.netmonitor.ui

import android.Manifest
import android.app.AppOpsManager
import android.app.Application
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
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

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess

    private val _hasNotificationPermission = MutableStateFlow(true)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission

    private val _networkDetails = MutableStateFlow(NetworkInfoProvider.getDetails(application))
    val networkDetails: StateFlow<NetworkDetails> = _networkDetails

    val isServiceRunning: StateFlow<Boolean> = NetworkMonitorService.isRunning

    /** Speed-Samples für den Live-Graph (Zeitfenster aus Settings) */
    @Suppress("OPT_IN_USAGE")
    val recentSamples: StateFlow<List<SpeedSample>> = settingsStore.graphWindow
        .flatMapLatest { windowSec -> repository.getRecentSamples(windowSec * 1000L) }
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
                delay(updateInterval.value.toLong())
            }
        }
    }

    fun stopSpeedUpdates() {
        speedJob?.cancel()
    }

    fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val app = getApplication<Application>()
            _hasNotificationPermission.value = ContextCompat.checkSelfPermission(
                app, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            _hasNotificationPermission.value = true
        }
    }

    fun checkUsageAccess() {
        val app = getApplication<Application>()
        val appOps = app.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            app.packageName
        )
        _hasUsageAccess.value = mode == AppOpsManager.MODE_ALLOWED
    }

    fun loadAppTraffic() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val pm = app.packageManager
            checkUsageAccess()

            if (_hasUsageAccess.value) {
                loadAppTrafficViaNetworkStats(app, pm)
            } else {
                loadAppTrafficViaTrafficStats(pm)
            }
        }
    }

    private fun loadAppTrafficViaNetworkStats(app: Application, pm: PackageManager) {
        try {
            val nsm = app.getSystemService(NetworkStatsManager::class.java)
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val uidToApp = apps.associateBy { it.uid }

            // Query since device boot for consistency with TrafficStats
            val end = System.currentTimeMillis()
            val start = end - 30L * 24 * 60 * 60 * 1000 // Last 30 days

            val uidTraffic = mutableMapOf<Int, Pair<Long, Long>>() // uid -> (rx, tx)

            // Query WiFi
            queryNetworkStats(nsm, ConnectivityManager.TYPE_WIFI, start, end, uidTraffic)
            // Query Mobile
            queryNetworkStats(nsm, ConnectivityManager.TYPE_MOBILE, start, end, uidTraffic)

            val list = uidTraffic.mapNotNull { (uid, traffic) ->
                val (rx, tx) = traffic
                if (rx <= 0 && tx <= 0) return@mapNotNull null
                val appInfo = uidToApp[uid] ?: return@mapNotNull null

                AppTrafficInfo(
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    uid = uid,
                    rxBytes = rx,
                    txBytes = tx
                )
            }.sortedByDescending { it.rxBytes + it.txBytes }

            _appTraffic.value = list
        } catch (_: Exception) {
            // Fallback to TrafficStats
            loadAppTrafficViaTrafficStats(pm)
        }
    }

    @Suppress("DEPRECATION")
    private fun queryNetworkStats(
        nsm: NetworkStatsManager,
        networkType: Int,
        start: Long,
        end: Long,
        result: MutableMap<Int, Pair<Long, Long>>
    ) {
        try {
            val stats = nsm.querySummary(networkType, null, start, end)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                val (prevRx, prevTx) = result.getOrDefault(uid, Pair(0L, 0L))
                result[uid] = Pair(prevRx + bucket.rxBytes, prevTx + bucket.txBytes)
            }
            stats.close()
        } catch (_: Exception) {
            // Permission denied or network type not available
        }
    }

    private fun loadAppTrafficViaTrafficStats(pm: PackageManager) {
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
            val result = speedTestEngine.runTest() ?: return@launch
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
