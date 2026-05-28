package com.datecountdown.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a countdown event.
 *
 * Time fields use epoch millis (Long) — conversion to/from [kotlinx.datetime.Instant]
 * happens in the mapper (issue #27). Enum fields store the enum entry name (not ordinal)
 * so that future reordering of [com.datecountdown.app.domain.EventColor] /
 * [com.datecountdown.app.domain.EventIcon] entries does not corrupt persisted data.
 */
@Entity(tableName = "events")
internal data class EventEntity(
  @PrimaryKey
  val id: String,
  val title: String,
  /** Epoch millis for [com.datecountdown.app.domain.Event.targetDateTime]. */
  @ColumnInfo(name = "target_date_time_millis")
  val targetDateTimeMillis: Long,
  /** Name of [com.datecountdown.app.domain.EventColor] entry (not ordinal). */
  @ColumnInfo(name = "color_name")
  val colorName: String,
  /** Name of [com.datecountdown.app.domain.EventIcon] entry (not ordinal). */
  @ColumnInfo(name = "icon_name")
  val iconName: String,
  /** Epoch millis for [com.datecountdown.app.domain.Event.createdAt]. */
  @ColumnInfo(name = "created_at_millis")
  val createdAtMillis: Long,
)
