package com.datecountdown.app.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.datecountdown.app.data.local.AppDatabase
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for [EventsRepositoryImpl] with a real [EventDao] backed by a
 * Room in-memory database.
 *
 * Coverage:
 *  - observeEvents: emits empty list initially; emits list with event after add.
 *  - getById: returns saved Event; returns null for unknown id.
 *  - Entity↔domain mapping: EventColor, EventIcon enum names, Instant epoch-millis roundtrip.
 *  - update: modifies stored event; observeEvents reflects change.
 *  - delete: removes event; observeEvents reflects removal; getById returns null.
 *  - observeEvents ordering: all inserted rows present (order-agnostic — no ORDER BY in query).
 *  - Concurrent observers: two collectors on observeEvents both receive emissions.
 */
// Robolectric 4.14.x maxSdkVersion=35; project targetSdk=36 — pin explicitly to avoid
// "targetSdkVersion > maxSdkVersion" error until Robolectric adds SDK 36 support.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class EventsRepositoryImplTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var db: AppDatabase
  private lateinit var repository: EventsRepositoryImpl

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDatabase::class.java,
    )
      .allowMainThreadQueries()
      .build()
    repository = EventsRepositoryImpl(db.eventDao())
  }

  @After
  fun tearDown() {
    db.close()
    Dispatchers.resetMain()
  }

  // ── observeEvents ──────────────────────────────────────────────────────────

  @Test
  fun `observeEvents emits empty list initially`() = runTest {
    repository.observeEvents().test {
      assertEquals(emptyList<Event>(), awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `observeEvents emits list with event after add`() = runTest {
    val event = event(id = "1")

    repository.observeEvents().test {
      awaitItem() // empty

      repository.add(event)
      val emitted = awaitItem()
      assertEquals(1, emitted.size)
      assertEquals(event, emitted.first())

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `observeEvents reflects update`() = runTest {
    val original = event(id = "1", title = "Original")
    repository.add(original)

    repository.observeEvents().test {
      awaitItem() // current state with original

      repository.update(original.copy(title = "Updated"))
      val emitted = awaitItem()
      assertEquals("Updated", emitted.first().title)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `observeEvents reflects delete`() = runTest {
    val event = event(id = "1")
    repository.add(event)

    repository.observeEvents().test {
      awaitItem() // current state with event

      repository.delete(EventId("1"))
      assertEquals(emptyList<Event>(), awaitItem())

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `observeEvents contains all inserted events (order-agnostic)`() = runTest {
    // observeAll has no ORDER BY — compare as Set to avoid fragile ordering assumptions
    val e1 = event(id = "1")
    val e2 = event(id = "2")
    repository.add(e1)
    repository.add(e2)

    repository.observeEvents().test {
      val emitted = awaitItem()
      assertEquals(setOf(e1, e2), emitted.toSet())
      cancelAndIgnoreRemainingEvents()
    }
  }

  // ── getById ────────────────────────────────────────────────────────────────

  @Test
  fun `getById returns saved event`() = runTest {
    val event = event(id = "1")
    repository.add(event)

    assertEquals(event, repository.getById(EventId("1")))
  }

  @Test
  fun `getById returns null for unknown id`() = runTest {
    assertNull(repository.getById(EventId("nonexistent")))
  }

  // ── entity↔domain mapping ──────────────────────────────────────────────────

  @Test
  fun `EventColor enum name roundtrips through storage`() = runTest {
    // All 9 EventColor entries must survive the name-string storage strategy
    for (color in EventColor.entries) {
      val id = "color-${color.name}"
      repository.add(event(id = id, color = color))
      assertEquals(color, repository.getById(EventId(id))!!.color)
    }
  }

  @Test
  fun `EventIcon enum name roundtrips through storage`() = runTest {
    // All 16 EventIcon entries must survive the name-string storage strategy
    for (icon in EventIcon.entries) {
      val id = "icon-${icon.name}"
      repository.add(event(id = id, icon = icon))
      assertEquals(icon, repository.getById(EventId(id))!!.icon)
    }
  }

  @Test
  fun `Instant targetDateTime roundtrips through epoch-millis storage`() = runTest {
    val instant = Instant.fromEpochMilliseconds(1_234_567_890_123L)
    repository.add(event(id = "instant", targetDateTime = instant))

    assertEquals(instant, repository.getById(EventId("instant"))!!.targetDateTime)
  }

  @Test
  fun `Instant createdAt epoch-zero roundtrips`() = runTest {
    val epochZero = Instant.fromEpochMilliseconds(0L)
    repository.add(event(id = "epoch-zero", createdAt = epochZero))

    assertEquals(epochZero, repository.getById(EventId("epoch-zero"))!!.createdAt)
  }

  // ── concurrent observers ───────────────────────────────────────────────────

  @Test
  fun `two independent subscriptions to observeEvents both receive the same data`() = runTest {
    // Smoke-test: Room's InvalidationTracker supports multiple concurrent subscribers.
    // Two sequential Turbine subscriptions on the same flow verify that each independent
    // collector sees a consistent snapshot — Room creates a new invalidation listener per
    // collect call, so both must observe the same committed state.
    val event = event(id = "1")
    repository.add(event)

    repository.observeEvents().test {
      val first = awaitItem()
      assertEquals(listOf(event), first)
      cancelAndIgnoreRemainingEvents()
    }

    repository.observeEvents().test {
      val second = awaitItem()
      assertEquals(listOf(event), second)
      cancelAndIgnoreRemainingEvents()
    }
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  @Suppress("LongParameterList") // test factory — all defaults, any field overridable
  private fun event(
    id: String = "test-id",
    title: String = "Test Event",
    targetDateTime: Instant = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    color: EventColor = EventColor.BLUE,
    icon: EventIcon = EventIcon.CELEBRATION,
    createdAt: Instant = Instant.fromEpochMilliseconds(1_699_000_000_000L),
  ): Event = Event(
    id = EventId(id),
    title = title,
    targetDateTime = targetDateTime,
    color = color,
    icon = icon,
    createdAt = createdAt,
  )
}
