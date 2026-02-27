<p align="center">
  <img src="app_icon.png" width="96" alt="NetMonitor Logo" />
</p>

<h1 align="center">NetMonitor</h1>

<p align="center">
  <a href="https://developer.android.com/about/versions/oreo"><img src="https://img.shields.io/badge/minSdk-26-brightgreen" alt="minSdk 26" /></a>
  <a href="https://developer.android.com/about/versions/14"><img src="https://img.shields.io/badge/targetSdk-34-blue" alt="targetSdk 34" /></a>
  <img src="https://img.shields.io/badge/version-0.1.0-orange" alt="Version 0.1.0" />
  <img src="https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="MIT License" /></a>
</p>

<p align="center">
  Real-time network speed monitor for Android with live status bar indicators.
</p>

---

## What it does

NetMonitor shows your current download and upload speeds directly in the Android status bar. Two separate notification icons display the speed values as dynamically rendered bitmap text that updates every second. No clutter, no ads, no background data collection -- just a lightweight foreground service that reads the system's built-in traffic counters.

### Status bar speed display

The app renders the current speed as a number directly into the notification small icon. The format encodes the unit:

- **Whole number** (e.g. `42`) = KB/s
- **Number with comma** (e.g. `4,2`) = MB/s

Two notifications run side by side: **download (left) and upload (right)**. The icons use the heaviest available system font (`sans-serif-black` bold) with thick stroke rendering, auto-scaled to fill the maximum available space in Android's 24dp icon area.

### In-app dashboard

The main screen shows:

- **Speed card** with live download/upload rates, updated at 1 Hz
- **Per-app traffic list** showing all apps sorted by total data usage (download + upload) since the last device reboot, with app icons and separate RX/TX counters
- **One-tap toggle** to start/stop the monitoring service

### How it works

NetMonitor reads Android's `TrafficStats` API, which provides cumulative byte counters for all network interfaces. The `TrafficMonitor` class samples these counters once per second, computes the delta, and calculates the per-second rate using precise nanosecond timing. No root access, no VPN, no packet inspection -- just the standard system API.

The `SpeedIconRenderer` creates 96x96 pixel `ALPHA_8` bitmaps (alpha-mask format, as required by Android notification small icons) with the speed value rendered as text. A single reused `Paint` object with auto-fit sizing ensures the number fills the available space while avoiding garbage collection overhead from the 1 Hz update cycle.

Per-app traffic data is read via `TrafficStats.getUidRxBytes()`/`getUidTxBytes()` using each app's UID. This shows cumulative traffic since the last boot, not per-session.

## Architecture

```
com.pepperonas.netmonitor/
  MainActivity.kt                -- Entry point, notification permission, service toggle
  NetMonitorApplication.kt       -- Application subclass
  model/
    AppTrafficInfo.kt             -- Data class: appName, packageName, uid, rxBytes, txBytes
  service/
    NetworkMonitorService.kt      -- Foreground service with two notifications (DL + UL)
  ui/
    MainScreen.kt                 -- Compose UI: speed card, service toggle, app traffic list
    MainViewModel.kt              -- StateFlows for speed and per-app traffic data
    theme/                        -- Material 3 theming (Color, Theme, Type)
  util/
    TrafficMonitor.kt             -- TrafficStats wrapper, speed calculation, formatting
    SpeedIconRenderer.kt          -- Renders speed value bitmaps for notification icons
```

### Key components

| Component | Responsibility |
|-----------|---------------|
| `TrafficMonitor` | Samples `TrafficStats` byte counters, computes bytes/sec via nanosecond deltas. `formatSpeedParts()` splits the result into value + unit for icon rendering. |
| `SpeedIconRenderer` | Kotlin `object` that creates `Bitmap.Config.ALPHA_8` icons (96x96 px). Auto-scales text size to fit. Uses `sans-serif-black` bold with `FILL_AND_STROKE` for maximum visibility. |
| `NetworkMonitorService` | Foreground service (`specialUse` type). Posts two notifications: download (ID 1, always left) and upload (ID 2, always right). 1 Hz update loop via `Handler`. |
| `MainViewModel` | Exposes `StateFlow<Speed>` and `StateFlow<List<AppTrafficInfo>>`. Speed updates at 1 Hz, app traffic loaded on demand. |

## Build

```bash
./gradlew assembleDebug          # Debug APK
./gradlew installDebug           # Install on connected device via ADB
./gradlew assembleRelease        # Release APK (ProGuard enabled)
```

Requires Android SDK. Set `ANDROID_HOME` or create `local.properties` with `sdk.dir`.

## Tech stack

| Component | Version |
|-----------|---------|
| Kotlin | 1.9.22 |
| Compose BOM | 2024.01.00 |
| Material 3 | via Compose BOM |
| Activity Compose | 1.8.2 |
| Lifecycle | 2.7.0 |
| Core KTX | 1.12.0 |
| AGP | 8.2.0 |
| Gradle | 8.2 |
| minSdk | 26 (Android 8.0) |
| targetSdk | 34 (Android 14) |

No network libraries, no database, no DI framework. Pure Android SDK + Jetpack Compose.

## Permissions

| Permission | Why |
|-----------|-----|
| `ACCESS_NETWORK_STATE` | Read system traffic counters via `TrafficStats` |
| `FOREGROUND_SERVICE` | Keep the monitoring service running in the background |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required since Android 14 for non-standard foreground services |
| `POST_NOTIFICATIONS` | Display speed notifications (runtime permission on Android 13+) |
| `QUERY_ALL_PACKAGES` | Resolve UIDs to app names for the per-app traffic breakdown |

## Author

**Martin Pfeffer** -- [celox.io](https://celox.io) -- [GitHub](https://github.com/pepperonas)

## License

MIT
