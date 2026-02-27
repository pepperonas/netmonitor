# CLAUDE.md

## Project Overview

NetMonitor is an Android network traffic monitoring app (Kotlin, Jetpack Compose, Material 3). It displays real-time download/upload speeds and per-app data usage via a foreground service with persistent notification.

## Build & Run

```bash
./gradlew assembleDebug          # Debug build
./gradlew installDebug           # Install on connected device via ADB
```

Requires `ANDROID_HOME` to be set (default: `~/Library/Android/sdk`).

## Project Structure

Single-module app (`app/`), package `com.pepperonas.netmonitor`:

- `MainActivity.kt` -- Entry point, notification permission, service toggle
- `service/NetworkMonitorService.kt` -- Foreground service, 500ms polling loop, notification updates
- `ui/MainScreen.kt` -- Compose UI with speed card + per-app traffic list
- `ui/MainViewModel.kt` -- StateFlows for speed and app traffic data
- `model/AppTrafficInfo.kt` -- Data class for per-app traffic
- `util/TrafficMonitor.kt` -- TrafficStats wrapper, speed calculation, formatting (`formatSpeed`, `formatSpeedParts`, `formatBytes`)
- `util/SpeedIconRenderer.kt` -- Generates 96x96 ALPHA_8 bitmap icons for notification small icons

## Key Patterns

- **No DI framework** -- Manual wiring, `NetMonitorApplication` is minimal
- **UI language is German** -- Notification channel name, button labels, section headers
- **Foreground service type**: `specialUse` (network monitoring)
- **Update rate**: 500ms (2 Hz) for both service notification and UI speed display
- **TrafficMonitor** is instantiated per consumer (ViewModel + Service each have their own)

## Dependencies

Compose BOM 2024.01.00, Activity Compose 1.8.2, Lifecycle 2.7.0, Core KTX 1.12.0. No Room, no network libraries, no DI.

## Versioning

`versionName` and `versionCode` in `app/build.gradle.kts`. Currently 1.0 / 1.
