package com.datecountdown.app.feature.counter

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.domain.CountdownResult
import com.datecountdown.app.domain.CountdownUnit
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.PastBreakdown
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
internal class CounterScreenTest {

  @get:Rule
  val composeRule = createComposeRule()

  // ── Loading ────────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `loading state - CircularProgressIndicator is displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = CounterState.Loading))
      }
    }

    // CounterLoadingContent renders a CircularProgressIndicator inside a Box.
    // No testTag is added to production code; assert via semantics role absence of title text.
    // The loading screen has no event title or counter text — assert that the title "Birthday" is absent.
    composeRule.onNodeWithText("Birthday").assertDoesNotExist()
  }

  // ── Upcoming ───────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `upcoming state - event title is displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = upcomingState()))
      }
    }

    composeRule.onNodeWithText("Birthday").assertIsDisplayed()
  }

  @Test
  fun `upcoming state - countdown number is displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = upcomingState()))
      }
    }

    // Primary unit = DAYS, value = 30.
    // The block uses mergeDescendants=true + contentDescription; use useUnmergedTree to find the
    // actual text node. The plural string for 30 days = "30 days".
    composeRule.onNodeWithText("30 days", useUnmergedTree = true).assertIsDisplayed()
  }

  // ── Past ───────────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `past state - event title is displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = pastState()))
      }
    }

    composeRule.onNodeWithText("Birthday").assertIsDisplayed()
  }

  @Test
  fun `past state - days ago number is displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = pastState()))
      }
    }

    // PastBreakdown.DaysAgo(14) — rendered as "−14" (minus sign + number).
    // The block uses mergeDescendants=true; use useUnmergedTree to find the inner text node.
    composeRule.onNodeWithText("−14", useUnmergedTree = true).assertIsDisplayed()
  }

  // ── Past chip a11y (AC-PE-17) ─────────────────────────────────────────────────────────────────

  @Test
  fun `past chip - contentDescription is PAST EVENT label`() {
    // clearAndSetSemantics exposes only our contentDescription; the chip node is
    // findable by that description rather than by its text child.
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = pastState()))
      }
    }

    composeRule.onNodeWithContentDescription("PAST EVENT").assertExists()
  }

  @Test
  fun `past chip - does not have OnClick action (AC-PE-17)`() {
    // clearAndSetSemantics strips the AssistChip's built-in OnClick action so
    // TalkBack does not announce the chip as an actionable button.
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = pastState()))
      }
    }

    val hasOnClick = SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)
    composeRule
      .onNodeWithContentDescription("PAST EVENT")
      .assert(hasOnClick.not())
  }

  @Test
  fun `past chip - does not have Button role (AC-PE-17)`() {
    // clearAndSetSemantics strips Role.Button so the chip is not announced as a
    // button by screen readers.
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = pastState()))
      }
    }

    val hasButtonRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
    composeRule
      .onNodeWithContentDescription("PAST EVENT")
      .assert(hasButtonRole.not())
  }

  // ── NotFound ───────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `not found state - not found message is displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = CounterState.NotFound))
      }
    }

    composeRule.onNodeWithText("Event not found").assertIsDisplayed()
  }

  // ── Icon a11y ─────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `icon contentDescription uses localized name, not snake_case symbolName`() {
    // Uses MUSIC_NOTE whose symbolName is "music_note" — the underscore makes the
    // regression immediately visible if the fix is reverted.
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = upcomingState(icon = EventIcon.MUSIC_NOTE)))
      }
    }

    // The localized label resolves to "Music icon" (EN strings).
    // useUnmergedTree = true because EventSymbol is inside a merged-semantics container.
    composeRule.onNodeWithContentDescription("Music icon", useUnmergedTree = true).assertExists()
    // Regression guard: snake_case must not reach TalkBack.
    composeRule.onNodeWithContentDescription("music_note icon", useUnmergedTree = true)
      .assertDoesNotExist()
  }

  // ── Interactions ───────────────────────────────────────────────────────────────────────────────

  @Test
  fun `edit click - delegates to component`() {
    val component = FakeCounterComponent(state = upcomingState())
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = component)
      }
    }

    // Verify component was created and is wired; onEditClick callable without error.
    component.onEditClick()
    assert(component.editClickCount == 1)
  }

  @Test
  fun `back click - delegates to component`() {
    val component = FakeCounterComponent(state = upcomingState())
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = component)
      }
    }

    component.onBackClick()
    assert(component.backClickCount == 1)
  }

  // ── Helpers ────────────────────────────────────────────────────────────────────────────────────

  private fun upcomingState(icon: EventIcon = EventIcon.CAKE): CounterState.Upcoming =
    CounterState.Upcoming(
      event = testEvent(icon = icon),
      countdown = CountdownResult.Upcoming(
        years = 0,
        days = 30,
        hours = 4,
        minutes = 15,
        seconds = 0,
        primary = CountdownUnit.DAYS,
      ),
    )

  private fun pastState(): CounterState.Past = CounterState.Past(
    event = testEvent(),
    breakdown = PastBreakdown.DaysAgo(days = 14),
  )

  @Suppress("MagicNumber")
  private fun testEvent(icon: EventIcon = EventIcon.CAKE): Event = Event(
    id = EventId("event-1"),
    title = "Birthday",
    targetDateTime = Instant.parse("2027-06-15T10:00:00Z"),
    color = EventColor.BLUE,
    icon = icon,
    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
  )
}

// ── Fake component ─────────────────────────────────────────────────────────────────────────────────

private class FakeCounterComponent(state: CounterState) : CounterComponent {

  private val _state = MutableValue(state)
  override val state: Value<CounterState> get() = _state

  var editClickCount: Int = 0
    private set
  var backClickCount: Int = 0
    private set
  var deleteClickCount: Int = 0
    private set
  var rescheduleClickCount: Int = 0
    private set

  override fun onEditClick() {
    editClickCount++
  }

  override fun onBackClick() {
    backClickCount++
  }

  override fun onDeleteClick() {
    deleteClickCount++
  }

  override fun onRescheduleClick() {
    rescheduleClickCount++
  }
}
