package com.datecountdown.app.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil

/**
 * Computes the [PastBreakdown] for an event that has already occurred.
 *
 * Call this after [CountdownCalculator.calculate] returns [CountdownResult.Past]:
 * ```
 * val result = CountdownCalculator().calculate(target = event.targetDateTime, now = now)
 * if (result is CountdownResult.Past) {
 *   val breakdown = PastEventProcessor().process(target = event.targetDateTime, now = now)
 *   // render breakdown
 * }
 * ```
 *
 * Unlike [CountdownCalculator], which provides a full multi-unit breakdown for upcoming events,
 * past events use a single unit: whole calendar days since the target (AC-PE-3). When the target
 * passed today, [PastBreakdown.Today] is returned instead of `DaysAgo(0)` (AC-PE-11).
 *
 * [timeZone] defaults to [TimeZone.currentSystemDefault] and is injectable for testability.
 */
class PastEventProcessor(
  private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {

  /**
   * Computes [PastBreakdown] from [target] and the current moment [now].
   *
   * Pre-condition: caller is expected to pass `target <= now` (the past case).
   * If `target > now` is passed defensively — the function clamps `daysAgo` to 0
   * and returns [PastBreakdown.Today] rather than a negative count. This simplifies
   * boundary handling when the event flips between past and present within the same tick.
   *
   * Algorithm:
   * 1. `daysAgo = target.daysUntil(now, timeZone)` — positive because `now >= target`.
   * 2. Clamp to 0 as a defensive guard for `target > now` edge at tick boundaries.
   * 3. `daysAgo == 0` → [PastBreakdown.Today] (AC-PE-11).
   * 4. `daysAgo >= 1` → [PastBreakdown.DaysAgo] (AC-PE-3).
   *
   * @param target the event's scheduled date-time.
   * @param now    the current moment, typically `Clock.System.now()`.
   */
  fun process(target: Instant, now: Instant): PastBreakdown {
    val daysAgo = target.daysUntil(now, timeZone).coerceAtLeast(0)
    return if (daysAgo == 0) PastBreakdown.Today else PastBreakdown.DaysAgo(days = daysAgo)
  }
}
