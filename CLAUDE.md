# CLAUDE.md

## Project Overview

NetMonitor is an Android network speed monitor (Kotlin, Jetpack Compose, Material 3). It displays real-time download/upload speeds as dynamic bitmap icons in the status bar via two foreground service notifications, plus a per-app traffic breakdown in the main UI.

## Build & Run

```bash
./gradlew assembleDebug          # Debug build
./gradlew installDebug           # Install on connected device via ADB
./gradlew assembleRelease        # Release build (ProGuard enabled)
```

Requires `ANDROID_HOME` (default: `~/Library/Android/sdk`).

## Project Structure

Single-module app (`app/`), package `com.pepperonas.netmonitor`:

- `MainActivity.kt` -- Entry point, notification permission handling, service toggle
- `NetMonitorApplication.kt` -- Application subclass (minimal)
- `service/NetworkMonitorService.kt` -- Foreground service, two notifications (DL + UL), 1 Hz update loop
- `ui/MainScreen.kt` -- Compose UI: speed card + per-app traffic list + service toggle button
- `ui/MainViewModel.kt` -- StateFlows for speed (1 Hz) and app traffic data
- `model/AppTrafficInfo.kt` -- Data class for per-app traffic (appName, packageName, uid, rxBytes, txBytes)
- `util/TrafficMonitor.kt` -- TrafficStats wrapper, speed calculation, formatting (`formatSpeed`, `formatSpeedParts`, `formatBytes`)
- `util/SpeedIconRenderer.kt` -- Renders 96x96 ALPHA_8 bitmap icons for notification small icons

## Key Patterns

- **No DI framework** -- Manual wiring, minimal Application class
- **UI language is German** -- Notification channel name, button labels, section headers
- **Two notifications** -- ID 1 = download (always left via `setWhen(MAX_VALUE)`), ID 2 = upload
- **Speed format convention** -- Whole number = KB/s, number with comma = MB/s (no unit text in icon)
- **No B/s range** -- Values below 1 KB are rounded up to KB/s (`(bytes + 512) / 1024`)
- **Update rate**: 1 Hz (1000ms) for both service notifications and UI speed display
- **TrafficMonitor** is instantiated per consumer (ViewModel + Service each have their own)
- **SpeedIconRenderer** uses `sans-serif-black` bold + `FILL_AND_STROKE` with auto-fit sizing

## Dependencies

Compose BOM 2024.01.00, Activity Compose 1.8.2, Lifecycle 2.7.0, Core KTX 1.12.0. No Room, no network libraries, no DI.

## Versioning

`versionName` and `versionCode` in `app/build.gradle.kts`. Currently 0.0.1 / 1. SemVer: bump both fields + version badge in README when releasing.
