@file:Suppress("TooManyFunctions")

package com.datecountdown.app.feature.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.datecountdown.app.core.design.theme.BlobShape
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.core.design.theme.EventIcon as DesignEventIcon
import com.datecountdown.app.core.design.theme.EventPaletteId
import com.datecountdown.app.core.design.theme.EventSymbol
import com.datecountdown.app.core.design.theme.eventPaletteByIndex
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon as DomainEventIcon
import com.datecountdown.app.domain.EventId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

// ── Stateful entry point ─────────────────────────────────────────────────────────────────────────

/**
 * Stateful entry point for the event-list screen.
 *
 * Subscribes to [EventListComponent.state] via Decompose's `subscribeAsState()` and dispatches
 * all user interactions through the component's named methods. This composable holds no local UI
 * state — it merely bridges the Decompose [EventListComponent] to the stateless
 * [EventListScreenContent].
 *
 * ## Stateless / stateful split
 * - [EventListScreen] (this): owns lifecycle bridging via [EventListComponent], never previewed.
 * - [EventListScreenContent]: pure, previewed, receives a snapshot of [EventListState] plus
 *   plain callbacks; has no awareness of the component or Store.
 *
 * ## Card placeholder strategy
 * Card bodies are intentionally minimal — a [BlobShape.Variant4] background with the event title
 * only. Full card content (date chip, countdown number, days-remaining label, icon badge) is
 * implemented in issues #37 (upcoming) and #38 (past).
 *
 * ## Empty-state derivation (AC-LS-14 / AC-LS-15)
 * Both GlobalEmpty and PartialEmpty are derived from [EventListState.Content] by the UI:
 * - GlobalEmpty: `upcoming.isEmpty() && past.isEmpty()`
 * - PartialEmpty: `upcoming.isEmpty() && past.isNotEmpty()`
 * Neither is a separate sealed subtype in the Store.
 */
@Composable
internal fun EventListScreen(component: EventListComponent) {
  val state by component.state.subscribeAsState()
  EventListScreenContent(
    state = state,
    onCardClick = component::onCardClick,
    onAddClick = component::onAddClick,
    onDelete = component::onDelete,
    onUndoDelete = component::onUndoDelete,
    onTogglePast = component::onTogglePast,
  )
}

// ── Stateless content ────────────────────────────────────────────────────────────────────────────

/**
 * Stateless event-list screen body.
 *
 * Receives a snapshot of [EventListState] and plain callback functions — no component reference,
 * no ViewModel, fully previewable.
 *
 * ## Snackbar lifecycle (AC-LS-9, AC-LS-10, AC-LS-21)
 * Driven exclusively by [EventListState.Content.pendingDelete]:
 * - null → A (new delete): launches a coroutine that shows an indefinite snackbar; [SnackbarResult]
 *   drives [onUndoDelete].
 * - A → B (AC-LS-10a replacement): key changes → previous coroutine cancelled (snackbar dismissed),
 *   new coroutine starts for the replacement event.
 * - A → null (timer expired or undo committed): key becomes null → coroutine cancelled, snackbar
 *   dismissed.
 * [SnackbarDuration.Indefinite] ensures the Store's 5-second timer is the single source of truth;
 * the UI never drives dismiss timing independently.
 */
@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventListScreenContent(
  state: EventListState,
  onCardClick: (eventId: String) -> Unit,
  onAddClick: () -> Unit,
  onDelete: (id: EventId) -> Unit,
  onUndoDelete: () -> Unit,
  onTogglePast: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  ObservePendingDeleteSnackbar(
    state = state,
    snackbarHostState = snackbarHostState,
    onUndoDelete = onUndoDelete,
  )

  val contentState = state as? EventListState.Content
  val upcomingCount = contentState?.upcoming?.size ?: 0
  val isGlobalEmpty = contentState?.upcoming?.isEmpty() == true && contentState.past.isEmpty()
  val subtitle = buildSubtitle(isGlobalEmpty = isGlobalEmpty, upcomingCount = upcomingCount)

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      ListTopBar(subtitle = subtitle, scrollBehavior = scrollBehavior)
    },
    floatingActionButton = { ListFab(onClick = onAddClick) },
    snackbarHost = {
      // AC-LS-21: liveRegion = Polite so screen readers announce snackbar content.
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      )
    },
  ) { innerPadding ->
    when (state) {
      EventListState.Loading -> LoadingContent(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )

      is EventListState.Error -> ErrorContent(
        message = stringResource(R.string.list_error_message),
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )

      is EventListState.Content -> ContentBody(
        state = state,
        isGlobalEmpty = isGlobalEmpty,
        onCardClick = onCardClick,
        onAddClick = onAddClick,
        onDelete = onDelete,
        onTogglePast = onTogglePast,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
    }
  }
}

/** Drives the delete snackbar from [EventListState.Content.pendingDelete] (AC-LS-9/10/21). */
@Composable
private fun ObservePendingDeleteSnackbar(
  state: EventListState,
  snackbarHostState: SnackbarHostState,
  onUndoDelete: () -> Unit,
) {
  val pendingDelete = (state as? EventListState.Content)?.pendingDelete
  val deletedLabel = stringResource(R.string.list_snackbar_deleted)
  val undoLabel = stringResource(R.string.list_snackbar_undo)

  // LaunchedEffect key = event id:
  //  - null→A: new effect, show snackbar indefinitely until Store timer or undo.
  //  - A→B: effect restarts (AC-LS-10a), old coroutine cancelled (old snackbar dismissed).
  //  - A→null: effect restarts with null key → immediate return, snackbar dismissed.
  LaunchedEffect(pendingDelete?.event?.id) {
    val pending = pendingDelete ?: return@LaunchedEffect
    val result = snackbarHostState.showSnackbar(
      message = "${pending.event.title} — $deletedLabel",
      actionLabel = undoLabel,
      duration = SnackbarDuration.Indefinite,
    )
    if (result == SnackbarResult.ActionPerformed) {
      onUndoDelete()
    }
  }
}

@Composable
private fun buildSubtitle(isGlobalEmpty: Boolean, upcomingCount: Int): String =
  if (isGlobalEmpty) {
    stringResource(R.string.list_subtitle_empty)
  } else {
    pluralStringResource(
      id = R.plurals.list_subtitle_events_ahead,
      count = upcomingCount,
      upcomingCount,
    )
  }

@Suppress("LongParameterList")
@Composable
private fun ContentBody(
  state: EventListState.Content,
  isGlobalEmpty: Boolean,
  onCardClick: (eventId: String) -> Unit,
  onAddClick: () -> Unit,
  onDelete: (id: EventId) -> Unit,
  onTogglePast: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isGlobalEmpty) {
    GlobalEmptyState(onAddClick = onAddClick, modifier = modifier)
  } else {
    EventsGrid(
      upcoming = state.upcoming,
      past = state.past,
      pastCollapsed = state.pastCollapsed,
      onCardClick = onCardClick,
      onAddClick = onAddClick,
      onDelete = onDelete,
      onTogglePast = onTogglePast,
      modifier = modifier,
    )
  }
}

// ── Top App Bar ──────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTopBar(
  subtitle: String,
  scrollBehavior: TopAppBarScrollBehavior,
  modifier: Modifier = Modifier,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  LargeTopAppBar(
    title = {
      Column {
        Text(text = stringResource(R.string.list_title))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    modifier = modifier,
    navigationIcon = {
      // AC-LS-2: menu is disabled in MVP (drawer not yet implemented).
      // enabled=false provides the correct TalkBack "disabled" announcement and excludes from focus.
      IconButton(
        onClick = { /* no-op: drawer is out of MVP scope */ },
        enabled = false,
      ) {
        Icon(
          imageVector = Icons.Filled.MoreVert, // placeholder — swap with menu/hamburger icon
          contentDescription = stringResource(R.string.list_menu_description),
        )
      }
    },
    actions = {
      // AC-LS-2: search is disabled in MVP.
      IconButton(
        onClick = { /* no-op: search is out of MVP scope */ },
        enabled = false,
      ) {
        Icon(
          imageVector = Icons.Filled.MoreVert, // placeholder — swap with search icon
          contentDescription = stringResource(R.string.list_search_description),
        )
      }

      // AC-LS-2 / AC-LS-19: more_vert is active — opens the settings menu.
      Box {
        IconButton(onClick = { menuExpanded = true }) {
          Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.list_more_description),
          )
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.list_menu_settings)) },
            onClick = { menuExpanded = false },
          )
          // AC-LS-19: "Enable notifications" item shown only when runtime permission is absent.
          // Permission checking is implemented in issue #44.
        }
      }
    },
    scrollBehavior = scrollBehavior,
  )
}

// ── Filter chips ─────────────────────────────────────────────────────────────────────────────────

/**
 * Horizontal row of filter chips. In MVP all chips are visual-only: "All" is always selected,
 * tapping any chip is a no-op. Functional filtering is tracked in issue #09 (post-MVP).
 *
 * AC-LS-3, AC-LS-22.
 */
@Composable
private fun FilterChipsRow(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // "All" — always selected. The check-mark is rendered by FilterChip (visual only in MVP).
    FilterChip(
      selected = true,
      onClick = { /* no-op: filtering is post-MVP */ },
      label = { Text(stringResource(R.string.list_filter_all)) },
    )
    FilterChip(
      selected = false,
      onClick = { /* no-op: filtering is post-MVP */ },
      label = { Text(stringResource(R.string.list_filter_soon)) },
    )
    FilterChip(
      selected = false,
      onClick = { /* no-op: filtering is post-MVP */ },
      label = { Text(stringResource(R.string.list_filter_this_month)) },
    )
    FilterChip(
      selected = false,
      onClick = { /* no-op: filtering is post-MVP */ },
      label = { Text(stringResource(R.string.list_filter_past)) },
    )
  }
}

// ── Events grid ──────────────────────────────────────────────────────────────────────────────────

/**
 * 2-column lazy grid containing:
 *  - filter chips row (full-width span)
 *  - upcoming cards (or [PartialEmptyState] when upcoming is empty but past is non-empty)
 *  - past section header + cards when past is non-empty
 *
 * Each event card is wrapped in [SwipeToDismissEventCard] per AC-LS-8.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
private fun EventsGrid(
  upcoming: List<Event>,
  past: List<Event>,
  pastCollapsed: Boolean,
  onCardClick: (eventId: String) -> Unit,
  onAddClick: () -> Unit,
  onDelete: (id: EventId) -> Unit,
  onTogglePast: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(count = 2),
    modifier = modifier,
    contentPadding = PaddingValues(
      start = 16.dp,
      end = 16.dp,
      top = 8.dp,
      bottom = 96.dp, // reserve space for the FAB
    ),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }, contentType = "filter_chips") {
      FilterChipsRow()
    }

    if (upcoming.isEmpty()) {
      // PartialEmpty: upcoming=0 but past>0 — show inline placeholder (AC-LS-15).
      item(span = { GridItemSpan(maxLineSpan) }, contentType = "partial_empty") {
        PartialEmptyState(onAddClick = onAddClick)
      }
    } else {
      items(items = upcoming, key = { it.id.value }, contentType = { "event_card" }) { event ->
        SwipeToDismissEventCard(
          event = event,
          onCardClick = { onCardClick(event.id.value) },
          onDelete = { onDelete(event.id) },
        )
      }
    }

    if (past.isNotEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }, contentType = "past_header") {
        PastSectionHeader(count = past.size, collapsed = pastCollapsed, onToggle = onTogglePast)
      }

      if (!pastCollapsed) {
        items(
          items = past,
          key = { "past_${it.id.value}" },
          contentType = { "event_card" },
        ) { event ->
          SwipeToDismissEventCard(
            event = event,
            onCardClick = { onCardClick(event.id.value) },
            onDelete = { onDelete(event.id) },
          )
        }
      }
    }
  }
}

// ── Swipe-to-dismiss card wrapper ────────────────────────────────────────────────────────────────

/**
 * Wraps an [EventCard] in a [SwipeToDismissBox] (end-to-start only, ≥50% threshold).
 *
 * AC-LS-8: when the swipe is released at ≥50% of the total swipe distance the delete is
 * committed; below 50% the card snaps back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissEventCard(
  event: Event,
  onCardClick: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
      if (value == SwipeToDismissBoxValue.EndToStart) {
        onDelete()
        true
      } else {
        false
      }
    },
    positionalThreshold = { totalDistance -> totalDistance * 0.5f },
  )

  SwipeToDismissBox(
    state = dismissState,
    modifier = modifier,
    enableDismissFromStartToEnd = false,
    enableDismissFromEndToStart = true,
    backgroundContent = { SwipeDeleteBackground() },
  ) {
    EventCard(event = event, onClick = onCardClick)
  }
}

/** Red background revealed during swipe-to-delete (AC-LS-8). */
@Composable
private fun SwipeDeleteBackground(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
      ),
    contentAlignment = Alignment.CenterEnd,
  ) {
    Row(
      modifier = Modifier.padding(end = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.list_swipe_delete_label),
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.labelLarge,
      )
      Icon(
        imageVector = Icons.Filled.Delete,
        contentDescription = stringResource(R.string.list_swipe_delete_icon_description),
        tint = MaterialTheme.colorScheme.onErrorContainer,
      )
    }
  }
}

// ── Event card (placeholder) ─────────────────────────────────────────────────────────────────────

/**
 * Placeholder event card for the list screen.
 *
 * Renders the event's color palette and icon blob (72dp, [BlobShape.Variant4]) with the event
 * title. Full card content — date chip, countdown number, days-remaining label — is implemented
 * in issue #37 (upcoming) and #38 (past). The card is a single merged semantics node for
 * accessibility (AC-LS-7, AC-LS-20).
 *
 * Square `aspectRatio(1f)` approximates the design's ~204dp card height in a 2-column grid.
 */
@Composable
private fun EventCard(
  event: Event,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val palette = remember(event.color.ordinal, isDark) {
    eventPaletteByIndex(index = event.color.ordinal, dark = isDark)
  }
  val designIcon = remember(event.icon.ordinal) {
    DesignEventIcon.entries[event.icon.ordinal]
  }
  val iconDesc = stringResource(R.string.list_card_icon_description, designIcon.symbolName)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(ratio = 1f)
      .clip(BlobShape.Variant4)
      .background(palette.container)
      .clickable(role = Role.Button, onClickLabel = event.title, onClick = onClick)
      .semantics(mergeDescendants = true) {
        contentDescription = event.title
      },
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(16.dp),
    ) {
      // Icon blob (72dp, Variant4) — placeholder; full rendering in issues #37/#38.
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(BlobShape.Variant4)
          .background(palette.hero),
        contentAlignment = Alignment.Center,
      ) {
        EventSymbol(
          icon = designIcon,
          size = 36.sp,
          tint = palette.onHero,
          contentDescription = iconDesc,
        )
      }

      Text(
        text = event.title,
        style = MaterialTheme.typography.bodyMedium,
        color = palette.onContainer,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

// ── Past section header ──────────────────────────────────────────────────────────────────────────

/**
 * Full-width header for the "Past" section (AC-LS-11).
 *
 * Displays "Прошедшие · N" (or localized equivalent) with a collapse/expand button. The
 * collapsed state is persisted via [EventListComponent.onTogglePast] → Store →
 * [SettingsRepository] (AC-LS-12).
 */
@Composable
private fun PastSectionHeader(
  count: Int,
  collapsed: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val toggleDescription = stringResource(R.string.list_past_toggle_description)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Filled.MoreVert, // placeholder — replace with history icon
      contentDescription = stringResource(R.string.list_past_section_history_description),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = stringResource(R.string.list_past_section_title, count),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(weight = 1f),
    )
    IconButton(
      onClick = onToggle,
      modifier = Modifier.semantics {
        contentDescription = toggleDescription
        role = Role.Button
      },
    ) {
      val label = if (collapsed) {
        stringResource(R.string.list_past_expand)
      } else {
        stringResource(R.string.list_past_collapse)
      }
      Text(
        text = if (collapsed) "↓ $label" else "↑ $label",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

// ── Empty / loading / error states ───────────────────────────────────────────────────────────────

/**
 * Global empty state (AC-LS-14): no events at all — upcoming=0 AND past=0.
 *
 * Displays a teal blob, descriptive copy, and a primary "New event" button.
 *
 * Note: the spec calls for a `hourglass_empty` glyph inside the blob. That codepoint is not yet
 * included in the bundled Material Symbols Rounded subset. The blob is rendered as a plain solid
 * shape until the font subset is extended (tracked as a follow-up in issues #37/#38).
 * The blob uses [BlobShape.Variant1] (132dp, the large empty-state preset).
 */
@Composable
private fun GlobalEmptyState(
  onAddClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(horizontal = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    val tealPalette = eventPaletteByIndex(
      index = EventPaletteId.TEAL.ordinal,
      dark = isSystemInDarkTheme(),
    )
    // Teal blob — hourglass_empty glyph is absent from the current EventIcon subset.
    // Substitute EventSymbol(HOURGLASS_EMPTY, ...) once the font subset is extended (#37/#38).
    Box(
      modifier = Modifier
        .size(132.dp)
        .clip(BlobShape.Variant1)
        .background(tealPalette.container)
        .semantics { contentDescription = "" }, // decorative; heading below is the a11y label
    )

    Spacer(modifier = Modifier.height(32.dp))

    Text(
      text = stringResource(R.string.list_empty_heading),
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = stringResource(R.string.list_empty_body),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.list_fab_label))
    }
  }
}

/**
 * Partial empty state (AC-LS-15): upcoming=0, past>0.
 *
 * Rendered as a full-width span inside the grid, above the past section. Does NOT replace
 * the whole screen — past events remain visible below.
 */
@Composable
private fun PartialEmptyState(
  onAddClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = stringResource(R.string.list_partial_empty_heading),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = stringResource(R.string.list_partial_empty_body),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = onAddClick) {
      Text(stringResource(R.string.list_fab_label))
    }
  }
}

/** Loading state (AC-LS-13): empty screen with no error indicator. */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
  Box(modifier = modifier)
}

/** Error state (AC-LS-16): displays a human-readable error message. */
@Composable
private fun ErrorContent(
  message: String,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.error,
    )
  }
}

// ── FAB ──────────────────────────────────────────────────────────────────────────────────────────

/** Extended FAB for creating a new event (AC-LS-6). */
@Composable
private fun ListFab(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ExtendedFloatingActionButton(
    text = { Text(stringResource(R.string.list_fab_label)) },
    icon = {
      Icon(
        imageVector = Icons.Filled.Add,
        contentDescription = stringResource(R.string.list_fab_icon_description),
      )
    },
    onClick = onClick,
    modifier = modifier,
  )
}

// ── Previews ─────────────────────────────────────────────────────────────────────────────────────

private val previewNow: Instant = Clock.System.now()

@Suppress("MagicNumber")
private fun previewEvent(
  id: String,
  title: String,
  daysFromNow: Int,
  color: EventColor = EventColor.BLUE,
  icon: DomainEventIcon = DomainEventIcon.CELEBRATION,
): Event = Event(
  id = EventId(id),
  title = title,
  targetDateTime = previewNow.plus(daysFromNow.days),
  color = color,
  icon = icon,
  createdAt = previewNow,
)

@Suppress("MagicNumber")
private val previewUpcoming = listOf(
  previewEvent("1", "Trip to Japan", 14, EventColor.TEAL, DomainEventIcon.FLIGHT),
  previewEvent("2", "Wedding anniversary", 42, EventColor.PINK, DomainEventIcon.FAVORITE),
  previewEvent("3", "Product launch", 7, EventColor.INDIGO, DomainEventIcon.ROCKET_LAUNCH),
  previewEvent("4", "Christmas", 210, EventColor.RED, DomainEventIcon.SNOWING),
)

@Suppress("MagicNumber")
private val previewPast = listOf(
  previewEvent("5", "Graduation party", -30, EventColor.ORANGE, DomainEventIcon.SCHOOL),
  previewEvent("6", "Concert night", -60, EventColor.PURPLE, DomainEventIcon.MUSIC_NOTE),
)

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun EventListLoadingPreview() {
  DateCountdownTheme {
    EventListScreenContent(
      state = EventListState.Loading,
      onCardClick = {},
      onAddClick = {},
      onDelete = {},
      onUndoDelete = {},
      onTogglePast = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun EventListGlobalEmptyPreview() {
  DateCountdownTheme {
    EventListScreenContent(
      state = EventListState.Content(
        upcoming = emptyList(),
        past = emptyList(),
        pastCollapsed = false,
        pendingDelete = null,
      ),
      onCardClick = {},
      onAddClick = {},
      onDelete = {},
      onUndoDelete = {},
      onTogglePast = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun EventListUpcomingOnlyPreview() {
  DateCountdownTheme {
    EventListScreenContent(
      state = EventListState.Content(
        upcoming = previewUpcoming,
        past = emptyList(),
        pastCollapsed = false,
        pendingDelete = null,
      ),
      onCardClick = {},
      onAddClick = {},
      onDelete = {},
      onUndoDelete = {},
      onTogglePast = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun EventListWithCollapsedPastPreview() {
  DateCountdownTheme {
    EventListScreenContent(
      state = EventListState.Content(
        upcoming = previewUpcoming,
        past = previewPast,
        pastCollapsed = true,
        pendingDelete = null,
      ),
      onCardClick = {},
      onAddClick = {},
      onDelete = {},
      onUndoDelete = {},
      onTogglePast = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun EventListWithExpandedPastPreview() {
  DateCountdownTheme {
    EventListScreenContent(
      state = EventListState.Content(
        upcoming = previewUpcoming,
        past = previewPast,
        pastCollapsed = false,
        pendingDelete = null,
      ),
      onCardClick = {},
      onAddClick = {},
      onDelete = {},
      onUndoDelete = {},
      onTogglePast = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun EventListPartialEmptyPreview() {
  DateCountdownTheme {
    EventListScreenContent(
      state = EventListState.Content(
        upcoming = emptyList(),
        past = previewPast,
        pastCollapsed = false,
        pendingDelete = null,
      ),
      onCardClick = {},
      onAddClick = {},
      onDelete = {},
      onUndoDelete = {},
      onTogglePast = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun EventListErrorPreview() {
  DateCountdownTheme {
    EventListScreenContent(
      state = EventListState.Error(cause = RuntimeException("Database read failed")),
      onCardClick = {},
      onAddClick = {},
      onDelete = {},
      onUndoDelete = {},
      onTogglePast = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "EventCard — Teal/Flight", showBackground = true)
@Composable
private fun EventCardPreview() {
  DateCountdownTheme {
    Surface {
      EventCard(
        event = previewEvent("p1", "Trip to Japan", 14, EventColor.TEAL, DomainEventIcon.FLIGHT),
        onClick = {},
        modifier = Modifier.size(180.dp),
      )
    }
  }
}
