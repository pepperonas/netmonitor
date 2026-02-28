# Changelog

## v0.4.0

### New features
- **Speed Test** -- HTTP download/upload speed test with animated gauge, latency measurement, and result history
- **Data Budget** -- Monthly data limit with configurable warning threshold and color-coded progress indicator
- **Historical Statistics** -- Daily, weekly, and monthly traffic views with bar charts and peak values
- **Home Screen Widget** -- Glance-based widget showing current speeds and total traffic
- **Quick Settings Tile** -- Toggle monitoring from the Android Quick Settings panel
- **Network Info Card** -- Shows connection type, WiFi details (SSID, signal, link speed), mobile carrier, IP address, VPN status
- **Settings Screen** -- Theme (system/light/dark), update rate, auto-start, notification style, data budget, graph time window
- **Live Speed Graph** -- Canvas-based line chart with configurable time window (30s to 5min)
- **Per-App Traffic** -- Traffic breakdown by app with icons, sorted by total usage
- **Internationalization** -- Full English and German translations

### Architecture
- Room database with per-second samples and daily summaries
- DataStore Preferences for all settings
- Bottom navigation with 5 tabs (Live, Stats, Test, Apps, Settings)
- Database migration v1 to v2 for speed test results

## v0.3.0
- Fix service toggle state synchronization
- Polish UI elements

## v0.2.0
- Auto-start monitoring on app launch
- Improved UI with state-aware toggle button

## v0.1.0
- Separate notification channels for download and upload
- About section with developer info
- Version number in top app bar

## v0.0.1
- Initial release
- Real-time download/upload speed in status bar
- Foreground service with dual notifications
- Per-app traffic overview
