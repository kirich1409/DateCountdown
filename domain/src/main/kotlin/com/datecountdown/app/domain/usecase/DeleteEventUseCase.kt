package com.datecountdown.app.domain.usecase

import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.EventsRepository
import com.datecountdown.app.domain.NotificationScheduler

/**
 * Cancels any scheduled alarm and removes the event identified by [id] from the repository.
 *
 * Both operations are idempotent — cancelling a non-existent alarm and deleting a non-existent
 * row are silent no-ops, so it is safe to call this use case even when the event may have been
 * deleted concurrently.
 *
 * The alarm is cancelled before the repository delete so that, in the unlikely event of an
 * exception during delete, no orphaned alarm remains.
 */
class DeleteEventUseCase(
  private val repo: EventsRepository,
  private val scheduler: NotificationScheduler,
) {

  suspend operator fun invoke(id: EventId) {
    scheduler.cancel(id)
    repo.delete(id)
  }
}
