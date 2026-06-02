# DateCountdown

Android app for counting down to / up from dates and events.

> ⚠️ This repository is source-available for reference only. You may read the code, but you may **not** use, copy, modify, or distribute it. See [LICENSE](LICENSE).

## Features

- Event list with color palettes and custom icons
- Full-screen countdown and countup display for any event
- Light/dark/system theme with Material 3 dynamic color (Android 12+) and 9 fixed tonal palettes
- Reminders and notifications for upcoming events
- Add and edit events via a bottom sheet

## Tech stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.3.20 |
| Jetpack Compose | BOM-managed |
| Decompose | 3.5.0 |
| MVIKotlin | 4.4.0 |
| Metro DI | 1.1.1 |
| Room | 2.8.4 |
| DataStore | 1.2.1 |
| kotlinx-datetime | 0.6.2 |
| AGP | 9.0.1 |

- `minSdk 29`, `compileSdk`/`targetSdk 36`, JDK 17 toolchain

## Architecture

9-module multi-module Android app; feature modules depend only on `:domain` and `:core:*`, with no feature-to-feature dependencies; the Metro DI graph is assembled exclusively in `:app`. See [docs/architecture/module-structure.md](docs/architecture/module-structure.md) for the full contract.

## Building

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Build release APK | `./gradlew assembleRelease` |
| Unit tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug` |
| Detekt | `./gradlew detekt` |

## Privacy

Privacy policy: https://kirich1409.github.io/DateCountdown/privacy-policy.html

## License

Source-available, all rights reserved. See [LICENSE](LICENSE).
