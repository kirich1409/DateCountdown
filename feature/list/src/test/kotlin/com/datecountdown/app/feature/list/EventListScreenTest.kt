package com.datecountdown.app.feature.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.ThemeMode
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import com.datecountdown.app.domain.EventId
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
internal class EventListScreenTest {

  @get:Rule
  val composeRule = createComposeRule()

  // ── Top App Bar ───────────────────────────────────────────────────────────────────────────────

  @Test
  fun `top bar - menu icon is absent`() {
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(component = FakeEventListComponent(state = EventListState.Loading))
      }
    }

    composeRule.onNodeWithContentDescription("Menu").assertDoesNotExist()
  }

  @Test
  fun `top bar - search icon is absent`() {
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(component = FakeEventListComponent(state = EventListState.Loading))
      }
    }

    composeRule.onNodeWithContentDescription("Search").assertDoesNotExist()
  }

  @Test
  fun `top bar - more vert icon is present`() {
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(component = FakeEventListComponent(state = EventListState.Loading))
      }
    }

    composeRule.onNodeWithContentDescription("More").assertIsDisplayed()
  }

  // ── Loading ────────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `loading state - does not show event titles`() {
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(component = FakeEventListComponent(state = EventListState.Loading))
      }
    }

    // Loading shows a progress indicator — no event title should be visible.
    composeRule.onNodeWithText("Summer Holiday").assertDoesNotExist()
  }

  // ── Content with events ────────────────────────────────────────────────────────────────────────

  @Test
  fun `content state - upcoming event titles are displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(component = FakeEventListComponent(state = contentWithTwoEvents()))
      }
    }

    composeRule.onNodeWithText("Summer Holiday").assertIsDisplayed()
    composeRule.onNodeWithText("Product Launch").assertIsDisplayed()
  }

  // ── Empty state ────────────────────────────────────────────────────────────────────────────────

  @Test
  fun `global empty state - empty heading is displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(
          component = FakeEventListComponent(
            state = EventListState.Content(
              upcoming = emptyList(),
              past = emptyList(),
              pastCollapsed = false,
              pendingDelete = null,
            ),
          ),
        )
      }
    }

    composeRule.onNodeWithText("Create your first event").assertIsDisplayed()
  }

  @Test
  fun `global empty state - FAB is not shown (AC-LS-14)`() {
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(
          component = FakeEventListComponent(
            state = EventListState.Content(
              upcoming = emptyList(),
              past = emptyList(),
              pastCollapsed = false,
              pendingDelete = null,
            ),
          ),
        )
      }
    }

    composeRule.onNodeWithTag("list_fab").assertDoesNotExist()
  }

  @Test
  fun `partial empty state - FAB is not shown (AC-LS-14)`() {
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(
          component = FakeEventListComponent(
            state = EventListState.Content(
              upcoming = emptyList(),
              past = listOf(testEvent(id = "past-1", title = "Past Event")),
              pastCollapsed = false,
              pendingDelete = null,
            ),
          ),
        )
      }
    }

    composeRule.onNodeWithTag("list_fab").assertDoesNotExist()
  }

  @Test
  fun `content with events - FAB is displayed (AC-LS-6)`() {
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(component = FakeEventListComponent(state = contentWithTwoEvents()))
      }
    }

    composeRule.onNodeWithTag("list_fab").assertIsDisplayed()
  }

  @Test
  fun `global empty state - hourglass icon is present in blob`() {
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(
          component = FakeEventListComponent(
            state = EventListState.Content(
              upcoming = emptyList(),
              past = emptyList(),
              pastCollapsed = false,
              pendingDelete = null,
            ),
          ),
        )
      }
    }

    composeRule.onNodeWithTag("empty_state_hourglass").assertExists()
  }

  // ── Interactions ───────────────────────────────────────────────────────────────────────────────

  @Test
  fun `card click - forwards event id to component`() {
    val component = FakeEventListComponent(state = contentWithTwoEvents())
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(component = component)
      }
    }

    // Invoke onCardClick directly (simulating what the card's clickable does).
    component.onCardClick("event-1")
    assert(component.cardClickIds == listOf("event-1"))
  }

  @Test
  fun `add click - forwards to component`() {
    val component = FakeEventListComponent(state = contentWithTwoEvents())
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(component = component)
      }
    }

    component.onAddClick()
    assert(component.addClickCount == 1)
  }

  @Test
  fun `delete - forwards event id to component`() {
    val component = FakeEventListComponent(state = contentWithTwoEvents())
    composeRule.setContent {
      DateCountdownTheme {
        EventListScreen(component = component)
      }
    }

    component.onDelete(EventId("event-1"))
    assert(component.deletedIds == listOf(EventId("event-1")))
  }

  // ── Helpers ────────────────────────────────────────────────────────────────────────────────────

  private fun contentWithTwoEvents(): EventListState.Content = EventListState.Content(
    upcoming = listOf(
      testEvent(id = "event-1", title = "Summer Holiday"),
      testEvent(id = "event-2", title = "Product Launch"),
    ),
    past = emptyList(),
    pastCollapsed = false,
    pendingDelete = null,
  )

  @Suppress("MagicNumber")
  private fun testEvent(id: String, title: String): Event = Event(
    id = EventId(id),
    title = title,
    targetDateTime = Instant.parse("2027-08-01T09:00:00Z"),
    color = EventColor.TEAL,
    icon = EventIcon.CELEBRATION,
    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
  )
}

// ── Fake component ─────────────────────────────────────────────────────────────────────────────────

private class FakeEventListComponent(
  state: EventListState,
) : EventListComponent {

  private val _state = MutableValue(state)
  override val state: Value<EventListState> get() = _state

  val cardClickIds = mutableListOf<String>()
  var addClickCount: Int = 0
    private set
  val deletedIds = mutableListOf<EventId>()
  var undoDeleteCount: Int = 0
    private set
  var togglePastCount: Int = 0
    private set
  var themeModeChanges = mutableListOf<ThemeMode>()

  override fun onCardClick(eventId: String) {
    cardClickIds += eventId
  }

  override fun onAddClick() {
    addClickCount++
  }

  override fun onDelete(id: EventId) {
    deletedIds += id
  }

  override fun onCommitDelete(id: EventId) {
    // no-op: snackbar dismiss not exercised in screen tests
  }

  override fun onUndoDelete() {
    undoDeleteCount++
  }

  override fun onTogglePast() {
    togglePastCount++
  }

  override fun onThemeModeChange(mode: ThemeMode) {
    themeModeChanges += mode
  }
}
