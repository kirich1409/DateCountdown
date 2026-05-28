package com.datecountdown.app.domain

import kotlinx.datetime.Instant

/**
 * Core domain model representing a user-defined countdown event.
 *
 * The [id] is stable across edits (AC-DM-1). [title] is stored trimmed; the
 * 1–60 character constraint is enforced by callers (AC-DM-4). Notification
 * scheduling is derived from [targetDateTime] — there are no notification
 * fields on this model (AC-DM-1).
 */
data class Event(
  val id: EventId,
  val title: String,
  val targetDateTime: Instant,
  val color: EventColor,
  val icon: EventIcon,
  val createdAt: Instant,
)
