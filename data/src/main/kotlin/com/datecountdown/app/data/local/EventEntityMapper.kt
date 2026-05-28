package com.datecountdown.app.data.local

import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import kotlinx.datetime.Instant

/**
 * Bidirectional mapper between [EventEntity] (Room storage) and [Event] (domain model).
 *
 * Mapping contract:
 * - Time fields: [EventEntity.targetDateTimeMillis] / [EventEntity.createdAtMillis] ↔
 *   [Instant] via [Instant.fromEpochMilliseconds] / [Instant.toEpochMilliseconds].
 * - Enum fields: stored as entry name (string) ↔ resolved via [enumValueOf].
 *   If a stored name is unknown (e.g. data written by a newer app version or a manual DB edit),
 *   [enumValueOf] throws [IllegalArgumentException] — this is acceptable for v1; no enum
 *   migration is performed.
 */

internal fun EventEntity.toDomain(): Event = Event(
  id = EventId(id),
  title = title,
  targetDateTime = Instant.fromEpochMilliseconds(targetDateTimeMillis),
  color = enumValueOf<EventColor>(colorName),
  icon = enumValueOf<EventIcon>(iconName),
  createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
)

internal fun Event.toEntity(): EventEntity = EventEntity(
  id = id.value,
  title = title,
  targetDateTimeMillis = targetDateTime.toEpochMilliseconds(),
  colorName = color.name,
  iconName = icon.name,
  createdAtMillis = createdAt.toEpochMilliseconds(),
)
