package com.datecountdown.app.data

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Unit tests for [NotificationSchedulerImpl] using [org.robolectric.shadows.ShadowAlarmManager].
 *
 * Coverage (AC-NT-1/3/4/5/6/15):
 *  - schedule registers RTC_WAKEUP exact alarm at targetMillis.
 *  - Past events and equal-to-now boundary are no-ops (AC-NT-4).
 *  - cancel removes the alarm; idempotent when called for an unknown id.
 *  - Re-scheduling same id updates the alarm, not duplicates (FLAG_UPDATE_CURRENT).
 *  - Two distinct event ids produce two independent alarms.
 *  - PendingIntent ComponentName targets AlarmReceiver class.
 *  - Extras contain ONLY EXTRA_EVENT_ID — no title, no other PII (AC-NT-15).
 */
// Robolectric 4.14.x maxSdkVersion=35; project targetSdk=36 — pin explicitly to avoid
// "targetSdkVersion > maxSdkVersion" error until Robolectric adds SDK 36 support.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationSchedulerImplTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  private val shadowAlarmManager = Shadows.shadowOf(alarmManager)

  // Clock pinned to a known instant; 100 s ahead is "future", anything ≤ is "past".
  private val clock = FakeClock(initial = Instant.fromEpochMilliseconds(1_700_000_000_000L))
  private val scheduler = NotificationSchedulerImpl(context, alarmManager, clock)

  // ── helpers ───────────────────────────────────────────────────────────────

  private fun event(
    id: String = "evt-1",
    targetMillis: Long = 1_700_000_100_000L, // +100 s — safely in the future
    title: String = "Test Event",
  ): Event = Event(
    id = EventId(id),
    title = title,
    targetDateTime = Instant.fromEpochMilliseconds(targetMillis),
    color = EventColor.ORANGE,
    icon = EventIcon.CELEBRATION,
    createdAt = clock.now(),
  )

  // ── schedule: future event ────────────────────────────────────────────────

  @Test
  fun `schedule registers exactly one RTC_WAKEUP alarm for a future event`() = runTest {
    scheduler.schedule(event())

    val alarms = shadowAlarmManager.scheduledAlarms
    assertEquals(1, alarms.size)
    assertEquals(AlarmManager.RTC_WAKEUP, alarms.first().getType())
  }

  @Test
  fun `schedule sets triggerAtMs to event targetDateTime epoch millis`() = runTest {
    val targetMillis = 1_700_000_100_000L
    scheduler.schedule(event(targetMillis = targetMillis))

    assertEquals(targetMillis, shadowAlarmManager.scheduledAlarms.first().getTriggerAtMs())
  }

  // ── schedule: past-event guard (AC-NT-4) ─────────────────────────────────

  @Test
  fun `schedule is no-op for a past event`() = runTest {
    // targetMillis is 1 ms before clock.now()
    scheduler.schedule(event(targetMillis = clock.now().toEpochMilliseconds() - 1))

    assertTrue("No alarm expected for past event", shadowAlarmManager.scheduledAlarms.isEmpty())
  }

  @Test
  fun `schedule is no-op when targetMillis equals clock now (boundary)`() = runTest {
    // AC-NT-4 guard is <=, so exactly now is also a no-op.
    scheduler.schedule(event(targetMillis = clock.now().toEpochMilliseconds()))

    assertTrue(
      "No alarm expected when targetMillis == now (≤ guard)",
      shadowAlarmManager.scheduledAlarms.isEmpty(),
    )
  }

  // ── cancel ────────────────────────────────────────────────────────────────

  @Test
  fun `cancel removes alarm after it was scheduled`() = runTest {
    scheduler.schedule(event())
    assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

    scheduler.cancel(EventId("evt-1"))

    assertTrue("Alarm must be removed after cancel", shadowAlarmManager.scheduledAlarms.isEmpty())
  }

  @Test
  fun `cancel is idempotent for an id that was never scheduled`() = runTest {
    // Must not throw; ShadowAlarmManager must stay empty.
    scheduler.cancel(EventId("never-scheduled"))

    assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
  }

  // ── re-schedule (FLAG_UPDATE_CURRENT, AC-NT-3/5) ─────────────────────────

  @Test
  fun `re-scheduling the same event id replaces the alarm, not duplicates`() = runTest {
    val originalMillis = 1_700_000_100_000L
    val updatedMillis = 1_700_000_200_000L

    scheduler.schedule(event(targetMillis = originalMillis))
    scheduler.schedule(event(targetMillis = updatedMillis))

    val alarms = shadowAlarmManager.scheduledAlarms
    assertEquals("Exactly one alarm after re-schedule", 1, alarms.size)
    assertEquals("triggerAtMs must reflect the updated time", updatedMillis, alarms.first().getTriggerAtMs())
  }

  // ── two distinct events ───────────────────────────────────────────────────

  @Test
  fun `scheduling two events with different ids produces two alarms`() = runTest {
    scheduler.schedule(event(id = "evt-1", targetMillis = 1_700_000_100_000L))
    scheduler.schedule(event(id = "evt-2", targetMillis = 1_700_000_200_000L))

    assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
  }

  // ── PendingIntent content ─────────────────────────────────────────────────

  @Test
  @Suppress("DEPRECATION") // ScheduledAlarm.operation has no getter alternative in Robolectric 4.14
  fun `PendingIntent carries EXTRA_EVENT_ID equal to event id value`() = runTest {
    val eventId = "evt-abc"
    scheduler.schedule(event(id = eventId))

    val pendingIntent = shadowAlarmManager.scheduledAlarms.first().operation
    val savedIntent = Shadows.shadowOf(pendingIntent).savedIntent
    assertEquals(eventId, savedIntent.getStringExtra(NotificationSchedulerImpl.EXTRA_EVENT_ID))
  }

  @Test
  @Suppress("DEPRECATION") // ScheduledAlarm.operation has no getter alternative in Robolectric 4.14
  fun `PendingIntent component targets AlarmReceiver class`() = runTest {
    scheduler.schedule(event())

    val pendingIntent = shadowAlarmManager.scheduledAlarms.first().operation
    val savedIntent = Shadows.shadowOf(pendingIntent).savedIntent
    assertEquals(
      "com.datecountdown.app.notifications.AlarmReceiver",
      savedIntent.component?.className,
    )
  }

  @Test
  @Suppress("DEPRECATION") // ScheduledAlarm.operation has no getter alternative in Robolectric 4.14
  fun `PendingIntent extras contain only EXTRA_EVENT_ID — no title or other PII`() = runTest {
    scheduler.schedule(event(title = "My Birthday Party"))

    val pendingIntent = shadowAlarmManager.scheduledAlarms.first().operation
    val extras = Shadows.shadowOf(pendingIntent).savedIntent.extras
    val keys = extras?.keySet() ?: emptySet()
    assertEquals(
      "Only EXTRA_EVENT_ID must be present in extras (AC-NT-15)",
      setOf(NotificationSchedulerImpl.EXTRA_EVENT_ID),
      keys,
    )
  }

  // ── internal helpers ──────────────────────────────────────────────────────

  /**
   * Minimal [Clock] substitute that returns a fixed [Instant].
   * [now] is a mutable var so tests can advance time without restarting the scheduler.
   */
  private class FakeClock(initial: Instant) : Clock {
    var now: Instant = initial
    override fun now(): Instant = now
  }
}
