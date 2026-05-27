package com.datecountdown.app.core.design.theme

/**
 * Determines which color scheme the app renders with.
 *
 * This enum lives in `:core:design` as a UI concern (it only affects composables).
 * Persistence (reading/writing the user's preference) is intentionally absent here and
 * arrives in issue #28 (`SettingsRepository`) + issue #44 (settings dialog).
 * `DateCountdownTheme` receives a [ThemeMode] value; the caller is responsible for
 * supplying it — in production that will be the DataStore-backed flow from #28.
 */
enum class ThemeMode {
  /** Follow the system dark-mode setting (`isSystemInDarkTheme()`). */
  SYSTEM,

  /** Always render with the light color scheme regardless of system setting. */
  LIGHT,

  /** Always render with the dark color scheme regardless of system setting. */
  DARK,
}
