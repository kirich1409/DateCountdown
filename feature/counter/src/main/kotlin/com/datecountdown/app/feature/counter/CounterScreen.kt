@file:Suppress("MagicNumber", "TooManyFunctions")

package com.datecountdown.app.feature.counter

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import com.datecountdown.app.core.design.theme.LocalResolvedDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.datecountdown.app.core.common.R as CommonR
import com.datecountdown.app.core.design.theme.BlobShape
import com.datecountdown.app.core.design.theme.ContentSize
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.core.design.theme.GLASS_ALPHA_DEFAULT
import com.datecountdown.app.core.design.theme.GLASS_ALPHA_LEGACY
import com.datecountdown.app.core.design.theme.GlassSurface
import com.datecountdown.app.core.design.theme.scrimAlphaFor
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

// Date-chip background alpha (AC-CL-18).
// Light: 0.4 — onContainer (dark text) over composite(hero, container, 0.4) → 4.93–5.97:1 ✓.
// Dark:  0.8 — onContainer (light text) over composite(hero, container, 0.8) → 4.75–6.32:1 ✓.
//   At 0.4 the mid-tone composite yields only 2.3–2.7:1 in dark; 0.8 is the minimum alpha
//   that clears 4.5:1 across all 9 dark palettes (worst case: GREEN 4.751:1, PURPLE 4.899:1).
private const val CHIP_BG_ALPHA_LIGHT = 0.4f
private const val CHIP_BG_ALPHA_DARK = 0.8f

/**
 * Two decorative blob shapes placed at the top-end and bottom-start corners of the hero background.
 * Purely visual — excluded from the accessibility tree by being non-clickable, un-described Boxes.
 */
@Composable
private fun BoxScope.CounterBlobDecorations(
  palette: com.datecountdown.app.core.design.theme.EventPalette,
) {
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
}

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
fun CounterScreen(
  component: CounterComponent,
  modifier: Modifier = Modifier,
) {
  BackHandler { component.onBackClick() }

  val state by component.state.subscribeAsState()

  // Stable lambda refs: method references on a Decompose component are not stable across
  // recompositions — wrap in remember(component) so downstream composables can skip correctly.
  val onBackClick = remember(component) { component::onBackClick }
  val onEditClick = remember(component) { component::onEditClick }
  val onDeleteClick = remember(component) { component::onDeleteClick }
  val onRescheduleClick = remember(component) { component::onRescheduleClick }

  CounterScreenContent(
    state = state,
    onBackClick = onBackClick,
    onEditClick = onEditClick,
    onDeleteClick = onDeleteClick,
    onRescheduleClick = onRescheduleClick,
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
private fun CounterScreenContent(
  state: CounterState,
  onBackClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onRescheduleClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (state) {
    CounterState.Loading -> CounterLoadingContent(modifier = modifier)

    CounterState.NotFound -> CounterNotFoundContent(
      onBackClick = onBackClick,
      modifier = modifier,
    )

    is CounterState.Error -> CounterErrorContent(
      cause = state.cause,
      onBackClick = onBackClick,
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
      onRescheduleClick = onRescheduleClick,
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
 * AC-CL-18: a per-palette minimum black scrim is applied on glass cells in light theme to
 * maintain ≥4.5:1 contrast for white text. Scrim alpha is computed via [scrimAlphaFor].
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
  val isDark = LocalResolvedDarkTheme.current
  val palette = remember(event.color.ordinal, isDark) {
    eventPaletteByIndex(index = event.color.ordinal, dark = isDark)
  }
  // AC-CL-18: per-palette minimum scrim to ensure white text ≥4.5:1 contrast on glass cells.
  // glassAlpha mirrors GlassSurface.kt tier constants (GLASS_ALPHA_DEFAULT=0.16 / GLASS_ALPHA_LEGACY=0.26).
  // MUST stay in sync with GlassSurface — see #149 for the planned design-token extraction.
  // isDark is now LocalResolvedDarkTheme.current (fix #169): a forced-LIGHT theme on a dark system
  // correctly applies the light-mode scrim; forced-DARK skips the black scrim as intended.
  val scrimColor = remember(palette.hero, isDark) {
    if (isDark) {
      // dark hero is light → black scrim would reduce contrast, so disabled.
      Color.Transparent
    } else {
      val glassAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) GLASS_ALPHA_DEFAULT else GLASS_ALPHA_LEGACY
      Color.Black.copy(alpha = scrimAlphaFor(hero = palette.hero, glassAlpha = glassAlpha))
    }
  }

  // Edge-to-edge: bars are intentionally transparent; the full-bleed hero Box below paints behind
  // them. Only icon appearance (light vs dark) is controlled here — bars themselves are not colored.
  val view = LocalView.current
  if (!view.isInEditMode) {
    // DisposableEffect instead of LaunchedEffect: the onDispose block restores the app-wide theme
    // default (from RootContent) when the user navigates back to the list screen. Without this
    // restore, the counter's last hero-tint would leak onto the list/edit bars because RootContent's
    // LaunchedEffect is keyed on isDark and does not re-fire when isDark is unchanged.
    DisposableEffect(palette.hero, isDark) {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        // Bars are transparent (WindowCompat.enableEdgeToEdge in MainActivity); the hero Box draws
        // edge-to-edge behind them. Icon tint follows the hero luminance while this screen is shown.
        val useLightIcons = ColorUtils.calculateLuminance(palette.hero.toArgb()) <= 0.5
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !useLightIcons
        insetsController.isAppearanceLightNavigationBars = !useLightIcons
      }
      onDispose {
        // Restore the app-wide theme default so list/edit screens keep legible bar icons.
        val window = (view.context as? Activity)?.window
        if (window != null) {
          val insetsController = WindowCompat.getInsetsController(window, view)
          insetsController.isAppearanceLightStatusBars = !isDark
          insetsController.isAppearanceLightNavigationBars = !isDark
        }
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(palette.hero),
  ) {
    CounterBlobDecorations(palette = palette)

    Scaffold(
      topBar = {
        CounterTopBar(
          showEdit = true,
          showMore = true,
          onHeroColor = palette.onHero,
          onBackClick = onBackClick,
          onEditClick = onEditClick,
          onDeleteClick = onDeleteClick,
        )
      },
      containerColor = Color.Transparent,
    ) { innerPadding ->
      // #187/#189/#190: BoxWithConstraints spans the full Scaffold body so maxWidth reflects
      // actual window width (before the column cap is applied). This is the measuring point
      // for the adaptive font derivation (#190) — measuring inside the 520dp column would
      // make the font scale never fire (520 < 600 threshold on all screens).
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        // #190: adaptive primaryValueMaxFontSize.
        // At ≤600dp (phones): cap = 96sp (unchanged). Above 600dp: linearly ramp to 160sp
        // at 1280dp. Derived from usable content width, not the device diagonal.
        // Values at key breakpoints: 360dp→96sp, 600dp→96sp, 800dp→≈115sp, 1280dp→160sp.
        // remember(maxWidth): derivation depends only on window width; memoised to avoid
        // recomputing on every countdown tick (~1 Hz). Recomputes only on resize.
        val upcomingMaxFontSize = remember(maxWidth) { adaptivePrimaryMaxFontSize(maxWidth) }

        // #189: center the content cluster vertically only on large windows (maxWidth ≥ 520dp).
        // On phone the window is always narrower than CounterColumnMax — centering is skipped
        // and the original top-pinned layout (Arrangement.spacedBy + trailing weight spacer)
        // is preserved byte-for-byte.
        // remember(maxWidth): same resize-only dependency as upcomingMaxFontSize above.
        val centerContent = remember(maxWidth) { maxWidth >= ContentSize.CounterColumnMax }

        Column(
          modifier = Modifier
            .fillMaxSize()
            // #187: center the content column horizontally; phone widths (<520dp) pass through
            // unchanged — wrapContentWidth + widthIn(max=520) is a no-op when available width
            // is already ≤520dp.
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = ContentSize.CounterColumnMax)
            .padding(horizontal = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          // #189: on large windows (centerContent=true), distribute free space symmetrically
          // around the content cluster. On phone, top-pin with a trailing weight spacer (original
          // layout from origin/main).
          verticalArrangement = if (centerContent) {
            Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
          } else {
            Arrangement.spacedBy(24.dp)
          },
        ) {
          Spacer(modifier = Modifier.height(8.dp))

          CounterHeader(
            event = event,
            headerLabel = stringResource(R.string.counter_label_until),
            palette = palette,
            onHeroColor = palette.onHero,
            isDark = isDark,
          )

          CounterPrimary(
            countdown = countdown,
            onHeroColor = palette.onHero,
            maxAutoSizeFontSize = upcomingMaxFontSize,
          )

          GlassRow(
            countdown = countdown,
            scrimColor = scrimColor,
            onHeroColor = palette.onHero,
          )

          // Phone branch: trailing spacer pins the cluster to the top (matches origin/main).
          // Large-window branch: CenterVertically above makes this spacer redundant.
          if (!centerContent) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

// ── Past counter ──────────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen past counter composable.
 *
 * AC-PE-6: background is surfaceContainerHighest (neutral graphite), not the event palette hero.
 * AC-PE-7: errorContainer chip "СОБЫТИЕ В ПРОШЛОМ", event title, date string.
 * AC-PE-8: large −N / "Сегодня" with autosize, "N дней назад" label below.
 * AC-PE-9: Reschedule + Delete buttons at the bottom.
 * AC-PE-10: top bar shows arrow_back only (more_vert hidden — Delete is explicit on screen).
 */
@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastCounter(
  event: Event,
  breakdown: PastBreakdown,
  onBackClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onRescheduleClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest

  // Edge-to-edge: past screen uses neutral surface color, not the event palette hero.
  val isDark = LocalResolvedDarkTheme.current
  val view = LocalView.current
  if (!view.isInEditMode) {
    // DisposableEffect: onDispose restores the app-wide theme default (from RootContent) when the
    // user navigates back. Without this, the surface-tint leaks onto the list/edit bars.
    DisposableEffect(surfaceColor, isDark) {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        // Bars are transparent (WindowCompat.enableEdgeToEdge in MainActivity); the surface Box
        // draws edge-to-edge behind them. Icon tint follows surface luminance while this screen is shown.
        val useLightIcons = ColorUtils.calculateLuminance(surfaceColor.toArgb()) <= 0.5
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !useLightIcons
        insetsController.isAppearanceLightNavigationBars = !useLightIcons
      }
      onDispose {
        // Restore the app-wide theme default so list/edit screens keep legible bar icons.
        val window = (view.context as? Activity)?.window
        if (window != null) {
          val insetsController = WindowCompat.getInsetsController(window, view)
          insetsController.isAppearanceLightStatusBars = !isDark
          insetsController.isAppearanceLightNavigationBars = !isDark
        }
      }
    }
  }

  val designIcon = DesignEventIcon.entries[event.icon.ordinal]
  val formattedDate = remember(event.targetDateTime) {
    event.targetDateTime
      .toLocalDateTime(TimeZone.currentSystemDefault())
      .toJavaLocalDateTime()
      .format(dateChipFormatter)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(surfaceColor),
  ) {
    Scaffold(
      topBar = {
        // AC-PE-10: more_vert hidden on past — Delete is already an explicit bottom button.
        CounterTopBar(
          showEdit = false,
          showMore = false,
          onHeroColor = MaterialTheme.colorScheme.onSurface,
          onBackClick = onBackClick,
          onEditClick = {},  // no-op: edit button is hidden (showEdit = false)
          onDeleteClick = onDeleteClick,
        )
      },
      containerColor = Color.Transparent,
    ) { innerPadding ->
      // #187/#189/#190: BoxWithConstraints spans the full Scaffold body so maxWidth reflects
      // actual window width (before the column cap). Same rationale as UpcomingCounter.
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        // #190: same adaptive font derivation as UpcomingCounter.
        // remember(maxWidth): recomputes only on resize, not on every countdown tick (~1 Hz).
        val pastMaxFontSize = remember(maxWidth) { adaptivePrimaryMaxFontSize(maxWidth) }

        // #189: center the header+primary cluster above the pinned button row only on large
        // windows. On phone (maxWidth < CounterColumnMax=520dp), the layout is top-pinned —
        // identical to origin/main — with only the trailing Spacer(weight(1f)) before buttons.
        // remember(maxWidth): same resize-only dependency as pastMaxFontSize above.
        val centerContent = remember(maxWidth) { maxWidth >= ContentSize.CounterColumnMax }

        Column(
          modifier = Modifier
            .fillMaxSize()
            // #187: cap + center content column; no-op on phone widths (<520dp).
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = ContentSize.CounterColumnMax),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // On large windows: a leading spacer pushes the header+primary cluster down from the
          // top so that the cluster centers within the space above the pinned button row.
          // On phone: omitted — cluster starts at the top (original top-pinned behavior).
          if (centerContent) {
            Spacer(modifier = Modifier.weight(1f))
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Header: muted blob icon + past-event chip + title + date
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            // AC-PE-6: приглушённая blob-иконка — surfaceContainerHigh bg, onSurfaceVariant tint.
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(BlobShape.Variant4)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
              contentAlignment = Alignment.Center,
            ) {
              EventSymbol(
                icon = designIcon,
                size = 36.sp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = stringResource(
                  R.string.counter_icon_description,
                  stringResource(designIcon.labelRes),
                ),
              )
            }

            Text(
              text = event.title,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )

            Text(
              text = formattedDate,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // AC-PE-7: chip "СОБЫТИЕ В ПРОШЛОМ" with errorContainer colors.
          // AC-PE-17: clearAndSetSemantics replaces the AssistChip's built-in Role.Button +
          // OnClick action with a plain contentDescription, so TalkBack reads the label as
          // a static status text and does not announce "double tap to activate".
          val pastChipDesc = stringResource(R.string.counter_chip_past)
          @Suppress("EmptyFunctionBlock")
          AssistChip(
            onClick = {},  // no-op: required by AssistChip API; role removed via clearAndSetSemantics
            label = { Text(pastChipDesc) },
            modifier = Modifier.clearAndSetSemantics { contentDescription = pastChipDesc },
            colors = AssistChipDefaults.assistChipColors(
              containerColor = MaterialTheme.colorScheme.errorContainer,
              labelColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
          )

          Spacer(modifier = Modifier.height(32.dp))

          // AC-PE-8: large primary number with autosize.
          PastPrimary(
            breakdown = breakdown,
            onSurfaceColor = MaterialTheme.colorScheme.onSurface,
            maxAutoSizeFontSize = pastMaxFontSize,
            modifier = Modifier.padding(horizontal = 24.dp),
          )

          // Trailing spacer: pins the button row to the bottom in both phone and large-window
          // layouts. On phone this also top-pins the cluster (original behavior). On large windows
          // the leading spacer above balances this one, centering the header+primary cluster
          // within the space above the buttons.
          Spacer(modifier = Modifier.weight(1f))

          // AC-PE-9: Reschedule + Delete buttons.
          // #188: fillMaxWidth so the two weight(1f) buttons divide the full column width; then
          // wrapContentWidth + widthIn(max=ButtonMax) center and cap the row at 360dp on wide
          // screens. On phone the row width ≈ column width (≤360dp) so fillMaxWidth is a no-op
          // relative to the original behavior.
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .wrapContentWidth(Alignment.CenterHorizontally)
              .widthIn(max = ContentSize.ButtonMax)
              .padding(horizontal = 16.dp)
              .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            val rescheduleDesc = stringResource(R.string.counter_a11y_reschedule)
            FilledTonalButton(
              onClick = onRescheduleClick,
              modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = rescheduleDesc },
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              ),
            ) {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,  // decorative — button contentDescription covers semantics
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.counter_reschedule_button))
            }

            val deleteDesc = stringResource(R.string.counter_a11y_delete)
            Button(
              onClick = onDeleteClick,
              modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = deleteDesc },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
              ),
            ) {
              Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,  // decorative — button contentDescription covers semantics
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.counter_delete_button))
            }
          }
        }
      }
    }
  }
}

// ── Top App Bar ───────────────────────────────────────────────────────────────────────────────────

/**
 * Transparent top bar for both upcoming and past counter screens.
 *
 * AC-CL-13: upcoming has arrow_back + edit + more_vert (showEdit=true, showMore=true).
 * AC-PE-10: past has arrow_back only (showEdit=false, showMore=false) — Delete is on-screen.
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
  showMore: Boolean = true,
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

      // AC-PE-10: share intentionally omitted from MVP top bar (mockups show it, spec is the contract).
      if (showMore) {
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
  isDark: Boolean,
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
        contentDescription = stringResource(R.string.counter_icon_description, stringResource(designIcon.labelRes)),
      )
    }

    // AC-CL-18: full alpha — contrast is guaranteed by the per-palette scrim on glass cells,
    // not by reducing label opacity. Alpha reduction is removed to satisfy WCAG AA for small text.
    Text(
      text = headerLabel,
      style = MaterialTheme.typography.labelSmall,
      color = onHeroColor,
      letterSpacing = 2.sp,
    )

    // Title line length is bounded by CounterColumnMax (520dp) — ReadableTextMax (600dp) would
    // be a no-op here since the enclosing column is already capped at 520dp.
    Text(
      text = event.title,
      style = MaterialTheme.typography.headlineSmall,
      color = onHeroColor,
      fontWeight = FontWeight.SemiBold,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )

    // AC-CL-18: date-chip contrast is theme-dependent.
    // Light: onContainer (dark text) over container@0.4 blended onto hero — ratio 4.93–5.97:1 ✓.
    // Dark: hero is light, container is dark — onContainer (light text) over container@0.8 — ratio 4.75–6.32:1 ✓.
    //   At 0.4 the mid-tone composite yields only 2.3–2.7:1; 0.8 is the minimum alpha
    //   that clears 4.5:1 across all 9 dark palettes (worst case: GREEN 4.751:1).
    val chipBgAlpha = if (isDark) CHIP_BG_ALPHA_DARK else CHIP_BG_ALPHA_LIGHT
    CounterDateChip(
      event = event,
      labelColor = palette.onContainer,
      containerColor = palette.container.copy(alpha = chipBgAlpha),
    )
  }
}

/**
 * Date chip displaying the event's target date/time.
 *
 * AC-CL-14: format "30 апреля 2026 · 11:00" — full date with month name and wall-clock time.
 * Uses AssistChip for Material 3 chip styling. Non-interactive — clearAndSetSemantics exposes
 * the date text without announcing it as a button (AC-PE-17).
 *
 * AC-CL-18: [labelColor] uses [EventPalette.onContainer] in both themes; [containerColor] alpha
 * is theme-dependent (light 0.4, dark 0.8). Light achieves 4.93–5.97:1; dark 4.75–6.32:1.
 * In dark theme the hero is light and the container is dark, so a higher alpha is required
 * to push the composite bg dark enough for onContainer (light text) to clear 4.5:1.
 */
@Composable
private fun CounterDateChip(
  event: Event,
  labelColor: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
) {
  val formattedDate = remember(event.targetDateTime) {
    event.targetDateTime
      .toLocalDateTime(TimeZone.currentSystemDefault())
      .toJavaLocalDateTime()
      .format(dateChipFormatter)
  }

  @Suppress("EmptyFunctionBlock")
  AssistChip(
    onClick = {},  // no-op: visual chip only — clearAndSetSemantics neutralises the click role
    label = { Text(text = formattedDate, style = MaterialTheme.typography.bodySmall) },
    modifier = modifier.clearAndSetSemantics { contentDescription = formattedDate },
    colors = AssistChipDefaults.assistChipColors(
      containerColor = containerColor,
      labelColor = labelColor,
    ),
  )
}

// ── Primary number display ────────────────────────────────────────────────────────────────────────

/**
 * Primary countdown number block.
 *
 * AC-CL-4: primary=YEARS shows years (large) + days (smaller) in a column.
 * AC-CL-5/6: all other primaries show one large number with pluralised label below.
 * AC-CL-15: BasicText + TextAutoSize.StepBased clamps the primary number between 24sp floor
 *   and an adaptive max (96sp on phone, up to 160sp on large screens — see adaptivePrimaryMaxFontSize).
 *   maxLines=1 and softWrap=false prevent wrapping so autoSize is forced to shrink the font
 *   rather than accepting a two-line layout.
 * AC-CL-19: the whole block is wrapped with [semantics(mergeDescendants=true)] and a
 * descriptive [contentDescription] so TalkBack reads "3 days until event", not digit-by-digit.
 */
@Suppress("LongMethod")
@Composable
private fun CounterPrimary(
  countdown: CountdownResult.Upcoming,
  onHeroColor: Color,
  // #190: adaptive ceiling derived from BoxWithConstraints.maxWidth by the caller.
  // 24sp floor is kept; only the max scales up on wide screens.
  maxAutoSizeFontSize: TextUnit = primaryValueMaxFontSize,
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
          // AC-CL-15: autosize clamps displayLarge between 24sp floor and adaptive max;
          // maxLines=1 + softWrap=false prevent wrapping so autoSize shrinks font rather than
          // accepting a 2-line layout.
          BasicText(
            text = yearsLabel,
            style = MaterialTheme.typography.displayLarge.copy(
              fontWeight = FontWeight.Bold,
              color = onHeroColor,
            ),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(
              minFontSize = primaryValueMinFontSize,
              maxFontSize = maxAutoSizeFontSize,
            ),
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
        // AC-CL-15: see primaryValueMinFontSize / adaptivePrimaryMaxFontSize.
        BasicText(
          text = label,
          style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Bold,
            color = onHeroColor,
          ),
          maxLines = 1,
          softWrap = false,
          autoSize = TextAutoSize.StepBased(
            minFontSize = primaryValueMinFontSize,
            maxFontSize = maxAutoSizeFontSize,
          ),
        )
      }

      CountdownUnit.HOURS -> {
        val label = pluralStringResource(
          id = CommonR.plurals.time_unit_hour,
          count = countdown.hours,
          countdown.hours,
        )
        BasicText(
          text = label,
          style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Bold,
            color = onHeroColor,
          ),
          maxLines = 1,
          softWrap = false,
          autoSize = TextAutoSize.StepBased(
            minFontSize = primaryValueMinFontSize,
            maxFontSize = maxAutoSizeFontSize,
          ),
        )
      }

      CountdownUnit.MINUTES -> {
        val label = pluralStringResource(
          id = CommonR.plurals.time_unit_minute,
          count = countdown.minutes,
          countdown.minutes,
        )
        BasicText(
          text = label,
          style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Bold,
            color = onHeroColor,
          ),
          maxLines = 1,
          softWrap = false,
          autoSize = TextAutoSize.StepBased(
            minFontSize = primaryValueMinFontSize,
            maxFontSize = maxAutoSizeFontSize,
          ),
        )
      }

      CountdownUnit.SECONDS -> {
        val label = pluralStringResource(
          id = CommonR.plurals.time_unit_second,
          count = countdown.seconds,
          countdown.seconds,
        )
        BasicText(
          text = label,
          style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Bold,
            color = onHeroColor,
          ),
          maxLines = 1,
          softWrap = false,
          autoSize = TextAutoSize.StepBased(
            minFontSize = primaryValueMinFontSize,
            maxFontSize = maxAutoSizeFontSize,
          ),
        )
      }
    }
  }
}

/**
 * Primary past display block.
 *
 * AC-PE-8: large −N (or "Сегодня" when DaysAgo == 0 is expressed as Today) with autosize,
 * secondary "N дней назад" label below for DaysAgo case.
 * AC-PE-11: Today → large "Сегодня" only; no secondary label.
 * AC-PE-16: BasicText + TextAutoSize.StepBased (24sp floor, 96–160sp adaptive max) with
 *   maxLines=1 + softWrap=false prevents overflow and forces single-line layout at all fontScale values.
 * AC-PE-17: semantics mergeDescendants so TalkBack reads the whole block as one unit.
 */
@Composable
private fun PastPrimary(
  breakdown: PastBreakdown,
  onSurfaceColor: Color,
  // #190: adaptive ceiling derived from BoxWithConstraints.maxWidth by the caller.
  maxAutoSizeFontSize: TextUnit = primaryValueMaxFontSize,
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
        // AC-PE-11: event happened today — show "Сегодня", no number, no secondary label.
        // AC-PE-16: 24sp floor, adaptive max (96sp on phone, up to 160sp on large screens).
        BasicText(
          text = stringResource(R.string.counter_past_today),
          style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Bold,
            color = onSurfaceColor,
          ),
          maxLines = 1,
          softWrap = false,
          autoSize = TextAutoSize.StepBased(
            minFontSize = primaryValueMinFontSize,
            maxFontSize = maxAutoSizeFontSize,
          ),
        )
      }

      is PastBreakdown.DaysAgo -> {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          // AC-PE-8: large "−N" number.
          // AC-PE-16: 24sp floor, adaptive max.
          BasicText(
            text = "−${breakdown.days}",
            style = MaterialTheme.typography.displayLarge.copy(
              fontWeight = FontWeight.Bold,
              color = onSurfaceColor,
            ),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(
              minFontSize = primaryValueMinFontSize,
              maxFontSize = maxAutoSizeFontSize,
            ),
          )
          // AC-PE-8: secondary "N дней назад" label.
          val daysAgoLabel = pluralStringResource(
            id = CommonR.plurals.days_ago,
            count = breakdown.days,
            breakdown.days,
          )
          Text(
            text = daysAgoLabel,
            style = MaterialTheme.typography.headlineSmall,
            color = onSurfaceColor.copy(alpha = 0.7f),
          )
        }
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
 * AC-CL-18: [scrimColor] is the per-palette minimum black scrim pre-computed by the caller
 * via [scrimAlphaFor] to ensure white text ≥4.5:1 contrast. [Color.Transparent] in dark theme.
 */
@Suppress("LongMethod")
@Composable
private fun GlassRow(
  countdown: CountdownResult.Upcoming,
  scrimColor: Color,
  onHeroColor: Color,
  modifier: Modifier = Modifier,
) {
  // AC-CL-8: SECONDS → row hidden.
  if (countdown.primary == CountdownUnit.SECONDS) return

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

      // SECONDS: unreachable — early return above guarantees this branch is never entered.
      else -> error("GlassRow: SECONDS should have been excluded by early return")
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
      // AC-CL-9: leading-zero format "02", "08". Locale.ROOT ensures ASCII digits in all locales.
      // AC-CL-9: unit labels are short genitive plural CAPS forms ("ЧАСОВ"/"МИНУТ"/"СЕКУНД").
      //   Per design, all values share one abbreviation regardless of grammatical number.
      Text(
        text = String.format(Locale.ROOT, "%02d", value),
        style = MaterialTheme.typography.headlineLarge,
        color = onHeroColor,
        fontWeight = FontWeight.Bold,
      )
      // AC-CL-18: full alpha — contrast ensured by per-palette scrim passed via GlassSurface.
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = onHeroColor,
        letterSpacing = 1.5.sp,
      )
    }
  }
}

// ── Loading / NotFound / Error states ────────────────────────────────────────────────────────────

@Composable
private fun CounterLoadingContent(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
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
private fun CounterErrorContent(
  cause: Throwable,
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
        text = stringResource(R.string.counter_error_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
      )
      val causeLabel = cause.message?.takeIf { it.isNotBlank() }
        ?: cause::class.simpleName
      if (causeLabel != null) {
        Text(
          text = causeLabel,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
      }
      androidx.compose.material3.Button(onClick = onBackClick) {
        Text(stringResource(R.string.counter_not_found_back))
      }
    }
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
      onRescheduleClick = {},
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
      onRescheduleClick = {},
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
      onRescheduleClick = {},
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
      onRescheduleClick = {},
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
      onRescheduleClick = {},
    )
  }
}
