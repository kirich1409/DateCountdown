package com.datecountdown.app.core.common.plural

import android.content.res.Resources
import com.datecountdown.app.core.common.R
import com.datecountdown.app.domain.CountdownUnit

/**
 * Formats integer counts into locale-aware plural strings for countdown and event display.
 *
 * Inject [Resources] directly (not [android.content.Context]) to keep the dependency narrow.
 * At a Compose call site use `LocalContext.current.resources`.
 *
 * Plural rules are defined in:
 * - `res/values/plurals.xml`       — English (one / other)
 * - `res/values-ru/plurals.xml`    — Russian (one / few / many / other)
 */
class PluralFormatter(private val resources: Resources) {

  /** Formats a [CountdownUnit] count, e.g. "3 days", "1 год", "5 часов". */
  fun format(unit: CountdownUnit, count: Int): String = when (unit) {
    CountdownUnit.YEARS   -> resources.getQuantityString(R.plurals.time_unit_year,   count, count)
    CountdownUnit.DAYS    -> resources.getQuantityString(R.plurals.time_unit_day,    count, count)
    CountdownUnit.HOURS   -> resources.getQuantityString(R.plurals.time_unit_hour,   count, count)
    CountdownUnit.MINUTES -> resources.getQuantityString(R.plurals.time_unit_minute, count, count)
    CountdownUnit.SECONDS -> resources.getQuantityString(R.plurals.time_unit_second, count, count)
  }

  /** Formats a past-event days count, e.g. "2 days ago", "3 дня назад". */
  fun daysAgo(count: Int): String =
    resources.getQuantityString(R.plurals.days_ago, count, count)

  /** Formats an events count, e.g. "1 event", "5 событий". */
  fun events(count: Int): String =
    resources.getQuantityString(R.plurals.events_count, count, count)
}
