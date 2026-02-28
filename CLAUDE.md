# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Deploy

```bash
export ANDROID_HOME=~/Library/Android/sdk

./gradlew assembleDebug                    # Debug APK
./gradlew assembleRelease                  # Signed release APK (requires signing env vars or local.properties)
./gradlew test                             # Run unit tests
adb install -r app/build/outputs/apk/debug/app-debug.apk   # Install on connected device
adb shell am start -n com.pepperonas.netmonitor/.MainActivity  # Launch
```

**Signing** (release builds): Set env vars `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Keystore: `netmonitor-release.jks` in `~/My Drive/dev/keystore/netmonitor-keystore/`, alias `netmonitor`. CI uses GitHub Secrets via `.github/workflows/release.yml`.

## Release Process

1. Bump `versionName` and `versionCode` in `app/build.gradle.kts`
2. Update version badge in `README.md` and `CHANGELOG.md`
3. Commit, tag: `git tag v<version> && git push origin v<version>`

The `v*` tag triggers the GitHub Actions workflow to build signed APKs and create a GitHub Release.

## Architecture

Single-module Kotlin/Compose Android app. Package: `com.pepperonas.netmonitor`. No DI framework.

### Database (Room v2)

- `SpeedSample` -- per-second speed measurements (7-day retention)
- `DailyTrafficSummary` -- daily aggregate (365-day retention)
- `SpeedTestResult` -- speed test history (50 most recent)
- Migration v1->v2 adds `speed_test_results` table

### Service state is reactive

`NetworkMonitorService.isRunning` is a `StateFlow<Boolean>` (not a plain var). The ViewModel exposes it as `isServiceRunning`, and MainScreen collects it with `collectAsStateWithLifecycle()`. This ensures the UI toggle button always reflects actual service state.

### Two separate notification channels

Samsung One UI bundles notifications from the same channel. To keep download and upload icons separate in the status bar, they use distinct channels: `net_monitor_download` (ID 1) and `net_monitor_upload` (ID 2). Download is forced left via `setWhen(Long.MAX_VALUE)`.

### Speed format convention

No unit text in status bar icons. The number format encodes the unit:
- Whole number (e.g. `42`) = KB/s
- Number with comma (e.g. `4,2`) = MB/s

Values below 1 KB/s round up to KB. `TrafficMonitor.formatSpeedParts()` returns `FormattedSpeed(value, unit)` with comma as decimal separator for MB/s.

### Icon rendering

`SpeedIconRenderer` is a Kotlin `object` that renders into 96x96 `Bitmap.Config.ALPHA_8` bitmaps (alpha-mask, required by Android notification small icons). Single reused `Paint` with `sans-serif-black` bold, `FILL_AND_STROKE`, auto-fit text sizing up to 88px.

### Auto-start

Monitoring starts automatically in `MainActivity.onCreate()` (not `onResume`), so manually stopping the service won't cause it to restart when returning to the app.

### Navigation

Bottom navigation with 5 tabs: Live, Stats, Speed Test, Apps, Settings. Routes defined in `AppNavigation.kt` `Screen` sealed class.

## Key Conventions

- **i18n** -- All user-facing strings use `stringResource(R.string.*)`. English in `values/strings.xml`, German in `values-de/strings.xml`
- **Update rate**: 1 Hz (1000ms) for both service notifications and in-app speed display
- **Foreground service type**: `specialUse` (required for Android 14+)
- **Compose BOM 2024.01.00** -- use `Divider` not `HorizontalDivider` (newer API not available in this BOM version)
- **Charts** -- Custom Canvas-based (no charting libraries). SpeedGraph for live line chart, HourlyBarChart and DailyBarChart for statistics
- **Settings** -- DataStore Preferences in `SettingsStore.kt`. All settings exposed as `Flow` and collected in ViewModel
