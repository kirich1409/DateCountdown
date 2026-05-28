@file:Suppress("MagicNumber", "TooManyFunctions")

package com.datecountdown.app.feature.counter

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.datecountdown.app.core.common.R as CommonR
import com.datecountdown.app.core.design.theme.BlobShape
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.core.design.theme.EventPaletteId
import com.datecountdown.app.core.design.theme.GlassSurface
import com.datecountdown.app.core.design.theme.EventIcon as DesignEventIcon
import com.datecountdown.app.core.design.theme.EventSymbol
import com.datecountdown.app.core.design.theme.eventPaletteByIndex
import com.datecountdown.app.domain.CountdownResult
import com.datecountdown.app.domain.CountdownUnit
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon as DomainEventIcon
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.PastBreakdown
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.days

// File-level formatter allocation: avoids re-allocation inside composables (AC-CL-14).
private val dateChipFormatter: DateTimeFormatter =
  DateTimeFormatter.ofPattern("dd MMMM yyyy · HH:mm", Locale.getDefault())

// ── Stateful entry point ──────────────────────────────────────────────────────────────────────────

/**
 * Stateful entry point for the event-counter screen.
 *
 * Subscribes to [CounterComponent.state] via Decompose's [subscribeAsState] and delegates all
 * user interactions to the component. Registers a [BackHandler] so the system back button
 * correctly pops the counter off the stack (AC-NAV-6).
 *
 * This composable holds no local UI state — it is a thin lifecycle bridge to the stateless
 * [CounterScreenContent].
 */
@Composable
internal fun CounterScreen(
  component: CounterComponent,
  modifier: Modifier = Modifier,
) {
  BackHandler { component.onBackClick() }

  val state by component.state.subscribeAsState()
  CounterScreenContent(
    state = state,
    onBackClick = component::onBackClick,
    onEditClick = component::onEditClick,
    onDeleteClick = component::onDeleteClick,
    modifier = modifier,
  )
}

// ── Stateless content ─────────────────────────────────────────────────────────────────────────────

/**
 * Stateless event-counter screen body.
 *
 * Receives a snapshot of [CounterState] and plain callbacks — no component reference, fully
 * previewable. Exhaustive [when] over every state (no else) so that adding a new subtype to
 * [CounterState] is a compile error.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun CounterScreenContent(
  state: CounterState,
  onBackClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (state) {
    CounterState.Loading -> CounterLoadingContent(modifier = modifier)

    CounterState.NotFound -> CounterNotFoundContent(
      onBackClick = onBackClick,
      modifier = modifier,
    )

    is CounterState.Error -> CounterErrorContent(
      modifier = modifier,
    )

    is CounterState.Upcoming -> UpcomingCounter(
      event = state.event,
      countdown = state.countdown,
      onBackClick = onBackClick,
      onEditClick = onEditClick,
      onDeleteClick = onDeleteClick,
      modifier = modifier,
    )

    is CounterState.Past -> PastCounter(
      event = state.event,
      breakdown = state.breakdown,
      onBackClick = onBackClick,
      onDeleteClick = onDeleteClick,
      modifier = modifier,
    )
  }
}

// ── Upcoming counter ──────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen upcoming counter composable.
 *
 * Hero background uses [EventPalette.hero] colour derived from [event.color.ordinal].
 * AC-CL-13: top bar includes arrow_back, edit, and more_vert (containing "Delete").
 * AC-CL-14: header shows blob icon, "UNTIL" label, event title, and date chip.
 * AC-CL-18: amber (ordinal 8) and orange (ordinal 0) palettes receive a scrim on glass cells
 * to maintain ≥4.5:1 contrast.
 */
@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingCounter(
  event: Event,
  countdown: CountdownResult.Upcoming,
  onBackClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val palette = remember(event.color.ordinal, isDark) {
    eventPaletteByIndex(index = event.color.ordinal, dark = isDark)
  }
  // AC-CL-18: amber (0=ORANGE, 8=AMBER) are light palettes — add scrim to maintain contrast.
  val needsScrim = event.color == EventColor.ORANGE || event.color == EventColor.AMBER

  // Edge-to-edge: requires Activity.enableEdgeToEdge() in :app/MainActivity.kt (already present).
  // Status/navigation bars are colored to match palette.hero so they blend into the background.
  // Light-icon / dark-icon selection uses luminance: hero luminance > 0.5 → light bg → dark icons.
  val view = LocalView.current
  if (!view.isInEditMode) {
    val heroArgb = palette.hero.toArgb()
    val useLightIcons = ColorUtils.calculateLuminance(heroArgb) <= 0.5
    SideEffect {
      val window = (view.context as? Activity)?.window ?: return@SideEffect
      @Suppress("DEPRECATION")
      window.statusBarColor = heroArgb
      @Suppress("DEPRECATION")
      window.navigationBarColor = heroArgb
      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = !useLightIcons
      insetsController.isAppearanceLightNavigationBars = !useLightIcons
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(palette.hero),
  ) {
    // Background blob decorations — purely decorative, excluded from accessibility tree.
    Box(
      modifier = Modifier
        .size(200.dp)
        .offset(x = 120.dp, y = (-60).dp)
        .align(Alignment.TopEnd)
        .clip(BlobShape.Variant2)
        .background(palette.container.copy(alpha = 0.3f)),
    )
    Box(
      modifier = Modifier
        .size(200.dp)
        .offset(x = (-80).dp, y = 60.dp)
        .align(Alignment.BottomStart)
        .clip(BlobShape.Variant3)
        .background(palette.container.copy(alpha = 0.2f)),
    )

    Scaffold(
      topBar = {
        CounterTopBar(
          showEdit = true,
          onHeroColor = palette.onHero,
          onBackClick = onBackClick,
          onEditClick = onEditClick,
          onDeleteClick = onDeleteClick,
        )
      },
      containerColor = Color.Transparent,
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        Spacer(modifier = Modifier.height(8.dp))

        CounterHeader(
          event = event,
          headerLabel = stringResource(R.string.counter_label_until),
          palette = palette,
          onHeroColor = palette.onHero,
        )

        CounterPrimary(
          countdown = countdown,
          onHeroColor = palette.onHero,
        )

        GlassRow(
          countdown = countdown,
          needsScrim = needsScrim,
          onHeroColor = palette.onHero,
        )

        Spacer(modifier = Modifier.weight(1f))
      }
    }
  }
}

// ── Past counter ──────────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen past counter composable.
 *
 * AC-CL-13: past top bar has only arrow_back and more_vert (no edit button).
 */
@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastCounter(
  event: Event,
  breakdown: PastBreakdown,
  onBackClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val palette = remember(event.color.ordinal, isDark) {
    eventPaletteByIndex(index = event.color.ordinal, dark = isDark)
  }

  // Edge-to-edge: system bars colored to match palette.hero (same logic as UpcomingCounter).
  val view = LocalView.current
  if (!view.isInEditMode) {
    val heroArgb = palette.hero.toArgb()
    val useLightIcons = ColorUtils.calculateLuminance(heroArgb) <= 0.5
    SideEffect {
      val window = (view.context as? Activity)?.window ?: return@SideEffect
      @Suppress("DEPRECATION")
      window.statusBarColor = heroArgb
      @Suppress("DEPRECATION")
      window.navigationBarColor = heroArgb
      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = !useLightIcons
      insetsController.isAppearanceLightNavigationBars = !useLightIcons
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(palette.hero),
  ) {
    // Decorative background blobs — excluded from accessibility tree.
    Box(
      modifier = Modifier
        .size(200.dp)
        .offset(x = 120.dp, y = (-60).dp)
        .align(Alignment.TopEnd)
        .clip(BlobShape.Variant2)
        .background(palette.container.copy(alpha = 0.3f)),
    )
    Box(
      modifier = Modifier
        .size(200.dp)
        .offset(x = (-80).dp, y = 60.dp)
        .align(Alignment.BottomStart)
        .clip(BlobShape.Variant3)
        .background(palette.container.copy(alpha = 0.2f)),
    )

    Scaffold(
      topBar = {
        CounterTopBar(
          showEdit = false,
          onHeroColor = palette.onHero,
          onBackClick = onBackClick,
          onEditClick = {},
          onDeleteClick = onDeleteClick,
        )
      },
      containerColor = Color.Transparent,
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        Spacer(modifier = Modifier.height(8.dp))

        CounterHeader(
          event = event,
          headerLabel = stringResource(R.string.counter_label_since),
          palette = palette,
          onHeroColor = palette.onHero,
        )

        PastPrimary(
          breakdown = breakdown,
          onHeroColor = palette.onHero,
        )

        Spacer(modifier = Modifier.weight(1f))
      }
    }
  }
}

// ── Top App Bar ───────────────────────────────────────────────────────────────────────────────────

/**
 * Transparent top bar for both upcoming and past counter screens.
 *
 * AC-CL-13: upcoming has arrow_back + edit + more_vert; past has arrow_back + more_vert only.
 * AC-CL-17: all icons carry contentDescriptions.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterTopBar(
  showEdit: Boolean,
  onHeroColor: Color,
  onBackClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  TopAppBar(
    title = {},
    modifier = modifier,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = Color.Transparent,
      navigationIconContentColor = onHeroColor,
      actionIconContentColor = onHeroColor,
    ),
    navigationIcon = {
      IconButton(onClick = onBackClick) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.counter_back_description),
        )
      }
    },
    actions = {
      if (showEdit) {
        IconButton(onClick = onEditClick) {
          Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = stringResource(R.string.counter_edit_description),
          )
        }
      }

      Box {
        IconButton(onClick = { menuExpanded = true }) {
          Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.counter_more_description),
          )
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.counter_menu_delete)) },
            onClick = {
              menuExpanded = false
              onDeleteClick()
            },
          )
        }
      }
    },
  )
}

// ── Header ────────────────────────────────────────────────────────────────────────────────────────

/**
 * Counter header: blob icon, "UNTIL"/"SINCE" label, event title, date chip.
 *
 * AC-CL-14: blob icon (72dp, [BlobShape.Variant4]) filled with [EventPalette.container] colour,
 * contains the [EventSymbol] icon. Label "ДО СОБЫТИЯ" / "ПРОШЛО" shown in CAPS.
 * Title is capped at 2 lines with ellipsis (AC-CL-16).
 * Date chip shows the full target date: "30 апреля 2026 · 11:00".
 */
@Suppress("LongParameterList")
@Composable
private fun CounterHeader(
  event: Event,
  headerLabel: String,
  palette: com.datecountdown.app.core.design.theme.EventPalette,
  onHeroColor: Color,
  modifier: Modifier = Modifier,
) {
  val designIcon = DesignEventIcon.entries[event.icon.ordinal]

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // AC-CL-17: blob icon shape is decorative; EventSymbol carries the contentDescription.
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(BlobShape.Variant4)
        .background(palette.container),
      contentAlignment = Alignment.Center,
    ) {
      EventSymbol(
        icon = designIcon,
        size = 36.sp,
        tint = palette.onContainer,
        contentDescription = stringResource(R.string.counter_icon_description, designIcon.symbolName),
      )
    }

    Text(
      text = headerLabel,
      style = MaterialTheme.typography.labelSmall,
      color = onHeroColor.copy(alpha = 0.7f),
      letterSpacing = 2.sp,
    )

    Text(
      text = event.title,
      style = MaterialTheme.typography.headlineSmall,
      color = onHeroColor,
      fontWeight = FontWeight.SemiBold,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )

    CounterDateChip(
      event = event,
      onHeroColor = onHeroColor,
      containerColor = palette.container.copy(alpha = 0.4f),
    )
  }
}

/**
 * Date chip displaying the event's target date/time.
 *
 * AC-CL-14: format "30 апреля 2026 · 11:00" — full date with month name and wall-clock time.
 */
@Composable
private fun CounterDateChip(
  event: Event,
  onHeroColor: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
) {
  val formattedDate = remember(event.targetDateTime) {
    event.targetDateTime
      .toLocalDateTime(TimeZone.currentSystemDefault())
      .toJavaLocalDateTime()
      .format(dateChipFormatter)
  }

  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = containerColor,
  ) {
    Text(
      text = formattedDate,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      style = MaterialTheme.typography.bodySmall,
      color = onHeroColor,
    )
  }
}

// ── Primary number display ────────────────────────────────────────────────────────────────────────

/**
 * Primary countdown number block.
 *
 * AC-CL-4: primary=YEARS shows years (large) + days (smaller) in a column.
 * AC-CL-5/6: all other primaries show one large number with pluralised label below.
 * AC-CL-19: the whole block is wrapped with [semantics(mergeDescendants=true)] and a
 * descriptive [contentDescription] so TalkBack reads "3 days until event", not digit-by-digit.
 *
 * TODO (AC-CL-15): apply auto-size / clamped sp to prevent primary number overflow at max fontScale.
 */
@Suppress("LongMethod")
@Composable
private fun CounterPrimary(
  countdown: CountdownResult.Upcoming,
  onHeroColor: Color,
  modifier: Modifier = Modifier,
) {
  // Build the accessibility phrase before the composable tree (AC-CL-19).
  val a11yDescription = buildUpcomingA11yDescription(countdown)

  Box(
    modifier = modifier.semantics(mergeDescendants = true) {
      contentDescription = a11yDescription
    },
    contentAlignment = Alignment.Center,
  ) {
    when (countdown.primary) {
      CountdownUnit.YEARS -> {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          val yearsLabel = pluralStringResource(
            id = CommonR.plurals.time_unit_year,
            count = countdown.years,
            countdown.years,
          )
          Text(
            text = yearsLabel,
            style = MaterialTheme.typography.displayLarge,
            color = onHeroColor,
            fontWeight = FontWeight.Bold,
          )
          val daysLabel = pluralStringResource(
            id = CommonR.plurals.time_unit_day,
            count = countdown.days,
            countdown.days,
          )
          Text(
            text = daysLabel,
            style = MaterialTheme.typography.headlineMedium,
            color = onHeroColor.copy(alpha = 0.85f),
          )
        }
      }

      CountdownUnit.DAYS -> {
        val label = pluralStringResource(
          id = CommonR.plurals.time_unit_day,
          count = countdown.days,
          countdown.days,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.displayLarge,
          color = onHeroColor,
          fontWeight = FontWeight.Bold,
        )
      }

      CountdownUnit.HOURS -> {
        val label = pluralStringResource(
          id = CommonR.plurals.time_unit_hour,
          count = countdown.hours,
          countdown.hours,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.displayLarge,
          color = onHeroColor,
          fontWeight = FontWeight.Bold,
        )
      }

      CountdownUnit.MINUTES -> {
        val label = pluralStringResource(
          id = CommonR.plurals.time_unit_minute,
          count = countdown.minutes,
          countdown.minutes,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.displayLarge,
          color = onHeroColor,
          fontWeight = FontWeight.Bold,
        )
      }

      CountdownUnit.SECONDS -> {
        val label = pluralStringResource(
          id = CommonR.plurals.time_unit_second,
          count = countdown.seconds,
          countdown.seconds,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.displayLarge,
          color = onHeroColor,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

/**
 * Primary past display block.
 *
 * AC-PE-11: when breakdown is [PastBreakdown.Today], shows "Today" (no number).
 * Otherwise shows "[N] days ago" from [PastBreakdown.DaysAgo.days].
 */
@Composable
private fun PastPrimary(
  breakdown: PastBreakdown,
  onHeroColor: Color,
  modifier: Modifier = Modifier,
) {
  val a11yDescription = buildPastA11yDescription(breakdown)

  Box(
    modifier = modifier.semantics(mergeDescendants = true) {
      contentDescription = a11yDescription
    },
    contentAlignment = Alignment.Center,
  ) {
    when (breakdown) {
      PastBreakdown.Today -> {
        Text(
          text = stringResource(R.string.counter_past_today),
          style = MaterialTheme.typography.displayLarge,
          color = onHeroColor,
          fontWeight = FontWeight.Bold,
        )
      }

      is PastBreakdown.DaysAgo -> {
        val label = pluralStringResource(
          id = CommonR.plurals.days_ago,
          count = breakdown.days,
          breakdown.days,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.displayLarge,
          color = onHeroColor,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

// ── Glass row ─────────────────────────────────────────────────────────────────────────────────────

/**
 * AC-CL-8: row of frosted-glass cells showing time units below the primary.
 *
 * | primary   | cells         |
 * |-----------|---------------|
 * | YEARS     | h, m, s (3)   |
 * | DAYS      | h, m, s (3)   |
 * | HOURS     | m, s (2)      |
 * | MINUTES   | s (1)         |
 * | SECONDS   | row hidden    |
 *
 * AC-CL-9: values are zero-padded ("02", "08"); unit label in CAPS below each number.
 * AC-CL-18: [needsScrim] passes a scrim colour to [GlassSurface] on light hero backgrounds.
 */
@Suppress("LongMethod")
@Composable
private fun GlassRow(
  countdown: CountdownResult.Upcoming,
  needsScrim: Boolean,
  onHeroColor: Color,
  modifier: Modifier = Modifier,
) {
  // AC-CL-8: SECONDS → row hidden.
  if (countdown.primary == CountdownUnit.SECONDS) return

  val scrimColor = if (needsScrim) Color.Black.copy(alpha = 0.15f) else Color.Transparent

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
  ) {
    // Build the list of (value, label) pairs for this primary unit.
    val cells: List<Pair<Int, String>> = when (countdown.primary) {
      CountdownUnit.YEARS,
      CountdownUnit.DAYS,
      -> listOf(
        countdown.hours to stringResource(R.string.counter_unit_hours),
        countdown.minutes to stringResource(R.string.counter_unit_minutes),
        countdown.seconds to stringResource(R.string.counter_unit_seconds),
      )

      CountdownUnit.HOURS -> listOf(
        countdown.minutes to stringResource(R.string.counter_unit_minutes),
        countdown.seconds to stringResource(R.string.counter_unit_seconds),
      )

      CountdownUnit.MINUTES -> listOf(
        countdown.seconds to stringResource(R.string.counter_unit_seconds),
      )

      // SECONDS: hidden — handled by early return above.
      CountdownUnit.SECONDS -> emptyList()
    }

    cells.forEach { (value, label) ->
      GlassCell(
        value = value,
        label = label,
        scrimColor = scrimColor,
        onHeroColor = onHeroColor,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

/**
 * Single frosted-glass cell with a zero-padded number and CAPS unit label (AC-CL-9).
 *
 * Uses [GlassSurface] with optional scrim to maintain contrast on light hero backgrounds
 * (AC-CL-18).
 */
@Composable
private fun GlassCell(
  value: Int,
  label: String,
  scrimColor: Color,
  onHeroColor: Color,
  modifier: Modifier = Modifier,
) {
  GlassSurface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    scrimColor = scrimColor,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      // AC-CL-9: leading-zero format "02", "08".
      Text(
        text = String.format(Locale.getDefault(), "%02d", value),
        style = MaterialTheme.typography.headlineLarge,
        color = onHeroColor,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = onHeroColor.copy(alpha = 0.7f),
        letterSpacing = 1.5.sp,
      )
    }
  }
}

// ── Loading / NotFound / Error states ────────────────────────────────────────────────────────────

@Composable
private fun CounterLoadingContent(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize())
}

@Composable
private fun CounterNotFoundContent(
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(R.string.counter_not_found_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      androidx.compose.material3.Button(onClick = onBackClick) {
        Text(stringResource(R.string.counter_not_found_back))
      }
    }
  }
}

@Composable
private fun CounterErrorContent(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = stringResource(R.string.counter_error_message),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.error,
    )
  }
}

// ── Accessibility helpers ─────────────────────────────────────────────────────────────────────────

@Composable
private fun buildUpcomingA11yDescription(countdown: CountdownResult.Upcoming): String {
  val primaryValue = when (countdown.primary) {
    CountdownUnit.YEARS -> pluralStringResource(
      CommonR.plurals.time_unit_year, countdown.years, countdown.years,
    )
    CountdownUnit.DAYS -> pluralStringResource(
      CommonR.plurals.time_unit_day, countdown.days, countdown.days,
    )
    CountdownUnit.HOURS -> pluralStringResource(
      CommonR.plurals.time_unit_hour, countdown.hours, countdown.hours,
    )
    CountdownUnit.MINUTES -> pluralStringResource(
      CommonR.plurals.time_unit_minute, countdown.minutes, countdown.minutes,
    )
    CountdownUnit.SECONDS -> pluralStringResource(
      CommonR.plurals.time_unit_second, countdown.seconds, countdown.seconds,
    )
  }
  return stringResource(R.string.counter_a11y_until, primaryValue)
}

@Composable
private fun buildPastA11yDescription(breakdown: PastBreakdown): String =
  when (breakdown) {
    PastBreakdown.Today -> stringResource(R.string.counter_a11y_today)
    is PastBreakdown.DaysAgo -> stringResource(
      R.string.counter_a11y_since,
      pluralStringResource(CommonR.plurals.days_ago, breakdown.days, breakdown.days),
    )
  }

// ── Previews ─────────────────────────────────────────────────────────────────────────────────────

private val previewNow: Instant = Clock.System.now()

@Suppress("MagicNumber")
private fun previewEvent(
  color: EventColor = EventColor.BLUE,
  icon: DomainEventIcon = DomainEventIcon.CELEBRATION,
  daysOffset: Long = 42L,
): Event = Event(
  id = EventId("preview"),
  title = "Trip to Japan",
  targetDateTime = previewNow.plus(daysOffset.days),
  color = color,
  icon = icon,
  createdAt = previewNow,
)

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun CounterLoadingPreview() {
  DateCountdownTheme {
    CounterScreenContent(
      state = CounterState.Loading,
      onBackClick = {},
      onEditClick = {},
      onDeleteClick = {},
    )
  }
}

@Suppress("UnusedPrivateMember", "MagicNumber")
@PreviewLightDark
@Composable
private fun CounterUpcomingYearsDaysPreview() {
  DateCountdownTheme {
    CounterScreenContent(
      state = CounterState.Upcoming(
        event = previewEvent(color = EventColor.INDIGO, daysOffset = 3011L),
        countdown = CountdownResult.Upcoming(
          years = 8,
          days = 46,
          hours = 3,
          minutes = 17,
          seconds = 44,
          primary = CountdownUnit.YEARS,
        ),
      ),
      onBackClick = {},
      onEditClick = {},
      onDeleteClick = {},
    )
  }
}

@Suppress("UnusedPrivateMember", "MagicNumber")
@PreviewLightDark
@Composable
private fun CounterUpcomingTodayHoursPreview() {
  DateCountdownTheme {
    CounterScreenContent(
      state = CounterState.Upcoming(
        event = previewEvent(color = EventColor.TEAL, daysOffset = 0L),
        countdown = CountdownResult.Upcoming(
          years = 0,
          days = 0,
          hours = 4,
          minutes = 22,
          seconds = 8,
          primary = CountdownUnit.HOURS,
        ),
      ),
      onBackClick = {},
      onEditClick = {},
      onDeleteClick = {},
    )
  }
}

@Suppress("UnusedPrivateMember", "MagicNumber")
@PreviewLightDark
@Composable
private fun CounterPastPreview() {
  DateCountdownTheme {
    CounterScreenContent(
      state = CounterState.Past(
        event = previewEvent(color = EventColor.PURPLE, daysOffset = -30L),
        breakdown = PastBreakdown.DaysAgo(days = 30),
      ),
      onBackClick = {},
      onEditClick = {},
      onDeleteClick = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun CounterPastTodayPreview() {
  DateCountdownTheme {
    CounterScreenContent(
      state = CounterState.Past(
        event = previewEvent(color = EventColor.GREEN, daysOffset = 0L),
        breakdown = PastBreakdown.Today,
      ),
      onBackClick = {},
      onEditClick = {},
      onDeleteClick = {},
    )
  }
}
