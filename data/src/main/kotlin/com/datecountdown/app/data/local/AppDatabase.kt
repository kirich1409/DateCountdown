package com.datecountdown.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Root Room database for the app.
 *
 * Use [DATABASE_NAME] as the file name in [androidx.room.Room.databaseBuilder] (issue #27).
 * Schema export is enabled ([exportSchema] = true) — version-1 JSON lives in `data/schemas/`.
 */
@Database(
  entities = [EventEntity::class],
  version = 1,
  exportSchema = true,
)
// Promoted to public so the Metro AppGraph in :app can call databaseBuilder and eventDao().
// EventEntity stays internal — it is only referenced inside :data's mapper.
abstract class AppDatabase : RoomDatabase() {

  abstract fun eventDao(): EventDao

  companion object {
    const val DATABASE_NAME = "events.db"
  }
}
