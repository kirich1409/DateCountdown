package com.datecountdown.app.core.design.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The dark-theme flag **resolved from the app's [ThemeMode]**, not the raw system setting.
 *
 * Provided by [DateCountdownTheme] (the only provider). Downstream event-palette lookups
 * (`eventPaletteByIndex(..., dark = LocalResolvedDarkTheme.current)`) must read this instead of
 * `isSystemInDarkTheme()`, otherwise palettes render in the wrong appearance when the user forces
 * LIGHT/DARK while the system is in the opposite mode (issue #169).
 *
 * `true`  → render dark-variant event palettes.
 * `false` → render light-variant event palettes.
 *
 * ## Default
 * The default is `false` (light). It is a **fallback for composables rendered outside
 * [DateCountdownTheme]** — chiefly `@Preview`s that are not wrapped in the theme. Inside the app
 * this default is never observed: `MainActivity` wraps the whole tree in [DateCountdownTheme], which
 * always provides the resolved value. See issue #169.
 *
 * ## Why `staticCompositionLocalOf`
 * The resolved flag changes only on a theme switch or a uiMode config change — rarely — and is read
 * by many composables across all feature screens. `staticCompositionLocalOf` skips per-read
 * invalidation tracking; when the value does change, the whole subtree below the provider
 * recomposes, which is exactly the desired behavior on a theme flip. (This differs from
 * LocalNotificationPermissionState, which uses `compositionLocalOf` — its callback + banner flag
 * can change while we want only the few real consumers to recompose. Read/change-frequency drives
 * the choice.)
 */
val LocalResolvedDarkTheme = staticCompositionLocalOf { false }
