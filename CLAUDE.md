# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Multi-module Android app. Use the Gradle wrapper. `build-logic` is an `includedBuild` — Gradle
resolves it automatically; no separate build step is required.

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Build release APK (R8 enabled) | `./gradlew assembleRelease` |
| Install on device/emulator | `./gradlew installDebug` |
| All unit tests | `./gradlew testDebugUnitTest` |
| Unit tests — single module | `./gradlew :domain:test` or `./gradlew :feature:list:testDebugUnitTest` |
| Single test class | `./gradlew :domain:test --tests "com.datecountdown.app.domain.CountdownCalculatorTest"` |
| Instrumented / Compose UI tests (needs device) | `./gradlew connectedDebugAndroidTest` |
| Android Lint | `./gradlew lintDebug` |
| Detekt (static analysis + Compose rules) | `./gradlew detekt` |

Configuration cache and build cache are on by default (`gradle.properties`).

## Toolchain

- **AGP 9.0.1**, **Kotlin 2.3.20**, **KSP 2.3.9**, JDK 17 toolchain
- `minSdk 29`, `compileSdk`/`targetSdk 36`
- Gradle wrapper 9.1.0

**AGP-9 built-in-Kotlin gotcha (important):** AGP 9 ships a bundled Kotlin Gradle Plugin. Without
a classpath override you may silently pick up the wrong KGP version, which breaks KSP and Metro.
The fix is a `buildscript { dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20") } }`
block in the root `build.gradle.kts`. Do NOT set `android.kotlinOptions.builtInKotlin=false` — the
classpath upgrade is the correct mechanism. See `docs/spikes/1.0-stack-compat.md` for detail.

Dependencies are managed via the version catalog `gradle/libs.versions.toml` — add/bump versions
there, never inline in `build.gradle.kts`. Compose artifacts are declared without versions and
inherit from `androidx-compose-bom`.

Key library versions (pinned in `libs.versions.toml`):

| Library | Version |
|---------|---------|
| Decompose | 3.5.0 |
| MVIKotlin | 4.4.0 |
| Metro | 1.1.1 |
| Room | 2.8.4 |
| DataStore | 1.2.1 |
| kotlinx-datetime | 0.6.2 |
| Turbine (test) | 1.2.1 |
| detekt | 1.23.8 |
| Compose rules | 0.5.9 |

## Architecture

9-module multi-module Android app. See `docs/architecture/module-structure.md` for the full
contract; the summary below covers what you need day-to-day.

### Module structure

| Module | Convention plugin(s) | Role |
|--------|---------------------|------|
| `:app` | `android.application` + `compose` + serialization | Composition root: DI graph, navigation, receivers |
| `:feature:list` | `android.feature` | Event list screen |
| `:feature:counter` | `android.feature` | Full-screen countdown/countup |
| `:feature:edit` | `android.feature` | Add/edit bottom sheet |
| `:core:design` | `android.library` + `compose` | Design system (theme, palettes, icons, shapes) |
| `:core:common` | `android.library` | Shared utilities, `ClockProvider` |
| `:data` | `android.library` + KSP | Room DB, DataStore, repository impls, `NotificationScheduler` |
| `:domain` | `kotlin.library` | Pure Kotlin: domain model, use-cases, repository interfaces |
| `build-logic` | includedBuild | Convention plugins |

**Dependency rules (enforced by module boundaries):**

- `:app` → `:feature:*`, `:domain`, `:data`, `:core:*`
- `:feature:*` → `:domain`, `:core:design`, `:core:common` — NO feature→feature deps
- `:data` → `:domain` (implements its repository interfaces)
- `:core:design`, `:core:common` → nothing app-specific (leaf modules)
- `:domain` → nothing Android-specific (pure Kotlin/JVM — enforced by `kotlin.library` plugin)
- Metro DI graph assembly lives ONLY in `:app` — features receive dependencies via constructor, never via `@Inject` or graph lookup

### Navigation — Decompose

Navigation uses **Decompose 3.5.0** (`ChildStack` + `ChildSlot`), not Navigation-Compose or Nav3.

- `RootComponent` lives in `:app` (`app/src/main/java/com/datecountdown/app/navigation/RootComponent.kt`).
  It holds:
  - `ChildStack<Config, Child>` for the primary back-stack (List ↔ Counter).
  - `ChildSlot<EditConfig, EditChild>` for the edit bottom sheet overlay.
- Navigation configs are `@Serializable` sealed classes (kotlinx.serialization required for
  back-stack state persistence across process death).
- Navigate by calling stack/slot navigation functions directly (`pushToFront`, `pop`, `activate`,
  `dismiss`) — there is no `NavController` or `NavHost`.
- **Output pattern:** feature components emit `Output` sealed classes (e.g.
  `EventListComponent.Output.OpenCounter(id)`). `RootComponent` subscribes to these and translates
  them into navigation actions. Features never navigate directly or depend on each other.
- System back is handled via `BackCallback` registered with `BackHandler`; the Decompose back
  handler manages the stack automatically for stack-level presses.
- Store state is retained across configuration changes via MVIKotlin's `InstanceKeeper` integration.

### State management — MVIKotlin

Each feature screen uses an **MVIKotlin 4.4.0** `Store` for state. The feature template:

1. `FooComponent` interface — declares the component contract and the `Output` sealed class.
2. `DefaultFooComponent(componentContext: ComponentContext, …, output: (Output) -> Unit)` —
   Decompose component implementation; creates and retains the Store via `InstanceKeeper`.
3. `FooStore` (MVIKotlin) — holds state, processes intents, produces labels.
4. Compose screen receives state as a plain value (no ViewModel, no `collectAsStateWithLifecycle`
   over a ViewModel — subscribe to Decompose `Value<State>` instead).

### Dependency injection — Metro

**Metro 1.1.1** is the DI framework. Key rules:

- The root `@DependencyGraph` is defined in `:app` (`AppGraph`). It is the ONLY place where
  the object graph is assembled.
- Features are plain classes; they do NOT use `@Inject` or reference the graph. Dependencies
  are passed via constructor when `RootComponent` creates child components.
- `MainActivity` calls `createGraph<AppGraph>()`, gets `RootComponent` from the graph, and
  passes `defaultComponentContext()` (Decompose) to it.

### Data layer

- **Room 2.8.4** (standard Android Room, not KMP-Room) in `:data`: `EventEntity`, DAO, DB,
  mapper (`Long` epoch-millis ↔ `Instant`).
- **DataStore Preferences 1.2.1** in `:data`: theme settings, UI flags.
- **Repository interfaces** live in `:domain`; `*RepositoryImpl` lives in `:data`.
- `:domain` is pure Kotlin — no Android types, no Room, no DataStore.

## Design system (`:core:design`)

All design primitives live in `com.datecountdown.app.core.design.theme`. Wrap every screen and
preview in `DateCountdownTheme`.

- **Theme:** Material 3 with dynamic color (Android 12+ / API 31+); design-palette light/dark as
  fallback. `ThemeMode` enum controls light/dark/system.
- **EventPalette:** 9 fixed tonal palettes (`container`/`onContainer`/`hero`/`onHero`), indexed
  by ordinal. `:core:design` does NOT depend on `:domain`. The `EventColor → EventPalette` mapping
  is done in feature modules (`:core:design` is a leaf).
- **EventIcon / EventSymbol:** 16 Material Symbols Rounded icons identified by `EventIcon` enum.
  Each carries a `codepoint` (Unicode, from the bundled `material_symbols_rounded.ttf` variable
  font in `res/font`). Render with `EventSymbol(icon, size, tint, contentDescription)` — no
  network requests, no ligature substitution.
- **Fonts:** Roboto Flex (variable) + Material Symbols Rounded are **bundled in the APK** (`res/font`).
  The app makes no network requests for fonts.
- **BlobShape:** `RoundedPolygon`-based custom shape used for hero backgrounds and empty-state art.
- **GlassSurface:** frosted-glass composable using `RenderEffect` blur (API 31+) with a fallback
  for older devices.

## Conventions

- **Accessibility is a standing rule for ALL UI code, not a feature.** Every screen/component/state
  must satisfy [`docs/conventions/accessibility.md`](docs/conventions/accessibility.md) —
  `contentDescription`, touch targets ≥48dp, contrast, `fontScale` support, focus order, non-color
  selection signals, screen-reader announcements. Treated as a blocker in review. Apply from the
  first commit.
- **2-space indentation** throughout Kotlin sources (not the IntelliJ default of 4) — match it.
- **detekt** is the static analysis tool (not ktlint). Compose rules from
  `io.nlopez.compose.rules:detekt` are active. Key suppressions that are project-idiomatic:
  - `FunctionNaming` ignores `@Composable` (PascalCase is the Compose convention).
  - `MagicNumber` ignores `@Composable` (hex color literals like `Color(0xFF…)` are idiomatic).
  - Hex color constants at file scope use `@file:Suppress("MagicNumber")`.
- `buildConfig`, `aidl`, `shaders` are disabled in `buildFeatures` (set by convention plugins).
  Do not rely on `BuildConfig`.
- **Release builds:** R8 minification and resource shrinking are enabled. `allowBackup=false` is
  set in the manifest (events are not backed up to cloud). The app declares no `INTERNET`
  permission — all data is local.
- Unit tests use JUnit4 + `kotlinx-coroutines-test` (`runTest`) + Turbine for Flow assertions.
  Compose UI tests use `createAndroidComposeRule<ComponentActivity>()`.

## Sources of truth

- Architecture contract: `docs/architecture/module-structure.md`
- Stack verification (Decompose + Metro + KSP compatibility, AGP-9 gotchas): `docs/spikes/1.0-stack-compat.md`
- Feature specs: `docs/specs/datecountdown-mvp/` (01 data model, 02 counter logic, 04 list, 05 add/edit, 06 navigation, 07 notifications, 08 theme)
- Accessibility rules: `docs/conventions/accessibility.md`
