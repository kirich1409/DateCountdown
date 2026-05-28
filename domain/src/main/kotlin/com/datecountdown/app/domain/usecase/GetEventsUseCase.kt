package com.datecountdown.app.domain.usecase

import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Partitions all persisted events into upcoming and past, applying the
 * business-defined sort order for each section (AC-LS-4, AC-PE-15).
 *
 * An event is considered past as soon as [now >= targetDateTime][Clock.now]
 * (AC-PE-1). The exact instant of the boundary check is captured once per
 * emission so that both buckets are consistent with each other.
 *
 * @param clock Overridable for deterministic testing; defaults to [Clock.System].
 */
class GetEventsUseCase(
  private val repo: EventsRepository,
  private val clock: Clock = Clock.System,
) {

  /**
   * Returns a [Flow] that re-emits on every repository change.
   *
   * - **upcoming** — events where `targetDateTime > now`, sorted ascending
   *   (soonest first, AC-LS-4).
   * - **past** — events where `targetDateTime <= now`, sorted descending
   *   (most recent past first, AC-PE-15).
   */
  operator fun invoke(): Flow<EventsView> = repo.observeEvents().map { events ->
    val now = clock.now()
    val (past, upcoming) = events.partition { it.targetDateTime <= now }
    EventsView(
      upcoming = upcoming.sortedBy { it.targetDateTime },
      past = past.sortedByDescending { it.targetDateTime },
    )
  }
}

/**
 * Typed view produced by [GetEventsUseCase]: two pre-sorted lists that map
 * directly to the two visual sections on the list screen (AC-LS-4, AC-LS-11).
 */
data class EventsView(
  val upcoming: List<Event>,
  val past: List<Event>,
)
