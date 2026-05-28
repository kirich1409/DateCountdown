package com.datecountdown.app.domain.usecase

import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.EventsRepository
import com.datecountdown.app.domain.NotificationScheduler
import kotlinx.datetime.Clock

/**
 * Validates, persists, and schedules (or cancels) a single event from an [EventDraft].
 *
 * ## Validation (AC-AE-7/8, AC-DM-4)
 * [draft.title][EventDraft.title] is trimmed; the trimmed form must be 1–60 characters.
 * Throws [IllegalArgumentException] with `"title"` as the message if either constraint is
 * violated. The UI is expected to validate before calling, so this is a safety-net only.
 *
 * ## Create vs. edit
 * - **New** ([EventDraft.id] == `null`): a fresh [EventId] is generated from a random UUID and
 *   [createdAt] is set to [Clock.now].
 * - **Edit** ([EventDraft.id] != `null`): the existing [Event.id] is preserved and [createdAt]
 *   is fetched from the repository so the original creation timestamp is not lost. If the row
 *   no longer exists at save time (e.g., deleted concurrently), [Clock.now] is used as a
 *   defensive fallback — callers should treat this as a new event in that edge case.
 *
 * ## Notification scheduling (AC-NT-4, AC-NT-13, AC-PE-13a)
 * After a successful persist the scheduling decision depends on two conditions:
 * - [scheduleNotification] = `true` **and** [Event.targetDateTime] > [Clock.now] (upcoming) →
 *   [NotificationScheduler.schedule] is called to set (or replace) the exact alarm.
 * - In all other cases → [NotificationScheduler.cancel] is called to remove any previously-
 *   scheduled alarm. This covers: past target dates, user choosing «Save without notification»
 *   after exact-alarm permission was denied (AC-NT-13), and events shifted into the past on edit.
 *   [NotificationScheduler.schedule] no-ops on past events but does NOT cancel a pre-existing
 *   alarm, which is why the cancel branch must be explicit.
 *
 * ## scheduleNotification = false (AC-NT-13)
 * Pass `false` when the user has explicitly chosen to save without a notification after
 * [ExactAlarmPermissionChecker.canScheduleExactAlarms] returned `false`. The event is persisted
 * normally; the alarm for this event id is cancelled (or stays absent).
 *
 * ## SecurityException
 * On API 31–33 [NotificationScheduler.schedule] may throw [SecurityException] when
 * SCHEDULE_EXACT_ALARM has been denied. Callers must pre-check via [ExactAlarmPermissionChecker]
 * and pass [scheduleNotification] = `false` when denied, so this exception is not expected in
 * normal operation after the check (AC-NT-13).
 *
 * @param clock Overridable for deterministic testing; defaults to [Clock.System].
 */
class SaveEventUseCase(
  private val repo: EventsRepository,
  private val scheduler: NotificationScheduler,
  private val clock: Clock = Clock.System,
) {

  private companion object {
    /** Maximum allowed length of a trimmed event title (AC-DM-4). */
    const val MAX_TITLE_LENGTH = 60
  }

  /**
   * Validates [draft], persists the resulting [Event], and schedules or cancels its alarm.
   *
   * @param draft The event draft to validate and persist.
   * @param scheduleNotification When `true` (default) an exact alarm is scheduled for upcoming
   *   events. Pass `false` when the user explicitly chose to save without a notification after
   *   [ExactAlarmPermissionChecker] returned `false` (AC-NT-13); the event is still persisted
   *   and any prior alarm for this id is cancelled.
   * @return The persisted [Event] (includes the generated [EventId] for new events).
   * @throws IllegalArgumentException if the trimmed title is empty or longer than 60 characters.
   */
  suspend operator fun invoke(draft: EventDraft, scheduleNotification: Boolean = true): Event {
    val trimmedTitle = draft.title.trim()
    require(trimmedTitle.isNotEmpty() && trimmedTitle.length <= MAX_TITLE_LENGTH) { "title" }

    val now = clock.now()
    val event = if (draft.id == null) {
      Event(
        id = EventId(java.util.UUID.randomUUID().toString()),
        title = trimmedTitle,
        targetDateTime = draft.targetDateTime,
        color = draft.color,
        icon = draft.icon,
        createdAt = now,
      )
    } else {
      // Preserve the original creation timestamp when editing (AC-DM-1).
      // The ?: fallback guards against the unlikely race where the event was
      // deleted between the sheet opening and the user tapping Save.
      val originalCreatedAt = repo.getById(draft.id)?.createdAt ?: now
      Event(
        id = draft.id,
        title = trimmedTitle,
        targetDateTime = draft.targetDateTime,
        color = draft.color,
        icon = draft.icon,
        createdAt = originalCreatedAt,
      )
    }

    if (draft.id == null) {
      repo.add(event)
    } else {
      repo.update(event)
    }

    // Schedule only upcoming events when scheduling is requested (AC-NT-4, AC-NT-13).
    // Cancel for past targets, user-opted-out saves, and events shifted into the past on edit
    // (AC-PE-13a). NotificationScheduler.schedule() no-ops on past events but does NOT remove
    // an existing alarm, so the cancel branch must be explicit in all non-schedule cases.
    if (scheduleNotification && event.targetDateTime > now) {
      scheduler.schedule(event)
    } else {
      scheduler.cancel(event.id)
    }

    return event
  }
}
