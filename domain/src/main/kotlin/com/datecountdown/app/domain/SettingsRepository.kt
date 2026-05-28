package com.datecountdown.app.domain

import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to user preferences.
 *
 * The implementation lives in `:data` (DataStore Preferences-backed).
 * This interface is the only boundary that domain and feature modules depend on (AC-DM-9/10/11).
 */
interface SettingsRepository {

  /**
   * Emits the current [ThemeMode] and re-emits whenever it changes (AC-DM-9).
   * Defaults to [ThemeMode.SYSTEM] when no preference has been stored.
   */
  val themeMode: Flow<ThemeMode>

  /** Persists the chosen [mode] (AC-DM-9). */
  suspend fun setThemeMode(mode: ThemeMode)

  /**
   * Emits whether the "past events" section is collapsed and re-emits on change (AC-DM-10).
   * Defaults to `false` when no preference has been stored.
   */
  val pastCollapsed: Flow<Boolean>

  /** Persists the [value] for the "past events" collapsed state (AC-DM-10). */
  suspend fun setPastCollapsed(value: Boolean)
}
