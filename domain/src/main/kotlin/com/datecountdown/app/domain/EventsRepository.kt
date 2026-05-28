package com.datecountdown.app.domain

import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to the persisted collection of [Event]s.
 *
 * The implementation lives in `:data` (Room-backed). This interface is the
 * only boundary the domain and feature modules depend on (AC-DM-5/6).
 */
interface EventsRepository {

  /**
   * Returns a [Flow] that emits the full list of events and re-emits whenever
   * any event is added, updated, or deleted (AC-DM-5).
   */
  fun observeEvents(): Flow<List<Event>>

  /** Persists a new [event] (AC-DM-6). */
  suspend fun add(event: Event)

  /** Replaces the stored event that has the same [Event.id] (AC-DM-6). */
  suspend fun update(event: Event)

  /** Removes the event identified by [id] (AC-DM-6). */
  suspend fun delete(id: EventId)

  /**
   * Returns the event with the given [id], or `null` if it does not exist
   * (AC-DM-6).
   */
  suspend fun getById(id: EventId): Event?
}
