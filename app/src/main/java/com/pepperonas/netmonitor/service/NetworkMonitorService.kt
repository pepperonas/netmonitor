package com.pepperonas.netmonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.pepperonas.netmonitor.MainActivity
import com.pepperonas.netmonitor.R
import com.pepperonas.netmonitor.util.SpeedIconRenderer
import com.pepperonas.netmonitor.util.TrafficMonitor

class NetworkMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val trafficMonitor = TrafficMonitor()
    private lateinit var notificationManager: NotificationManager

    private val updateRunnable = object : Runnable {
        override fun run() {
            val speed = trafficMonitor.sample()
            updateNotifications(speed)
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
        isRunning = true
        handler.post(updateRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        notificationManager.cancel(NOTIFICATION_ID_UP)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannels() {
        val downChannel = NotificationChannel(
            CHANNEL_ID_DOWN, "Download-Geschwindigkeit", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Zeigt aktuelle Download-Geschwindigkeit"
            setShowBadge(false)
        }
        val upChannel = NotificationChannel(
            CHANNEL_ID_UP, "Upload-Geschwindigkeit", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Zeigt aktuelle Upload-Geschwindigkeit"
            setShowBadge(false)
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
            .setContentText("Download")
            .setWhen(Long.MAX_VALUE)
            .setShowWhen(false)
            .setSortKey("a")
            .build()
    }

    private fun buildUploadNotification(speed: TrafficMonitor.Speed): Notification {
        val parts = TrafficMonitor.formatSpeedParts(speed.txBytesPerSec)
        val icon = SpeedIconRenderer.createUploadBitmap(parts.value, parts.unit)

        return baseBuilder(CHANNEL_ID_UP)
            .setSmallIcon(IconCompat.createWithBitmap(icon))
            .setContentTitle("\u2191 ${parts.value} ${parts.unit}")
            .setContentText("Upload")
            .setWhen(Long.MAX_VALUE - 1)
            .setShowWhen(false)
            .setSortKey("b")
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
            .addAction(0, "Stopp", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    private fun updateNotifications(speed: TrafficMonitor.Speed) {
        notificationManager.notify(NOTIFICATION_ID_DOWN, buildDownloadNotification(speed))
        notificationManager.notify(NOTIFICATION_ID_UP, buildUploadNotification(speed))
    }

    companion object {
        const val CHANNEL_ID_DOWN = "net_monitor_download"
        const val CHANNEL_ID_UP = "net_monitor_upload"
        const val NOTIFICATION_ID_DOWN = 1
        const val NOTIFICATION_ID_UP = 2
        const val ACTION_STOP = "com.pepperonas.netmonitor.STOP"
        const val UPDATE_INTERVAL = 1000L // 1 Hz
        var isRunning = false
            private set
    }
}
