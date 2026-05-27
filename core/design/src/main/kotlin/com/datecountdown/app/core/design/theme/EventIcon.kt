@file:Suppress("MagicNumber") // Unicode codepoints from the Material Symbols cmap table.

package com.datecountdown.app.core.design.theme

/**
 * The 16 event icon identifiers used throughout the app.
 *
 * Each entry carries:
 * - [symbolName] — the canonical Material Symbols Rounded glyph name.
 * - [codepoint] — the Unicode codepoint of the glyph in the bundled
 *   `material_symbols_rounded.ttf` (res/font). Used by [EventSymbol]
 *   to render the icon without relying on ligature substitution.
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
 *   contentDescription = stringResource(R.string.event_icon_desc, event.icon.symbolName),
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
) {
  CELEBRATION("celebration", 0xea65),
  CAKE("cake", 0xe7e9),
  BEACH_ACCESS("beach_access", 0xeb3e),
  ROCKET_LAUNCH("rocket_launch", 0xeb9b),
  SCHOOL("school", 0xe80c),
  FAVORITE("favorite", 0xe87e),
  MUSIC_NOTE("music_note", 0xe405),
  DIRECTIONS_RUN("directions_run", 0xe566),
  FLIGHT("flight", 0xe539),
  MOVIE("movie", 0xe404),
  BOOK_2("book_2", 0xf53e),
  SPA("spa", 0xeb4c),
  RESTAURANT("restaurant", 0xe56c),
  SPORTS_ESPORTS("sports_esports", 0xea28),
  REDEEM("redeem", 0xe8f6),
  SNOWING("snowing", 0xe80f),
}
