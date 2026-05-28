package com.datecountdown.app.domain

/**
 * Color-scheme preference for the app.
 *
 * **Ordinal contract** — the ordinal of each entry is a stable index into
 * `:core:design`'s `ThemeMode` enum (which is the UI-layer concern used by
 * `DateCountdownTheme`). Feature modules map between the two enums by ordinal:
 *
 * ```kotlin
 * val designTheme = com.datecountdown.app.core.design.theme.ThemeMode.entries[themeMode.ordinal]
 * ```
 *
 * Entry order is fixed:
 *   0=SYSTEM, 1=LIGHT, 2=DARK.
 *
 * **Do not reorder entries** — doing so breaks the theme mapping in all feature modules.
 * **Do not rename entries** — `SettingsRepositoryImpl` persists names via [ThemeMode.name]
 * and reconstructs them via [ThemeMode.valueOf]; renames would corrupt stored preferences.
 */
enum class ThemeMode {
  /** Follow the system dark-mode setting. */
  SYSTEM,

  /** Always render with the light color scheme. */
  LIGHT,

  /** Always render with the dark color scheme. */
  DARK,
}
