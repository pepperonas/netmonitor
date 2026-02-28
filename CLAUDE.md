# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Deploy

```bash
export ANDROID_HOME=~/Library/Android/sdk

./gradlew assembleDebug                    # Debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease                  # Signed release APK (needs signing config)
./gradlew test                             # Unit tests
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.pepperonas.netmonitor/.MainActivity
```

**Signing:** Checks env vars first (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`), falls back to `local.properties`. Keystore: `netmonitor-release.jks` in `~/My Drive/dev/keystore/netmonitor-keystore/`. CI uses base64-encoded keystore in GitHub Secrets.

## Release Process

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
2. Update version badge in `README.md`
3. Build release APK locally, rename to `NetMonitor-v<version>-release.apk` (replaces old APK in repo root)
4. Commit, tag: `git tag v<version> && git push origin v<version>`

The `v*` tag triggers `.github/workflows/release.yml` which decodes the keystore from `KEYSTORE_BASE64` secret, builds signed+debug APKs, and uploads both to a GitHub Release.

## Architecture

Single-module Kotlin/Compose app. Package: `com.pepperonas.netmonitor`. No DI framework — `NetMonitorApplication` holds lazy singletons (`database`, `repository`, `settingsStore`). ViewModel accesses them via `(application as NetMonitorApplication).repository`.

### Data flow

```
TrafficStats (system API)
  → TrafficMonitor.sample() [1 Hz]
  → NetworkMonitorService (foreground, Handler-based loop)
      ├→ SpeedIconRenderer → dual notifications (ALPHA_8 bitmaps)
      ├→ TrafficRepository.recordSample()
      │    ├→ SpeedSampleDao (per-second, 7-day retention)
      │    └→ DailyTrafficDao (daily aggregate, 365-day retention)
      └→ SpeedWidget().updateAll() [every 2s]
  → MainViewModel (StateFlow)
      → Compose UI (collectAsStateWithLifecycle)
```

### Service & notifications

`NetworkMonitorService` uses a `Handler` loop (not coroutines) for 1 Hz updates. Coroutines (`serviceScope`) are used separately for DB writes, settings observation, and widget updates.

Two separate notification channels (`net_monitor_download` ID 1, `net_monitor_upload` ID 2) because Samsung One UI bundles same-channel notifications. Download forced left via `setWhen(Long.MAX_VALUE)` + `setSortKey("a")`.

Service state is a companion `StateFlow<Boolean>`. The notification style setting is observed via `serviceScope.launch { settingsStore.notificationStyle.collect { ... } }` and applied in `updateNotifications()` — values: `"both"`, `"download"`, `"upload"`.

### Settings flow

All settings in `SettingsStore` are `Flow`s, collected in ViewModel via `stateIn(WhileSubscribed(5000))`. The service reads `notificationStyle` reactively. `MainActivity.onCreate()` reads `autoStart` with blocking `runBlocking { settingsStore.autoStart.first() }` before deciding to start the service. Auto-start check is in `onCreate` (not `onResume`) so manually stopping via notification won't restart it on app return.

### Widget

`SpeedWidget` (Glance) reads the latest `SpeedSample` from Room via `SpeedSampleDao.getLatest()`. Only shows speed if the sample is <5s old, otherwise shows 0. The service triggers `SpeedWidget().updateAll()` every 2 seconds via a counter in `persistSample()`.

### Speed test

`SpeedTestEngine` uses `HttpURLConnection` against public endpoints (Hetzner, OVH, Tele2). Three server lists with fallback: `DOWNLOAD_URLS`, `UPLOAD_URLS`, `LATENCY_URLS`. Latency uses GET with `Range: bytes=0-0` (not HEAD), returns median of 5 pings. Returns `null` on total failure (all servers unreachable), setting `Phase.ERROR`. The ViewModel skips DB persistence when result is null. All connections use try/finally for disconnect.

### Database (Room, version 2)

Three entities: `SpeedSample`, `DailyTrafficSummary`, `SpeedTestResult`. Migration v1→v2 adds `speed_test_results` table. `exportSchema = false`.

**Retention:** `TrafficRepository.cleanup()` deletes speed samples >7 days, daily summaries >365 days, keeps last 50 speed test results.

### Speed format convention

Status bar icons show only a number — the format encodes the unit:
- Whole number (`42`) = KB/s
- Comma decimal (`4,2`) = MB/s (German locale, comma separator)

`SpeedIconRenderer` renders into 96×96 `ALPHA_8` bitmaps (Android notification requirement). Single reused `Paint`, auto-fit text sizing.

### Navigation

5 tabs via `AppNavigation.kt`: Live (`live`), Stats (`stats`), Speed Test (`speedtest`), Apps (`apps`), Settings (`settings`). Each tab defined as `Screen` sealed class with route, `@StringRes` title, icon.

## Key Conventions

- **i18n** — All user-facing strings via `stringResource(R.string.*)` (Compose) or `getString(R.string.*)` (Service/TileService). English default in `values/strings.xml`, German in `values-de/strings.xml`. Always add to both.
- **Compose BOM 2024.01.00** — Use `Divider` not `HorizontalDivider` (newer API not in this BOM).
- **Charts** — Custom Canvas-based composables, no charting libraries. `SpeedGraph` (line), `HourlyBarChart`/`DailyBarChart` (bars).
- **Foreground service type** — `specialUse` (required for Android 14+).
- **WiFi info** — Requires `ACCESS_WIFI_STATE`. `NetworkInfoProvider.getWifiDetails()` uses deprecated `WifiManager.getConnectionInfo()` with try-catch for SecurityException.
- **TrafficStats values are cumulative** — The monitor computes deltas per second. Per-app traffic via `getUidRxBytes(uid)` resets on reboot.
- **ProGuard** — Minimal rules in `app/proguard-rules.pro`: keeps Room entities and RoomDatabase subclasses only. Release builds use `proguard-android-optimize.txt` + these rules.
- **Graph time window** — `recentSamples` uses `flatMapLatest` on the `graphWindow` setting Flow so it dynamically re-subscribes when the user changes the time window.
