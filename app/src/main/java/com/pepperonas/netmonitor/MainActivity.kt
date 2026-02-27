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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.pepperonas.netmonitor.service.NetworkMonitorService
import com.pepperonas.netmonitor.ui.MainScreen
import com.pepperonas.netmonitor.ui.MainViewModel
import com.pepperonas.netmonitor.ui.theme.NetMonitorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startMonitorService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onToggleService = { running ->
                            if (running) stopMonitorService() else requestStartService()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startSpeedUpdates()
        viewModel.loadAppTraffic()
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
