package com.datecountdown.app.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests for [CountdownCalculator].
 *
 * All timestamps use [TimeZone.UTC] to eliminate host-timezone drift.
 * The calculator instance is constructed with [TimeZone.UTC] for the same reason.
 *
 * Coverage: AC-CL-1 (arithmetic), AC-CL-2 (past detection),
 * AC-CL-3 (primary unit selection), AC-CL-7 (today/sub-day edge),
 * leap-year boundaries, screenshot reference scenarios.
 */
class CountdownCalculatorTest {

  private val calculator = CountdownCalculator(timeZone = TimeZone.UTC)

  // Test fixture mirroring LocalDateTime constructor — six params are inherent to the domain.
  @Suppress("LongParameterList")
  private fun ts(
    year: Int,
    month: Int,
    day: Int,
    hour: Int = 0,
    minute: Int = 0,
    second: Int = 0,
  ): Instant =
    LocalDateTime(
      year = year,
      monthNumber = month,
      dayOfMonth = day,
      hour = hour,
      minute = minute,
      second = second,
    ).toInstant(TimeZone.UTC)

  // ── AC-CL-2: past detection ──────────────────────────────────────────────

  @Test
  fun `target equal to now is Past`() {
    val now = ts(2026, 5, 28)
    assertSame(CountdownResult.Past, calculator.calculate(target = now, now = now))
  }

  @Test
  fun `target one second before now is Past`() {
    val now = ts(2026, 5, 28, hour = 12)
    val target = ts(2026, 5, 28, hour = 11, minute = 59, second = 59)
    assertSame(CountdownResult.Past, calculator.calculate(target = target, now = now))
  }

  @Test
  fun `target one day before now is Past`() {
    val now = ts(2026, 5, 28)
    val target = ts(2026, 5, 27)
    assertSame(CountdownResult.Past, calculator.calculate(target = target, now = now))
  }

  @Test
  fun `target one year before now is Past`() {
    val now = ts(2026, 5, 28)
    val target = ts(2025, 5, 28)
    assertSame(CountdownResult.Past, calculator.calculate(target = target, now = now))
  }

  // ── AC-CL-1: arithmetic correctness — exact single-unit cases ────────────

  @Test
  fun `exact one second remaining - primary SECONDS, seconds=1, all others 0`() {
    val now = ts(2026, 5, 28, hour = 12, minute = 0, second = 0)
    val target = ts(2026, 5, 28, hour = 12, minute = 0, second = 1)
    assertEquals(
      CountdownResult.Upcoming(
        years = 0, days = 0, hours = 0, minutes = 0, seconds = 1, primary = CountdownUnit.SECONDS,
      ),
      calculator.calculate(target = target, now = now),
    )
  }

  @Test
  fun `exact one minute remaining - primary MINUTES, minutes=1, seconds=0`() {
    val now = ts(2026, 5, 28, hour = 12, minute = 0)
    val target = ts(2026, 5, 28, hour = 12, minute = 1)
    assertEquals(
      CountdownResult.Upcoming(
        years = 0, days = 0, hours = 0, minutes = 1, seconds = 0, primary = CountdownUnit.MINUTES,
      ),
      calculator.calculate(target = target, now = now),
    )
  }

  @Test
  fun `exact one hour remaining - primary HOURS, hours=1, all others 0`() {
    val now = ts(2026, 5, 28, hour = 11)
    val target = ts(2026, 5, 28, hour = 12)
    assertEquals(
      CountdownResult.Upcoming(years = 0, days = 0, hours = 1, minutes = 0, seconds = 0, primary = CountdownUnit.HOURS),
      calculator.calculate(target = target, now = now),
    )
  }

  @Test
  fun `exact one day remaining - primary DAYS, days=1, all others 0`() {
    val now = ts(2026, 5, 27)
    val target = ts(2026, 5, 28)
    assertEquals(
      CountdownResult.Upcoming(years = 0, days = 1, hours = 0, minutes = 0, seconds = 0, primary = CountdownUnit.DAYS),
      calculator.calculate(target = target, now = now),
    )
  }

  @Test
  fun `exact one year remaining - primary YEARS, years=1, days=0`() {
    val now = ts(2025, 5, 28)
    val target = ts(2026, 5, 28)
    assertEquals(
      CountdownResult.Upcoming(years = 1, days = 0, hours = 0, minutes = 0, seconds = 0, primary = CountdownUnit.YEARS),
      calculator.calculate(target = target, now = now),
    )
  }

  // ── AC-CL-1: arithmetic correctness — compound cases ────────────────────

  @Test
  fun `1 day 5 hours 30 minutes 45 seconds remaining - primary DAYS`() {
    val now = ts(2026, 5, 27, hour = 6, minute = 29, second = 15)
    val target = ts(2026, 5, 28, hour = 12, minute = 0, second = 0)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(1, result.days)
    assertEquals(5, result.hours)
    assertEquals(30, result.minutes)
    assertEquals(45, result.seconds)
    assertEquals(CountdownUnit.DAYS, result.primary)
  }

  @Test
  fun `screenshot reference - counter-today - 4 hours remaining - primary HOURS`() {
    val now = ts(2026, 5, 28, hour = 8)
    val target = ts(2026, 5, 28, hour = 12)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.days)
    assertEquals(4, result.hours)
    assertEquals(0, result.minutes)
    assertEquals(0, result.seconds)
    assertEquals(CountdownUnit.HOURS, result.primary)
  }

  @Test
  fun `screenshot reference - counter-near - 3 days remaining - primary DAYS`() {
    val now = ts(2026, 5, 28)
    val target = ts(2026, 5, 31)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.years)
    assertEquals(3, result.days)
    assertEquals(CountdownUnit.DAYS, result.primary)
  }

  @Test
  fun `screenshot reference - counter-far - 248 days remaining - primary DAYS`() {
    val now = ts(2026, 1, 1)
    val target = ts(2026, 9, 6)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.years)
    assertEquals(248, result.days)
    assertEquals(CountdownUnit.DAYS, result.primary)
  }

  @Test
  fun `screenshot reference - counter-long - 9 years 253 days remaining - primary YEARS`() {
    val now = ts(2026, 1, 1)
    val target = ts(2035, 9, 11)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(9, result.years)
    assertEquals(253, result.days)
    assertEquals(CountdownUnit.YEARS, result.primary)
  }

  // ── AC-CL-3: primary unit selection top-down ─────────────────────────────

  @Test
  fun `years=0 days=1 hours=5 - primary DAYS`() {
    val now = ts(2026, 5, 27, hour = 7)
    val target = ts(2026, 5, 28, hour = 12)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.years)
    assertEquals(CountdownUnit.DAYS, result.primary)
  }

  @Test
  fun `years=0 days=0 hours=2 minutes=30 - primary HOURS`() {
    val now = ts(2026, 5, 28, hour = 9, minute = 30)
    val target = ts(2026, 5, 28, hour = 12)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.days)
    assertEquals(2, result.hours)
    assertEquals(30, result.minutes)
    assertEquals(CountdownUnit.HOURS, result.primary)
  }

  @Test
  fun `years=0 days=0 hours=0 minutes=5 seconds=30 - primary MINUTES`() {
    val now = ts(2026, 5, 28, hour = 11, minute = 54, second = 30)
    val target = ts(2026, 5, 28, hour = 12)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.days)
    assertEquals(0, result.hours)
    assertEquals(5, result.minutes)
    assertEquals(30, result.seconds)
    assertEquals(CountdownUnit.MINUTES, result.primary)
  }

  @Test
  fun `years=0 days=0 hours=0 minutes=0 seconds=10 - primary SECONDS`() {
    val now = ts(2026, 5, 28, hour = 11, minute = 59, second = 50)
    val target = ts(2026, 5, 28, hour = 12)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.days)
    assertEquals(0, result.hours)
    assertEquals(0, result.minutes)
    assertEquals(10, result.seconds)
    assertEquals(CountdownUnit.SECONDS, result.primary)
  }

  @Test
  fun `years=1 days=0 hours=0 - primary YEARS, days=0`() {
    val now = ts(2025, 6, 1)
    val target = ts(2026, 6, 1)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(1, result.years)
    assertEquals(0, result.days)
    assertEquals(CountdownUnit.YEARS, result.primary)
  }

  // ── AC-CL-7: today edge — 0 days but nonzero hours ───────────────────────

  @Test
  fun `0 days 4 hours remaining - primary HOURS (counter-today pattern)`() {
    val now = ts(2026, 5, 28, hour = 8)
    val target = ts(2026, 5, 28, hour = 12)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.days)
    assertEquals(4, result.hours)
    assertEquals(CountdownUnit.HOURS, result.primary)
  }

  @Test
  fun `0 days 0 hours 30 minutes remaining - primary MINUTES`() {
    val now = ts(2026, 5, 28, hour = 11, minute = 30)
    val target = ts(2026, 5, 28, hour = 12)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.days)
    assertEquals(0, result.hours)
    assertEquals(30, result.minutes)
    assertEquals(CountdownUnit.MINUTES, result.primary)
  }

  // ── Boundary: year / day transitions ─────────────────────────────────────

  @Test
  fun `year boundary - one second before new year - primary SECONDS`() {
    val now = ts(2024, 12, 31, hour = 23, minute = 59, second = 59)
    val target = ts(2025, 1, 1)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.years)
    assertEquals(0, result.days)
    assertEquals(0, result.hours)
    assertEquals(0, result.minutes)
    assertEquals(1, result.seconds)
    assertEquals(CountdownUnit.SECONDS, result.primary)
  }

  // ── Boundary: sub-second precision lock-in ───────────────────────────────

  @Test
  fun `target one millisecond after now - Upcoming with seconds=0 and primary=SECONDS`() {
    val now = ts(2026, 5, 28, hour = 12)
    val target = now + kotlin.time.Duration.parse("1ms")
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.years)
    assertEquals(0, result.days)
    assertEquals(0, result.hours)
    assertEquals(0, result.minutes)
    assertEquals(0, result.seconds)
    assertEquals(CountdownUnit.SECONDS, result.primary)
  }

  // ── Leap year arithmetic correctness ─────────────────────────────────────

  @Test
  fun `target leap day Feb 29 2024 from Feb 28 2024 - 1 day`() {
    val now = ts(2024, 2, 28)
    val target = ts(2024, 2, 29)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.years)
    assertEquals(1, result.days)
    assertEquals(CountdownUnit.DAYS, result.primary)
  }

  @Test
  fun `target Mar 1 2024 from Feb 28 2024 - 2 days (Feb 29 exists in 2024)`() {
    val now = ts(2024, 2, 28)
    val target = ts(2024, 3, 1)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.years)
    assertEquals(2, result.days)
    assertEquals(CountdownUnit.DAYS, result.primary)
  }

  @Test
  fun `target Mar 1 2025 from Feb 28 2025 - 1 day (non-leap year)`() {
    val now = ts(2025, 2, 28)
    val target = ts(2025, 3, 1)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(0, result.years)
    assertEquals(1, result.days)
    assertEquals(CountdownUnit.DAYS, result.primary)
  }

  @Test
  fun `target Feb 28 2026 from Feb 28 2025 - 1 year (yearsUntil correct)`() {
    val now = ts(2025, 2, 28)
    val target = ts(2026, 2, 28)
    val result = calculator.calculate(target = target, now = now) as CountdownResult.Upcoming
    assertEquals(1, result.years)
    assertEquals(0, result.days)
    assertEquals(CountdownUnit.YEARS, result.primary)
  }

  // ── Constructor smoke test ────────────────────────────────────────────────

  @Test
  fun `default constructor does not throw and returns reasonable result`() {
    val defaultCalculator = CountdownCalculator()
    val now = ts(2026, 5, 28, hour = 12)
    val target = ts(2027, 5, 28, hour = 12)
    // Just verify it returns Upcoming (not Past) and does not throw.
    assert(defaultCalculator.calculate(target = target, now = now) is CountdownResult.Upcoming)
  }
}
