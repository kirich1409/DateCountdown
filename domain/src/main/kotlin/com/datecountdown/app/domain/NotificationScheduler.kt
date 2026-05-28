package com.datecountdown.app.domain

/**
 * Schedules and cancels exact alarms for upcoming [Event]s.
 *
 * The implementation lives in `:data` (AlarmManager-backed). This interface is the
 * only boundary the domain and feature modules depend on (AC-NT-1/3/5).
 *
 * ## PII contract (AC-NT-15)
 * Implementations MUST NOT place event titles or any other personally-identifiable
 * information in PendingIntent extras, Intent extras, or logs. Only [EventId.value]
 * (a UUID string) is permitted in scheduling artifacts.
 *
 * ## Past-event contract (AC-NT-4)
 * Implementations MUST silently skip (or cancel) scheduling for events whose
 * [Event.targetDateTime] is in the past. Callers do not need to pre-filter.
 *
 * ## Exact-alarm permission (AC-NT-13)
 * On API 31-33 [schedule] propagates [SecurityException] when the system denied
 * SCHEDULE_EXACT_ALARM. Callers are responsible for surfacing the choice dialog
 * described in AC-NT-13. There is no silent inexact fallback.
 */
interface NotificationScheduler {

  /**
   * Schedules an exact alarm for [event] at [Event.targetDateTime].
   *
   * If an alarm for the same event was previously scheduled, it is replaced
   * ([FLAG_UPDATE_CURRENT] semantics — AC-NT-3/5).
   *
   * If [Event.targetDateTime] is in the past, the call is a no-op (AC-NT-4).
   *
   * @throws SecurityException on API 31-33 when SCHEDULE_EXACT_ALARM has been denied (AC-NT-13).
   */
  suspend fun schedule(event: Event)

  /**
   * Cancels the exact alarm previously scheduled for the event identified by [id].
   *
   * If no alarm was scheduled for [id], the call is a no-op.
   */
  suspend fun cancel(id: EventId)
}
