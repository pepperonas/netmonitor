package com.pepperonas.netmonitor.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    // Theme: "system", "light", "dark"
    val theme: Flow<String> = context.dataStore.data.map {
        it[KEY_THEME] ?: "system"
    }

    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[KEY_THEME] = value }
    }

    // Update-Intervall in ms
    val updateInterval: Flow<Int> = context.dataStore.data.map {
        it[KEY_UPDATE_INTERVAL] ?: 1000
    }

    suspend fun setUpdateInterval(value: Int) {
        context.dataStore.edit { it[KEY_UPDATE_INTERVAL] = value }
    }

    // Notification-Style: "both", "download", "upload"
    val notificationStyle: Flow<String> = context.dataStore.data.map {
        it[KEY_NOTIFICATION_STYLE] ?: "both"
    }

    suspend fun setNotificationStyle(value: String) {
        context.dataStore.edit { it[KEY_NOTIFICATION_STYLE] = value }
    }

    // Auto-Start
    val autoStart: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_AUTO_START] ?: true
    }

    suspend fun setAutoStart(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_START] = value }
    }

    // Graph-Zeitfenster in Sekunden
    val graphWindow: Flow<Int> = context.dataStore.data.map {
        it[KEY_GRAPH_WINDOW] ?: 60
    }

    suspend fun setGraphWindow(value: Int) {
        context.dataStore.edit { it[KEY_GRAPH_WINDOW] = value }
    }

    // Data budget in bytes (0 = disabled)
    val dataBudget: Flow<Long> = context.dataStore.data.map {
        it[KEY_DATA_BUDGET] ?: 0L
    }

    suspend fun setDataBudget(value: Long) {
        context.dataStore.edit { it[KEY_DATA_BUDGET] = value }
    }

    // Budget warning threshold (percent, default 80)
    val budgetWarningPercent: Flow<Int> = context.dataStore.data.map {
        it[KEY_BUDGET_WARNING] ?: 80
    }

    suspend fun setBudgetWarningPercent(value: Int) {
        context.dataStore.edit { it[KEY_BUDGET_WARNING] = value }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_UPDATE_INTERVAL = intPreferencesKey("update_interval")
        private val KEY_NOTIFICATION_STYLE = stringPreferencesKey("notification_style")
        private val KEY_AUTO_START = booleanPreferencesKey("auto_start")
        private val KEY_GRAPH_WINDOW = intPreferencesKey("graph_window")
        private val KEY_DATA_BUDGET = longPreferencesKey("data_budget")
        private val KEY_BUDGET_WARNING = intPreferencesKey("budget_warning_percent")
    }
}
