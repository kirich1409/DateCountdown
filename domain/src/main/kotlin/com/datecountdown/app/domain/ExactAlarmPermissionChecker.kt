package com.datecountdown.app.domain

/**
 * Checks whether the system allows the app to schedule exact alarms.
 *
 * Implemented in `:data` atop [android.app.AlarmManager.canScheduleExactAlarms] (API 31+).
 * On API < 31 returns `true` — exact alarms are allowed without a runtime check on those
 * versions (AC-NT-13).
 *
 * The interface lives in `:domain` (pure Kotlin) so feature modules can depend on it without
 * pulling in Android types. The implementation resides in `:data`.
 */
interface ExactAlarmPermissionChecker {
  /**
   * Returns `true` when the app is allowed to schedule exact alarms; `false` when permission
   * has been denied (API 31+) and the user must be prompted.
   *
   * This is a lightweight synchronous call — safe to invoke on the main thread before save.
   */
  fun canScheduleExactAlarms(): Boolean
}
