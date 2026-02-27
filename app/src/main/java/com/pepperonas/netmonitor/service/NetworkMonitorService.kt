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
import com.pepperonas.netmonitor.MainActivity
import com.pepperonas.netmonitor.R
import com.pepperonas.netmonitor.util.TrafficMonitor

class NetworkMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val trafficMonitor = TrafficMonitor()

    private val updateRunnable = object : Runnable {
        override fun run() {
            val speed = trafficMonitor.sample()
            updateNotification(speed)
            handler.postDelayed(this, UPDATE_INTERVAL)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification(TrafficMonitor.Speed(0, 0))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isRunning = true
        handler.post(updateRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Netzwerk-Monitor", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Zeigt aktuelle Netzwerkgeschwindigkeit"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(speed: TrafficMonitor.Speed): Notification {
        val down = TrafficMonitor.formatSpeed(speed.rxBytesPerSec)
        val up = TrafficMonitor.formatSpeed(speed.txBytesPerSec)

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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_network)
            .setContentTitle("\u2193 $down    \u2191 $up")
            .setContentIntent(openIntent)
            .addAction(0, "Stopp", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(speed: TrafficMonitor.Speed) {
        val notification = buildNotification(speed)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "net_monitor"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.pepperonas.netmonitor.STOP"
        const val UPDATE_INTERVAL = 500L // 2 Hz
        var isRunning = false
            private set
    }
}
