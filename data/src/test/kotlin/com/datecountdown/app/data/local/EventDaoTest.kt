package com.datecountdown.app.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for [EventDao] using a Room in-memory database.
 *
 * Coverage:
 *  - upsert-as-insert: row stored; getById returns the entity.
 *  - upsert-as-update: same id, different fields; getById returns updated entity.
 *  - getById returns null for unknown id.
 *  - observeAll: Flow emits empty list initially, then 1 row, then 2 rows (Turbine).
 *  - observeAll after delete: Flow emits list without the deleted row.
 *  - deleteById removes row; subsequent getById returns null.
 *  - Epoch-millis roundtrip for targetDateTimeMillis: mid-range, epoch-0, large value.
 *  - Empty title string is stored and retrieved unchanged.
 */
// Robolectric 4.14.x maxSdkVersion=35; project targetSdk=36 — pin explicitly to avoid
// "targetSdkVersion > maxSdkVersion" error until Robolectric adds SDK 36 support.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class EventDaoTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var db: AppDatabase
  private lateinit var dao: EventDao

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDatabase::class.java,
    )
      .allowMainThreadQueries()
      .build()
    dao = db.eventDao()
  }

  @After
  fun tearDown() {
    db.close()
    Dispatchers.resetMain()
  }

  // ── upsert as insert ───────────────────────────────────────────────────────

  @Test
  fun `upsert stores entity and getById returns it`() = runTest {
    val entity = entity(id = "1")

    dao.upsert(entity)

    assertEquals(entity, dao.getById("1"))
  }

  @Test
  fun `upsert with same id overwrites and getById returns updated entity`() = runTest {
    val original = entity(id = "1", title = "Original")
    val updated = original.copy(title = "Updated")

    dao.upsert(original)
    dao.upsert(updated)

    assertEquals(updated, dao.getById("1"))
  }

  @Test
  fun `getById returns null for unknown id`() = runTest {
    assertNull(dao.getById("nonexistent"))
  }

  // ── observeAll Flow emissions ──────────────────────────────────────────────

  @Test
  fun `observeAll emits empty list initially, then updates on inserts`() = runTest {
    dao.observeAll().test {
      assertEquals(emptyList<EventEntity>(), awaitItem()) // initial emission

      dao.upsert(entity(id = "1"))
      assertEquals(1, awaitItem().size)

      dao.upsert(entity(id = "2"))
      assertEquals(2, awaitItem().size)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `observeAll emits list without deleted row after deleteById`() = runTest {
    dao.upsert(entity(id = "1"))
    dao.upsert(entity(id = "2"))

    dao.observeAll().test {
      val initial = awaitItem()
      assertEquals(2, initial.size)

      dao.deleteById("1")
      val afterDelete = awaitItem()
      assertEquals(1, afterDelete.size)
      assertNull(afterDelete.firstOrNull { it.id == "1" })

      cancelAndIgnoreRemainingEvents()
    }
  }

  // ── deleteById ────────────────────────────────────────────────────────────

  @Test
  fun `deleteById removes row and getById returns null`() = runTest {
    dao.upsert(entity(id = "1"))

    dao.deleteById("1")

    assertNull(dao.getById("1"))
  }

  // ── epoch-millis roundtrip ─────────────────────────────────────────────────

  @Test
  fun `targetDateTimeMillis mid-range value roundtrips exactly`() = runTest {
    val millis = 1_234_567_890_123L
    val e = entity(id = "rt-mid", targetDateTimeMillis = millis)

    dao.upsert(e)

    assertEquals(millis, dao.getById("rt-mid")!!.targetDateTimeMillis)
  }

  @Test
  fun `targetDateTimeMillis epoch-zero roundtrips exactly`() = runTest {
    val e = entity(id = "rt-zero", targetDateTimeMillis = 0L)

    dao.upsert(e)

    assertEquals(0L, dao.getById("rt-zero")!!.targetDateTimeMillis)
  }

  @Test
  fun `targetDateTimeMillis very-large value (year 9999) roundtrips exactly`() = runTest {
    // Instant.parse("9999-12-31T23:59:59Z").toEpochMilliseconds()
    val millis = 253_402_300_799_000L
    val e = entity(id = "rt-large", targetDateTimeMillis = millis)

    dao.upsert(e)

    assertEquals(millis, dao.getById("rt-large")!!.targetDateTimeMillis)
  }

  // ── edge: empty title ──────────────────────────────────────────────────────

  @Test
  fun `empty title string roundtrips unchanged`() = runTest {
    val e = entity(id = "empty-title", title = "")

    dao.upsert(e)

    assertEquals("", dao.getById("empty-title")!!.title)
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  @Suppress("LongParameterList") // test factory — all defaults, any field overridable
  private fun entity(
    id: String = "test-id",
    title: String = "Test Event",
    targetDateTimeMillis: Long = 1_700_000_000_000L,
    colorName: String = "BLUE",
    iconName: String = "CELEBRATION",
    createdAtMillis: Long = 1_699_000_000_000L,
  ): EventEntity = EventEntity(
    id = id,
    title = title,
    targetDateTimeMillis = targetDateTimeMillis,
    colorName = colorName,
    iconName = iconName,
    createdAtMillis = createdAtMillis,
  )
}
