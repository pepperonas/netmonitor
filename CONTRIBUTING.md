# Contributing to NetMonitor

Thanks for your interest in contributing to NetMonitor! This document provides guidelines for contributing to the project.

## Getting started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/<your-username>/netmonitor.git`
3. Create a feature branch: `git checkout -b feature/your-feature`
4. Make your changes
5. Test your changes: `./gradlew assembleDebug && ./gradlew test`
6. Commit and push: `git push origin feature/your-feature`
7. Open a Pull Request

## Development setup

- Android Studio Hedgehog (2023.1) or later
- JDK 17
- Android SDK with API 34

```bash
export ANDROID_HOME=~/Library/Android/sdk
./gradlew assembleDebug
```

## Code style

- Kotlin with Jetpack Compose
- Follow existing patterns in the codebase
- Use `MaterialTheme` tokens for all colors/typography (no hardcoded values)
- Use `stringResource()` for all user-facing text (see `res/values/strings.xml`)
- German translations go in `res/values-de/strings.xml`

## Architecture guidelines

- Single-module app, no DI framework
- `ViewModel` + `StateFlow` for reactive UI state
- Room for persistence, DataStore for preferences
- Foreground service for monitoring
- Canvas-based custom charts (no charting libraries)

## What to contribute

- Bug fixes
- Performance improvements
- New language translations
- UI/UX improvements
- Test coverage improvements
- Documentation improvements

## Pull request process

1. Update the README.md if your change adds user-facing features
2. Add translations to both `values/strings.xml` and `values-de/strings.xml`
3. Ensure the debug build compiles: `./gradlew assembleDebug`
4. Run tests: `./gradlew test`
5. Keep PRs focused -- one feature or fix per PR

## Reporting bugs

Open an issue with:
- Device model and Android version
- Steps to reproduce
- Expected vs. actual behavior
- Logcat output if applicable

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
