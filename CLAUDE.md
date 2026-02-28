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
3. Commit, tag: `git tag v<version> && git push origin v<version>`

The `v*` tag triggers `.github/workflows/release.yml` to build signed APKs and create a GitHub Release.

## Architecture

Single-module Kotlin/Compose app. Package: `com.pepperonas.netmonitor`. No DI framework.

### Data flow

```
TrafficStats (system API)
  → TrafficMonitor.sample() [1 Hz]
  → NetworkMonitorService (foreground, Handler-based loop)
      ├→ SpeedIconRenderer → dual notifications (ALPHA_8 bitmaps)
      └→ TrafficRepository.recordSample()
           ├→ SpeedSampleDao (per-second, 7-day retention)
           └→ DailyTrafficDao (daily aggregate, 365-day retention)
  → MainViewModel (StateFlow)
      → Compose UI (collectAsStateWithLifecycle)
```

`NetMonitorApplication` holds lazy singletons: `database`, `repository`, `settingsStore`. ViewModel accesses them via `(application as NetMonitorApplication).repository`.

### Database (Room, version 2)

Three entities: `SpeedSample`, `DailyTrafficSummary`, `SpeedTestResult`. Migration v1→v2 adds `speed_test_results` table. Database file: `netmonitor.db`, `exportSchema = false`.

**Retention:** `TrafficRepository.cleanup()` deletes speed samples >7 days, daily summaries >365 days, keeps last 50 speed test results.

### Service & notifications

`NetworkMonitorService` uses a `Handler` loop (not coroutines) for 1 Hz updates. Two separate notification channels (`net_monitor_download` ID 1, `net_monitor_upload` ID 2) because Samsung One UI bundles same-channel notifications. Download forced left via `setWhen(Long.MAX_VALUE)` + `setSortKey("a")`.

Service state is a companion `StateFlow<Boolean>`, not a var. The ViewModel exposes it directly so the toggle button is always in sync even when the service is stopped externally.

Auto-start is in `onCreate` (not `onResume`) so manually stopping the service via notification won't restart it when returning to the app.

### Speed format convention

Status bar icons show only a number — the format encodes the unit:
- Whole number (`42`) = KB/s
- Comma decimal (`4,2`) = MB/s (German locale, comma separator)

`SpeedIconRenderer` renders into 96×96 `ALPHA_8` bitmaps (Android notification requirement). Single reused `Paint`, auto-fit text sizing.

### Navigation

5 tabs via `AppNavigation.kt`: Live (`live`), Stats (`stats`), Speed Test (`speedtest`), Apps (`apps`), Settings (`settings`). Each tab defined as `Screen` sealed class with route, `@StringRes` title, icon. State saved/restored across tab switches.

## Key Conventions

- **i18n** — All user-facing strings via `stringResource(R.string.*)`. English default in `values/strings.xml`, German in `values-de/strings.xml`. Always add to both.
- **Compose BOM 2024.01.00** — Use `Divider` not `HorizontalDivider` (newer API not in this BOM).
- **Charts** — Custom Canvas-based composables, no charting libraries. `SpeedGraph` (line), `HourlyBarChart`/`DailyBarChart` (bars).
- **Settings** — `SettingsStore` wraps DataStore Preferences. All settings are `Flow`, collected in ViewModel via `stateIn(WhileSubscribed(5000))`.
- **Speed test** — `SpeedTestEngine` uses `HttpURLConnection` against public endpoints (Hetzner, OVH, Tele2). 10s download, 8s upload, 5× latency pings. Progress via `StateFlow<Progress>`.
- **Foreground service type** — `specialUse` (required for Android 14+).
- **WiFi info** — Requires `ACCESS_WIFI_STATE` permission. `NetworkInfoProvider.getWifiDetails()` is wrapped in try-catch for SecurityException fallback.
- **TrafficStats values are cumulative** — The monitor computes deltas per second. Per-app traffic via `getUidRxBytes(uid)` resets on reboot.
