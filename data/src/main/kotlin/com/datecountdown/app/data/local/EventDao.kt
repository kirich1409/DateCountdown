package com.datecountdown.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [EventEntity] — covers AC-DM-5 (observe all) and AC-DM-6 (add/update/delete/getById).
 *
 * [upsert] handles both add and update in a single call (AC-DM-6). Physical delete (no soft-delete flag).
 */
@Dao
// Promoted to public so AppDatabase.eventDao() is accessible from :app's Metro graph.
interface EventDao {

  @Query("SELECT * FROM events")
  fun observeAll(): Flow<List<EventEntity>>

  @Query("SELECT * FROM events WHERE id = :id")
  suspend fun getById(id: String): EventEntity?

  @Upsert
  suspend fun upsert(entity: EventEntity)

  @Query("DELETE FROM events WHERE id = :id")
  suspend fun deleteById(id: String)
}
