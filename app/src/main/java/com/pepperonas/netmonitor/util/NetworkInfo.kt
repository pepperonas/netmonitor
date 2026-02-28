package com.pepperonas.netmonitor.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import java.net.Inet4Address
import java.net.NetworkInterface

data class NetworkDetails(
    val connectionType: String,
    val connectionIcon: String, // "wifi", "mobile", "ethernet", "vpn", "none"
    val wifiSsid: String?,
    val wifiSignalStrength: Int?, // dBm
    val wifiSignalLevel: Int?, // 0-4
    val wifiFrequency: Int?, // MHz
    val wifiLinkSpeed: Int?, // Mbps
    val mobileCarrier: String?,
    val mobileNetworkType: String?,
    val localIp: String?,
    val isVpn: Boolean
)

object NetworkInfoProvider {

    fun getDetails(context: Context): NetworkDetails {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }

        if (network == null || caps == null) {
            return NetworkDetails(
                connectionType = "Nicht verbunden",
                connectionIcon = "none",
                wifiSsid = null, wifiSignalStrength = null, wifiSignalLevel = null,
                wifiFrequency = null, wifiLinkSpeed = null,
                mobileCarrier = null, mobileNetworkType = null,
                localIp = getLocalIpAddress(),
                isVpn = false
            )
        }

        val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> getWifiDetails(context, isVpn)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> getMobileDetails(context, isVpn)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkDetails(
                connectionType = "Ethernet",
                connectionIcon = "ethernet",
                wifiSsid = null, wifiSignalStrength = null, wifiSignalLevel = null,
                wifiFrequency = null, wifiLinkSpeed = null,
                mobileCarrier = null, mobileNetworkType = null,
                localIp = getLocalIpAddress(),
                isVpn = isVpn
            )
            else -> NetworkDetails(
                connectionType = if (isVpn) "VPN" else "Unbekannt",
                connectionIcon = if (isVpn) "vpn" else "none",
                wifiSsid = null, wifiSignalStrength = null, wifiSignalLevel = null,
                wifiFrequency = null, wifiLinkSpeed = null,
                mobileCarrier = null, mobileNetworkType = null,
                localIp = getLocalIpAddress(),
                isVpn = isVpn
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun getWifiDetails(context: Context, isVpn: Boolean): NetworkDetails {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wm.connectionInfo

        val ssid = info?.ssid?.removePrefix("\"")?.removeSuffix("\"")
            ?.takeIf { it != "<unknown ssid>" }
        val rssi = info?.rssi?.takeIf { it != 0 }
        val level = rssi?.let { WifiManager.calculateSignalLevel(it, 5) }
        val freq = info?.frequency?.takeIf { it > 0 }
        val linkSpeed = info?.linkSpeed?.takeIf { it > 0 }

        val band = when {
            freq == null -> null
            freq < 3000 -> "2.4 GHz"
            freq < 6000 -> "5 GHz"
            else -> "6 GHz"
        }

        return NetworkDetails(
            connectionType = "WiFi" + (band?.let { " ($it)" } ?: ""),
            connectionIcon = "wifi",
            wifiSsid = ssid,
            wifiSignalStrength = rssi,
            wifiSignalLevel = level,
            wifiFrequency = freq,
            wifiLinkSpeed = linkSpeed,
            mobileCarrier = null,
            mobileNetworkType = null,
            localIp = getLocalIpAddress(),
            isVpn = isVpn
        )
    }

    private fun getMobileDetails(context: Context, isVpn: Boolean): NetworkDetails {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val carrier = tm.networkOperatorName?.takeIf { it.isNotEmpty() }

        @Suppress("DEPRECATION")
        val netType = when (tm.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA -> "2G"

            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"

            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"

            TelephonyManager.NETWORK_TYPE_NR -> "5G"

            else -> "Mobil"
        }

        return NetworkDetails(
            connectionType = "$netType" + (carrier?.let { " ($it)" } ?: ""),
            connectionIcon = "mobile",
            wifiSsid = null,
            wifiSignalStrength = null,
            wifiSignalLevel = null,
            wifiFrequency = null,
            wifiLinkSpeed = null,
            mobileCarrier = carrier,
            mobileNetworkType = netType,
            localIp = getLocalIpAddress(),
            isVpn = isVpn
        )
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }
}
