package com.datecountdown.app.domain.usecase

import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.EventsRepository

/**
 * Fetches a single [Event] by its [id] (AC-DM-6).
 *
 * Returns `null` when no event with the given [id] exists — callers decide
 * how to handle the absence (show empty state, navigate back, etc.).
 */
class GetEventUseCase(private val repo: EventsRepository) {

  suspend operator fun invoke(id: EventId): Event? = repo.getById(id)
}
