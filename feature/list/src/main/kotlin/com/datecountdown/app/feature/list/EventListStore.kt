package com.datecountdown.app.feature.list

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.NotificationScheduler
import com.datecountdown.app.domain.SettingsRepository
import com.datecountdown.app.domain.ThemeMode
import com.datecountdown.app.domain.usecase.DeleteEventUseCase
import com.datecountdown.app.domain.usecase.GetEventsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * MVIKotlin Store for the event-list screen.
 *
 * Sealed Intent / State / Label hierarchies are public because they cross the component boundary
 * (DefaultEventListComponent → Store). Messages and the pending-delete job are internal
 * implementation details.
 */
internal interface EventListStore : Store<EventListStore.Intent, EventListState, EventListStore.Label> {

  sealed interface Intent {
    /** Trigger (or re-trigger) the events flow subscription. Idempotent — the bootstrapper also fires this. */
    data object LoadEvents : Intent

    /**
     * Begin the soft-delete sequence for [id] (AC-LS-9, AC-LS-10).
     *
     * - Alarm is cancelled immediately via [NotificationScheduler].
     * - A 5-second coroutine is launched; on expiry [DeleteEventUseCase] removes the event from
     *   the repository.
     * - If a delete is already pending (AC-LS-10a), the previous one is committed synchronously
     *   before the new window opens.
     *
     * Process-death gap: if the Executor's scope is cancelled while the timer is running, the
     * alarm remains cancelled but the repository delete never executes — the event survives
     * without an alarm until the next launch. Alarm restoration on restart is tracked in #45.
     */
    data class DeleteEvent(val id: EventId) : Intent

    /**
     * Cancel the pending delete and restore the event (AC-LS-10).
     *
     * Re-schedules the alarm if [Event.targetDateTime] is still in the future.
     * No-op when there is no pending delete.
     */
    data object UndoDelete : Intent

    /** Flip the pastCollapsed flag and persist it (AC-LS-11). */
    data object TogglePastSection : Intent

    /**
     * Persist the selected theme mode (AC-TH-10).
     *
     * Calls [SettingsRepository.setThemeMode] and does NOT dispatch an optimistic message —
     * [settings.themeMode] Flow is the single source of truth and will re-emit the new value,
     * updating the state via [Message.SetData].
     */
    data class UpdateThemeMode(val mode: ThemeMode) : Intent
  }

  sealed interface Label {
    /** Request the UI to show a deletion snackbar with undo affordance (AC-LS-9, AC-LS-21). */
    data class ShowDeletedSnackbar(val event: Event) : Label
  }
}

// ── Internal messages dispatched by the Executor to the Reducer ───────────────────────────────────

private sealed interface Message {
  data class SetData(
    val upcoming: List<Event>,
    val past: List<Event>,
    val pastCollapsed: Boolean,
    val themeMode: ThemeMode,
  ) : Message

  data class SetPendingDelete(val pending: PendingDelete) : Message
  data object ClearPendingDelete : Message
  data class SetError(val cause: Throwable) : Message
}

// ── Factory ────────────────────────────────────────────────────────────────────────────────────────

@Suppress("LongParameterList")
internal class EventListStoreFactory(
  private val storeFactory: StoreFactory,
  private val getEvents: GetEventsUseCase,
  private val deleteEvent: DeleteEventUseCase,
  private val scheduler: NotificationScheduler,
  private val settings: SettingsRepository,
  private val clock: Clock = Clock.System,
  private val undoWindowMs: Long = 5_000L,
) {

  fun create(): EventListStore =
    object : EventListStore, Store<EventListStore.Intent, EventListState, EventListStore.Label> by storeFactory.create(
      name = "EventListStore",
      initialState = EventListState.Loading,
      bootstrapper = SimpleBootstrapper(Unit),
      executorFactory = {
        Executor(
          getEvents = getEvents,
          deleteEvent = deleteEvent,
          scheduler = scheduler,
          settings = settings,
          clock = clock,
          undoWindowMs = undoWindowMs,
        )
      },
      reducer = EventListReducer,
    ) {}
}

// ── Executor ───────────────────────────────────────────────────────────────────────────────────────

/**
 * CoroutineExecutor subclass chosen over the DSL because:
 *  - the pending-delete [Job] must survive across multiple Intent dispatches (field on the Executor);
 *  - [state()] is called synchronously inside intent handlers to look up the pending event.
 */
private class Executor(
  private val getEvents: GetEventsUseCase,
  private val deleteEvent: DeleteEventUseCase,
  private val scheduler: NotificationScheduler,
  private val settings: SettingsRepository,
  private val clock: Clock,
  private val undoWindowMs: Long,
) : CoroutineExecutor<EventListStore.Intent, Unit, EventListState, Message, EventListStore.Label>() {

  /** Job + the Event being held in the undo window. Null when no delete is pending. */
  private var pendingDeleteJob: PendingDeleteJob? = null

  override fun executeAction(action: Unit) {
    observeEvents()
  }

  override fun executeIntent(intent: EventListStore.Intent) {
    when (intent) {
      EventListStore.Intent.LoadEvents -> observeEvents()
      is EventListStore.Intent.DeleteEvent -> softDelete(intent.id)
      EventListStore.Intent.UndoDelete -> undoDelete()
      EventListStore.Intent.TogglePastSection -> togglePastSection()
      is EventListStore.Intent.UpdateThemeMode -> updateThemeMode(intent.mode)
    }
  }

  // ── Private helpers ──────────────────────────────────────────────────────────────────────────────

  private var observeJob: Job? = null

  private fun observeEvents() {
    observeJob?.cancel()
    observeJob = scope.launch {
      combine(
        getEvents(),
        settings.pastCollapsed,
        settings.themeMode,
      ) { eventsView, pastCollapsed, themeMode ->
        Message.SetData(
          upcoming = eventsView.upcoming,
          past = eventsView.past,
          pastCollapsed = pastCollapsed,
          themeMode = themeMode,
        )
      }.collect { message ->
        dispatch(message)
      }
    }
    // Error propagation: exceptions from the combined flow are surfaced via the Job's failure;
    // wrap in a separate launch so we can catch and dispatch SetError.
    observeJob?.invokeOnCompletion { cause ->
      if (cause != null && cause !is CancellationException) {
        dispatch(Message.SetError(cause))
      }
    }
  }

  private fun softDelete(id: EventId) {
    val currentState = state()
    val content = currentState as? EventListState.Content ?: return

    val event = (content.upcoming + content.past).firstOrNull { it.id == id } ?: return

    // AC-LS-10a: if another delete is already pending, commit it immediately before opening the
    // new window.
    val existing = pendingDeleteJob
    if (existing != null) {
      existing.job.cancel()
      val committedEvent = existing.event
      pendingDeleteJob = null
      scope.launch { deleteEvent(committedEvent.id) }
    }

    // Cancel the alarm immediately for instant UX feedback.
    scope.launch { scheduler.cancel(id) }

    val expiresAt: Instant = clock.now().plus(undoWindowMs.milliseconds)
    dispatch(Message.SetPendingDelete(PendingDelete(event = event, expiresAt = expiresAt)))
    publish(EventListStore.Label.ShowDeletedSnackbar(event = event))

    val job = scope.launch {
      delay(undoWindowMs)
      deleteEvent(id)
      pendingDeleteJob = null
      dispatch(Message.ClearPendingDelete)
    }

    pendingDeleteJob = PendingDeleteJob(job = job, event = event)
  }

  private fun undoDelete() {
    val pending = pendingDeleteJob ?: return
    pending.job.cancel()
    pendingDeleteJob = null
    dispatch(Message.ClearPendingDelete)

    // Restore the alarm only if the event is still upcoming.
    if (pending.event.targetDateTime > clock.now()) {
      scope.launch { scheduler.schedule(pending.event) }
    }
  }

  private fun togglePastSection() {
    val currentState = state()
    val content = currentState as? EventListState.Content ?: return
    val newValue = !content.pastCollapsed
    scope.launch { settings.setPastCollapsed(newValue) }
    // Do not dispatch an optimistic message — settings.pastCollapsed Flow is the single source of
    // truth and will re-emit with the new value, updating the state via SetData.
  }

  private fun updateThemeMode(mode: ThemeMode) {
    scope.launch { settings.setThemeMode(mode) }
    // Do not dispatch an optimistic message — settings.themeMode Flow is the single source of
    // truth and will re-emit with the new value, updating the state via SetData.
  }
}

// ── Reducer ────────────────────────────────────────────────────────────────────────────────────────

private object EventListReducer : Reducer<EventListState, Message> {

  override fun EventListState.reduce(msg: Message): EventListState =
    when (msg) {
      is Message.SetData -> when (this) {
        is EventListState.Content -> copy(
          upcoming = msg.upcoming,
          past = msg.past,
          pastCollapsed = msg.pastCollapsed,
          themeMode = msg.themeMode,
          // Preserve the in-memory pendingDelete across data refreshes.
        )
        else -> EventListState.Content(
          upcoming = msg.upcoming,
          past = msg.past,
          pastCollapsed = msg.pastCollapsed,
          themeMode = msg.themeMode,
          pendingDelete = null,
        )
      }

      is Message.SetPendingDelete -> when (this) {
        is EventListState.Content -> copy(pendingDelete = msg.pending)
        else -> this
      }

      Message.ClearPendingDelete -> when (this) {
        is EventListState.Content -> copy(pendingDelete = null)
        else -> this
      }

      is Message.SetError -> EventListState.Error(cause = msg.cause)
    }
}

// ── Internal data holder ───────────────────────────────────────────────────────────────────────────

private data class PendingDeleteJob(
  val job: Job,
  val event: Event,
)
