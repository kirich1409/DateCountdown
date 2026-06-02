package com.datecountdown.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.domain.CountdownCalculator
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.EventsRepository
import com.datecountdown.app.domain.PastEventProcessor
import com.datecountdown.app.domain.usecase.DeleteEventUseCase
import com.datecountdown.app.domain.usecase.GetEventUseCase
import com.datecountdown.app.domain.usecase.GetEventsUseCase
import com.datecountdown.app.domain.usecase.SaveEventUseCase
import com.datecountdown.app.navigation.RootComponent
import com.datecountdown.app.navigation.RootContent
import com.datecountdown.app.notifications.AlarmReceiver
import com.datecountdown.app.notifications.NotificationChannels
import kotlinx.coroutines.launch

/**
 * Composition root for the application.
 *
 * Pattern (per spike 1.0):
 *  1. Metro [AppGraph] is created once in [DateCountdownApp] and lives for the process lifetime.
 *     [MainActivity] reads it via `(application as DateCountdownApp).graph`.
 *  2. [RootComponent] is created with [defaultComponentContext] — this binds Decompose's
 *     lifecycle/state-keeper/back-handler to the Activity (requires [ComponentActivity]).
 *  3. [setContent] delegates to [RootContent] which wires [RootComponent.stack] (List ↔ Counter)
 *     and [RootComponent.editSlot] (add/edit bottom sheet overlay).
 *
 * **Deep-link routing (AC-NAV-7):**
 *  Tap on a notification delivers [AlarmReceiver.EXTRA_EVENT_ID] via an explicit Intent.
 *  [handleDeepLink] reads the extra, validates the event still exists, and calls
 *  [RootComponent.pushCounter]. [launchMode="singleTop"] (AndroidManifest) routes subsequent
 *  taps through [onNewIntent] so a stacked Activity is never created.
 */
class MainActivity : ComponentActivity() {

  private lateinit var root: RootComponent
  private lateinit var eventsRepository: EventsRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Ensure notification channel exists before any alarm can fire (AC-NT-7).
    // Idempotent — safe to call on every Activity creation.
    NotificationChannels.ensureChannel(applicationContext)

    // Metro root graph — singleton for the process lifetime; owned by DateCountdownApp.
    // Reading it here (instead of creating a new instance) prevents duplicate DataStore / Room
    // instances on config-changes such as system theme switches (bug #138).
    val graph = (application as DateCountdownApp).graph

    eventsRepository = graph.eventsRepository

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
    root = RootComponent(
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
      exactAlarmChecker = graph.exactAlarmPermissionChecker,
    )

    handleDeepLink(intent)

    // WindowCompat.enableEdgeToEdge makes bars transparent and disables nav-bar contrast
    // enforcement (API 29+), so the hero/surface Box shows through the 3-button nav bar on all
    // API levels. androidx.activity.enableEdgeToEdge applied a dark scrim on API 29–34 instead.
    WindowCompat.enableEdgeToEdge(window)
    setContent {
      val domainThemeMode by graph.settingsRepository.themeMode.collectAsStateWithLifecycle(
        initialValue = com.datecountdown.app.domain.ThemeMode.SYSTEM,
      )
      // Bridge domain ThemeMode → design ThemeMode by ordinal. Both enums declare the same three
      // entries in the same order (SYSTEM=0, LIGHT=1, DARK=2), so ordinal mapping is stable.
      val designThemeMode = com.datecountdown.app.core.design.theme.ThemeMode.entries[domainThemeMode.ordinal]
      DateCountdownTheme(themeMode = designThemeMode) {
        RootContent(root = root, settings = graph.settingsRepository)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleDeepLink(intent)
  }

  /**
   * Handles notification deep-link routing (AC-NAV-7).
   *
   * Reads [AlarmReceiver.EXTRA_EVENT_ID] from [intent], validates the event still exists in the
   * repository, and pushes Counter if so. If the event has been deleted between scheduling and
   * the notification tap — silently stays on List (no crash, no toast). The extra is removed
   * after processing so subsequent re-delivers (e.g. rotation) are no-ops.
   */
  private fun handleDeepLink(intent: Intent?) {
    val eventIdValue = intent?.getStringExtra(AlarmReceiver.EXTRA_EVENT_ID) ?: return
    lifecycleScope.launch {
      val exists = runCatching { eventsRepository.getById(EventId(eventIdValue)) }
        .getOrNull() != null
      if (exists) {
        root.pushCounter(eventId = eventIdValue)
      }
      // AC-NAV-7: invalid / deleted event → stay on List, no crash, no toast.
      intent.removeExtra(AlarmReceiver.EXTRA_EVENT_ID)
    }
  }
}
