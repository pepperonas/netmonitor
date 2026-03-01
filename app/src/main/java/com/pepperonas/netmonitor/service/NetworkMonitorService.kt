package com.pepperonas.netmonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.pepperonas.netmonitor.MainActivity
import com.pepperonas.netmonitor.NetMonitorApplication
import com.pepperonas.netmonitor.R
import com.pepperonas.netmonitor.util.SpeedIconRenderer
import com.pepperonas.netmonitor.widget.SpeedWidget
import com.pepperonas.netmonitor.util.TrafficMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll

class NetworkMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val trafficMonitor = TrafficMonitor()
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var notificationStyle = "both"

    private val updateRunnable = object : Runnable {
        override fun run() {
            val speed = trafficMonitor.sample()
            updateNotifications(speed)
            persistSample(speed)
            handler.postDelayed(this, UPDATE_INTERVAL)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildDownloadNotification(TrafficMonitor.Speed(0, 0))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID_DOWN, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID_DOWN, notification)
        }
        notificationManager.notify(NOTIFICATION_ID_UP, buildUploadNotification(TrafficMonitor.Speed(0, 0)))
        _isRunning.value = true
        handler.post(updateRunnable)

        // Observe notification style setting
        val settingsStore = (application as NetMonitorApplication).settingsStore
        serviceScope.launch {
            settingsStore.notificationStyle.collect { style ->
                notificationStyle = style
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        _isRunning.value = false
        handler.removeCallbacks(updateRunnable)
        notificationManager.cancel(NOTIFICATION_ID_UP)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private var widgetUpdateCounter = 0

    private fun persistSample(speed: TrafficMonitor.Speed) {
        val repo = (application as NetMonitorApplication).repository
        val connType = getConnectionType()
        serviceScope.launch {
            repo.recordSample(speed.rxBytesPerSec, speed.txBytesPerSec, connType)
            // Update widget every 2 seconds (not every sample to reduce overhead)
            widgetUpdateCounter++
            if (widgetUpdateCounter % 2 == 0) {
                SpeedWidget().updateAll(this@NetworkMonitorService)
            }
        }
    }

    private fun getConnectionType(): String {
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return "none"
        val caps = cm.getNetworkCapabilities(network) ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            else -> "other"
        }
    }

    private fun createNotificationChannels() {
        // Remove old channels (importance can't be changed after creation)
        OLD_CHANNEL_IDS.forEach { notificationManager.deleteNotificationChannel(it) }

        val downChannel = NotificationChannel(
            CHANNEL_ID_DOWN, getString(R.string.channel_download), NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.channel_download_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }
        val upChannel = NotificationChannel(
            CHANNEL_ID_UP, getString(R.string.channel_upload), NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.channel_upload_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }
        notificationManager.createNotificationChannel(downChannel)
        notificationManager.createNotificationChannel(upChannel)
    }

    private fun buildDownloadNotification(speed: TrafficMonitor.Speed): Notification {
        val parts = TrafficMonitor.formatSpeedParts(speed.rxBytesPerSec)
        val icon = SpeedIconRenderer.createDownloadBitmap(parts.value, parts.unit)

        return baseBuilder(CHANNEL_ID_DOWN)
            .setSmallIcon(IconCompat.createWithBitmap(icon))
            .setContentTitle("\u2193 ${parts.value} ${parts.unit}")
            .setContentText(getString(R.string.download))
            .setWhen(Long.MAX_VALUE)
            .setShowWhen(false)
            .setSortKey("a")
            .setGroup(GROUP_KEY_DOWN)
            .build()
    }

    private fun buildUploadNotification(speed: TrafficMonitor.Speed): Notification {
        val parts = TrafficMonitor.formatSpeedParts(speed.txBytesPerSec)
        val icon = SpeedIconRenderer.createUploadBitmap(parts.value, parts.unit)

        return baseBuilder(CHANNEL_ID_UP)
            .setSmallIcon(IconCompat.createWithBitmap(icon))
            .setContentTitle("\u2191 ${parts.value} ${parts.unit}")
            .setContentText(getString(R.string.upload))
            .setWhen(Long.MAX_VALUE - 1)
            .setShowWhen(false)
            .setSortKey("b")
            .setGroup(GROUP_KEY_UP)
            .build()
    }

    private fun baseBuilder(channelId: String): NotificationCompat.Builder {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, NetworkMonitorService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notification_stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    private fun updateNotifications(speed: TrafficMonitor.Speed) {
        when (notificationStyle) {
            "download" -> {
                notificationManager.notify(NOTIFICATION_ID_DOWN, buildDownloadNotification(speed))
                notificationManager.cancel(NOTIFICATION_ID_UP)
            }
            "upload" -> {
                notificationManager.notify(NOTIFICATION_ID_DOWN, buildUploadNotification(speed))
                notificationManager.cancel(NOTIFICATION_ID_UP)
            }
            else -> {
                notificationManager.notify(NOTIFICATION_ID_DOWN, buildDownloadNotification(speed))
                notificationManager.notify(NOTIFICATION_ID_UP, buildUploadNotification(speed))
            }
        }
    }

    companion object {
        const val CHANNEL_ID_DOWN = "net_monitor_download_v2"
        const val CHANNEL_ID_UP = "net_monitor_upload_v2"
        const val NOTIFICATION_ID_DOWN = 1
        const val NOTIFICATION_ID_UP = 2
        const val ACTION_STOP = "com.pepperonas.netmonitor.STOP"
        const val UPDATE_INTERVAL = 1000L // 1 Hz
        private const val GROUP_KEY_DOWN = "net_monitor_dl"
        private const val GROUP_KEY_UP = "net_monitor_ul"
        private val OLD_CHANNEL_IDS = listOf("net_monitor_download", "net_monitor_upload")

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }
}
