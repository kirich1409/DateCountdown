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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.datecountdown.app.core.design.theme.BlobShape
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.core.design.theme.LocalNotificationPermissionState
import com.datecountdown.app.core.design.theme.EventPaletteId
import com.datecountdown.app.core.design.theme.eventPaletteByIndex
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon as DomainEventIcon
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.ThemeMode
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
 * ## Card design
 * Event cards are flat rounded rectangles with [EventCardShape] (28dp corners), displaying the
 * event icon, date chip, countdown/countup number, days-remaining label, and event title.
 *
 * ## Empty-state derivation (AC-LS-14 / AC-LS-15)
 * Both GlobalEmpty and PartialEmpty are derived from [EventListState.Content] by the UI:
 * - GlobalEmpty: `upcoming.isEmpty() && past.isEmpty()`
 * - PartialEmpty: `upcoming.isEmpty() && past.isNotEmpty()`
 * Neither is a separate sealed subtype in the Store.
 */
@Composable
fun EventListScreen(component: EventListComponent) {
  val state by component.state.subscribeAsState()
  EventListScreenContent(
    state = state,
    onCardClick = component::onCardClick,
    onAddClick = component::onAddClick,
    onDelete = component::onDelete,
    onUndoDelete = component::onUndoDelete,
    onCommitDelete = component::onCommitDelete,
    onTogglePast = component::onTogglePast,
    onThemeModeChange = component::onThemeModeChange,
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
 * - null → A (new delete): launches a coroutine that shows an [SnackbarDuration.Indefinite]
 *   snackbar inside [withTimeoutOrNull]. Timeout is 5 s for sighted users; under an active screen
 *   reader [AccessibilityManager.calculateRecommendedTimeoutMillis] extends it so Undo remains
 *   reachable (AC-LS-21, AC-ACC-6/8).
 * - ActionPerformed (Undo): [onUndoDelete] restores the event; a restore announcement is made
 *   for screen readers.
 * - null from [withTimeoutOrNull] (timeout expired) or Dismissed (swipe): [onCommitDelete]
 *   commits the pending deletion.
 * - A → B (AC-LS-10a replacement): key changes → previous coroutine cancelled; the previous
 *   pending delete (A) was already committed synchronously by the Store via AC-LS-10a when B's
 *   DeleteEvent arrived; on return to the screen the snackbar resumes from current pendingDelete.
 * - A → null (undo committed from Store side): key becomes null → coroutine cancelled, snackbar
 *   dismissed without triggering [onCommitDelete].
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
  onCommitDelete: (id: EventId) -> Unit,
  onTogglePast: () -> Unit,
  onThemeModeChange: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

  ObservePendingDeleteSnackbar(
    state = state,
    snackbarHostState = snackbarHostState,
    onUndoDelete = onUndoDelete,
    onCommitDelete = onCommitDelete,
  )

  val contentState = state as? EventListState.Content
  val upcomingCount = contentState?.upcoming?.size ?: 0
  val isGlobalEmpty = contentState?.upcoming?.isEmpty() == true && contentState.past.isEmpty()
  val subtitle = buildSubtitle(isGlobalEmpty = isGlobalEmpty, upcomingCount = upcomingCount)

  if (showSettingsDialog) {
    SettingsThemeDialog(
      currentMode = state.themeMode,
      onModeSelected = { mode ->
        onThemeModeChange(mode)
        showSettingsDialog = false
      },
      onDismiss = { showSettingsDialog = false },
    )
  }

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      ListTopBar(
        subtitle = subtitle,
        scrollBehavior = scrollBehavior,
        onSettingsClick = { showSettingsDialog = true },
      )
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
    // Banner is hoisted above the state branch so it renders in Loading / Error states too (AC-NT-12).
    val permissionState = LocalNotificationPermissionState.current
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      if (permissionState.shouldShowBanner) {
        NotificationBanner(
          onBannerClick = permissionState.triggerRequest,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
      when (state) {
        EventListState.Loading -> LoadingContent(
          modifier = Modifier.weight(weight = 1f),
        )

        is EventListState.Error -> ErrorContent(
          message = stringResource(R.string.list_error_message),
          modifier = Modifier.weight(weight = 1f),
        )

        is EventListState.Content -> ContentBody(
          state = state,
          isGlobalEmpty = isGlobalEmpty,
          onCardClick = onCardClick,
          onAddClick = onAddClick,
          onDelete = onDelete,
          onTogglePast = onTogglePast,
          modifier = Modifier.weight(weight = 1f),
        )
      }
    }
  }
}

/**
 * Drives the delete snackbar from [EventListState.Content.pendingDelete] (AC-LS-9/10/21).
 *
 * ## Timeout mechanism
 * Base window is 5 s for sighted users. Under an active screen reader (TalkBack / Switch Access)
 * [AccessibilityManager.calculateRecommendedTimeoutMillis] returns an extended value — effectively
 * until the user dismisses — so the Undo action remains reachable (AC-LS-21, AC-ACC-6/8).
 *
 * [SnackbarDuration.Indefinite] is used so the snackbar stays up until either:
 *  - the user taps Undo (ActionPerformed) → [onUndoDelete]
 *  - the user swipes it away (Dismissed) → [onCommitDelete]
 *  - [withTimeoutOrNull] fires (returns null) → [onCommitDelete]
 * On timeout the coroutine is cancelled; [SnackbarHostState.showSnackbar]'s `finally` block
 * resets `currentSnackbarData` to null, dismissing the snackbar automatically.
 */
@Composable
private fun ObservePendingDeleteSnackbar(
  state: EventListState,
  snackbarHostState: SnackbarHostState,
  onUndoDelete: () -> Unit,
  onCommitDelete: (id: EventId) -> Unit,
) {
  val pendingDelete = (state as? EventListState.Content)?.pendingDelete
  val deletedLabel = stringResource(R.string.list_snackbar_deleted)
  val undoLabel = stringResource(R.string.list_snackbar_undo)
  val restoredLabel = stringResource(R.string.list_snackbar_restored)
  val a11yManager = LocalAccessibilityManager.current
  val view = LocalView.current

  // LaunchedEffect key = event id:
  //  - null→A: new effect, show snackbar with a11y-aware timeout.
  //  - A→B: effect restarts (AC-LS-10a), old coroutine cancelled; the previous pending delete (A)
  //    was already committed synchronously by the Store when B's DeleteEvent arrived.
  //  - A→null: effect restarts with null key → immediate return, snackbar dismissed.
  LaunchedEffect(pendingDelete?.event?.id) {
    val pending = pendingDelete ?: return@LaunchedEffect
    val timeoutMs = a11yManager?.calculateRecommendedTimeoutMillis(
      originalTimeoutMillis = 5000L,
      containsIcons = false,
      containsText = true,
      containsControls = true,
    ) ?: 5000L
    val result = withTimeoutOrNull(timeoutMs) {
      snackbarHostState.showSnackbar(
        message = "${pending.event.title} — $deletedLabel",
        actionLabel = undoLabel,
        duration = SnackbarDuration.Indefinite,
      )
    }
    when (result) {
      SnackbarResult.ActionPerformed -> {
        onUndoDelete()
        // Announce restore to screen-reader users; the snackbar liveRegion only announces
        // deletion — the restore action has no visual feedback the SR would pick up.
        view.announceForAccessibility(restoredLabel)
      }
      else -> onCommitDelete(pending.event.id) // null (timeout) or Dismissed (swipe) → commit
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

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTopBar(
  subtitle: String,
  scrollBehavior: TopAppBarScrollBehavior,
  onSettingsClick: () -> Unit,
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
          imageVector = Icons.Filled.Menu,
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
          imageVector = Icons.Filled.Search,
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
            onClick = {
              menuExpanded = false
              onSettingsClick()
            },
          )
          // AC-LS-19: "Enable notifications" item — visible only when banner is active.
          val permissionState = LocalNotificationPermissionState.current
          if (permissionState.shouldShowBanner) {
            DropdownMenuItem(
              text = { Text(stringResource(R.string.list_menu_enable_notifications)) },
              onClick = {
                menuExpanded = false
                permissionState.triggerRequest()
              },
            )
          }
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

// ── Notification disabled banner ─────────────────────────────────────────────────────────────────

/**
 * Persistent banner shown when POST_NOTIFICATIONS permission is absent (AC-NT-12).
 *
 * Visible only when [LocalNotificationPermissionState.shouldShowBanner] is `true`.
 * Tapping anywhere on the banner triggers the permission request launcher.
 *
 * Accessibility:
 * - Icon is decorative (`contentDescription = null`) — the Text already describes the action.
 * - The whole [Surface] is the touch target (natural height ≥ 48 dp with 12 dp vertical padding).
 */
@Composable
private fun NotificationBanner(
  onBannerClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onBannerClick,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Notifications,
        // Decorative — the adjacent Text is the accessible label for this action.
        contentDescription = null,
      )
      Text(
        text = stringResource(R.string.list_notifications_disabled_banner),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
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

    // Compute now once for the entire grid — no per-second ticking (AC-CL-12).
    val now = Clock.System.now()

    if (upcoming.isEmpty()) {
      // PartialEmpty: upcoming=0 but past>0 — show inline placeholder (AC-LS-15).
      item(span = { GridItemSpan(maxLineSpan) }, contentType = "partial_empty") {
        PartialEmptyState(onAddClick = onAddClick)
      }
    } else {
      itemsIndexed(
        items = upcoming,
        key = { _, event -> event.id.value },
        contentType = { _, _ -> "event_card" },
      ) { index, event ->
        SwipeToDismissEventCard(
          event = event,
          now = now,
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
        itemsIndexed(
          items = past,
          key = { _, event -> "past_${event.id.value}" },
          contentType = { _, _ -> "event_card" },
        ) { index, event ->
          SwipeToDismissPastCard(
            event = event,
            now = now,
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
 * Wraps an upcoming [EventCard] in a [SwipeToDismissBox] (end-to-start only, ≥50% threshold).
 *
 * AC-LS-8: when the swipe is released at ≥50% of the total swipe distance the delete is
 * committed; below 50% the card snaps back.
 *
 * @param now Reference instant passed through to [EventCard] for countdown calculation (AC-CL-12).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissEventCard(
  event: Event,
  now: Instant,
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
    EventCard(event = event, now = now, onClick = onCardClick)
  }
}

/**
 * Wraps a [PastEventCard] in a [SwipeToDismissBox] (end-to-start only, ≥50% threshold).
 *
 * @param now Reference instant passed through to [PastEventCard] (AC-CL-12).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissPastCard(
  event: Event,
  now: Instant,
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
    PastEventCard(event = event, now = now, onClick = onCardClick)
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
 * Displays a teal blob with a hourglass icon, descriptive copy, and a primary "New event" button.
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
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(132.dp)
        .clip(BlobShape.Variant1)
        .background(tealPalette.container)
        .semantics { contentDescription = "" }, // decorative; heading below is the a11y label
    ) {
      Icon(
        imageVector = HourglassIcon,
        contentDescription = null, // decorative — heading below is the a11y label
        tint = tealPalette.onContainer,
        modifier = Modifier
          .size(56.dp)
          .testTag("empty_state_hourglass"),
      )
    }

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

// ── Settings theme dialog ────────────────────────────────────────────────────────────────────────

/**
 * Material 3 AlertDialog for choosing the app theme (AC-TH-10).
 *
 * Displays three radio-button rows (System / Light / Dark). Tapping a row selects it,
 * persists the choice via [onModeSelected], and dismisses the dialog. A "Close" button
 * is provided as an accessible dismiss affordance for TalkBack users who cannot easily
 * swipe-dismiss.
 *
 * Accessibility: each row uses [Modifier.selectable] with [Role.RadioButton] so that
 * TalkBack announces the row as a selectable radio-button and handles the click. The inner
 * [RadioButton] has `onClick = null` so the row is the single interactive element.
 */
@Composable
private fun SettingsThemeDialog(
  currentMode: ThemeMode,
  onModeSelected: (ThemeMode) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.settings_theme_title)) },
    text = {
      Column {
        ThemeModeRow(
          label = stringResource(R.string.settings_theme_system),
          selected = currentMode == ThemeMode.SYSTEM,
          onClick = { onModeSelected(ThemeMode.SYSTEM) },
        )
        ThemeModeRow(
          label = stringResource(R.string.settings_theme_light),
          selected = currentMode == ThemeMode.LIGHT,
          onClick = { onModeSelected(ThemeMode.LIGHT) },
        )
        ThemeModeRow(
          label = stringResource(R.string.settings_theme_dark),
          selected = currentMode == ThemeMode.DARK,
          onClick = { onModeSelected(ThemeMode.DARK) },
        )
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.settings_theme_dialog_close))
      }
    },
    modifier = modifier,
  )
}

@Composable
private fun ThemeModeRow(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .selectable(
        selected = selected,
        onClick = onClick,
        role = Role.RadioButton,
      )
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    RadioButton(
      selected = selected,
      // null — the enclosing Row owns the click via selectable(); having two click targets
      // on the same logical item would produce double TalkBack announcements.
      onClick = null,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
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
      onCommitDelete = {},
      onTogglePast = {},
      onThemeModeChange = {},
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
      onCommitDelete = {},
      onTogglePast = {},
      onThemeModeChange = {},
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
      onCommitDelete = {},
      onTogglePast = {},
      onThemeModeChange = {},
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
      onCommitDelete = {},
      onTogglePast = {},
      onThemeModeChange = {},
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
      onCommitDelete = {},
      onTogglePast = {},
      onThemeModeChange = {},
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
      onCommitDelete = {},
      onTogglePast = {},
      onThemeModeChange = {},
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
      onCommitDelete = {},
      onTogglePast = {},
      onThemeModeChange = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun SettingsThemeDialogSystemPreview() {
  DateCountdownTheme {
    SettingsThemeDialog(
      currentMode = ThemeMode.SYSTEM,
      onModeSelected = {},
      onDismiss = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun SettingsThemeDialogLightPreview() {
  DateCountdownTheme {
    SettingsThemeDialog(
      currentMode = ThemeMode.LIGHT,
      onModeSelected = {},
      onDismiss = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun SettingsThemeDialogDarkPreview() {
  DateCountdownTheme {
    SettingsThemeDialog(
      currentMode = ThemeMode.DARK,
      onModeSelected = {},
      onDismiss = {},
    )
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun NotificationBannerPreview() {
  DateCountdownTheme {
    NotificationBanner(onBannerClick = {})
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun EventListWithNotificationBannerPreview() {
  DateCountdownTheme {
    CompositionLocalProvider(
      LocalNotificationPermissionState provides com.datecountdown.app.core.design.theme.NotificationPermissionState(
        shouldShowBanner = true,
        triggerRequest = {},
      ),
    ) {
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
        onCommitDelete = {},
        onTogglePast = {},
        onThemeModeChange = {},
      )
    }
  }
}

