package com.datecountdown.app.di

import android.app.Application
import androidx.room.Room
import com.datecountdown.app.data.EventsRepositoryImpl
import com.datecountdown.app.data.local.AppDatabase
import com.datecountdown.app.data.local.EventDao
import com.datecountdown.app.domain.EventsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Root Metro dependency graph for the app scope.
 *
 * Scoped with [AppScope] so that [SingleIn]-annotated providers produce one instance per graph
 * lifetime. The graph is created once in [com.datecountdown.app.MainActivity] via
 * [dev.zacsweers.metro.createGraphFactory]; its lifetime therefore matches the Activity.
 *
 * [AppDatabase] and [EventDao] are provided as singletons here — the database must not be
 * rebuilt per injection. [EventsRepository] is likewise scoped so all callers share the same
 * instance.
 *
 * Note: [AppDatabase], [EventDao], and [EventEntity] were all promoted from `internal` to public.
 * [EventDao]'s generated Room impl exposes [EventEntity] in its signatures, so Kotlin requires
 * [EventEntity] to also be public once [EventDao] is public. None of these types are part of
 * the intended public API of [:data] — they are implementation details exposed only to satisfy
 * the Metro wiring in [:app].
 */
@DependencyGraph(AppScope::class)
interface AppGraph {

  /** Accessor exposed so downstream components can receive [EventsRepository] by type. */
  val eventsRepository: EventsRepository

  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides application: Application): AppGraph
  }

  @SingleIn(AppScope::class)
  @Provides
  fun provideAppDatabase(application: Application): AppDatabase =
    Room.databaseBuilder(
      application,
      AppDatabase::class.java,
      AppDatabase.DATABASE_NAME,
    ).build()

  @SingleIn(AppScope::class)
  @Provides
  fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()

  @SingleIn(AppScope::class)
  @Provides
  fun provideEventsRepository(dao: EventDao): EventsRepository = EventsRepositoryImpl(dao)
}
