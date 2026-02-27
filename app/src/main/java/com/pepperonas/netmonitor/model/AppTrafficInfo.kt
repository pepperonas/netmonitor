package com.pepperonas.netmonitor.model

data class AppTrafficInfo(
    val appName: String,
    val packageName: String,
    val uid: Int,
    val rxBytes: Long,
    val txBytes: Long
)
