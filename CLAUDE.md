# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew assembleDebug          # Debug APK
./gradlew installDebug           # Install on connected device via ADB
./gradlew assembleRelease        # Signed release APK (ProGuard + shrinkResources)
```

Requires `ANDROID_HOME` (default: `~/Library/Android/sdk`).

## Release Process

SemVer in `app/build.gradle.kts` (`versionName` + `versionCode`). When releasing:
1. Bump `versionName` and `versionCode` in `app/build.gradle.kts`
2. Update version badge in `README.md`
3. Commit, then tag: `git tag v0.0.2 && git push origin v0.0.2`

The `v*` tag triggers `.github/workflows/release.yml` which builds signed release + debug APKs and uploads them to a GitHub Release.

**Signing**: env vars `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (CI via GitHub Secrets) or `local.properties` (local dev). Keystore: `netmonitor-release.jks`, alias `netmonitor`.

## Architecture

Single-module Kotlin/Compose app, package `com.pepperonas.netmonitor`. No DI framework, no database, no network libraries.

**Data flow**: `TrafficMonitor.sample()` reads `TrafficStats` byte counters, computes delta over nanosecond intervals, returns `Speed(rxBytesPerSec, txBytesPerSec)`. Each consumer (ViewModel, Service) owns its own `TrafficMonitor` instance.

**Two notification icons in status bar**: `NetworkMonitorService` posts two separate notifications (ID 1 = download, ID 2 = upload). Download is forced left via `setWhen(Long.MAX_VALUE)`. `SpeedIconRenderer` renders the speed value into 96x96 `ALPHA_8` bitmaps using `IconCompat.createWithBitmap()`.

**Speed format convention**: Whole number = KB/s, number with comma = MB/s. No unit text in the icon — the format itself encodes the unit. Values below 1 KB round up to KB/s. `TrafficMonitor.formatSpeedParts()` returns `FormattedSpeed(value, unit)` with comma as decimal separator for MB/s.

**SpeedIconRenderer**: Kotlin `object`, reuses a single `Paint` (sans-serif-black bold, FILL_AND_STROKE, 3px stroke). Auto-fits text size (up to 88px) to fill the 96px bitmap width.

## Key Conventions

- **UI language is German** — button labels, notification text, section headers
- **Update rate**: 1 Hz (1000ms) for both service and UI
- **Foreground service type**: `specialUse` (network monitoring)
- **No tests** — project has no test suite currently
