package com.datecountdown.app.feature.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI tests for [EventCard] — verifies same-day and multi-day display (bug #165, AC-LS-5).
 *
 * Strings are resolved via [ApplicationProvider] so the test remains locale-independent:
 * it always asserts the same text the card renders, regardless of device locale.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
internal class EventCardTest {

  @get:Rule
  val composeRule = createComposeRule()

  // ── Same-day (totalDays == 0) ──────────────────────────────────────────────────────────────────

  @Test
  fun `same-day upcoming event - shows today label`() {
    // Event is 4 hours from now; totalDays == 0.
    val now = Instant.parse("2026-06-01T10:00:00Z")
    val todayEvent = eventAt(targetIso = "2026-06-01T14:00:00Z")

    composeRule.setContent {
      DateCountdownTheme {
        EventCard(event = todayEvent, now = now, isDark = false, onClick = {})
      }
    }

    val todayLabel = ApplicationProvider.getApplicationContext<android.content.Context>()
      .getString(R.string.card_today_label)
    composeRule.onNodeWithText(todayLabel).assertIsDisplayed()
  }

  @Test
  fun `same-day upcoming event - does not show zero days left`() {
    val now = Instant.parse("2026-06-01T10:00:00Z")
    val todayEvent = eventAt(targetIso = "2026-06-01T14:00:00Z")

    composeRule.setContent {
      DateCountdownTheme {
        EventCard(event = todayEvent, now = now, isDark = false, onClick = {})
      }
    }

    // "0" number and any "days left" plural must not appear (bug #165).
    composeRule.onNodeWithText("0").assertDoesNotExist()
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    // Plural for 0 in English resolves to "other": "0 days left".
    val zeroLabel = context.resources.getQuantityString(R.plurals.days_left, 0, 0)
    composeRule.onNodeWithText(zeroLabel).assertDoesNotExist()
  }

  // ── Multi-day (totalDays >= 1) ─────────────────────────────────────────────────────────────────

  @Test
  fun `multi-day upcoming event - shows day count, not today label`() {
    val now = Instant.parse("2026-06-01T10:00:00Z")
    // Target is exactly 3 days later (same hour → totalDays == 3).
    val futureEvent = eventAt(targetIso = "2026-06-04T10:00:00Z")

    composeRule.setContent {
      DateCountdownTheme {
        EventCard(event = futureEvent, now = now, isDark = false, onClick = {})
      }
    }

    // "3" should be visible as the large number.
    composeRule.onNodeWithText("3").assertIsDisplayed()

    // Today label must not appear.
    val todayLabel = ApplicationProvider.getApplicationContext<android.content.Context>()
      .getString(R.string.card_today_label)
    composeRule.onNodeWithText(todayLabel).assertDoesNotExist()
  }

  // ── Helpers ────────────────────────────────────────────────────────────────────────────────────

  private fun eventAt(targetIso: String): Event = Event(
    id = EventId("test-event"),
    title = "Test Event",
    targetDateTime = Instant.parse(targetIso),
    color = EventColor.BLUE,
    icon = EventIcon.CELEBRATION,
    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
  )
}
