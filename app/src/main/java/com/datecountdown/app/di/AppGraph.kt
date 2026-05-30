package com.datecountdown.app.di

import android.app.AlarmManager
import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.datecountdown.app.data.EventsRepositoryImpl
import com.datecountdown.app.data.ExactAlarmPermissionCheckerImpl
import com.datecountdown.app.data.NotificationSchedulerImpl
import com.datecountdown.app.data.SettingsRepositoryImpl
import com.datecountdown.app.data.local.AppDatabase
import com.datecountdown.app.data.local.EventDao
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.datecountdown.app.domain.EventsRepository
import com.datecountdown.app.domain.ExactAlarmPermissionChecker
import com.datecountdown.app.domain.NotificationScheduler
import com.datecountdown.app.domain.SettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Root Metro dependency graph for the app scope.
 *
 * Scoped with [AppScope] so that [SingleIn]-annotated providers produce one instance per graph
 * lifetime. The graph is created once in [com.datecountdown.app.DateCountdownApp] via
 * [dev.zacsweers.metro.createGraphFactory]; its lifetime matches the process.
 *
 * [AppDatabase] and [EventDao] are provided as singletons here — the database must not be
 * rebuilt per injection. [EventsRepository], [SettingsRepository], and [NotificationScheduler]
 * are likewise scoped so all callers share the same instance.
 *
 * Note: [AppDatabase], [EventDao], and [EventEntity] were all promoted from `internal` to public.
 * [EventDao]'s generated Room impl exposes [EventEntity] in its signatures, so Kotlin requires
 * [EventEntity] to also be public once [EventDao] is public. [SettingsRepositoryImpl] and
 * [NotificationSchedulerImpl] are also public for the same cross-module Metro wiring reason.
 * None of these types are part of the intended public API of [:data] — they are implementation
 * details exposed only to satisfy the Metro wiring in [:app].
 */
@DependencyGraph(AppScope::class)
interface AppGraph {

  /** Accessor exposed so downstream components can receive [EventsRepository] by type. */
  val eventsRepository: EventsRepository

  /** Accessor exposed so downstream components can receive [SettingsRepository] by type. */
  val settingsRepository: SettingsRepository

  /** Accessor exposed so downstream components can receive [NotificationScheduler] by type. */
  val notificationScheduler: NotificationScheduler

  /** Accessor exposed so feature components can obtain the MVIKotlin [StoreFactory]. */
  val storeFactory: StoreFactory

  /** Accessor exposed so [com.datecountdown.app.navigation.RootComponent] can thread it to the edit component. */
  val exactAlarmPermissionChecker: ExactAlarmPermissionChecker

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

  @SingleIn(AppScope::class)
  @Provides
  fun provideDataStore(application: Application): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
      produceFile = { application.preferencesDataStoreFile("settings") },
    )

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
    SettingsRepositoryImpl(dataStore)

  @SingleIn(AppScope::class)
  @Provides
  fun provideAlarmManager(application: Application): AlarmManager =
    requireNotNull(application.getSystemService(AlarmManager::class.java)) {
      "AlarmManager system service unavailable — this should never happen on a real device"
    }

  @SingleIn(AppScope::class)
  @Provides
  fun provideNotificationScheduler(
    application: Application,
    alarmManager: AlarmManager,
  ): NotificationScheduler = NotificationSchedulerImpl(
    context = application,
    alarmManager = alarmManager,
  )

  @SingleIn(AppScope::class)
  @Provides
  fun provideStoreFactory(): StoreFactory = DefaultStoreFactory()

  @SingleIn(AppScope::class)
  @Provides
  fun provideExactAlarmPermissionChecker(application: Application): ExactAlarmPermissionChecker =
    ExactAlarmPermissionCheckerImpl(context = application)
}
