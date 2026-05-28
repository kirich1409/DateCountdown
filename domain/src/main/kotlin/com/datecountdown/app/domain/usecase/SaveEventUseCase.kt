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
 * ## Notification scheduling (AC-NT-4, AC-PE-13a)
 * After a successful persist:
 * - If [Event.targetDateTime] > [Clock.now] (upcoming) → [NotificationScheduler.schedule] is
 *   called to set (or replace) the exact alarm.
 * - Otherwise (past, or user shifted an upcoming event into the past on edit) →
 *   [NotificationScheduler.cancel] is called to remove any previously-scheduled alarm.
 *   [NotificationScheduler.schedule] no-ops on past events but does NOT cancel a pre-existing
 *   alarm, which is why the cancel branch must be explicit.
 *
 * ## SecurityException
 * On API 31–33 [NotificationScheduler.schedule] may throw [SecurityException] when
 * SCHEDULE_EXACT_ALARM has been denied (AC-NT-13). This use case lets it propagate; the
 * call site in the feature module is responsible for surfacing the permission-rationale dialog
 * described in AC-NT-13 (tracked in issue #48 — out of scope here).
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
   * @return The persisted [Event] (includes the generated [EventId] for new events).
   * @throws IllegalArgumentException if the trimmed title is empty or longer than 60 characters.
   * @throws SecurityException on API 31–33 when SCHEDULE_EXACT_ALARM is denied (propagated from
   *   [NotificationScheduler.schedule]).
   */
  suspend operator fun invoke(draft: EventDraft): Event {
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

    // Schedule only upcoming events (AC-NT-4). For past targets — or when an edit shifts a
    // previously-upcoming event into the past (AC-PE-13a) — cancel any lingering alarm explicitly.
    // NotificationScheduler.schedule() no-ops on past events but does NOT remove an existing alarm.
    if (event.targetDateTime > now) {
      scheduler.schedule(event)
    } else {
      scheduler.cancel(event.id)
    }

    return event
  }
}
