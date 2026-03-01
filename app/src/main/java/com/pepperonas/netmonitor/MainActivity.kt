package com.pepperonas.netmonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pepperonas.netmonitor.service.NetworkMonitorService
import com.pepperonas.netmonitor.ui.AppNavigation
import com.pepperonas.netmonitor.ui.MainViewModel
import com.pepperonas.netmonitor.ui.theme.NetMonitorTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.checkNotificationPermission()
        if (granted) startMonitorService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeSetting by viewModel.theme.collectAsStateWithLifecycle()
            val darkTheme = when (themeSetting) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            NetMonitorTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        onToggleService = { running ->
                            if (running) stopMonitorService() else requestStartService()
                        }
                    )
                }
            }
        }

        // Auto-start monitoring on app launch (if enabled in settings)
        if (!NetworkMonitorService.isRunning.value) {
            val autoStart = runBlocking {
                (application as NetMonitorApplication).settingsStore.autoStart.first()
            }
            if (autoStart) {
                requestStartService()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startSpeedUpdates()
        viewModel.loadAppTraffic()
        viewModel.checkNotificationPermission()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopSpeedUpdates()
    }

    private fun requestStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startMonitorService()
    }

    private fun startMonitorService() {
        ContextCompat.startForegroundService(
            this, Intent(this, NetworkMonitorService::class.java)
        )
    }

    private fun stopMonitorService() {
        stopService(Intent(this, NetworkMonitorService::class.java))
    }
}
