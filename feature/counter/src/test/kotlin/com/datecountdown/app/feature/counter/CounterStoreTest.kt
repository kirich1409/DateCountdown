package com.datecountdown.app.feature.counter

import app.cash.turbine.test
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.datecountdown.app.domain.CountdownCalculator
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.EventsRepository
import com.datecountdown.app.domain.NotificationScheduler
import com.datecountdown.app.domain.PastEventProcessor
import com.datecountdown.app.domain.usecase.DeleteEventUseCase
import com.datecountdown.app.domain.usecase.GetEventUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CounterStoreFactory] / [CounterStore].
 *
 * Uses [DefaultStoreFactory] (synchronous executors) — no mvikotlin-main-test in the catalog.
 *
 * [StandardTestDispatcher] is passed to both [Dispatchers.setMain] and every [runTest] call
 * so that delays inside the Executor's scope (1 Hz tick loop) share the same test scheduler
 * as [advanceTimeBy].
 *
 * P0 coverage: bootstrap branches (Upcoming/Past/NotFound/Error), delete→NavigateBack label.
 * P1 coverage: Tick intent recomputes countdown, Upcoming→Past transition, StartTicking/StopTicking lifecycle.
 */
class CounterStoreTest {

  // ── Fixtures ──────────────────────────────────────────────────────────────────────────────────────

  private val testDispatcher = StandardTestDispatcher()

  private val fixedNow: Instant = Instant.parse("2025-06-15T12:00:00Z")

  @Before
  fun setUp() {
    // CoroutineExecutor's scope defaults to Dispatchers.Main — point it at the test scheduler
    // so that the 1 Hz tick delay is controlled by advanceTimeBy.
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ── Helpers ───────────────────────────────────────────────────────────────────────────────────────

  private fun createStore(
    eventId: EventId = EventId("e1"),
    repo: FakeEventsRepository = FakeEventsRepository(),
    clock: Clock = Clock.fixed(fixedNow),
  ): CounterStore = CounterStoreFactory(
    storeFactory = DefaultStoreFactory(),
    eventId = eventId,
    getEvent = GetEventUseCase(repo = repo),
    deleteEvent = DeleteEventUseCase(repo = repo, scheduler = NoOpNotificationScheduler),
    calculator = CountdownCalculator(),
    pastProcessor = PastEventProcessor(),
    clock = clock,
  ).create()

  // ── Bootstrap ─────────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `bootstrap — event in future — transitions to Upcoming`() = runTest(testDispatcher) {
    val target = fixedNow.plus(86_400.seconds)
    val event = testEvent(id = "e1", targetDateTime = target)
    val store = createStore(repo = FakeEventsRepository(event = event))
    advanceUntilIdle()

    assertTrue("Expected Upcoming state for a future event", store.state is CounterState.Upcoming)
    assertEquals(event, (store.state as CounterState.Upcoming).event)

    store.dispose()
  }

  @Test
  fun `bootstrap — event in past — transitions to Past`() = runTest(testDispatcher) {
    val target = fixedNow.minus(86_400.seconds)
    val event = testEvent(id = "e1", targetDateTime = target)
    val store = createStore(repo = FakeEventsRepository(event = event))
    advanceUntilIdle()

    assertTrue("Expected Past state for a past event", store.state is CounterState.Past)
    assertEquals(event, (store.state as CounterState.Past).event)

    store.dispose()
  }

  @Test
  fun `bootstrap — event not found — transitions to NotFound`() = runTest(testDispatcher) {
    val store = createStore(repo = FakeEventsRepository(event = null))
    advanceUntilIdle()

    assertEquals(CounterState.NotFound, store.state)

    store.dispose()
  }

  @Test
  fun `bootstrap — repository throws — transitions to Error`() = runTest(testDispatcher) {
    val cause = RuntimeException("DB unavailable")
    val store = createStore(repo = FakeEventsRepository(throwOnGet = cause))
    advanceUntilIdle()

    val errorState = store.state as? CounterState.Error
    assertNotNull("Expected Error state on repository exception", errorState)
    assertEquals(cause, errorState!!.cause)

    store.dispose()
  }

  // ── Tick ─────────────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `Tick intent — recomputes countdown with updated time`() = runTest(testDispatcher) {
    val target = fixedNow.plus(3600.seconds)
    val event = testEvent(id = "e1", targetDateTime = target)
    val store = createStore(repo = FakeEventsRepository(event = event))
    advanceUntilIdle()

    val stateBefore = store.state as CounterState.Upcoming
    val totalSecsBefore = stateBefore.countdown.hours * 3600 +
      stateBefore.countdown.minutes * 60 +
      stateBefore.countdown.seconds

    // Intent.Tick is documented as directly usable in tests for deterministic time control.
    store.accept(CounterStore.Intent.Tick(now = fixedNow.plus(10.seconds)))
    advanceUntilIdle()

    val stateAfter = store.state as CounterState.Upcoming
    val totalSecsAfter = stateAfter.countdown.hours * 3600 +
      stateAfter.countdown.minutes * 60 +
      stateAfter.countdown.seconds

    assertTrue("countdown should decrease after Tick", totalSecsAfter < totalSecsBefore)

    store.dispose()
  }

  @Test
  fun `Tick — crossing target time — transitions from Upcoming to Past`() =
    runTest(testDispatcher) {
      val target = fixedNow.plus(5.seconds)
      val event = testEvent(id = "e1", targetDateTime = target)
      val store = createStore(repo = FakeEventsRepository(event = event))
      advanceUntilIdle()

      assertTrue("Should be Upcoming before crossing target", store.state is CounterState.Upcoming)

      store.accept(CounterStore.Intent.Tick(now = target.plus(1.seconds)))
      advanceUntilIdle()

      assertTrue("Should be Past after tick crosses target", store.state is CounterState.Past)

      store.dispose()
    }

  @Test
  fun `StartTicking and StopTicking — ticker launches then halts updates`() =
    runTest(testDispatcher) {
      val target = fixedNow.plus(3600.seconds)
      val event = testEvent(id = "e1", targetDateTime = target)

      // Use a mutable clock so the tick loop reads advancing time.
      val mutableClock = MutableClock(fixedNow)
      val store = createStore(
        repo = FakeEventsRepository(event = event),
        clock = mutableClock,
      )
      advanceUntilIdle()

      store.accept(CounterStore.Intent.StartTicking)

      // Advance virtual time past the 1 Hz interval; update clock to simulate passage of time.
      mutableClock.now = fixedNow.plus(2.seconds)
      advanceTimeBy(delayTimeMillis = 1_100L)

      assertNotNull("Should still be Upcoming while ticking", store.state as? CounterState.Upcoming)

      store.accept(CounterStore.Intent.StopTicking)

      // After stop, further time advances should not change the countdown.
      val countdownAfterStop = (store.state as CounterState.Upcoming).countdown
      mutableClock.now = fixedNow.plus(10.seconds)
      advanceTimeBy(delayTimeMillis = 5_000L)

      assertEquals(
        "Countdown must not change after StopTicking",
        countdownAfterStop,
        (store.state as CounterState.Upcoming).countdown,
      )

      store.dispose()
    }

  @Test
  fun `StartTicking — calling twice is idempotent`() = runTest(testDispatcher) {
    val event = testEvent(id = "e1", targetDateTime = fixedNow.plus(3600.seconds))
    val store = createStore(repo = FakeEventsRepository(event = event))
    advanceUntilIdle()

    store.accept(CounterStore.Intent.StartTicking)
    store.accept(CounterStore.Intent.StartTicking)

    assertTrue("State should still be Upcoming", store.state is CounterState.Upcoming)

    store.dispose()
  }

  // ── Delete ────────────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `Delete — from Upcoming state — calls deleteEvent and publishes NavigateBack`() =
    runTest(testDispatcher) {
      val repo = FakeEventsRepository(event = testEvent(id = "e1", targetDateTime = fixedNow.plus(3600.seconds)))
      val store = createStore(eventId = EventId("e1"), repo = repo)
      advanceUntilIdle()

      store.labels.test {
        store.accept(CounterStore.Intent.Delete)
        advanceUntilIdle()

        val label = awaitItem()
        assertEquals(CounterStore.Label.NavigateBack, label)

        cancelAndIgnoreRemainingEvents()
      }

      assertTrue("deleteEvent should have been called", repo.deletedIds.contains(EventId("e1")))

      store.dispose()
    }

  @Test
  fun `Delete — from Past state — calls deleteEvent and publishes NavigateBack`() =
    runTest(testDispatcher) {
      val repo = FakeEventsRepository(event = testEvent(id = "e1", targetDateTime = fixedNow.minus(3600.seconds)))
      val store = createStore(eventId = EventId("e1"), repo = repo)
      advanceUntilIdle()

      store.labels.test {
        store.accept(CounterStore.Intent.Delete)
        advanceUntilIdle()

        val label = awaitItem()
        assertEquals(CounterStore.Label.NavigateBack, label)

        cancelAndIgnoreRemainingEvents()
      }

      assertTrue("deleteEvent should have been called", repo.deletedIds.contains(EventId("e1")))

      store.dispose()
    }

  @Test
  fun `Delete — from NotFound state — is a no-op`() = runTest(testDispatcher) {
    val repo = FakeEventsRepository(event = null)
    val store = createStore(repo = repo)
    advanceUntilIdle()

    assertEquals(CounterState.NotFound, store.state)

    store.labels.test {
      store.accept(CounterStore.Intent.Delete)
      advanceUntilIdle()
      expectNoEvents()
    }

    assertTrue("deleteEvent must NOT be called when state is NotFound", repo.deletedIds.isEmpty())

    store.dispose()
  }

  // ── Event helpers ─────────────────────────────────────────────────────────────────────────────────

  private fun testEvent(id: String, targetDateTime: Instant): Event = Event(
    id = EventId(id),
    title = "Test event $id",
    targetDateTime = targetDateTime,
    color = EventColor.BLUE,
    icon = EventIcon.CELEBRATION,
    createdAt = fixedNow,
  )
}

// ── Test fakes ────────────────────────────────────────────────────────────────────────────────────

/** Fixed-time [Clock] for deterministic tests. */
private fun Clock.Companion.fixed(now: Instant): Clock = object : Clock {
  override fun now(): Instant = now
}

/** Mutable clock used in tick-lifecycle tests where advancing time is required. */
private class MutableClock(var now: Instant) : Clock {
  override fun now(): Instant = now
}

/** In-memory [EventsRepository] that supports a single event and tracks delete calls. */
private class FakeEventsRepository(
  private val event: Event? = null,
  private val throwOnGet: Exception? = null,
) : EventsRepository {
  val deletedIds = mutableListOf<EventId>()

  override fun observeEvents(): Flow<List<Event>> =
    MutableStateFlow(if (event != null) listOf(event) else emptyList())

  override suspend fun add(event: Event) = Unit
  override suspend fun update(event: Event) = Unit

  override suspend fun delete(id: EventId) {
    deletedIds += id
  }

  override suspend fun getById(id: EventId): Event? {
    if (throwOnGet != null) throw throwOnGet
    return event?.takeIf { it.id == id }
  }
}

/** No-op [NotificationScheduler] — Counter tests do not need alarm side-effects. */
private object NoOpNotificationScheduler : NotificationScheduler {
  override suspend fun schedule(event: Event) = Unit
  override suspend fun cancel(id: EventId) = Unit
}
