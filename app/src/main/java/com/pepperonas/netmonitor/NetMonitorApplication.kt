package com.pepperonas.netmonitor

import android.app.Application
import com.pepperonas.netmonitor.data.NetMonitorDatabase
import com.pepperonas.netmonitor.data.SettingsStore
import com.pepperonas.netmonitor.data.TrafficRepository

class NetMonitorApplication : Application() {

    val database: NetMonitorDatabase by lazy { NetMonitorDatabase.getInstance(this) }
    val repository: TrafficRepository by lazy {
        TrafficRepository(database.speedSampleDao(), database.dailyTrafficDao())
    }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
}
