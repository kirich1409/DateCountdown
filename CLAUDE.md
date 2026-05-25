# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Single-module Android app (`:app`). Use the Gradle wrapper.

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Install on device/emulator | `./gradlew installDebug` |
| Unit tests (JVM) | `./gradlew testDebugUnitTest` |
| Single unit test class | `./gradlew testDebugUnitTest --tests "com.example.datecountdown.ui.main.MainScreenViewModelTest"` |
| Single unit test method | `./gradlew testDebugUnitTest --tests "com.example.datecountdown.ui.main.MainScreenViewModelTest.uiState_initiallyLoading"` |
| Instrumented / Compose UI tests (needs device) | `./gradlew connectedDebugAndroidTest` |
| Android Lint | `./gradlew lintDebug` |

No ktlint/detekt is configured — only Android Lint. Configuration cache and build cache are on by default (`gradle.properties`).

## Toolchain

AGP 9.0.1, Kotlin 2.3.20, JDK 17 toolchain. `minSdk 29`, `compile/targetSdk 36`. Dependencies are managed via the version catalog `gradle/libs.versions.toml` — add/bump versions there, not inline in `build.gradle.kts`. Compose is pulled through the BOM (`androidx-compose-bom`); individual Compose artifacts are declared without versions and inherit from the BOM.

## Architecture

UI-only app: single Activity, Compose, MVVM. There is **no DI framework** — dependencies are wired manually (see below).

**Navigation uses Navigation 3 (`androidx.navigation3`), not the classic Navigation-Compose.** This is the key thing that diverges from most examples and training data:
- Destinations are `NavKey` objects (`NavigationKeys.kt`), declared as `@Serializable data object`/`data class` (kotlinx.serialization is required for back-stack persistence).
- `MainNavigation()` (`Navigation.kt`) holds a back stack via `rememberNavBackStack(...)` and renders it with `NavDisplay` + an `entryProvider { entry<Key> { ... } }` block. Navigate by mutating the back stack directly: `backStack.add(key)` / `backStack.removeLastOrNull()`. There is no `NavController`/`NavHost`.
- Screens receive an `onItemClick: (NavKey) -> Unit` callback rather than a nav controller, keeping them navigation-agnostic.

**Screen structure (the `ui/main` package is the template to follow for new screens):**
- `MainScreenViewModel` exposes a single `StateFlow<MainScreenUiState>`. The repository's `Flow` is mapped to a sealed UI state and exposed via `stateIn(viewModelScope, WhileSubscribed(5000), Loading)`. State is a sealed interface with `Loading` / `Success(data)` / `Error(throwable)` — `.catch { emit(Error(it)) }` converts stream failures into the error state.
- The composable is split in two: a **stateful** `MainScreen(onItemClick, ...)` that obtains the ViewModel, collects state via `collectAsStateWithLifecycle()`, and `when`-dispatches on the UI state; and a **stateless** `internal MainScreen(data, ...)` that only renders. Previews and UI tests target the stateless overload.
- ViewModels are created with the Compose `viewModel { MainScreenViewModel(DefaultDataRepository()) }` factory — the dependency is constructed inline in the default parameter value. To swap a dependency in a test or preview, pass a different `viewModel`/instance through the parameter.

**Data layer:** repositories are interfaces (`DataRepository`) with a `Default…` implementation, exposing data as `Flow`. Tests substitute a `Fake…` implementation of the interface.

## Conventions

- **Accessibility is a standing rule for ALL UI code, not a feature.** Every screen/component/state must
  satisfy [`docs/conventions/accessibility.md`](docs/conventions/accessibility.md) — contentDescription,
  touch targets ≥48dp, contrast, fontScale support, focus order, non-color selection signals, screen-reader
  announcements. Treated as a blocker in review, like style. Apply from the first commit.
- **2-space indentation** throughout Kotlin sources (not the IntelliJ default of 4) — match it.
- `buildConfig`, `aidl`, `shaders` are disabled in `buildFeatures`; only `compose` is on. Don't rely on `BuildConfig`.
- Theme lives in the `theme/` package (`DateCountdownTheme`, `Color.kt`, `Type.kt`); wrap new previews/screens in `DateCountdownTheme`.
- Unit tests use JUnit4 + `kotlinx-coroutines-test` (`runTest`). Compose UI tests use `createAndroidComposeRule<ComponentActivity>()` and feed the stateless composable fake data.
