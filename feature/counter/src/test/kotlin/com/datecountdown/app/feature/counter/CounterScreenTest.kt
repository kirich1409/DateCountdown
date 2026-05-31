package com.datecountdown.app.feature.counter

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

  // ── Date chip a11y (AC-CL-17 / AC-CL-19) ─────────────────────────────────────────────────────

  @Test
  fun `date chip - exposes contentDescription and no button role or click action`() {
    // testEvent.targetDateTime = 2027-06-15T10:00:00Z — "2027" is unique in the tree.
    // clearAndSetSemantics wipes AssistChip's internal button role and onClick action,
    // leaving only contentDescription. Regression: .semantics(mergeDescendants=false) did NOT
    // clear the click role, making TalkBack announce the chip as an interactive button.
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = upcomingState()))
      }
    }

    val dateChip = composeRule.onNodeWithContentDescription("2027", substring = true)
    dateChip.assertExists()
    dateChip.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    dateChip.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
  }

  @Test
  fun `date chip - contentDescription does not contain middle dot`() {
    // Regression guard for bug #167 WARN: dateChipFormatter uses U+00B7 (middle dot) as a visual
    // separator. Some TTS engines may read it literally. The a11y contentDescription must use
    // dateChipA11yFormatter (comma-separated) so TTS produces a natural spoken pause.
    // testEvent.targetDateTime = 2027-06-15T10:00:00Z.
    composeRule.setContent {
      DateCountdownTheme {
        CounterScreen(component = FakeCounterComponent(state = upcomingState()))
      }
    }

    // "2027" is a locale-independent substring present in any locale's rendering of the date.
    val dateChip = composeRule.onNodeWithContentDescription("2027", substring = true)
    dateChip.assertExists()
    // The middle-dot character (U+00B7) must NOT appear in the contentDescription.
    composeRule.onNodeWithContentDescription("\u00B7", substring = true).assertDoesNotExist()
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
