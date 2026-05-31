@file:Suppress("TooManyFunctions", "LongMethod")

package com.datecountdown.app.feature.edit

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.datecountdown.app.core.design.theme.BlobShape
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.core.design.theme.EventSymbol
import com.datecountdown.app.core.design.theme.eventPaletteByIndex
import com.datecountdown.app.core.design.theme.EventIcon as DesignEventIcon
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon as DomainEventIcon
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime

private const val EXACT_ALARM_DIALOG_TAG = "ExactAlarmDialog"

// ---------------------------------------------------------------------------
// Stateful entry point
// ---------------------------------------------------------------------------

/**
 * Stateful entry point for the add/edit sheet.
 *
 * Subscribes to [AddEditComponent.state] and translates it into the stateless content composable.
 * System back is forwarded to [AddEditComponent.onDismissRequest] so the store can decide whether
 * to close immediately (no unsaved changes) or show the discard-confirmation dialog (AC-AE-10).
 */
@Composable
fun AddEditScreen(
  component: AddEditComponent,
  modifier: Modifier = Modifier,
) {
  BackHandler { component.onDismissRequest() }
  val state by component.state.subscribeAsState()
  val isEditMode = component.eventId != null

  when (val s = state) {
    AddEditState.Loading -> AddEditLoadingContent(modifier = modifier.fillMaxSize())
    is AddEditState.LoadError -> AddEditLoadErrorContent(
      cause = s.cause,
      onBack = component::onDismissRequest,
      modifier = modifier.fillMaxSize(),
    )
    is AddEditState.Form -> AddEditFormContent(
      state = s,
      isEditMode = isEditMode,
      component = component,
      modifier = modifier,
    )
  }
}

// ---------------------------------------------------------------------------
// Loading state
// ---------------------------------------------------------------------------

@Composable
private fun AddEditLoadingContent(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}

// ---------------------------------------------------------------------------
// Load-error state
// ---------------------------------------------------------------------------

@Composable
private fun AddEditLoadErrorContent(
  cause: Throwable,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = cause.message ?: stringResource(R.string.add_edit_save_error_generic),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.error,
    )
    Spacer(modifier = Modifier.height(16.dp))
    TextButton(onClick = onBack) {
      Text(text = stringResource(R.string.add_edit_load_error_back))
    }
  }
}

// ---------------------------------------------------------------------------
// Form state
// ---------------------------------------------------------------------------

@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditFormContent(
  state: AddEditState.Form,
  isEditMode: Boolean,
  component: AddEditComponent,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val saveEnabled = state.title.trim().isNotEmpty() && !state.isSaving

  // Picker visibility — survives config change
  var showDatePicker by rememberSaveable { mutableStateOf(false) }
  var showTimePicker by rememberSaveable { mutableStateOf(false) }
  // Intermediate date (millis) held between DatePicker confirm and TimePicker confirm
  var pendingDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }

  if (state.showDiscardConfirmation) {
    DiscardConfirmationDialog(
      onConfirm = component::onDiscardConfirmed,
      onDismiss = component::onDismissConfirmCancel,
    )
  }

  if (state.exactAlarmDenied) {
    val context = LocalContext.current
    ExactAlarmDeniedDialog(
      onOpenSettings = {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
          data = Uri.fromParts("package", context.packageName, null)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Rare OEM forks may not handle this action — log the failure so it's visible on-device.
        runCatching { context.startActivity(intent) }
          .onFailure { Log.w(EXACT_ALARM_DIALOG_TAG, "ACTION_REQUEST_SCHEDULE_EXACT_ALARM unhandled", it) }
        component.onExactAlarmDialogDismiss()
      },
      onSaveWithoutNotification = component::onSaveWithoutNotification,
      onDismiss = component::onExactAlarmDialogDismiss,
    )
  }

  if (showDatePicker) {
    // State scoped inside this conditional: each open starts with the current committed date,
    // preventing uncommitted selections from leaking across open/cancel/reopen cycles (AC-AE-4).
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = state.targetDateTime.toEpochMilliseconds(),
    )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            pendingDateMillis = datePickerState.selectedDateMillis
            showDatePicker = false
            showTimePicker = true
          },
        ) {
          Text(text = stringResource(android.R.string.ok))
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(text = stringResource(android.R.string.cancel))
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showTimePicker) {
    // State scoped inside this conditional: each open starts with the current committed time,
    // preventing uncommitted selections from leaking across open/cancel/reopen cycles (AC-AE-4).
    val localNow = state.targetDateTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val timePickerState = rememberTimePickerState(
      initialHour = localNow.hour,
      initialMinute = localNow.minute,
      is24Hour = true,
    )
    AlertDialog(
      onDismissRequest = {
        pendingDateMillis = null
        showTimePicker = false
      },
      confirmButton = {
        TextButton(
          onClick = {
            val dateMillis = pendingDateMillis
            if (dateMillis != null) {
              val combined = combineDateAndTime(
                dateMillis = dateMillis,
                hour = timePickerState.hour,
                minute = timePickerState.minute,
              )
              component.onTargetDateTimeChange(combined)
            }
            pendingDateMillis = null
            showTimePicker = false
          },
        ) {
          Text(text = stringResource(android.R.string.ok))
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            pendingDateMillis = null
            showTimePicker = false
          },
        ) {
          Text(text = stringResource(android.R.string.cancel))
        }
      },
      text = {
        TimePicker(state = timePickerState)
      },
      title = null,
    )
  }

  Scaffold(
    topBar = {
      // AC-AE-16: top bar traverses AFTER the form so Save is last (a11y rule 7).
      AddEditTopBar(
        isEditMode = isEditMode,
        saveEnabled = saveEnabled,
        isSaving = state.isSaving,
        onClose = component::onDismissRequest,
        onSave = component::onSaveClick,
        modifier = Modifier.semantics {
          isTraversalGroup = true
          traversalIndex = 1f
        },
      )
    },
    modifier = modifier,
  ) { innerPadding ->
    // Form fields traversed before top bar (traversalIndex 0f < 1f).
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .semantics { isTraversalGroup = true; traversalIndex = 0f }
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      // AC-AE-2: live-updating preview card
      EventPreviewCard(
        title = state.title,
        targetDateTime = state.targetDateTime,
        color = state.color,
        icon = state.icon,
        isDark = isDark,
      )

      // AC-AE-3: title field
      TitleField(
        title = state.title,
        onTitleChange = component::onTitleChange,
      )

      // AC-AE-4: date + time picker
      DateTimeRow(
        targetDateTime = state.targetDateTime,
        onClick = { showDatePicker = true },
      )

      // AC-AE-5: 9-color swatch row
      ColorPickerSection(
        selected = state.color,
        onSelect = component::onColorChange,
        isDark = isDark,
      )

      // AC-AE-6: 16-icon grid
      IconPickerSection(
        selected = state.icon,
        onSelect = component::onIconChange,
      )

      // Save error shown inline near the bottom (AC-AE-7)
      if (state.saveError != null) {
        Text(
          text = state.saveError.message
            ?: stringResource(R.string.add_edit_save_error_generic),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Assertive
          },
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditTopBar(
  isEditMode: Boolean,
  saveEnabled: Boolean,
  isSaving: Boolean,
  onClose: () -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  TopAppBar(
    modifier = modifier,
    title = {
      Text(
        text = if (isEditMode) {
          stringResource(R.string.add_edit_title_edit)
        } else {
          stringResource(R.string.add_edit_title_create)
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    navigationIcon = {
      IconButton(onClick = onClose) {
        Icon(
          imageVector = Icons.Filled.Close,
          contentDescription = stringResource(R.string.add_edit_close_cd),
        )
      }
    },
    actions = {
      Button(
        onClick = onSave,
        enabled = saveEnabled,
        modifier = Modifier.padding(end = 8.dp),
      ) {
        if (isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Text(text = stringResource(R.string.add_edit_save))
        }
      }
    },
  )
}

// ---------------------------------------------------------------------------
// Event preview card (AC-AE-2)
// ---------------------------------------------------------------------------

@Suppress("LongParameterList")
@Composable
private fun EventPreviewCard(
  title: String,
  targetDateTime: Instant,
  color: EventColor,
  icon: DomainEventIcon,
  isDark: Boolean,
  modifier: Modifier = Modifier,
) {
  val palette = remember(color.ordinal, isDark) {
    eventPaletteByIndex(index = color.ordinal, dark = isDark)
  }
  val designIcon = remember(icon.ordinal) {
    DesignEventIcon.entries[icon.ordinal]
  }
  val dateLabel = remember(targetDateTime) { formatEventDateTimePreview(targetDateTime) }
  val displayTitle = title.ifBlank { "…" }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(palette.container)
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(BlobShape.Variant4)
        .background(palette.hero),
      contentAlignment = Alignment.Center,
    ) {
      EventSymbol(
        icon = designIcon,
        size = 22.sp,
        tint = palette.onHero,
        contentDescription = null, // decorative — card's merged semantics provides a11y
      )
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = displayTitle,
        style = MaterialTheme.typography.titleMedium,
        color = palette.onContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = dateLabel,
        style = MaterialTheme.typography.bodySmall,
        color = palette.onContainer,
        maxLines = 1,
      )
    }
  }
}

// ---------------------------------------------------------------------------
// Title field (AC-AE-3)
// ---------------------------------------------------------------------------

@Composable
private fun TitleField(
  title: String,
  onTitleChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = title,
    onValueChange = onTitleChange,
    label = { Text(text = stringResource(R.string.add_edit_title_label)) },
    placeholder = { Text(text = stringResource(R.string.add_edit_title_placeholder)) },
    singleLine = true,
    modifier = modifier.fillMaxWidth(),
  )
}

// ---------------------------------------------------------------------------
// Date + time row (AC-AE-4)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeRow(
  targetDateTime: Instant,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val formattedDateTime = remember(targetDateTime) { formatEventDateTime(targetDateTime) }
  Surface(
    onClick = onClick,
    shape = OutlinedTextFieldDefaults.shape,
    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stringResource(R.string.add_edit_datetime_label),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = formattedDateTime,
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      Icon(
        imageVector = Icons.Filled.DateRange,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

// ---------------------------------------------------------------------------
// Color picker section (AC-AE-5)
// ---------------------------------------------------------------------------

@Composable
private fun ColorPickerSection(
  selected: EventColor,
  onSelect: (EventColor) -> Unit,
  isDark: Boolean,
  modifier: Modifier = Modifier,
) {
  val colorNames = stringArrayResource(R.array.color_names)
  val colors = EventColor.entries
  // Layout: 7 swatches in first row + 2 in second row (matches эталон screenshot)
  val firstRowColors = colors.take(7)
  val secondRowColors = colors.drop(7)

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.add_edit_color_picker_label),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      firstRowColors.forEach { color ->
        ColorSwatch(
          color = color,
          colorName = colorNames[color.ordinal],
          isSelected = color == selected,
          isDark = isDark,
          onClick = { onSelect(color) },
        )
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      secondRowColors.forEach { color ->
        ColorSwatch(
          color = color,
          colorName = colorNames[color.ordinal],
          isSelected = color == selected,
          isDark = isDark,
          onClick = { onSelect(color) },
        )
      }
    }
  }
}

@Suppress("LongParameterList")
@Composable
private fun ColorSwatch(
  color: EventColor,
  colorName: String,
  isSelected: Boolean,
  isDark: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = remember(color.ordinal, isDark) {
    eventPaletteByIndex(index = color.ordinal, dark = isDark)
  }
  val a11yDesc = if (isSelected) {
    stringResource(R.string.add_edit_color_selected, colorName)
  } else {
    stringResource(R.string.add_edit_color_not_selected, colorName)
  }
  val swatchShape = if (isSelected) RoundedCornerShape(8.dp) else CircleShape

  // 48dp touch target wrapping 40dp visual swatch (AC-AE-5)
  Box(
    modifier = modifier
      .size(48.dp)
      .clickable(
        role = Role.RadioButton,
        onClickLabel = a11yDesc,
        onClick = onClick,
      )
      .semantics {
        selected = isSelected
        role = Role.RadioButton
      },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(swatchShape)
        .background(palette.hero)
        .then(
          if (isSelected) {
            Modifier.border(
              width = 2.dp,
              color = MaterialTheme.colorScheme.onSurface,
              shape = swatchShape,
            )
          } else {
            Modifier
          },
        ),
      contentAlignment = Alignment.Center,
    ) {
      if (isSelected) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = null,
          tint = palette.onHero,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Icon picker section (AC-AE-6)
// ---------------------------------------------------------------------------

@Composable
private fun IconPickerSection(
  selected: DomainEventIcon,
  onSelect: (DomainEventIcon) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.add_edit_icon_picker_label),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // 8 columns × 2 rows = 16 icons (AC-AE-6, matches эталон)
    val icons = DomainEventIcon.entries
    val columnCount = 8
    val rowCount = (icons.size + columnCount - 1) / columnCount
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      repeat(rowCount) { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          repeat(columnCount) { col ->
            val index = row * columnCount + col
            if (index < icons.size) {
              val domainIcon = icons[index]
              IconPickerCell(
                domainIcon = domainIcon,
                isSelected = domainIcon == selected,
                onClick = { onSelect(domainIcon) },
                modifier = Modifier.weight(1f),
              )
            } else {
              // Empty placeholder to maintain grid alignment
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun IconPickerCell(
  domainIcon: DomainEventIcon,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val designIcon = remember(domainIcon.ordinal) {
    DesignEventIcon.entries[domainIcon.ordinal]
  }
  val a11yDesc = stringResource(R.string.add_edit_icon_a11y, stringResource(designIcon.labelRes))

  val cellShape = if (isSelected) RoundedCornerShape(12.dp) else CircleShape
  val cellColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
  else MaterialTheme.colorScheme.surfaceVariant
  val iconTint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
  else MaterialTheme.colorScheme.onSurfaceVariant

  Box(
    modifier = modifier
      .size(48.dp)
      .clip(cellShape)
      .background(cellColor)
      .clickable(
        role = Role.RadioButton,
        onClickLabel = a11yDesc,
        onClick = onClick,
      )
      .semantics {
        selected = isSelected
        role = Role.RadioButton
      },
    contentAlignment = Alignment.Center,
  ) {
    EventSymbol(
      icon = designIcon,
      size = 24.sp,
      tint = iconTint,
      contentDescription = null, // cell's merged clickable semantics carries a11y
    )
  }
}

// ---------------------------------------------------------------------------
// Discard confirmation dialog (AC-AE-10)
// ---------------------------------------------------------------------------

@Composable
private fun DiscardConfirmationDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = stringResource(R.string.add_edit_discard_title)) },
    text = { Text(text = stringResource(R.string.add_edit_discard_message)) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(text = stringResource(R.string.add_edit_discard_yes))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(text = stringResource(R.string.add_edit_discard_no))
      }
    },
  )
}

/**
 * Dialog shown when the user taps Save but exact-alarm permission has been denied (AC-NT-13).
 *
 * Provides two resolution paths:
 *  - "Open settings" → launches ACTION_REQUEST_SCHEDULE_EXACT_ALARM, then dismisses the dialog
 *    so the user can retry Save after granting permission.
 *  - "Save without notification" → saves the event without scheduling an alarm.
 *
 * Dismissing the dialog (back or outside tap) clears the state without performing the save.
 */
@Composable
private fun ExactAlarmDeniedDialog(
  onOpenSettings: () -> Unit,
  onSaveWithoutNotification: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = stringResource(R.string.exact_alarm_denied_title)) },
    text = { Text(text = stringResource(R.string.exact_alarm_denied_message)) },
    confirmButton = {
      TextButton(onClick = onOpenSettings) {
        Text(text = stringResource(R.string.exact_alarm_denied_open_settings))
      }
    },
    dismissButton = {
      TextButton(onClick = onSaveWithoutNotification) {
        Text(text = stringResource(R.string.exact_alarm_denied_save_without))
      }
    },
  )
}

// ---------------------------------------------------------------------------
// Date/time formatting helpers
// ---------------------------------------------------------------------------

// Preview card: "27 мая 2026 · 12:00" — full month name, title-case, middle-dot separator (U+00B7)
private val dateTimePreviewFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy · HH:mm", Locale.getDefault())

// Date field: "27 мая 2026, 12:00" — full month name, title-case, comma separator (per эталон)
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", Locale.getDefault())

private fun formatEventDateTimePreview(instant: Instant): String =
  dateTimePreviewFormatter
    .format(instant.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())

private fun formatEventDateTime(instant: Instant): String =
  dateTimeFormatter
    .format(instant.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())

/**
 * Combines a date expressed as epoch-millis with an explicit hour and minute (both in system
 * timezone) and returns the resulting [Instant].
 *
 * [dateMillis] represents the start-of-day UTC epoch from the M3 DatePicker. We re-derive the
 * local date from it (system timezone) then apply [hour]/[minute] to match user intent —
 * avoiding the UTC-midnight offset artifact that would occur if we naively added the millis.
 */
private fun combineDateAndTime(dateMillis: Long, hour: Int, minute: Int): Instant {
  val tz = TimeZone.currentSystemDefault()
  val pickerInstant = Instant.fromEpochMilliseconds(dateMillis)
  val localDate = pickerInstant.toLocalDateTime(TimeZone.UTC).date
  val localDateTime = LocalDateTime(
    date = localDate,
    time = LocalTime(hour = hour, minute = minute, second = 0, nanosecond = 0),
  )
  return localDateTime.toInstant(tz)
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Suppress("UnusedPrivateMember", "MagicNumber")
@PreviewLightDark
@Composable
private fun AddEditLoadingPreview() {
  DateCountdownTheme {
    Surface {
      AddEditLoadingContent(modifier = Modifier.fillMaxSize())
    }
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun AddEditCreatePreview() {
  DateCountdownTheme {
    Surface {
      AddEditFormContent(
        state = AddEditState.Form(
          title = "Trip to Japan",
          targetDateTime = Instant.parse("2026-10-15T10:00:00Z"),
          color = EventColor.TEAL,
          icon = DomainEventIcon.FLIGHT,
        ),
        isEditMode = false,
        component = PreviewAddEditComponent,
      )
    }
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun AddEditEditPreview() {
  DateCountdownTheme {
    Surface {
      AddEditFormContent(
        state = AddEditState.Form(
          title = "Birthday party",
          targetDateTime = Instant.parse("2026-06-20T18:30:00Z"),
          color = EventColor.PINK,
          icon = DomainEventIcon.CAKE,
          hasUnsavedChanges = true,
        ),
        isEditMode = true,
        component = PreviewAddEditComponent,
      )
    }
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun AddEditDiscardConfirmationPreview() {
  DateCountdownTheme {
    Surface {
      AddEditFormContent(
        state = AddEditState.Form(
          title = "Product launch",
          targetDateTime = Instant.parse("2026-09-01T09:00:00Z"),
          color = EventColor.INDIGO,
          icon = DomainEventIcon.ROCKET_LAUNCH,
          hasUnsavedChanges = true,
          showDiscardConfirmation = true,
        ),
        isEditMode = false,
        component = PreviewAddEditComponent,
      )
    }
  }
}

// ---------------------------------------------------------------------------
// Preview stub — never used in production code
// ---------------------------------------------------------------------------

private object PreviewAddEditComponent : AddEditComponent {
  override val eventId: String? = null
  override val state: com.arkivanov.decompose.value.Value<AddEditState> =
    com.arkivanov.decompose.value.MutableValue(
      AddEditState.Form(
        title = "",
        targetDateTime = Instant.parse("2026-10-01T12:00:00Z"),
        color = EventColor.BLUE,
        icon = DomainEventIcon.CELEBRATION,
      ),
    )

  override fun onTitleChange(title: String) = Unit
  override fun onTargetDateTimeChange(dateTime: kotlinx.datetime.Instant) = Unit
  override fun onColorChange(color: EventColor) = Unit
  override fun onIconChange(icon: DomainEventIcon) = Unit
  override fun onSaveClick() = Unit
  override fun onDismissRequest() = Unit
  override fun onDiscardConfirmed() = Unit
  override fun onDismissConfirmCancel() = Unit
  override fun onSaveWithoutNotification() = Unit
  override fun onExactAlarmDialogDismiss() = Unit
}
