# NetMonitor

Android app that monitors network traffic in real time. Shows current download/upload speeds and per-app data usage since device boot.

## Features

- **Live speed display** -- Download and upload speeds updated at 2 Hz
- **Per-app traffic breakdown** -- Lists all apps sorted by total data usage (rx + tx)
- **Foreground service** -- Persistent notification with current speeds, keeps monitoring in background
- **Dynamic notification icons** -- `SpeedIconRenderer` generates bitmap icons showing speed values directly in the notification area
- **Material 3 / Compose UI** -- Dynamic color support on Android 12+

## Screenshots

*TODO*

## Architecture

```
com.pepperonas.netmonitor/
  MainActivity.kt              -- Entry point, permission handling, service toggle
  NetMonitorApplication.kt     -- Application subclass
  model/
    AppTrafficInfo.kt           -- Data class for per-app traffic stats
  service/
    NetworkMonitorService.kt    -- Foreground service, polls TrafficMonitor at 500ms
  ui/
    MainScreen.kt               -- Compose UI (speed card, app traffic list)
    MainViewModel.kt            -- ViewModel with speed + app traffic StateFlows
    theme/                      -- Material 3 theming (Color, Theme, Type)
  util/
    TrafficMonitor.kt           -- Wraps TrafficStats, calculates bytes/sec deltas
    SpeedIconRenderer.kt        -- Renders 96x96 ALPHA_8 bitmaps for notification icons
```

### Key Components

**TrafficMonitor** -- Samples `TrafficStats.getTotalRx/TxBytes()` and computes per-second rates via nanosecond deltas. Companion provides `formatSpeed()` (human-readable string) and `formatSpeedParts()` (split value/unit for icon rendering).

**SpeedIconRenderer** -- Kotlin `object` that creates `Bitmap.Config.ALPHA_8` icons (96x96 px) suitable as notification small icons. Supports download-only, upload-only, and combined layouts. Reuses `Paint` objects for GC efficiency at high update rates.

**NetworkMonitorService** -- Foreground service (`specialUse` type) with a 500ms update loop. Shows download/upload speeds in the notification title. Stop action via notification button.

## Build

```bash
./gradlew assembleDebug
./gradlew installDebug    # Install on connected device
```

## Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 1.9.22 |
| Compose BOM | 2024.01.00 |
| AGP | 8.2.0 |
| Gradle | 8.2 |
| minSdk | 26 (Android 8.0) |
| targetSdk | 34 (Android 14) |

## Permissions

| Permission | Purpose |
|-----------|---------|
| `ACCESS_NETWORK_STATE` | Read network traffic counters |
| `FOREGROUND_SERVICE` | Run persistent monitoring service |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required since Android 14 |
| `POST_NOTIFICATIONS` | Show notification (runtime permission on Android 13+) |
| `QUERY_ALL_PACKAGES` | Read per-app traffic via UID lookup |

## License

MIT
