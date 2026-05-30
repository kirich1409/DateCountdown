@file:Suppress("MagicNumber", "TooManyFunctions")

package com.datecountdown.app.feature.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datecountdown.app.core.design.theme.BlobShape
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.core.design.theme.EventIcon as DesignEventIcon
import com.datecountdown.app.core.design.theme.EventSymbol
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon as DomainEventIcon
import com.datecountdown.app.domain.EventId
import com.datecountdown.app.domain.PastBreakdown
import com.datecountdown.app.domain.PastEventProcessor
import com.datecountdown.app.core.common.R as CommonR
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

/**
 * Past-event card for the list screen (issue #38).
 *
 * Renders a neutral gray card (not the event's palette) showing:
 * - Top-left: 44dp muted icon blob ([surfaceContainer] background, [onSurfaceVariant] tint).
 * - Top-right: "ПРОШЛО"/"PAST" badge in [surfaceContainer].
 * - Center: large `−N` number (or "Сегодня"/"Today" when [PastBreakdown.Today]).
 * - Below number: "N дней назад" plural label (only for [PastBreakdown.DaysAgo]).
 * - Bottom: event title with strike-through decoration, 1 line with ellipsis.
 *
 * ## Accessibility (AC-PE-17)
 * The card is a single merged semantics node: "{title}, {timeClause}".
 * [timeClause] is either "Сегодня"/"Today" or "N дней назад"/"N days ago".
 *
 * @param event    The past event to render.
 * @param now      The reference instant; computed once at the grid call site (AC-CL-12).
 * @param onClick  Callback invoked when the user taps the card.
 * @param modifier Modifier applied to the root [Surface].
 */
@Composable
internal fun PastEventCard(
  event: Event,
  now: Instant,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val breakdown = remember(event.targetDateTime, now) {
    PastEventProcessor().process(target = event.targetDateTime, now = now)
  }
  val designIcon = remember(event.icon.ordinal) {
    DesignEventIcon.entries[event.icon.ordinal]
  }

  val todayLabel = stringResource(R.string.past_card_today_label)
  val timeClause = when (breakdown) {
    PastBreakdown.Today -> todayLabel
    is PastBreakdown.DaysAgo -> pluralStringResource(
      CommonR.plurals.days_ago,
      count = breakdown.days,
      breakdown.days,
    )
  }
  val cardDescription = stringResource(R.string.past_card_a11y_description, event.title, timeClause)

  Surface(
    shape = EventCardShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(ratio = 1f)
      .clickable(role = Role.Button, onClickLabel = event.title, onClick = onClick)
      .semantics(mergeDescendants = true) {
        contentDescription = cardDescription
        role = Role.Button
      },
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        PastIconBlob(icon = designIcon)
        PastBadge()
      }
      PastCountdown(
        breakdown = breakdown,
        todayLabel = todayLabel,
        timeClause = timeClause,
        title = event.title,
      )
    }
  }
}

/**
 * 44dp muted icon blob in the top-left of the past-event card (AC-PE-4).
 *
 * Uses [surfaceContainer] background and [onSurfaceVariant] tint — intentionally neutral,
 * unlike the upcoming [EventCard] which uses the event's palette hero color.
 */
@Composable
private fun PastIconBlob(
  icon: DesignEventIcon,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .size(44.dp)
      .clip(BlobShape.Variant4)
      .background(MaterialTheme.colorScheme.surfaceContainer),
    contentAlignment = Alignment.Center,
  ) {
    EventSymbol(
      icon = icon,
      size = 22.sp,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      contentDescription = null, // decorative — card-level mergeDescendants provides a11y
    )
  }
}

/**
 * "ПРОШЛО"/"PAST" badge displayed in the top-right of the past-event card (AC-PE-4).
 */
@Composable
private fun PastBadge(modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier,
  ) {
    Text(
      text = stringResource(R.string.past_badge),
      style = MaterialTheme.typography.labelSmall,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

/**
 * Bottom section of the past-event card: days-ago number, plural label, and struck-through title.
 *
 * For [PastBreakdown.Today]: shows [todayLabel] in the number slot, no secondary label.
 * For [PastBreakdown.DaysAgo]: shows `−N` (AC-PE-5), then "N days ago" label.
 */
@Suppress("LongParameterList")
@Composable
private fun PastCountdown(
  breakdown: PastBreakdown,
  todayLabel: String,
  timeClause: String,
  title: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    // Large number (or "Today" label) — displaySmall weight
    val numberText = when (breakdown) {
      PastBreakdown.Today -> todayLabel
      is PastBreakdown.DaysAgo -> "−${breakdown.days}" // minus sign U+2212, not hyphen
    }
    Text(
      text = numberText,
      style = MaterialTheme.typography.displaySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    // "N days ago" label — only for DaysAgo branch (AC-PE-5)
    if (breakdown is PastBreakdown.DaysAgo) {
      Text(
        text = timeClause,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    // Struck-through title (AC-PE-5)
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textDecoration = TextDecoration.LineThrough,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

// ── Previews ─────────────────────────────────────────────────────────────────────────────────────

private val previewNow: Instant = Clock.System.now()

private fun previewPastEvent(
  id: String,
  title: String,
  daysAgo: Int,
): Event = Event(
  id = EventId(id),
  title = title,
  targetDateTime = previewNow.minus(daysAgo.days),
  color = EventColor.BLUE,
  icon = DomainEventIcon.CELEBRATION,
  createdAt = previewNow.minus((daysAgo + 30).days),
)

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun PastEventCardOneDayPreview() {
  DateCountdownTheme {
    Surface {
      PastEventCard(
        event = previewPastEvent(id = "1", title = "Trip to Japan", daysAgo = 1),
        now = previewNow,
        onClick = {},
        modifier = Modifier.width(180.dp),
      )
    }
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun PastEventCardTwentyFiveDaysPreview() {
  DateCountdownTheme {
    Surface {
      PastEventCard(
        event = previewPastEvent(id = "2", title = "Wedding anniversary", daysAgo = 25),
        now = previewNow,
        onClick = {},
        modifier = Modifier.width(180.dp),
      )
    }
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun PastEventCardFourHundredDaysPreview() {
  DateCountdownTheme {
    Surface {
      PastEventCard(
        event = previewPastEvent(id = "3", title = "Product launch event that was a long time ago", daysAgo = 400),
        now = previewNow,
        onClick = {},
        modifier = Modifier.width(180.dp),
      )
    }
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun PastEventCardTodayPreview() {
  DateCountdownTheme {
    Surface {
      // Same day — daysAgo=0 → PastBreakdown.Today (AC-PE-11)
      PastEventCard(
        event = previewPastEvent(id = "4", title = "Morning run", daysAgo = 0),
        now = previewNow,
        onClick = {},
        modifier = Modifier.width(180.dp),
      )
    }
  }
}
