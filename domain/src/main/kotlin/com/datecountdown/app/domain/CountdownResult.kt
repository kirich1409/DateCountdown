package com.datecountdown.app.domain

/**
 * The primary display unit selected by [CountdownCalculator] per AC-CL-3.
 *
 * Order matches priority: YEARS → DAYS → HOURS → MINUTES → SECONDS.
 * The first non-zero unit wins; if all are zero, SECONDS is returned.
 *
 * When primary = YEARS, the UI shows both [CountdownResult.Upcoming.years] and
 * [CountdownResult.Upcoming.days] (see AC-CL-4). All five unit values are always
 * populated in [CountdownResult.Upcoming] regardless of which unit is primary —
 * consumers decide what to render based on this field.
 *
 * Months are intentionally absent: the breakdown uses years + days (not months),
 * per AC-CL-1.
 */
enum class CountdownUnit { YEARS, DAYS, HOURS, MINUTES, SECONDS }

/**
 * Typed output of [CountdownCalculator.calculate].
 *
 * Two cases:
 * - [Upcoming] — target is in the future; carries a full five-unit breakdown and
 *   the selected [CountdownUnit].
 * - [Past] — target is in the past or exactly now (`target ≤ now`). Granular
 *   breakdown for past events is handled by `PastEventProcessor` (issue #31).
 */
sealed interface CountdownResult {

  /**
   * The event has not yet occurred (`target > now`).
   *
   * Unit semantics:
   * - [years] — whole calendar years remaining (leap-year-aware via kotlinx-datetime).
   * - [days] — whole calendar days remaining after subtracting [years].
   * - [hours], [minutes], [seconds] — wall-clock remainder after subtracting [years] and [days].
   *
   * The breakdown has no months field: AC-CL-1 specifies years/days/hours/minutes/seconds only.
   */
  data class Upcoming(
    val years: Int,
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    /** First non-zero unit top-down; SECONDS when all are zero (AC-CL-3). */
    val primary: CountdownUnit,
  ) : CountdownResult

  /** The event is in the past or at exactly the current moment (`target ≤ now`). */
  data object Past : CountdownResult
}
