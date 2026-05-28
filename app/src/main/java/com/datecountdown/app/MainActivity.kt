package com.datecountdown.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.core.design.theme.ThemeMode
import com.datecountdown.app.di.AppGraph
import com.datecountdown.app.domain.CountdownCalculator
import com.datecountdown.app.domain.PastEventProcessor
import com.datecountdown.app.domain.usecase.DeleteEventUseCase
import com.datecountdown.app.domain.usecase.GetEventUseCase
import com.datecountdown.app.domain.usecase.GetEventsUseCase
import com.datecountdown.app.domain.usecase.SaveEventUseCase
import com.datecountdown.app.navigation.RootComponent
import dev.zacsweers.metro.createGraphFactory

/**
 * Composition root for the application.
 *
 * Pattern (per spike 1.0):
 *  1. Metro [AppGraph] is created once via [createGraphFactory] + [AppGraph.Factory.create];
 *     its lifetime spans the Activity.
 *  2. [RootComponent] is created with [defaultComponentContext] — this binds Decompose's
 *     lifecycle/state-keeper/back-handler to the Activity (requires [ComponentActivity]).
 *  3. [setContent] renders a minimal placeholder; epic 4 replaces this with the real nav host.
 *
 * TODO(epic 4): replace the placeholder [Text] with the real Decompose nav host composable that
 *  dispatches on [RootComponent.stack] and [RootComponent.editSlot].
 */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Metro root graph — holds all app-scoped singletons (AppDatabase, EventDao, EventsRepository).
    // Application is passed through AppGraph.Factory so Room.databaseBuilder has a Context.
    val graph: AppGraph = createGraphFactory<AppGraph.Factory>().create(application)

    // Use cases and domain helpers constructed manually (no DI framework in feature modules).
    val getEvents = GetEventsUseCase(repo = graph.eventsRepository)
    val deleteEvent = DeleteEventUseCase(
      repo = graph.eventsRepository,
      scheduler = graph.notificationScheduler,
    )
    val getEvent = GetEventUseCase(repo = graph.eventsRepository)
    val saveEvent = SaveEventUseCase(
      repo = graph.eventsRepository,
      scheduler = graph.notificationScheduler,
    )
    val calculator = CountdownCalculator()
    val pastProcessor = PastEventProcessor()

    // Decompose root component — binds navigation lifecycle to this Activity.
    val root = RootComponent(
      componentContext = defaultComponentContext(),
      storeFactory = graph.storeFactory,
      getEvents = getEvents,
      deleteEvent = deleteEvent,
      getEvent = getEvent,
      saveEvent = saveEvent,
      calculator = calculator,
      pastProcessor = pastProcessor,
      notificationScheduler = graph.notificationScheduler,
      settings = graph.settingsRepository,
    )

    enableEdgeToEdge()
    setContent {
      // TODO(#28/#44): replace ThemeMode.SYSTEM with the DataStore-backed value from
      //  SettingsRepository once issue #28 (settings persistence) lands.
      DateCountdownTheme(themeMode = ThemeMode.SYSTEM) {
        // Placeholder: display the active stack config name so the skeleton is visually verifiable.
        // TODO(epic 4, #35/#39/#41): replace with NavHost that renders feature screens.
        val stack by root.stack.subscribeAsState()
        Text(text = "Active: ${stack.active.configuration}")
      }
    }
  }
}
