@file:Suppress("MagicNumber", "TooManyFunctions")

package com.datecountdown.app.feature.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datecountdown.app.core.design.theme.BlobShape // used by EventIconBlob
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.core.design.theme.EventIcon as DesignEventIcon
import com.datecountdown.app.core.design.theme.EventPaletteId
import com.datecountdown.app.core.design.theme.EventSymbol
import com.datecountdown.app.core.design.theme.eventPaletteByIndex
import com.datecountdown.app.domain.CountdownCalculator
import com.datecountdown.app.domain.CountdownResult
import com.datecountdown.app.domain.Event
import com.datecountdown.app.domain.EventColor
import com.datecountdown.app.domain.EventIcon as DomainEventIcon
import com.datecountdown.app.domain.EventId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Rounded-rectangle shape for upcoming and past event cards (bug #141). */
internal val EventCardShape = RoundedCornerShape(28.dp)

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

/** Formats [target] as "09 MAY" (locale-aware, uppercased). */
private fun formatEventDate(target: Instant): String =
  dateFormatter
    .format(target.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())
    .uppercase(Locale.getDefault())

/**
 * Upcoming event card for the list screen (issue #37).
 *
 * Displays:
 * - Rounded-rectangle surface in the event's palette container color ([EventCardShape]).
 * - Top-right date chip showing the target date in "09 MAY" locale-aware format.
 * - Top-left icon blob (44dp, [BlobShape.Variant4] clip) with hero background.
 * - Large days-remaining number using the hero color.
 * - Plural "N days left" / "N дней осталось" label (or "Today" / "Сегодня" for same-day events).
 * - Event title, 1 line with ellipsis.
 *
 * ## No per-second ticking (AC-CL-12)
 * [now] is passed from the call site. The call site computes `Clock.System.now()` once per
 * recomposition of the grid, not on a per-second tick. This composable performs no time reads.
 *
 * ## Accessibility (AC-LS-20)
 * The card is a single merged semantics node with contentDescription:
 * "{title}, {N} дней до {date}" / "{title}, {N} days until {date}" (via string resource).
 *
 * @param event The upcoming event to render.
 * @param isDark Whether the device is in dark theme — determines palette selection.
 * @param now The reference instant for countdown calculation; must be pre-computed at call site.
 * @param onClick Callback invoked when the user taps the card.
 * @param modifier Modifier applied to the root [Box].
 */
@Composable
internal fun EventCard(
  event: Event,
  now: Instant,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isDark: Boolean = isSystemInDarkTheme(),
) {
  val palette = remember(event.color.ordinal, isDark) {
    eventPaletteByIndex(index = event.color.ordinal, dark = isDark)
  }
  val designIcon = remember(event.icon.ordinal) {
    DesignEventIcon.entries[event.icon.ordinal]
  }
  val totalDays = remember(event.targetDateTime, now) {
    computeTotalDays(target = event.targetDateTime, now = now)
  }
  val dateLabel = remember(event.targetDateTime) { formatEventDate(event.targetDateTime) }
  // Same-day upcoming: show "Today"/"Сегодня" instead of "0 days left" (AC-LS-5 / bug #165).
  val todayLabel = if (totalDays == 0) stringResource(R.string.card_today_label) else null
  val daysLabel = if (todayLabel == null) {
    pluralStringResource(R.plurals.days_left, count = totalDays, totalDays)
  } else {
    null
  }
  val cardDescription = if (todayLabel != null) {
    stringResource(R.string.list_card_today_a11y_description, event.title)
  } else {
    stringResource(R.string.list_card_a11y_description, event.title, totalDays, dateLabel)
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(ratio = 1f)
      .clip(EventCardShape)
      .background(palette.container)
      .clickable(role = Role.Button, onClickLabel = event.title, onClick = onClick)
      .semantics(mergeDescendants = true) {
        contentDescription = cardDescription
        role = Role.Button
      },
  ) {
    EventDateChip(
      label = dateLabel,
      textColor = palette.onContainer,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 12.dp, end = 12.dp),
    )
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      EventIconBlob(icon = designIcon, heroColor = palette.hero, onHeroColor = palette.onHero)
      EventCardCountdown(
        totalDays = totalDays,
        todayLabel = todayLabel,
        daysLabel = daysLabel,
        title = event.title,
        heroColor = palette.hero,
        onContainerColor = palette.onContainer,
      )
    }
  }
}

/**
 * Computes total days (years × 365 + days) from [now] to [target].
 * Returns 0 for past events (boundary race — normally won't happen for upcoming cards).
 */
private fun computeTotalDays(target: Instant, now: Instant): Int {
  val result = CountdownCalculator().calculate(target = target, now = now)
  return when (result) {
    is CountdownResult.Upcoming -> result.years * 365 + result.days
    CountdownResult.Past -> 0
  }
}

/**
 * Bottom section of the card: large days number (or "Today" label), plural label, and event title.
 *
 * When [todayLabel] is non-null (same-day event, totalDays == 0):
 * - The large slot shows [todayLabel] ("Today"/"Сегодня") instead of the number.
 * - The secondary [daysLabel] row is hidden (mirrors [PastCountdown] Today-branch, AC-PE-11).
 *
 * When [todayLabel] is null (totalDays >= 1): renders the number + [daysLabel] as before.
 */
@Suppress("LongParameterList")
@Composable
private fun EventCardCountdown(
  totalDays: Int,
  todayLabel: String?,
  daysLabel: String?,
  title: String,
  heroColor: androidx.compose.ui.graphics.Color,
  onContainerColor: androidx.compose.ui.graphics.Color,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    // Large number slot — replaced by "Today" for same-day events (bug #165).
    Text(
      text = todayLabel ?: totalDays.toString(),
      style = MaterialTheme.typography.displaySmall,
      color = heroColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    // "N days left" label — omitted for same-day (mirrors PastCountdown.Today branch).
    if (daysLabel != null) {
      Text(
        text = daysLabel,
        style = MaterialTheme.typography.labelMedium,
        color = onContainerColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      color = onContainerColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Date chip displayed in the top-right of the event card.
 *
 * Shows the target date in locale-aware "09 MAY" format (already uppercased by caller).
 */
@Composable
private fun EventDateChip(
  label: String,
  textColor: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Text(
    text = label,
    style = MaterialTheme.typography.labelSmall,
    color = textColor,
    modifier = modifier,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}

/**
 * 44dp icon blob in the top-left of the event card.
 *
 * Clipped to [BlobShape.Variant4], hero-colored background, onHero-colored icon.
 * The icon is decorative — the card's merged semantics node carries the full description.
 */
@Composable
private fun EventIconBlob(
  icon: DesignEventIcon,
  heroColor: androidx.compose.ui.graphics.Color,
  onHeroColor: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .size(44.dp)
      .clip(BlobShape.Variant4)
      .background(heroColor),
    contentAlignment = Alignment.Center,
  ) {
    EventSymbol(
      icon = icon,
      size = 22.sp,
      tint = onHeroColor,
      contentDescription = null, // decorative — card-level mergeDescendants provides a11y
    )
  }
}

// ── Previews ─────────────────────────────────────────────────────────────────────────────────────

@Suppress("MagicNumber")
private val previewNowCard: Instant = Clock.System.now()

@Suppress("MagicNumber")
private fun previewCardEvent(
  id: String,
  title: String,
  daysFromNow: Int,
  color: EventColor = EventColor.BLUE,
  icon: DomainEventIcon = DomainEventIcon.CELEBRATION,
): Event = Event(
  id = EventId(id),
  title = title,
  targetDateTime = previewNowCard.plus(daysFromNow.days),
  color = color,
  icon = icon,
  createdAt = previewNowCard,
)

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun EventCardNearPreview() {
  DateCountdownTheme {
    Surface {
      EventCard(
        event = previewCardEvent(
          id = "1",
          title = "Trip to Japan",
          daysFromNow = 3,
          color = EventColor.TEAL,
          icon = DomainEventIcon.FLIGHT,
        ),
        now = previewNowCard,
        isDark = isSystemInDarkTheme(),
        onClick = {},
        modifier = Modifier.size(180.dp),
      )
    }
  }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun EventCardFarPreview() {
  DateCountdownTheme {
    Surface {
      EventCard(
        event = previewCardEvent(
          id = "2",
          title = "Wedding anniversary",
          daysFromNow = 248,
          color = EventColor.PINK,
          icon = DomainEventIcon.FAVORITE,
        ),
        now = previewNowCard,
        isDark = isSystemInDarkTheme(),
        onClick = {},
        modifier = Modifier.size(180.dp),
      )
    }
  }
}

@Suppress("UnusedPrivateMember", "MagicNumber")
@PreviewLightDark
@Composable
private fun EventCardTodayPreview() {
  // 4 hours from now — totalDays=0, renders "Today"/"Сегодня" label (bug #165 fix, AC-LS-5)
  val todayNow = Clock.System.now()
  DateCountdownTheme {
    Surface {
      EventCard(
        event = Event(
          id = EventId("3"),
          title = "Product launch",
          targetDateTime = todayNow.plus(4.hours),
          color = EventColor.INDIGO,
          icon = DomainEventIcon.ROCKET_LAUNCH,
          createdAt = todayNow,
        ),
        now = todayNow,
        isDark = isSystemInDarkTheme(),
        onClick = {},
        modifier = Modifier.size(180.dp),
      )
    }
  }
}

@Suppress("UnusedPrivateMember", "MagicNumber")
@PreviewLightDark
@Composable
private fun EventCardAllPalettesPreview() {
  DateCountdownTheme {
    Surface {
      LazyVerticalGrid(
        columns = GridCells.Fixed(count = 3),
        modifier = Modifier.height(600.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        val colors = EventColor.entries
        val icons = listOf(
          DomainEventIcon.CELEBRATION,
          DomainEventIcon.FAVORITE,
          DomainEventIcon.FLIGHT,
          DomainEventIcon.CAKE,
          DomainEventIcon.ROCKET_LAUNCH,
          DomainEventIcon.SCHOOL,
          DomainEventIcon.MUSIC_NOTE,
          DomainEventIcon.SNOWING,
          DomainEventIcon.BEACH_ACCESS,
        )
        itemsIndexed(colors) { index, color ->
          EventCard(
            event = previewCardEvent(
              id = "palette_$index",
              title = color.name.lowercase().replaceFirstChar { it.uppercase() },
              daysFromNow = 10 + index * 7,
              color = color,
              icon = icons[index % icons.size],
            ),
            now = previewNowCard,
            isDark = isSystemInDarkTheme(),
            onClick = {},
          )
        }
      }
    }
  }
}
