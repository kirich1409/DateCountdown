package com.datecountdown.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.datecountdown.app.domain.SettingsRepository
import com.datecountdown.app.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore Preferences-backed implementation of [SettingsRepository] (AC-DM-9/10/11).
 *
 * Takes [DataStore] directly — constructed and scoped as a singleton in `AppGraph` — so
 * the store is never rebuilt per injection.
 *
 * Dispatcher: DataStore's internal coroutine machinery dispatches IO off the main thread
 * automatically; no explicit [kotlinx.coroutines.withContext] needed here.
 *
 * Visibility: public to satisfy the Metro cross-module provider in `:app` (same reason as
 * [EventsRepositoryImpl] — AppGraph constructs the impl directly and requires visibility).
 */
class SettingsRepositoryImpl(
  private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

  override val themeMode: Flow<ThemeMode> =
    dataStore.data.map { prefs ->
      prefs[KEY_THEME_MODE]?.let(ThemeMode::valueOf) ?: ThemeMode.SYSTEM
    }

  override suspend fun setThemeMode(mode: ThemeMode) {
    dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.name }
  }

  override val pastCollapsed: Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[KEY_PAST_COLLAPSED] ?: false }

  override suspend fun setPastCollapsed(value: Boolean) {
    dataStore.edit { prefs -> prefs[KEY_PAST_COLLAPSED] = value }
  }

  override val notificationsPermissionRequested: Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[KEY_NOTIFICATIONS_PERM_REQUESTED] ?: false }

  override suspend fun setNotificationsPermissionRequested() {
    dataStore.edit { prefs -> prefs[KEY_NOTIFICATIONS_PERM_REQUESTED] = true }
  }

  private companion object {
    val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    val KEY_PAST_COLLAPSED = booleanPreferencesKey("past_collapsed")
    val KEY_NOTIFICATIONS_PERM_REQUESTED = booleanPreferencesKey("notifications_permission_requested")
  }
}
