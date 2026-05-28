package com.datecountdown.app.domain

/**
 * Typed result of [PastEventProcessor.process].
 *
 * Two cases:
 * - [Today] — the event passed today: `daysAgo == 0` (AC-PE-11). The UI shows «Сегодня»,
 *   not «0 дней назад», and suppresses the large `−N` number.
 * - [DaysAgo] — the event passed at least one full calendar day ago.
 *
 * Use [PastEventProcessor] to compute this from an event's `targetDateTime` and the current
 * [kotlinx.datetime.Instant].
 */
sealed interface PastBreakdown {

  /**
   * The event passed today (`daysAgo == 0`). Per AC-PE-11 the UI must display «Сегодня»
   * instead of the numeric counter.
   */
  data object Today : PastBreakdown

  /**
   * The event passed [days] full calendar days ago (≥ 1).
   *
   * @param days whole calendar days elapsed since the event's target date in the device
   *   time zone; always ≥ 1.
   */
  data class DaysAgo(val days: Int) : PastBreakdown
}
