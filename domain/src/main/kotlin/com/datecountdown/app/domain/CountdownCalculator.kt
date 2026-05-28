package com.datecountdown.app.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.yearsUntil

/**
 * Computes the remaining time from [now] to [target] and returns a typed [CountdownResult].
 *
 * The breakdown uses five units — years, days, hours, minutes, seconds — with no months,
 * as required by AC-CL-1. Years are calendar-aware (leap-year-safe) via kotlinx-datetime.
 * Hours, minutes, and seconds are derived from the wall-clock sub-day remainder.
 *
 * [timeZone] defaults to [TimeZone.currentSystemDefault] and is injected for testability.
 *
 * Usage:
 * ```
 * val result = CountdownCalculator().calculate(target = event.targetDateTime, now = Clock.System.now())
 * ```
 */
class CountdownCalculator(
  private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {

  /**
   * Calculates the countdown from [now] to [target].
   *
   * Returns [CountdownResult.Past] when `target ≤ now` (AC-CL-2).
   *
   * Algorithm (stepwise, avoids months entirely):
   * 1. Whole calendar years from [now] to [target] in [timeZone].
   * 2. Anchor = [now] advanced by those years.
   * 3. Whole calendar days from the anchor to [target] in [timeZone].
   * 4. Anchor2 = anchor advanced by those days.
   * 5. Sub-day remainder = `target − anchor2` decomposed into hours/minutes/seconds.
   * 6. Primary unit = first non-zero unit top-down; SECONDS when all zero (AC-CL-3).
   *
   * Note: the sub-day duration uses wall-clock seconds, not calendar-day boundaries.
   * On DST-transition days the "hours" slot may therefore be 0..24 rather than 0..23;
   * the spec (AC-CL-1) does not impose a strict 24h cap here.
   */
  fun calculate(target: Instant, now: Instant): CountdownResult {
    if (target <= now) return CountdownResult.Past

    val years = now.yearsUntil(target, timeZone)
    val afterYears = now.plus(years, DateTimeUnit.YEAR, timeZone)

    val days = afterYears.daysUntil(target, timeZone)
    val afterDays = afterYears.plus(days, DateTimeUnit.DAY, timeZone)

    var hours = 0
    var minutes = 0
    var seconds = 0
    (target - afterDays).toComponents { h, m, s, _ ->
      hours = h.toInt()
      minutes = m
      seconds = s
    }

    val primary = when {
      years > 0 -> CountdownUnit.YEARS
      days > 0 -> CountdownUnit.DAYS
      hours > 0 -> CountdownUnit.HOURS
      minutes > 0 -> CountdownUnit.MINUTES
      else -> CountdownUnit.SECONDS
    }

    return CountdownResult.Upcoming(
      years = years,
      days = days,
      hours = hours,
      minutes = minutes,
      seconds = seconds,
      primary = primary,
    )
  }
}
