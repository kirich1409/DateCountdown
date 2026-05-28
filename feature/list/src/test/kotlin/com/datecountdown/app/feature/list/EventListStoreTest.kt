package com.datecountdown.app.feature.list

import app.cash.turbine.test
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.EventsRepository
import com.datecountdown.app.domain.NotificationScheduler
import com.datecountdown.app.domain.SettingsRepository
import com.datecountdown.app.domain.ThemeMode
import com.datecountdown.app.domain.usecase.DeleteEventUseCase
import com.datecountdown.app.domain.usecase.GetEventsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [EventListStoreFactory] / [EventListStore].
 *
 * Uses [DefaultStoreFactory] (synchronous intent dispatch) with [StandardTestDispatcher] so that
 * delays in the Executor's scope (undo-window timer) are controlled by [advanceTimeBy].
 *
 * Notes on timing:
 * - [store.accept] is synchronous: state changes from [dispatch] are visible immediately on return.
 * - [advanceUntilIdle] is required after any [scope.launch] side effect (scheduler calls,
 *   deleteEvent calls committed from AC-LS-10a).
 * - Do NOT call [advanceUntilIdle] after [store.accept(DeleteEvent)] when checking pendingDelete
 *   still active; that would also advance virtual time past the 100 ms undo window and trigger
 *   [ClearPendingDelete].
 *
 * P0 coverage: delete label, undo window timing, sequential delete (AC-LS-10a).
 * P1 coverage: bootstrap, empty repo, mixed events, undo restore, toggle settings.
 */
class EventListStoreTest {

  // ── Fixtures ──────────────────────────────────────────────────────────────────────────────────────

  private val testDispatcher = StandardTestDispatcher()

  /** Fixed "now"; upcoming events: targetDateTime > fixedNow, past events: targetDateTime < fixedNow. */
  private val fixedNow: Instant = Instant.parse("2025-06-15T12:00:00Z")
  private val fixedClock: Clock = Clock.fixed(fixedNow)

  @Before
  fun setUp() {
    // CoroutineExecutor's scope defaults to Dispatchers.Main — point it at the test scheduler
    // so delays (undo-window timer) are controlled by advanceTimeBy.
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ── Helpers ───────────────────────────────────────────────────────────────────────────────────────

  @Suppress("LongParameterList")
  private fun createStore(
    eventsFlow: MutableStateFlow<List<Event>> = MutableStateFlow(emptyList()),
    deleteRepo: FakeEventsRepository = FakeEventsRepository(),
    deleteScheduler: FakeNotificationScheduler = FakeNotificationScheduler(),
    scheduler: FakeNotificationScheduler = FakeNotificationScheduler(),
    settings: FakeSettingsRepository = FakeSettingsRepository(),
    clock: Clock = fixedClock,
    undoWindowMs: Long = 100L,
  ): EventListStore {
    val observeRepo = FakeEventsRepository(eventsFlow = eventsFlow)
    return EventListStoreFactory(
      storeFactory = DefaultStoreFactory(),
      getEvents = GetEventsUseCase(repo = observeRepo, clock = clock),
      deleteEvent = DeleteEventUseCase(repo = deleteRepo, scheduler = deleteScheduler),
      scheduler = scheduler,
      settings = settings,
      clock = clock,
      undoWindowMs = undoWindowMs,
    ).create()
  }

  // ── Bootstrap: LoadEvents ─────────────────────────────────────────────────────────────────────────

  @Test
  fun `bootstrap — empty repository — transitions to Content with empty lists`() =
    runTest(testDispatcher) {
      val store = createStore(eventsFlow = MutableStateFlow(emptyList()))
      advanceUntilIdle()

      val state = store.state as? EventListState.Content
      assertNotNull("Expected Content state after bootstrap with empty repo", state)
      assertTrue("upcoming should be empty", state!!.upcoming.isEmpty())
      assertTrue("past should be empty", state.past.isEmpty())

      store.dispose()
    }

  @Test
  fun `bootstrap — single upcoming event — appears in upcoming list`() =
    runTest(testDispatcher) {
      val event = upcomingEvent(id = "e1", offsetSeconds = 86_400L)
      val store = createStore(eventsFlow = MutableStateFlow(listOf(event)))
      advanceUntilIdle()

      val state = store.state as? EventListState.Content
      assertNotNull(state)
      assertEquals(listOf(event), state!!.upcoming)
      assertTrue(state.past.isEmpty())

      store.dispose()
    }

  @Test
  fun `bootstrap — mixed events — correctly partitioned into upcoming and past`() =
    runTest(testDispatcher) {
      val upcoming = upcomingEvent(id = "u1", offsetSeconds = 86_400L)
      val past = pastEvent(id = "p1", offsetSeconds = 86_400L)
      val store = createStore(eventsFlow = MutableStateFlow(listOf(upcoming, past)))
      advanceUntilIdle()

      val state = store.state as? EventListState.Content
      assertNotNull(state)
      assertEquals(listOf(upcoming), state!!.upcoming)
      assertEquals(listOf(past), state.past)

      store.dispose()
    }

  @Test
  fun `LoadEvents intent — re-subscribes and picks up new events`() =
    runTest(testDispatcher) {
      val eventsFlow = MutableStateFlow(listOf(upcomingEvent(id = "e1", offsetSeconds = 86_400L)))
      val store = createStore(eventsFlow = eventsFlow)
      advanceUntilIdle()

      assertEquals(1, (store.state as EventListState.Content).upcoming.size)

      eventsFlow.value = eventsFlow.value + upcomingEvent(id = "e2", offsetSeconds = 172_800L)
      store.accept(EventListStore.Intent.LoadEvents)
      advanceUntilIdle()

      assertEquals(2, (store.state as EventListState.Content).upcoming.size)

      store.dispose()
    }

  // ── Delete: label + pendingDelete ─────────────────────────────────────────────────────────────────

  @Test
  fun `DeleteEvent — publishes ShowDeletedSnackbar label with the deleted event`() =
    runTest(testDispatcher) {
      val event = upcomingEvent(id = "e1", offsetSeconds = 86_400L)
      val store = createStore(eventsFlow = MutableStateFlow(listOf(event)))
      advanceUntilIdle()

      store.labels.test {
        store.accept(EventListStore.Intent.DeleteEvent(id = event.id))

        val label = awaitItem()
        assertTrue(label is EventListStore.Label.ShowDeletedSnackbar)
        assertEquals(event, (label as EventListStore.Label.ShowDeletedSnackbar).event)

        cancelAndIgnoreRemainingEvents()
      }

      store.dispose()
    }

  @Test
  fun `DeleteEvent — sets pendingDelete immediately (synchronous dispatch)`() =
    runTest(testDispatcher) {
      val event = upcomingEvent(id = "e1", offsetSeconds = 86_400L)
      val store = createStore(eventsFlow = MutableStateFlow(listOf(event)))
      advanceUntilIdle()

      // dispatch() is synchronous in DefaultStore/CoroutineExecutor; no advanceUntilIdle needed.
      store.accept(EventListStore.Intent.DeleteEvent(id = event.id))

      val state = store.state as? EventListState.Content
      assertNotNull("pendingDelete should be set immediately after DeleteEvent", state?.pendingDelete)
      assertEquals(event, state?.pendingDelete?.event)

      store.dispose()
    }

  @Test
  fun `DeleteEvent — within undo window — repo delete is NOT yet called`() =
    runTest(testDispatcher) {
      val deleteRepo = FakeEventsRepository()
      val event = upcomingEvent(id = "e1", offsetSeconds = 86_400L)
      val store = createStore(
        eventsFlow = MutableStateFlow(listOf(event)),
        deleteRepo = deleteRepo,
        undoWindowMs = 100L,
      )
      advanceUntilIdle()

      store.accept(EventListStore.Intent.DeleteEvent(id = event.id))
      // Do NOT advance time — the 100 ms undo window has not expired.

      assertTrue("repo delete must not be called within the undo window", deleteRepo.deletedIds.isEmpty())

      store.dispose()
    }

  @Test
  fun `DeleteEvent — undo window expires — repo delete is called`() =
    runTest(testDispatcher) {
      val deleteRepo = FakeEventsRepository()
      val event = upcomingEvent(id = "e1", offsetSeconds = 86_400L)
      val store = createStore(
        eventsFlow = MutableStateFlow(listOf(event)),
        deleteRepo = deleteRepo,
        undoWindowMs = 100L,
      )
      advanceUntilIdle()

      store.accept(EventListStore.Intent.DeleteEvent(id = event.id))
      advanceTimeBy(delayTimeMillis = 150L)

      assertTrue("repo delete should be called after undo window expires", deleteRepo.deletedIds.contains(event.id))

      store.dispose()
    }

  @Test
  fun `DeleteEvent — scheduler cancel called via launched coroutine`() =
    runTest(testDispatcher) {
      val scheduler = FakeNotificationScheduler()
      val event = upcomingEvent(id = "e1", offsetSeconds = 86_400L)
      val store = createStore(
        eventsFlow = MutableStateFlow(listOf(event)),
        scheduler = scheduler,
        undoWindowMs = 100L,
      )
      advanceUntilIdle()

      store.accept(EventListStore.Intent.DeleteEvent(id = event.id))
      // scheduler.cancel is called inside scope.launch, so drain it.
      advanceUntilIdle()

      assertTrue("scheduler.cancel must be called", scheduler.cancelledIds.contains(event.id))

      store.dispose()
    }

  // ── Undo delete ───────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `UndoDelete — within window — clears pendingDelete (synchronous) and does not call repo delete`() =
    runTest(testDispatcher) {
      val deleteRepo = FakeEventsRepository()
      val event = upcomingEvent(id = "e1", offsetSeconds = 86_400L)
      val store = createStore(
        eventsFlow = MutableStateFlow(listOf(event)),
        deleteRepo = deleteRepo,
        undoWindowMs = 100L,
      )
      advanceUntilIdle()

      store.accept(EventListStore.Intent.DeleteEvent(id = event.id))
      // dispatch(ClearPendingDelete) from UndoDelete is synchronous; check state immediately.
      store.accept(EventListStore.Intent.UndoDelete)

      val state = store.state as? EventListState.Content
      assertNull("pendingDelete should be null after UndoDelete", state?.pendingDelete)
      assertTrue("repo delete must NOT be called after undo", deleteRepo.deletedIds.isEmpty())

      store.dispose()
    }

  @Test
  fun `UndoDelete — upcoming event — alarm rescheduled via launched coroutine`() =
    runTest(testDispatcher) {
      val scheduler = FakeNotificationScheduler()
      val event = upcomingEvent(id = "e1", offsetSeconds = 86_400L)
      val store = createStore(
        eventsFlow = MutableStateFlow(listOf(event)),
        scheduler = scheduler,
        undoWindowMs = 100L,
      )
      advanceUntilIdle()

      store.accept(EventListStore.Intent.DeleteEvent(id = event.id))
      store.accept(EventListStore.Intent.UndoDelete)
      // scheduler.schedule is called inside scope.launch; drain to pick it up.
      advanceUntilIdle()

      assertTrue(
        "alarm should be rescheduled for upcoming event after undo",
        scheduler.scheduledEventIds.contains(event.id),
      )

      store.dispose()
    }

  @Test
  fun `UndoDelete — past event — alarm is NOT rescheduled`() = runTest(testDispatcher) {
    val scheduler = FakeNotificationScheduler()
    val event = pastEvent(id = "p1", offsetSeconds = 86_400L)
    val store = createStore(
      eventsFlow = MutableStateFlow(listOf(event)),
      scheduler = scheduler,
      undoWindowMs = 100L,
    )
    advanceUntilIdle()

    store.accept(EventListStore.Intent.DeleteEvent(id = event.id))
    store.accept(EventListStore.Intent.UndoDelete)
    advanceUntilIdle()

    assertTrue("alarm must NOT be rescheduled for a past event", scheduler.scheduledEventIds.isEmpty())

    store.dispose()
  }

  // ── Sequential delete (AC-LS-10a) ─────────────────────────────────────────────────────────────────

  @Test
  @Suppress("LongMethod")
  fun `sequential DeleteEvent — second delete commits first immediately (AC-LS-10a)`() =
    runTest(testDispatcher) {
      val deleteRepo = FakeEventsRepository()
      val eventA = upcomingEvent(id = "eA", offsetSeconds = 86_400L)
      val eventB = upcomingEvent(id = "eB", offsetSeconds = 172_800L)
      val eventsFlow = MutableStateFlow(listOf(eventA, eventB))
      val store = createStore(
        eventsFlow = eventsFlow,
        deleteRepo = deleteRepo,
        undoWindowMs = 100L,
      )
      advanceUntilIdle()

      // Delete A — undo window opens; A should not be committed yet.
      store.accept(EventListStore.Intent.DeleteEvent(id = eventA.id))
      assertTrue("A should not be deleted yet (undo window open)", deleteRepo.deletedIds.isEmpty())

      // Delete B before A's window expires — AC-LS-10a: A must be committed synchronously.
      // Update the flow so B is now visible to the store state when softDelete(B) calls state().
      eventsFlow.value = listOf(eventB)
      advanceUntilIdle()  // drain SetData so state sees the updated flow
      store.accept(EventListStore.Intent.DeleteEvent(id = eventB.id))
      // The AC-LS-10a commit launches scope.launch { deleteEvent(A) }; drain it with a
      // 1 ms step so that the immediately-queued coroutines (deleteEvent(A), scheduler.cancel(B))
      // run, but B's 100 ms undo-window timer does NOT fire yet.
      advanceTimeBy(delayTimeMillis = 1L)

      assertTrue("A must be committed when B's delete is issued (AC-LS-10a)", deleteRepo.deletedIds.contains(eventA.id))

      val state = store.state as? EventListState.Content
      assertEquals(eventB, state?.pendingDelete?.event)

      store.dispose()
    }

  // ── Settings intents ──────────────────────────────────────────────────────────────────────────────

  @Test
  fun `TogglePastSection — calls setPastCollapsed with negated current value`() =
    runTest(testDispatcher) {
      val settings = FakeSettingsRepository(initialPastCollapsed = false)
      val store = createStore(settings = settings)
      advanceUntilIdle()

      store.accept(EventListStore.Intent.TogglePastSection)
      advanceUntilIdle()

      assertTrue("setPastCollapsed should have been called", settings.pastCollapsedSetCalls.isNotEmpty())
      assertEquals(true, settings.pastCollapsedSetCalls.last())

      store.dispose()
    }

  @Test
  fun `UpdateThemeMode — calls setThemeMode with the provided mode`() =
    runTest(testDispatcher) {
      val settings = FakeSettingsRepository()
      val store = createStore(settings = settings)
      advanceUntilIdle()

      store.accept(EventListStore.Intent.UpdateThemeMode(mode = ThemeMode.DARK))
      advanceUntilIdle()

      assertTrue("setThemeMode should have been called", settings.themeModeSetCalls.isNotEmpty())
      assertEquals(ThemeMode.DARK, settings.themeModeSetCalls.last())

      store.dispose()
    }

  // ── Event helpers ─────────────────────────────────────────────────────────────────────────────────

  /** Event whose targetDateTime is [offsetSeconds] seconds AFTER fixedNow (upcoming). */
  private fun upcomingEvent(id: String, offsetSeconds: Long): Event = Event(
    id = EventId(id),
    title = "Upcoming $id",
    targetDateTime = fixedNow.plus(offsetSeconds.seconds),
    color = EventColor.BLUE,
    icon = EventIcon.CELEBRATION,
    createdAt = fixedNow,
  )

  /** Event whose targetDateTime is [offsetSeconds] seconds BEFORE fixedNow (past). */
  private fun pastEvent(id: String, offsetSeconds: Long): Event = Event(
    id = EventId(id),
    title = "Past $id",
    targetDateTime = fixedNow.minus(offsetSeconds.seconds),
    color = EventColor.RED,
    icon = EventIcon.CAKE,
    createdAt = fixedNow,
  )
}

// ── Test fakes ────────────────────────────────────────────────────────────────────────────────────

/** Fixed-time [Clock] for deterministic tests. */
private fun Clock.Companion.fixed(now: Instant): Clock = object : Clock {
  override fun now(): Instant = now
}

/** In-memory [EventsRepository] that tracks delete calls. */
private class FakeEventsRepository(
  private val eventsFlow: MutableStateFlow<List<Event>> = MutableStateFlow(emptyList()),
) : EventsRepository {
  val deletedIds = mutableListOf<EventId>()

  override fun observeEvents(): Flow<List<Event>> = eventsFlow
  override suspend fun add(event: Event) = Unit
  override suspend fun update(event: Event) = Unit
  override suspend fun delete(id: EventId) { deletedIds += id }
  override suspend fun getById(id: EventId): Event? = eventsFlow.value.firstOrNull { it.id == id }
}

/** Fake [NotificationScheduler] that records schedule and cancel calls. */
private class FakeNotificationScheduler : NotificationScheduler {
  val scheduledEventIds = mutableListOf<EventId>()
  val cancelledIds = mutableListOf<EventId>()

  override suspend fun schedule(event: Event) { scheduledEventIds += event.id }
  override suspend fun cancel(id: EventId) { cancelledIds += id }
}

/** Fake [SettingsRepository] backed by [MutableStateFlow]s with setter call tracking. */
private class FakeSettingsRepository(
  initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
  initialPastCollapsed: Boolean = false,
) : SettingsRepository {

  val themeModeSetCalls = mutableListOf<ThemeMode>()
  val pastCollapsedSetCalls = mutableListOf<Boolean>()

  private val _themeMode = MutableStateFlow(initialThemeMode)
  private val _pastCollapsedState = MutableStateFlow(initialPastCollapsed)

  override val themeMode: Flow<ThemeMode> = _themeMode
  override val pastCollapsed: Flow<Boolean> = _pastCollapsedState

  override suspend fun setThemeMode(mode: ThemeMode) {
    themeModeSetCalls += mode
    _themeMode.value = mode
  }

  override suspend fun setPastCollapsed(value: Boolean) {
    pastCollapsedSetCalls += value
    _pastCollapsedState.value = value
  }

  override val notificationsPermissionRequested: Flow<Boolean> = MutableStateFlow(false)
  override suspend fun setNotificationsPermissionRequested() = Unit
}
