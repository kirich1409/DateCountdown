package com.datecountdown.app.feature.counter

import com.datecountdown.app.domain.CountdownResult
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.PastBreakdown

/**
 * UI state emitted by [CounterStore].
 *
 * Lifecycle:
 *  Loading → Upcoming | Past  (initial load)
 *  Upcoming ↔ Past             (real-time tick crossing midnight or year boundary)
 *  * → NotFound                (event deleted externally)
 *  * → Error                   (repository failure)
 */
sealed interface CounterState {

  /** Initial state — awaiting the first repository response. */
  data object Loading : CounterState

  /**
   * Event exists and its target is in the future.
   *
   * [countdown] is recomputed on every [CounterStore.Intent.Tick] (~1 Hz) so values update
   * in real time without a full repository re-fetch.
   */
  data class Upcoming(
    val event: Event,
    val countdown: CountdownResult.Upcoming,
  ) : CounterState

  /**
   * Event exists and its target is in the past (or today).
   *
   * [breakdown] is recomputed on every [CounterStore.Intent.Tick] (~1 Hz).
   */
  data class Past(
    val event: Event,
    val breakdown: PastBreakdown,
  ) : CounterState

  /**
   * No event found for the requested [EventId].
   *
   * Displayed as an empty / "event deleted" screen; the user can only navigate back.
   */
  data object NotFound : CounterState

  /** Repository or domain layer threw an exception. */
  data class Error(val cause: Throwable) : CounterState
}
