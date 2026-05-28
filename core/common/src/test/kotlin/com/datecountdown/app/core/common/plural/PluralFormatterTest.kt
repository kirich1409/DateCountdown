package com.datecountdown.app.core.common.plural

import android.app.Application
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.datecountdown.app.domain.CountdownUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Robolectric unit tests for [PluralFormatter].
 *
 * Coverage:
 *  - Russian plural rules (one / few / many / other) for all 5 time units,
 *    including boundary cases: 11, 12–14, 21, 22–24, 25, 111, 121.
 *  - English plural rules (one / other) for all 5 time units.
 *  - [PluralFormatter.daysAgo] for RU and EN.
 *  - [PluralFormatter.events] for RU and EN.
 *
 * String expectations are taken verbatim from the module's plural XML resources
 * (`res/values/plurals.xml` and `res/values-ru/plurals.xml`).
 */
// Robolectric 4.14.x maxSdkVersion=35; project targetSdk=36 — pin explicitly to avoid
// "targetSdkVersion > maxSdkVersion" error until Robolectric adds SDK 36 support.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class PluralFormatterTest {

  private fun formatter(locale: Locale): PluralFormatter {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
    val localizedResources = context.createConfigurationContext(config).resources
    return PluralFormatter(localizedResources)
  }

  // ---------------------------------------------------------------------------
  // Russian — CountdownUnit.YEARS (год / года / лет)
  // ---------------------------------------------------------------------------

  @Test
  fun `RU YEARS plural forms`() {
    val f = formatter(Locale.forLanguageTag("ru"))
    listOf(
      1 to "1 год",
      2 to "2 года",
      3 to "3 года",
      4 to "4 года",
      5 to "5 лет",
      10 to "10 лет",
      11 to "11 лет",
      12 to "12 лет",
      13 to "13 лет",
      14 to "14 лет",
      20 to "20 лет",
      21 to "21 год",
      22 to "22 года",
      23 to "23 года",
      24 to "24 года",
      25 to "25 лет",
      111 to "111 лет",
      121 to "121 год",
    ).forEach { (count, expected) ->
      assertEquals("YEARS count=$count", expected, f.format(CountdownUnit.YEARS, count))
    }
  }

  // ---------------------------------------------------------------------------
  // Russian — CountdownUnit.DAYS (день / дня / дней)
  // ---------------------------------------------------------------------------

  @Test
  fun `RU DAYS plural forms`() {
    val f = formatter(Locale.forLanguageTag("ru"))
    listOf(
      1 to "1 день",
      2 to "2 дня",
      3 to "3 дня",
      4 to "4 дня",
      5 to "5 дней",
      10 to "10 дней",
      11 to "11 дней",
      12 to "12 дней",
      13 to "13 дней",
      14 to "14 дней",
      20 to "20 дней",
      21 to "21 день",
      22 to "22 дня",
      23 to "23 дня",
      24 to "24 дня",
      25 to "25 дней",
      111 to "111 дней",
      121 to "121 день",
    ).forEach { (count, expected) ->
      assertEquals("DAYS count=$count", expected, f.format(CountdownUnit.DAYS, count))
    }
  }

  // ---------------------------------------------------------------------------
  // Russian — CountdownUnit.HOURS (час / часа / часов)
  // ---------------------------------------------------------------------------

  @Test
  fun `RU HOURS plural forms`() {
    val f = formatter(Locale.forLanguageTag("ru"))
    listOf(
      1 to "1 час",
      2 to "2 часа",
      3 to "3 часа",
      4 to "4 часа",
      5 to "5 часов",
      10 to "10 часов",
      11 to "11 часов",
      12 to "12 часов",
      13 to "13 часов",
      14 to "14 часов",
      20 to "20 часов",
      21 to "21 час",
      22 to "22 часа",
      23 to "23 часа",
      24 to "24 часа",
      25 to "25 часов",
      111 to "111 часов",
      121 to "121 час",
    ).forEach { (count, expected) ->
      assertEquals("HOURS count=$count", expected, f.format(CountdownUnit.HOURS, count))
    }
  }

  // ---------------------------------------------------------------------------
  // Russian — CountdownUnit.MINUTES (минута / минуты / минут)
  // ---------------------------------------------------------------------------

  @Test
  fun `RU MINUTES plural forms`() {
    val f = formatter(Locale.forLanguageTag("ru"))
    listOf(
      1 to "1 минута",
      2 to "2 минуты",
      3 to "3 минуты",
      4 to "4 минуты",
      5 to "5 минут",
      10 to "10 минут",
      11 to "11 минут",
      12 to "12 минут",
      13 to "13 минут",
      14 to "14 минут",
      20 to "20 минут",
      21 to "21 минута",
      22 to "22 минуты",
      23 to "23 минуты",
      24 to "24 минуты",
      25 to "25 минут",
      111 to "111 минут",
      121 to "121 минута",
    ).forEach { (count, expected) ->
      assertEquals("MINUTES count=$count", expected, f.format(CountdownUnit.MINUTES, count))
    }
  }

  // ---------------------------------------------------------------------------
  // Russian — CountdownUnit.SECONDS (секунда / секунды / секунд)
  // ---------------------------------------------------------------------------

  @Test
  fun `RU SECONDS plural forms`() {
    val f = formatter(Locale.forLanguageTag("ru"))
    listOf(
      1 to "1 секунда",
      2 to "2 секунды",
      3 to "3 секунды",
      4 to "4 секунды",
      5 to "5 секунд",
      10 to "10 секунд",
      11 to "11 секунд",
      12 to "12 секунд",
      13 to "13 секунд",
      14 to "14 секунд",
      20 to "20 секунд",
      21 to "21 секунда",
      22 to "22 секунды",
      23 to "23 секунды",
      24 to "24 секунды",
      25 to "25 секунд",
      111 to "111 секунд",
      121 to "121 секунда",
    ).forEach { (count, expected) ->
      assertEquals("SECONDS count=$count", expected, f.format(CountdownUnit.SECONDS, count))
    }
  }

  // ---------------------------------------------------------------------------
  // English — all CountdownUnit values (one / other)
  // ---------------------------------------------------------------------------

  @Test
  fun `EN YEARS plural forms`() {
    val f = formatter(Locale.ENGLISH)
    listOf(
      1 to "1 year",
      0 to "0 years",
      2 to "2 years",
      5 to "5 years",
      11 to "11 years",
      21 to "21 years",
      100 to "100 years",
    ).forEach { (count, expected) ->
      assertEquals("YEARS count=$count", expected, f.format(CountdownUnit.YEARS, count))
    }
  }

  @Test
  fun `EN DAYS plural forms`() {
    val f = formatter(Locale.ENGLISH)
    listOf(
      1 to "1 day",
      0 to "0 days",
      2 to "2 days",
      5 to "5 days",
      11 to "11 days",
      21 to "21 days",
      100 to "100 days",
    ).forEach { (count, expected) ->
      assertEquals("DAYS count=$count", expected, f.format(CountdownUnit.DAYS, count))
    }
  }

  @Test
  fun `EN HOURS plural forms`() {
    val f = formatter(Locale.ENGLISH)
    listOf(
      1 to "1 hour",
      0 to "0 hours",
      2 to "2 hours",
      5 to "5 hours",
      11 to "11 hours",
      21 to "21 hours",
      100 to "100 hours",
    ).forEach { (count, expected) ->
      assertEquals("HOURS count=$count", expected, f.format(CountdownUnit.HOURS, count))
    }
  }

  @Test
  fun `EN MINUTES plural forms`() {
    val f = formatter(Locale.ENGLISH)
    listOf(
      1 to "1 minute",
      0 to "0 minutes",
      2 to "2 minutes",
      5 to "5 minutes",
      11 to "11 minutes",
      21 to "21 minutes",
      100 to "100 minutes",
    ).forEach { (count, expected) ->
      assertEquals("MINUTES count=$count", expected, f.format(CountdownUnit.MINUTES, count))
    }
  }

  @Test
  fun `EN SECONDS plural forms`() {
    val f = formatter(Locale.ENGLISH)
    listOf(
      1 to "1 second",
      0 to "0 seconds",
      2 to "2 seconds",
      5 to "5 seconds",
      11 to "11 seconds",
      21 to "21 seconds",
      100 to "100 seconds",
    ).forEach { (count, expected) ->
      assertEquals("SECONDS count=$count", expected, f.format(CountdownUnit.SECONDS, count))
    }
  }

  // ---------------------------------------------------------------------------
  // daysAgo — RU (день назад / дня назад / дней назад)
  // ---------------------------------------------------------------------------

  @Test
  @Suppress("LongMethod") // table-driven plural rule coverage
  fun `RU daysAgo plural forms`() {
    val f = formatter(Locale.forLanguageTag("ru"))
    listOf(
      1 to "1 день назад",
      2 to "2 дня назад",
      3 to "3 дня назад",
      4 to "4 дня назад",
      5 to "5 дней назад",
      11 to "11 дней назад",
      12 to "12 дней назад",
      21 to "21 день назад",
      22 to "22 дня назад",
    ).forEach { (count, expected) ->
      assertEquals("daysAgo count=$count", expected, f.daysAgo(count))
    }
  }

  // ---------------------------------------------------------------------------
  // daysAgo — EN (day ago / days ago)
  // ---------------------------------------------------------------------------

  @Test
  fun `EN daysAgo plural forms`() {
    val f = formatter(Locale.ENGLISH)
    listOf(
      1 to "1 day ago",
      2 to "2 days ago",
      11 to "11 days ago",
      21 to "21 days ago",
    ).forEach { (count, expected) ->
      assertEquals("daysAgo count=$count", expected, f.daysAgo(count))
    }
  }

  // ---------------------------------------------------------------------------
  // events — RU (событие / события / событий)
  // ---------------------------------------------------------------------------

  @Test
  fun `RU events plural forms`() {
    val f = formatter(Locale.forLanguageTag("ru"))
    listOf(
      1 to "1 событие",
      2 to "2 события",
      3 to "3 события",
      4 to "4 события",
      5 to "5 событий",
      11 to "11 событий",
      21 to "21 событие",
    ).forEach { (count, expected) ->
      assertEquals("events count=$count", expected, f.events(count))
    }
  }

  // ---------------------------------------------------------------------------
  // events — EN (event / events)
  // ---------------------------------------------------------------------------

  @Test
  fun `EN events plural forms`() {
    val f = formatter(Locale.ENGLISH)
    listOf(
      1 to "1 event",
      2 to "2 events",
      11 to "11 events",
    ).forEach { (count, expected) ->
      assertEquals("events count=$count", expected, f.events(count))
    }
  }
}
