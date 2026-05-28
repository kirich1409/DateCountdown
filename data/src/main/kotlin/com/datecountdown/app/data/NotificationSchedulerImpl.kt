package com.datecountdown.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * [AlarmManager]-backed implementation of [NotificationScheduler] (AC-NT-1/3/5/6).
 *
 * ## PendingIntent request code (AC-NT-6)
 * A stable [Int] is derived from [EventId.value] via [String.hashCode]. This guarantees
 * that [cancel] can reconstruct the exact same [PendingIntent] that [schedule] registered,
 * so [AlarmManager.cancel] removes the correct alarm. Hash collisions across different
 * event IDs are theoretically possible but practically negligible for the expected event
 * count (<<1000 events per user).
 *
 * ## AlarmReceiver reference
 * The Intent targets `com.datecountdown.app.notifications.AlarmReceiver` via
 * [ComponentName] (string form) to avoid a compile-time dependency from `:data` on `:app`.
 * A direct `Intent(context, AlarmReceiver::class.java)` would create a `:data → :app`
 * module cycle. The string class name is a stable contract; #45 fills `onReceive`.
 *
 * ## PII contract (AC-NT-15)
 * Only [EventId.value] is placed in the Intent extra `EXTRA_EVENT_ID`. Event titles and
 * all other personally-identifiable fields MUST NOT appear in Intent extras or logs.
 *
 * ## Exact-alarm permission (AC-NT-13)
 * [SecurityException] from [AlarmManager.setExactAndAllowWhileIdle] on API 31-33
 * (denied SCHEDULE_EXACT_ALARM) is NOT caught here. Callers must handle it and present
 * the user with the explicit choice described in AC-NT-13.
 *
 * ## Past-event guard (AC-NT-4)
 * [schedule] is a no-op when [Event.targetDateTime] is in the past at the moment of the
 * call. No alarm is registered; no cancellation is issued.
 */
class NotificationSchedulerImpl(
  private val context: Context,
  private val alarmManager: AlarmManager,
  private val clock: Clock = Clock.System,
) : NotificationScheduler {

  companion object {
    /** Intent extra key carrying the event id. Only the id — no PII (AC-NT-15). */
    const val EXTRA_EVENT_ID = "event_id"

    private const val ALARM_RECEIVER_CLASS = "com.datecountdown.app.notifications.AlarmReceiver"
  }

  override suspend fun schedule(event: Event) = withContext(Dispatchers.Default) {
    val triggerMillis = event.targetDateTime.toEpochMilliseconds()

    // AC-NT-4: skip past events — no alarm should ever fire for a past targetDateTime.
    if (triggerMillis <= clock.now().toEpochMilliseconds()) return@withContext

    val pendingIntent = buildPendingIntent(
      requestCode = event.id.requestCode(),
      eventId = event.id.value,
      flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // SecurityException propagates to caller on API 31-33 when SCHEDULE_EXACT_ALARM denied (AC-NT-13).
    alarmManager.setExactAndAllowWhileIdle(
      AlarmManager.RTC_WAKEUP,
      triggerMillis,
      pendingIntent,
    )
  }

  override suspend fun cancel(id: EventId) = withContext(Dispatchers.Default) {
    val pendingIntent = buildPendingIntent(
      requestCode = id.requestCode(),
      eventId = id.value,
      // FLAG_NO_CREATE is not used: we always recreate to ensure cancel matches the original.
      flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    alarmManager.cancel(pendingIntent)
  }

  private fun buildPendingIntent(
    requestCode: Int,
    eventId: String,
    flags: Int,
  ): PendingIntent {
    val intent = Intent().apply {
      component = ComponentName(context, ALARM_RECEIVER_CLASS)
      // Only the id is placed here — no event title or other PII (AC-NT-15).
      putExtra(EXTRA_EVENT_ID, eventId)
    }
    return PendingIntent.getBroadcast(context, requestCode, intent, flags)
  }

  /**
   * Derives a stable [Int] request code from this [EventId] so the same PendingIntent
   * can be reconstructed in both [schedule] and [cancel] (AC-NT-6).
   */
  private fun EventId.requestCode(): Int = value.hashCode()
}
