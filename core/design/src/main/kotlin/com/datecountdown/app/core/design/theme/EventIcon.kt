@file:Suppress("MagicNumber") // Unicode codepoints from the Material Symbols cmap table.

package com.datecountdown.app.core.design.theme

import androidx.annotation.StringRes
import com.datecountdown.app.core.design.R

/**
 * The 16 event icon identifiers used throughout the app.
 *
 * Each entry carries:
 * - [symbolName] — the canonical Material Symbols Rounded glyph name.
 * - [codepoint] — the Unicode codepoint of the glyph in the bundled
 *   `material_symbols_rounded.ttf` (res/font). Used by [EventSymbol]
 *   to render the icon without relying on ligature substitution.
 * - [labelRes] — a string resource id for the localized, human-readable icon name,
 *   used as TalkBack `contentDescription` to avoid leaking snake_case glyph names.
 *
 * The bundled font is a 16-glyph static subset of Material Symbols Rounded with axes
 * pinned to FILL=1 (filled style), wght=400, GRAD=0, opsz=24. To add an outline variant
 * a separate font subset with FILL=0 would be needed.
 *
 * Codepoints are verified against `MaterialSymbolsRounded[FILL,GRAD,opsz,wght].codepoints`
 * (google/material-design-icons, variablefont/).
 *
 * Entry order matches `m3-app.jsx:780` (the icon-picker row order).
 *
 * Feature-module usage:
 * ```kotlin
 * EventSymbol(
 *   icon = event.icon,
 *   contentDescription = stringResource(R.string.counter_icon_description, stringResource(designIcon.labelRes)),
 * )
 * ```
 */
enum class EventIcon(
  /**
   * The Material Symbols Rounded glyph name as listed in the Material Symbols catalogue.
   */
  val symbolName: String,
  /**
   * Unicode codepoint of this glyph in the bundled Material Symbols Rounded static font.
   *
   * Verified against the font's cmap table and the canonical codepoints file
   * (google/material-design-icons, variablefont/).
   */
  val codepoint: Int,
  /**
   * String resource id for the localized, human-readable icon label (e.g. "Celebration" / "Праздник").
   * Use this as the TalkBack `contentDescription` instead of [symbolName] to avoid leaking
   * snake_case glyph names to screen readers.
   */
  @get:StringRes val labelRes: Int,
) {
  CELEBRATION("celebration", 0xea65, R.string.event_icon_label_celebration),
  CAKE("cake", 0xe7e9, R.string.event_icon_label_cake),
  BEACH_ACCESS("beach_access", 0xeb3e, R.string.event_icon_label_beach_access),
  ROCKET_LAUNCH("rocket_launch", 0xeb9b, R.string.event_icon_label_rocket_launch),
  SCHOOL("school", 0xe80c, R.string.event_icon_label_school),
  FAVORITE("favorite", 0xe87e, R.string.event_icon_label_favorite),
  MUSIC_NOTE("music_note", 0xe405, R.string.event_icon_label_music_note),
  DIRECTIONS_RUN("directions_run", 0xe566, R.string.event_icon_label_directions_run),
  FLIGHT("flight", 0xe539, R.string.event_icon_label_flight),
  MOVIE("movie", 0xe404, R.string.event_icon_label_movie),
  BOOK_2("book_2", 0xf53e, R.string.event_icon_label_book_2),
  SPA("spa", 0xeb4c, R.string.event_icon_label_spa),
  RESTAURANT("restaurant", 0xe56c, R.string.event_icon_label_restaurant),
  SPORTS_ESPORTS("sports_esports", 0xea28, R.string.event_icon_label_sports_esports),
  REDEEM("redeem", 0xe8f6, R.string.event_icon_label_redeem),
  SNOWING("snowing", 0xe80f, R.string.event_icon_label_snowing),
}
