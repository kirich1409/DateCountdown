package com.datecountdown.app.domain.usecase

import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import kotlinx.datetime.Instant

/**
 * Input passed by the Add/Edit screen to [SaveEventUseCase].
 *
 * - [id] == `null` — create a new event (SaveEventUseCase generates a fresh [EventId]).
 * - [id] != `null` — update the existing event with that id; its original [createdAt] is
 *   preserved from the repository.
 *
 * [title] is expected to be the raw string from the UI text field; [SaveEventUseCase] performs
 * trimming and length validation (AC-DM-4, AC-AE-7/8).
 */
data class EventDraft(
  val id: EventId? = null,
  val title: String,
  val targetDateTime: Instant,
  val color: EventColor,
  val icon: EventIcon,
)
