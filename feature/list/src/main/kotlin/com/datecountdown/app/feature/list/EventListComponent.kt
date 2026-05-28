package com.datecountdown.app.feature.list

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.NotificationScheduler
import com.datecountdown.app.domain.SettingsRepository
import com.datecountdown.app.domain.usecase.DeleteEventUseCase
import com.datecountdown.app.domain.usecase.GetEventsUseCase
import kotlinx.datetime.Instant

// ── Public state contract ──────────────────────────────────────────────────────────────────────────

/**
 * Pending soft-delete record held in memory during the 5-second undo window (AC-LS-9, AC-LS-10).
 *
 * [expiresAt] is used by the UI to drive the snackbar countdown (AC-LS-21).
 *
 * Note: if the process dies or the back-stack entry is popped while this is non-null, the alarm
 * was already cancelled (at Intent.DeleteEvent time) but the repository delete was never executed
 * — the event therefore survives without an alarm until the next app launch.
 * Alarm restoration on restart is owned by issue #45.
 */
data class PendingDelete(
  val event: Event,
  val expiresAt: Instant,
)

/**
 * UI state for the event-list screen (AC-LS-1 through AC-LS-21).
 *
 * GlobalEmpty (AC-LS-14) and PartialEmpty (AC-LS-15) are derived states — the UI detects them
 * from [Content.upcoming.isEmpty() && past.isEmpty()] and [Content.upcoming.isEmpty()] respectively;
 * they are not separate sealed variants.
 */
sealed interface EventListState {
  /** Initial state while the first emission from the repository has not yet arrived. */
  data object Loading : EventListState

  /**
   * Data is available. Both lists are already sorted:
   *  - [upcoming] ascending by targetDateTime (soonest first, AC-LS-4)
   *  - [past] descending by targetDateTime (most recent past first, AC-PE-15)
   */
  data class Content(
    val upcoming: List<Event>,
    val past: List<Event>,
    /** Whether the "Past" section header is collapsed (AC-LS-11). Persisted in SettingsRepository. */
    val pastCollapsed: Boolean,
    /**
     * Non-null while a soft-delete is in its 5-second undo window (AC-LS-9, AC-LS-10).
     * Only one pending delete is held at a time (AC-LS-10a).
     */
    val pendingDelete: PendingDelete?,
  ) : EventListState

  /** The events flow or settings emitted a terminal error. */
  data class Error(val cause: Throwable) : EventListState
}

// ── Component interface ────────────────────────────────────────────────────────────────────────────

/**
 * Component interface for the event-list screen.
 *
 * All user interactions that require navigation are routed through [Output] and translated to
 * navigation operations by [com.datecountdown.app.navigation.RootComponent]. Feature modules must
 * not depend on each other — communication happens exclusively via this Output contract.
 *
 * In-screen interactions (delete, undo, toggle past section) are handled via the named methods
 * below, which forward intents to the underlying [EventListStore].
 */
interface EventListComponent {

  /** Observable UI state backed by the MVIKotlin Store. */
  val state: Value<EventListState>

  /** User tapped an event card — navigate to the counter screen (AC-NAV-1). */
  fun onCardClick(eventId: String)

  /** User tapped the FAB / "add" button — open the edit sheet (AC-NAV-2). */
  fun onAddClick()

  /**
   * User dismissed a card via swipe / long-press delete (AC-LS-9).
   *
   * Triggers the soft-delete sequence: alarm is cancelled immediately, a 5-second undo window
   * opens, and the repository delete is committed on expiry.
   */
  fun onDelete(id: EventId)

  /**
   * User tapped "Undo" in the snackbar (AC-LS-10).
   *
   * Cancels the pending delete timer and restores the alarm if the event is still upcoming.
   */
  fun onUndoDelete()

  /** User tapped the "Past" section header — toggle the collapsed state (AC-LS-11). */
  fun onTogglePast()

  /**
   * Navigation outputs emitted by the list screen.
   *
   * EditEvent is intentionally absent: there is no edit affordance on the list screen per spec.
   */
  sealed interface Output {
    /** User tapped an event card — navigate to the counter screen. */
    data class OpenCounter(val eventId: String) : Output

    /** User tapped the FAB / "add" button — open the edit sheet for creation. */
    data object AddEvent : Output
  }
}

// ── Default implementation ─────────────────────────────────────────────────────────────────────────

/**
 * Production implementation of [EventListComponent].
 *
 * The MVIKotlin [EventListStore] is retained across configuration changes via
 * `instanceKeeper.getOrCreate { ... }` and destroyed only when this component's back-stack entry
 * is popped.
 *
 * [output] is called for navigation-level actions; in-screen actions are dispatched directly
 * to the Store via the named methods.
 *
 * @param getEvents   partitions and sorts events into upcoming / past.
 * @param deleteEvent cancels the alarm and removes the event from the repository.
 * @param scheduler   used directly for immediate alarm cancellation (soft-delete) and alarm
 *                    restoration (undo) independently of [deleteEvent].
 * @param settings    persists and observes the pastCollapsed preference.
 */
@Suppress("LongParameterList")
class DefaultEventListComponent(
  componentContext: ComponentContext,
  private val storeFactory: StoreFactory,
  private val getEvents: GetEventsUseCase,
  private val deleteEvent: DeleteEventUseCase,
  private val scheduler: NotificationScheduler,
  private val settings: SettingsRepository,
  private val output: (EventListComponent.Output) -> Unit,
) : EventListComponent, ComponentContext by componentContext {

  /**
   * Retained wrapper: the Store is created once and destroyed with the component (back-stack pop),
   * not on configuration change. [InstanceKeeper.Instance.onDestroy] calls [EventListStore.dispose]
   * so that the Store's CoroutineExecutor scope is cancelled and all subscriptions are released.
   */
  private val store: EventListStore = instanceKeeper.getOrCreate {
    object : InstanceKeeper.Instance {
      val store: EventListStore = EventListStoreFactory(
        storeFactory = storeFactory,
        getEvents = getEvents,
        deleteEvent = deleteEvent,
        scheduler = scheduler,
        settings = settings,
      ).create()

      override fun onDestroy() {
        store.dispose()
      }
    }
  }.store

  override val state: Value<EventListState> = store.asValue(lifecycle)

  override fun onCardClick(eventId: String) {
    output(EventListComponent.Output.OpenCounter(eventId = eventId))
  }

  override fun onAddClick() {
    output(EventListComponent.Output.AddEvent)
  }

  override fun onDelete(id: EventId) {
    store.accept(EventListStore.Intent.DeleteEvent(id = id))
  }

  override fun onUndoDelete() {
    store.accept(EventListStore.Intent.UndoDelete)
  }

  override fun onTogglePast() {
    store.accept(EventListStore.Intent.TogglePastSection)
  }
}
