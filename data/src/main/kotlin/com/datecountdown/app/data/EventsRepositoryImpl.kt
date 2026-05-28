package com.datecountdown.app.data

import com.datecountdown.app.data.local.EventDao
import com.datecountdown.app.data.local.toDomain
import com.datecountdown.app.data.local.toEntity
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.EventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of [EventsRepository] (AC-DM-5/6/7).
 *
 * Takes [EventDao] directly — not [AppDatabase] — to keep the constructor
 * narrow and make substitution in future tests straightforward.
 *
 * Dispatcher: Room's coroutine support dispatches suspend DAO calls off the
 * main thread automatically; [observeEvents] Flow is delivered on Room's
 * internal executor. No explicit [kotlinx.coroutines.withContext] needed here.
 */
class EventsRepositoryImpl(private val dao: EventDao) : EventsRepository {

  override fun observeEvents(): Flow<List<Event>> =
    dao.observeAll().map { entities -> entities.map { it.toDomain() } }

  override suspend fun add(event: Event) {
    dao.upsert(event.toEntity())
  }

  override suspend fun update(event: Event) {
    dao.upsert(event.toEntity())
  }

  override suspend fun delete(id: EventId) {
    dao.deleteById(id.value)
  }

  override suspend fun getById(id: EventId): Event? =
    dao.getById(id.value)?.toDomain()
}
