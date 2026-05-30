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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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
     * - The event is held as pending; the repository delete is NOT committed here — it is
     *   committed when the UI snackbar dismisses via [CommitDelete].
     * - If a delete is already pending (AC-LS-10a), the previous one is committed synchronously
     *   before the new window opens.
     *
     * Process-death gap: if the process dies while a delete is pending, the alarm was already
     * cancelled but the repository delete never executes — the event survives without an alarm
     * until the next launch. Alarm restoration on restart is tracked in #45.
     */
    data class DeleteEvent(val id: EventId) : Intent

    /**
     * Commit the pending delete to the repository (AC-LS-9).
     *
     * Called by the UI when the snackbar is dismissed without Undo (e.g. swipe-away or natural
     * expiry of the `withTimeoutOrNull` window — 5 s for sighted users, extended under an active
     * screen reader via `calculateRecommendedTimeoutMillis` — AC-ACC-6, AC-ACC-8).
     *
     * Idempotent: no-op when [id] does not match the current pending delete or there is no
     * pending delete. The [id] guard prevents a stale dismiss callback for a superseded snackbar
     * from committing a newer pending delete.
     */
    data class CommitDelete(val id: EventId) : Intent

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

internal class EventListStoreFactory(
  private val storeFactory: StoreFactory,
  private val getEvents: GetEventsUseCase,
  private val deleteEvent: DeleteEventUseCase,
  private val scheduler: NotificationScheduler,
  private val settings: SettingsRepository,
  private val clock: Clock = Clock.System,
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
        )
      },
      reducer = EventListReducer,
    ) {}
}

// ── Executor ───────────────────────────────────────────────────────────────────────────────────────

/**
 * CoroutineExecutor subclass chosen over the DSL because:
 *  - the pending-delete record must survive across multiple Intent dispatches (field on the Executor);
 *  - [state()] is called synchronously inside intent handlers to look up the pending event.
 */
private class Executor(
  private val getEvents: GetEventsUseCase,
  private val deleteEvent: DeleteEventUseCase,
  private val scheduler: NotificationScheduler,
  private val settings: SettingsRepository,
  private val clock: Clock,
) : CoroutineExecutor<EventListStore.Intent, Unit, EventListState, Message, EventListStore.Label>() {

  /** The Event being held in the undo window. Null when no delete is pending. */
  private var pendingDeleteJob: PendingDeleteJob? = null

  override fun executeAction(action: Unit) {
    observeEvents()
  }

  override fun executeIntent(intent: EventListStore.Intent) {
    when (intent) {
      EventListStore.Intent.LoadEvents -> observeEvents()
      is EventListStore.Intent.DeleteEvent -> softDelete(intent.id)
      is EventListStore.Intent.CommitDelete -> commitDelete(intent.id)
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
    val content = state() as? EventListState.Content ?: return

    // Guard: confirmValueChange on SwipeToDismissBox may fire multiple times for the same swipe.
    // A second DeleteEvent for the already-pending id is a no-op — the snackbar is already shown
    // and the event is already restorable.
    val event = (content.upcoming + content.past)
      .firstOrNull { it.id == id }
      .takeUnless { pendingDeleteJob?.event?.id == id }
      ?: return

    // AC-LS-10a: if another delete is already pending, commit it immediately before opening the
    // new window.
    val existing = pendingDeleteJob
    if (existing != null) {
      val committedEvent = existing.event
      pendingDeleteJob = null
      scope.launch { deleteEvent(committedEvent.id) }
    }

    // Cancel the alarm immediately for instant UX feedback.
    scope.launch { scheduler.cancel(id) }

    dispatch(Message.SetPendingDelete(PendingDelete(event = event)))
    publish(EventListStore.Label.ShowDeletedSnackbar(event = event))

    // No timer: the repository delete is committed via CommitDelete, dispatched by the UI when
    // the snackbar is dismissed without Undo (Indefinite snackbar, dismissed after withTimeoutOrNull
    // — 5 s baseline, extended under an active screen reader via calculateRecommendedTimeoutMillis).
    pendingDeleteJob = PendingDeleteJob(event = event)
  }

  /**
   * Commit the repository delete for the pending event identified by [id].
   *
   * The [id] guard prevents a stale dismiss callback for a superseded snackbar from committing a
   * newer pending delete (can happen when DeleteEvent(B) replaces DeleteEvent(A) and Compose
   * delivers the A-snackbar's Dismissed callback late).
   */
  private fun commitDelete(id: EventId) {
    val pending = pendingDeleteJob ?: return
    if (pending.event.id != id) return
    pendingDeleteJob = null
    scope.launch { deleteEvent(id) }
    dispatch(Message.ClearPendingDelete)
  }

  private fun undoDelete() {
    val pending = pendingDeleteJob ?: return
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
  val event: Event,
)
