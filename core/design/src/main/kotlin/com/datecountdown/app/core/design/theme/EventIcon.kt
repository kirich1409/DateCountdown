package com.datecountdown.app.core.design.theme

/**
 * The 16 event icon identifiers used throughout the app.
 *
 * Each entry carries the canonical Material Symbols Rounded glyph name via [symbolName].
 * The actual font file and codepoint-to-glyph rendering are introduced in issue #21;
 * until then [symbolName] is the stable hook consumers should use to request a glyph.
 *
 * Entry order matches `m3-app.jsx:780` (the icon-picker row order).
 *
 * Feature-module usage:
 * ```kotlin
 * // Resolves the icon name; rendering is delegated to the font-aware component added in #21.
 * val iconName = event.icon.symbolName  // e.g. "celebration"
 * ```
 */
enum class EventIcon(
  /**
   * The Material Symbols Rounded glyph name as listed in the Material Symbols catalogue.
   * Used as the stable identifier until the bundled font (issue #21) maps names to codepoints.
   */
  val symbolName: String,
) {
  CELEBRATION("celebration"),
  CAKE("cake"),
  BEACH_ACCESS("beach_access"),
  ROCKET_LAUNCH("rocket_launch"),
  SCHOOL("school"),
  FAVORITE("favorite"),
  MUSIC_NOTE("music_note"),
  DIRECTIONS_RUN("directions_run"),
  FLIGHT("flight"),
  MOVIE("movie"),
  BOOK_2("book_2"),
  SPA("spa"),
  RESTAURANT("restaurant"),
  SPORTS_ESPORTS("sports_esports"),
  REDEEM("redeem"),
  SNOWING("snowing"),
}
