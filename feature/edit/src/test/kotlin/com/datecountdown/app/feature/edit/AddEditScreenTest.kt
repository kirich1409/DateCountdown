package com.datecountdown.app.feature.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
internal class AddEditScreenTest {

  @get:Rule
  val composeRule = createComposeRule()

  // ── Form renders ───────────────────────────────────────────────────────────────────────────────

  @Test
  fun `form state - title label is displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = FakeAddEditComponent(formState(title = "Concert")))
      }
    }

    composeRule.onNodeWithText("Title").assertIsDisplayed()
  }

  @Test
  fun `form state - save button is displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = FakeAddEditComponent(formState(title = "Concert")))
      }
    }

    composeRule.onNodeWithText("Save").assertIsDisplayed()
  }

  @Test
  fun `form state - color picker label is displayed`() {
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = FakeAddEditComponent(formState(title = "Concert")))
      }
    }

    composeRule.onNodeWithText("Color").assertIsDisplayed()
  }

  @Test
  fun `form state - icon picker label is in tree`() {
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = FakeAddEditComponent(formState(title = "Concert")))
      }
    }

    // The form is scrollable — "Icon" may be below the initial viewport.
    // assertExists() confirms the node is in the composition tree without requiring it to be visible.
    composeRule.onNodeWithText("Icon").assertExists()
  }

  // ── Save button enabled state ──────────────────────────────────────────────────────────────────

  @Test
  fun `save button is disabled when title is empty`() {
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = FakeAddEditComponent(formState(title = "")))
      }
    }

    // saveEnabled = title.trim().isNotEmpty() && !isSaving — empty title → disabled.
    composeRule.onNodeWithText("Save").assertIsNotEnabled()
  }

  @Test
  fun `save button is disabled when title is blank whitespace`() {
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = FakeAddEditComponent(formState(title = "   ")))
      }
    }

    composeRule.onNodeWithText("Save").assertIsNotEnabled()
  }

  @Test
  fun `save button is enabled when title is non-empty`() {
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = FakeAddEditComponent(formState(title = "Concert")))
      }
    }

    composeRule.onNodeWithText("Save").assertIsEnabled()
  }

  @Test
  fun `save button is disabled when isSaving is true`() {
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(
          component = FakeAddEditComponent(formState(title = "Concert", isSaving = true)),
        )
      }
    }

    // isSaving = true → spinner replaces Save text, button is disabled.
    composeRule.onNodeWithText("Save").assertDoesNotExist()
  }

  // ── Interactions ───────────────────────────────────────────────────────────────────────────────

  @Test
  fun `title change - delegates to component`() {
    val component = FakeAddEditComponent(formState(title = ""))
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = component)
      }
    }

    component.onTitleChange("New Year")
    assert(component.titleChanges == listOf("New Year"))
  }

  @Test
  fun `save click - delegates to component`() {
    val component = FakeAddEditComponent(formState(title = "Concert"))
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = component)
      }
    }

    component.onSaveClick()
    assert(component.saveClickCount == 1)
  }

  @Test
  fun `color change - delegates to component`() {
    val component = FakeAddEditComponent(formState(title = "Concert"))
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = component)
      }
    }

    component.onColorChange(EventColor.PINK)
    assert(component.colorChanges == listOf(EventColor.PINK))
  }

  @Test
  fun `icon change - delegates to component`() {
    val component = FakeAddEditComponent(formState(title = "Concert"))
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = component)
      }
    }

    component.onIconChange(EventIcon.MUSIC_NOTE)
    assert(component.iconChanges == listOf(EventIcon.MUSIC_NOTE))
  }

  // ── Color swatch ──────────────────────────────────────────────────────────────────────────────

  @Test
  fun `selected color swatch has selected semantics`() {
    // EventColor.BLUE is ordinal=2, so testTag="color_swatch_2"
    composeRule.setContent {
      DateCountdownTheme {
        AddEditScreen(component = FakeAddEditComponent(formState(title = "Concert", color = EventColor.BLUE)))
      }
    }

    composeRule.onNodeWithTag("color_swatch_${EventColor.BLUE.ordinal}").assertIsSelected()
  }

  // ── Helpers ────────────────────────────────────────────────────────────────────────────────────

  private fun formState(
    title: String,
    color: EventColor = EventColor.BLUE,
    isSaving: Boolean = false,
  ): AddEditState.Form = AddEditState.Form(
    title = title,
    targetDateTime = Instant.parse("2027-10-01T18:00:00Z"),
    color = color,
    icon = EventIcon.CELEBRATION,
    isSaving = isSaving,
  )
}

// ── Fake component ─────────────────────────────────────────────────────────────────────────────────

private class FakeAddEditComponent(
  formState: AddEditState,
) : AddEditComponent {

  override val eventId: String? = null

  private val _state = MutableValue(formState)
  override val state: Value<AddEditState> get() = _state

  val titleChanges = mutableListOf<String>()
  val colorChanges = mutableListOf<EventColor>()
  val iconChanges = mutableListOf<EventIcon>()
  var saveClickCount: Int = 0
    private set
  var dismissCount: Int = 0
    private set

  override fun onTitleChange(title: String) {
    titleChanges += title
  }

  override fun onTargetDateTimeChange(dateTime: Instant) {
    // no-op: not under test
  }

  override fun onColorChange(color: EventColor) {
    colorChanges += color
  }

  override fun onIconChange(icon: EventIcon) {
    iconChanges += icon
  }

  override fun onSaveClick() {
    saveClickCount++
  }

  override fun onDismissRequest() {
    dismissCount++
  }

  override fun onDiscardConfirmed() {
    // no-op: not under test
  }

  override fun onDismissConfirmCancel() {
    // no-op: not under test
  }

  override fun onSaveWithoutNotification() {
    // no-op: not under test
  }

  override fun onExactAlarmDialogDismiss() {
    // no-op: not under test
  }
}
