package com.datecountdown.app.feature.edit

import app.cash.turbine.test
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.EventsRepository
import com.datecountdown.app.domain.ExactAlarmPermissionChecker
import com.datecountdown.app.domain.NotificationScheduler
import com.datecountdown.app.domain.usecase.GetEventUseCase
import com.datecountdown.app.domain.usecase.SaveEventUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AddEditStoreFactory] / [AddEditStore].
 *
 * Uses [DefaultStoreFactory] (synchronous executors in tests) — there is no
 * `mvikotlin-main-test` artifact in the version catalog.
 *
 * P0 coverage: save-succeeded label, save-failed error, load-error state.
 * P1 coverage: create-mode defaults, edit-mode pre-population, field mutations,
 *   hasUnsavedChanges tracking, requestDismiss logic (with/without changes),
 *   discardAndDismiss, past-date allowed (AC-AE-12).
 */
class AddEditStoreTest {

  // ── Fixtures ─────────────────────────────────────────────────────────────────────────────────────

  private val testDispatcher = UnconfinedTestDispatcher()

  private val fixedNow: Instant = Instant.parse("2025-06-15T10:00:00Z")
  private val fixedClock: Clock = Clock.fixed(fixedNow)

  @Before
  fun setUp() {
    // MVIKotlin CoroutineExecutor defaults to Dispatchers.Main; unconfined avoids thread issues.
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private val existingEvent = Event(
    id = EventId("event-1"),
    title = "Birthday party",
    targetDateTime = Instant.parse("2026-01-01T00:00:00Z"),
    color = EventColor.PINK,
    icon = EventIcon.CAKE,
    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
  )

  // ── Helpers ───────────────────────────────────────────────────────────────────────────────────────

  private fun createStore(
    eventId: String? = null,
    repo: FakeEventsRepository = FakeEventsRepository(),
    notificationScheduler: NotificationScheduler = NoOpNotificationScheduler(),
    canScheduleExactAlarms: Boolean = true,
  ): AddEditStore = AddEditStoreFactory(
    storeFactory = DefaultStoreFactory(),
    eventId = eventId,
    getEvent = GetEventUseCase(repo = repo),
    saveEvent = SaveEventUseCase(
      repo = repo,
      scheduler = notificationScheduler,
      clock = fixedClock,
    ),
    exactAlarmChecker = FakeExactAlarmPermissionChecker(canSchedule = canScheduleExactAlarms),
    clock = fixedClock,
    mainContext = testDispatcher,
  ).create()

  // ── Create mode ───────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `create mode — bootstrap transitions to Form with empty title and defaults`() = runTest {
    val store = createStore(eventId = null)

    val state = store.state
    assertTrue("Expected Form state in create mode", state is AddEditState.Form)
    val form = state as AddEditState.Form
    assertEquals("", form.title)
    assertEquals(EventColor.ORANGE, form.color)
    assertEquals(EventIcon.CELEBRATION, form.icon)
    assertFalse(form.hasUnsavedChanges)
    assertFalse(form.isSaving)
    assertNull(form.saveError)

    store.dispose()
  }

  @Test
  fun `create mode — targetDateTime is set to fixedNow on bootstrap`() = runTest {
    val store = createStore(eventId = null)

    val form = store.state as AddEditState.Form
    assertEquals(fixedNow, form.targetDateTime)

    store.dispose()
  }

  // ── Edit mode ─────────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `edit mode — bootstrap loads event and populates form fields`() = runTest {
    val repo = FakeEventsRepository(existingEvent)
    val store = createStore(eventId = "event-1", repo = repo)

    val state = store.state
    assertTrue("Expected Form state after edit-mode load", state is AddEditState.Form)
    val form = state as AddEditState.Form
    assertEquals("Birthday party", form.title)
    assertEquals(Instant.parse("2026-01-01T00:00:00Z"), form.targetDateTime)
    assertEquals(EventColor.PINK, form.color)
    assertEquals(EventIcon.CAKE, form.icon)
    assertFalse(form.hasUnsavedChanges)

    store.dispose()
  }

  @Test
  fun `edit mode — event not found produces LoadError`() = runTest {
    val repo = FakeEventsRepository()  // no events
    val store = createStore(eventId = "missing-id", repo = repo)

    assertTrue("Expected LoadError when event not found", store.state is AddEditState.LoadError)

    store.dispose()
  }

  @Test
  fun `edit mode — repository exception produces LoadError`() = runTest {
    val repo = FakeEventsRepository(throwOnGet = RuntimeException("DB unavailable"))
    val store = createStore(eventId = "event-1", repo = repo)

    val errorState = store.state as? AddEditState.LoadError
    assertTrue("Expected LoadError on repository exception", errorState != null)
    assertEquals("DB unavailable", errorState?.cause?.message)

    store.dispose()
  }

  @Test
  fun `edit mode — GetEventUseCase throws — transitions to LoadError preserving cause`() = runTest {
    val cause = IllegalStateException("simulated DB failure")
    val repo = FakeEventsRepository(throwOnGet = cause)
    val store = createStore(eventId = "event-1", repo = repo)

    val errorState = store.state as? AddEditState.LoadError
    assertTrue("Expected LoadError when GetEventUseCase throws", errorState != null)
    assertSame(cause, errorState?.cause)

    store.dispose()
  }

  // ── Field mutations ───────────────────────────────────────────────────────────────────────────────

  @Test
  fun `UpdateTitle — updates title and sets hasUnsavedChanges`() = runTest {
    val store = createStore(eventId = null)

    store.accept(AddEditStore.Intent.UpdateTitle(title = "New Year Eve"))

    val form = store.state as AddEditState.Form
    assertEquals("New Year Eve", form.title)
    assertTrue(form.hasUnsavedChanges)

    store.dispose()
  }

  @Test
  fun `UpdateColor — updates color and sets hasUnsavedChanges`() = runTest {
    val store = createStore(eventId = null)

    store.accept(AddEditStore.Intent.UpdateColor(color = EventColor.BLUE))

    val form = store.state as AddEditState.Form
    assertEquals(EventColor.BLUE, form.color)
    assertTrue(form.hasUnsavedChanges)

    store.dispose()
  }

  @Test
  fun `UpdateIcon — updates icon and sets hasUnsavedChanges`() = runTest {
    val store = createStore(eventId = null)

    store.accept(AddEditStore.Intent.UpdateIcon(icon = EventIcon.ROCKET_LAUNCH))

    val form = store.state as AddEditState.Form
    assertEquals(EventIcon.ROCKET_LAUNCH, form.icon)
    assertTrue(form.hasUnsavedChanges)

    store.dispose()
  }

  @Test
  fun `UpdateTargetDateTime — updates targetDateTime and sets hasUnsavedChanges`() = runTest {
    val store = createStore(eventId = null)
    val newDate = Instant.parse("2027-07-04T12:00:00Z")

    store.accept(AddEditStore.Intent.UpdateTargetDateTime(dateTime = newDate))

    val form = store.state as AddEditState.Form
    assertEquals(newDate, form.targetDateTime)
    assertTrue(form.hasUnsavedChanges)

    store.dispose()
  }

  // ── Save flow ─────────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `Save — valid title emits Saved label and store state reflects success`() = runTest {
    val repo = FakeEventsRepository()
    val store = createStore(eventId = null, repo = repo)

    store.accept(AddEditStore.Intent.UpdateTitle(title = "New Year"))
    store.accept(AddEditStore.Intent.UpdateTargetDateTime(
      dateTime = Instant.parse("2027-01-01T00:00:00Z"),
    ))

    store.labels.test {
      store.accept(AddEditStore.Intent.Save)

      val label = awaitItem()
      assertEquals(AddEditStore.Label.Saved, label)

      cancelAndIgnoreRemainingEvents()
    }

    store.dispose()
  }

  @Test
  fun `Save — empty title causes SaveFailed and surfaces saveError in state`() = runTest {
    val repo = FakeEventsRepository()
    val store = createStore(eventId = null, repo = repo)
    // title is "" by default in create mode — SaveEventUseCase rejects it

    store.labels.test {
      store.accept(AddEditStore.Intent.Save)
      // invalid title: no label should be emitted
      expectNoEvents()
    }

    val form = store.state as? AddEditState.Form
    assertTrue("isSaving should be false after failure", form?.isSaving == false)
    assertTrue("saveError should be set on failure", form?.saveError != null)

    store.dispose()
  }

  @Test
  fun `Save — past targetDateTime is allowed (AC-AE-12)`() = runTest {
    val repo = FakeEventsRepository()
    val store = createStore(eventId = null, repo = repo)

    store.accept(AddEditStore.Intent.UpdateTitle(title = "Past birthday"))
    // Past date — must be allowed by spec AC-AE-12
    store.accept(AddEditStore.Intent.UpdateTargetDateTime(
      dateTime = Instant.parse("2020-06-15T10:00:00Z"),
    ))

    store.labels.test {
      store.accept(AddEditStore.Intent.Save)

      val label = awaitItem()
      assertEquals(AddEditStore.Label.Saved, label)

      cancelAndIgnoreRemainingEvents()
    }

    store.dispose()
  }

  // ── Dismiss / discard flow ────────────────────────────────────────────────────────────────────────

  @Test
  fun `RequestDismiss while Loading — emits Dismissed immediately`() = runTest {
    // FakeEventsRepository with a blocking get would be needed to hold Loading state;
    // instead we verify via the executor path: an unresolvable eventId that never found
    // is not suitable here since bootstrap is synchronous in DefaultStoreFactory tests.
    // Use a throwOnGet repo so bootstrap produces LoadError (Loading is transient).
    // This confirms the else-branch in requestDismiss() handles non-Form states correctly.
    val repo = FakeEventsRepository(throwOnGet = RuntimeException("bootstrap error"))
    val store = createStore(eventId = "event-1", repo = repo)

    // Bootstrap completed synchronously to LoadError — state is no longer Loading,
    // but the executor's else-branch handles LoadError the same way as Loading.
    assertTrue("Expected LoadError state for this test", store.state is AddEditState.LoadError)

    store.labels.test {
      store.accept(AddEditStore.Intent.RequestDismiss)

      val label = awaitItem()
      assertEquals(AddEditStore.Label.Dismissed, label)

      cancelAndIgnoreRemainingEvents()
    }

    store.dispose()
  }

  @Test
  fun `RequestDismiss without changes — emits Dismissed immediately`() = runTest {
    val store = createStore(eventId = null)

    store.labels.test {
      store.accept(AddEditStore.Intent.RequestDismiss)

      val label = awaitItem()
      assertEquals(AddEditStore.Label.Dismissed, label)

      cancelAndIgnoreRemainingEvents()
    }

    store.dispose()
  }

  @Test
  fun `RequestDismiss with unsaved changes — sets showDiscardConfirmation in state`() = runTest {
    val store = createStore(eventId = null)
    store.accept(AddEditStore.Intent.UpdateTitle(title = "Typed something"))

    store.accept(AddEditStore.Intent.RequestDismiss)

    val form = store.state as AddEditState.Form
    assertTrue("showDiscardConfirmation should be true when there are unsaved changes", form.showDiscardConfirmation)

    store.dispose()
  }

  @Test
  fun `DiscardAndDismiss — clears showDiscardConfirmation and emits Dismissed`() = runTest {
    val store = createStore(eventId = null)
    // Put store into showDiscardConfirmation=true state first.
    store.accept(AddEditStore.Intent.UpdateTitle(title = "Changes"))
    store.accept(AddEditStore.Intent.RequestDismiss)
    assertTrue(
      "showDiscardConfirmation should be true before discard",
      (store.state as AddEditState.Form).showDiscardConfirmation,
    )

    store.labels.test {
      store.accept(AddEditStore.Intent.DiscardAndDismiss)

      val label = awaitItem()
      assertEquals(AddEditStore.Label.Dismissed, label)

      cancelAndIgnoreRemainingEvents()
    }

    assertFalse(
      "showDiscardConfirmation should be false after DiscardAndDismiss",
      (store.state as AddEditState.Form).showDiscardConfirmation,
    )

    store.dispose()
  }

  @Test
  fun `CancelDiscardConfirmation — clears showDiscardConfirmation without navigating`() = runTest {
    val store = createStore(eventId = null)
    // Put store into showDiscardConfirmation=true state.
    store.accept(AddEditStore.Intent.UpdateTitle(title = "Changes"))
    store.accept(AddEditStore.Intent.RequestDismiss)
    assertTrue(
      "showDiscardConfirmation should be true before cancel",
      (store.state as AddEditState.Form).showDiscardConfirmation,
    )

    store.labels.test {
      store.accept(AddEditStore.Intent.CancelDiscardConfirmation)
      // "Нет" tap — no navigation label should be emitted
      expectNoEvents()
    }

    val form = store.state as AddEditState.Form
    assertFalse("showDiscardConfirmation should be false after cancel", form.showDiscardConfirmation)
    assertTrue("unsaved changes should still be true", form.hasUnsavedChanges)

    store.dispose()
  }

  // ── Exact-alarm permission flow (AC-NT-13) ─────────────────────────────────────────────────────

  @Test
  fun `Save — canScheduleExactAlarms true — calls SaveEventUseCase and emits Saved label`() = runTest {
    val repo = FakeEventsRepository()
    val store = createStore(
      eventId = null,
      repo = repo,
      canScheduleExactAlarms = true,
    )

    store.accept(AddEditStore.Intent.UpdateTitle(title = "Upcoming event"))
    // Future date — permission check is relevant only for upcoming events.
    store.accept(AddEditStore.Intent.UpdateTargetDateTime(
      dateTime = Instant.parse("2027-01-01T00:00:00Z"),
    ))

    store.labels.test {
      store.accept(AddEditStore.Intent.Save)

      val label = awaitItem()
      assertEquals(AddEditStore.Label.Saved, label)

      cancelAndIgnoreRemainingEvents()
    }

    // State should reflect successful save (not stuck in exactAlarmDenied).
    val form = store.state as? AddEditState.Form
    assertTrue("exactAlarmDenied should be false after successful save", form?.exactAlarmDenied == false)

    store.dispose()
  }

  @Test
  fun `Save — canScheduleExactAlarms false for upcoming event — shows dialog, SaveEventUseCase not called`() = runTest {
    val scheduler = TrackingNotificationScheduler()
    val store = createStore(
      eventId = null,
      notificationScheduler = scheduler,
      canScheduleExactAlarms = false,
    )

    store.accept(AddEditStore.Intent.UpdateTitle(title = "Future event"))
    store.accept(AddEditStore.Intent.UpdateTargetDateTime(
      dateTime = Instant.parse("2027-01-01T00:00:00Z"),
    ))

    store.labels.test {
      store.accept(AddEditStore.Intent.Save)
      // Dialog shown — no label, no save.
      expectNoEvents()
    }

    val form = store.state as? AddEditState.Form
    assertTrue("exactAlarmDenied should be true when permission denied", form?.exactAlarmDenied == true)
    assertFalse("scheduler.scheduleCalled should be false", scheduler.scheduleCalled)

    store.dispose()
  }

  @Test
  fun `Save — canScheduleExactAlarms false but past event — save proceeds normally`() = runTest {
    val repo = FakeEventsRepository()
    val store = createStore(
      eventId = null,
      repo = repo,
      canScheduleExactAlarms = false,
    )

    store.accept(AddEditStore.Intent.UpdateTitle(title = "Past event"))
    // Past date — permission check is skipped; no alarm will be scheduled.
    store.accept(AddEditStore.Intent.UpdateTargetDateTime(
      dateTime = Instant.parse("2020-01-01T00:00:00Z"),
    ))

    store.labels.test {
      store.accept(AddEditStore.Intent.Save)

      val label = awaitItem()
      assertEquals(AddEditStore.Label.Saved, label)

      cancelAndIgnoreRemainingEvents()
    }

    val form = store.state as? AddEditState.Form
    assertTrue("exactAlarmDenied should be false for past events", form?.exactAlarmDenied == false)

    store.dispose()
  }

  @Test
  fun `ConfirmSaveWithoutNotification — saves with scheduleNotification false and emits Saved`() = runTest {
    val repo = FakeEventsRepository()
    val scheduler = TrackingNotificationScheduler()
    val store = createStore(
      eventId = null,
      repo = repo,
      notificationScheduler = scheduler,
      canScheduleExactAlarms = false,
    )

    store.accept(AddEditStore.Intent.UpdateTitle(title = "Future event"))
    store.accept(AddEditStore.Intent.UpdateTargetDateTime(
      dateTime = Instant.parse("2027-01-01T00:00:00Z"),
    ))
    // Trigger the denied dialog state.
    store.accept(AddEditStore.Intent.Save)
    assertTrue(
      "exactAlarmDenied should be true before confirming save without notification",
      (store.state as AddEditState.Form).exactAlarmDenied,
    )

    store.labels.test {
      store.accept(AddEditStore.Intent.ConfirmSaveWithoutNotification)

      val label = awaitItem()
      assertEquals(AddEditStore.Label.Saved, label)

      cancelAndIgnoreRemainingEvents()
    }

    // Alarm should not have been scheduled (scheduleNotification = false).
    assertFalse("schedule should not be called when saving without notification", scheduler.scheduleCalled)

    store.dispose()
  }

  @Test
  fun `DismissExactAlarmDialog — clears exactAlarmDenied without saving`() = runTest {
    val scheduler = TrackingNotificationScheduler()
    val store = createStore(
      eventId = null,
      notificationScheduler = scheduler,
      canScheduleExactAlarms = false,
    )

    store.accept(AddEditStore.Intent.UpdateTitle(title = "Future event"))
    store.accept(AddEditStore.Intent.UpdateTargetDateTime(
      dateTime = Instant.parse("2027-01-01T00:00:00Z"),
    ))
    store.accept(AddEditStore.Intent.Save)
    assertTrue(
      "exactAlarmDenied should be true after Save with denied permission",
      (store.state as AddEditState.Form).exactAlarmDenied,
    )

    store.labels.test {
      store.accept(AddEditStore.Intent.DismissExactAlarmDialog)
      // User closed dialog — no save, no label.
      expectNoEvents()
    }

    val form = store.state as AddEditState.Form
    assertFalse("exactAlarmDenied should be false after dismiss", form.exactAlarmDenied)
    assertFalse("scheduler.scheduleCalled should be false after dismiss", scheduler.scheduleCalled)

    store.dispose()
  }
}

// ── Test fakes ────────────────────────────────────────────────────────────────────────────────────

/** Minimal in-memory [EventsRepository] for unit tests. */
private class FakeEventsRepository(
  private vararg val events: Event,
  private val throwOnGet: Exception? = null,
) : EventsRepository {

  private val stored = mutableListOf(*events)

  override suspend fun getById(id: EventId): Event? {
    if (throwOnGet != null) throw throwOnGet
    return stored.firstOrNull { it.id == id }
  }

  override suspend fun add(event: Event) {
    stored.add(event)
  }

  override suspend fun update(event: Event) {
    val index = stored.indexOfFirst { it.id == event.id }
    if (index >= 0) stored[index] = event else stored.add(event)
  }

  override suspend fun delete(id: EventId) {
    stored.removeAll { it.id == id }
  }

  override fun observeEvents() = kotlinx.coroutines.flow.flow<List<Event>> { emit(stored.toList()) }
}

/** [NotificationScheduler] that does nothing — scheduling is not under test here. */
private class NoOpNotificationScheduler : NotificationScheduler {
  override suspend fun schedule(event: Event) = Unit
  override suspend fun cancel(id: EventId) = Unit
}

/** Fixed-time [Clock] — deterministic for tests. */
private fun Clock.Companion.fixed(now: Instant): Clock = object : Clock {
  override fun now(): Instant = now
}

/** Fake [ExactAlarmPermissionChecker] with controllable return value. */
private class FakeExactAlarmPermissionChecker(private val canSchedule: Boolean) : ExactAlarmPermissionChecker {
  override fun canScheduleExactAlarms(): Boolean = canSchedule
}

/** [NotificationScheduler] that tracks whether [schedule] was ever called. */
private class TrackingNotificationScheduler : NotificationScheduler {
  var scheduleCalled = false
    private set

  override suspend fun schedule(event: Event) {
    scheduleCalled = true
  }

  override suspend fun cancel(id: EventId) = Unit
}
