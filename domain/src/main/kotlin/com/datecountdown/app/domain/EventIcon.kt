package com.datecountdown.app.domain

/**
 * One of the 16 icon identities available for an [Event].
 *
 * **Ordinal contract** — the ordinal of each entry is a stable index into
 * `:core:design`'s `EventIcon` enum (which carries the Material Symbols
 * codepoint used for rendering). Feature modules map between the two enums
 * via ordinal — no lookup table is required:
 *
 * ```kotlin
 * val designIcon = com.datecountdown.app.core.design.theme.EventIcon.entries[event.icon.ordinal]
 * ```
 *
 * Entry order matches the icon-picker row order defined in `m3-app.jsx:780`:
 *   0=CELEBRATION, 1=CAKE, 2=BEACH_ACCESS, 3=ROCKET_LAUNCH, 4=SCHOOL,
 *   5=FAVORITE, 6=MUSIC_NOTE, 7=DIRECTIONS_RUN, 8=FLIGHT, 9=MOVIE,
 *   10=BOOK_2, 11=SPA, 12=RESTAURANT, 13=SPORTS_ESPORTS, 14=REDEEM, 15=SNOWING.
 *
 * **Do not reorder entries** — doing so breaks the icon mapping in all feature modules.
 */
enum class EventIcon {
  CELEBRATION,
  CAKE,
  BEACH_ACCESS,
  ROCKET_LAUNCH,
  SCHOOL,
  FAVORITE,
  MUSIC_NOTE,
  DIRECTIONS_RUN,
  FLIGHT,
  MOVIE,
  BOOK_2,
  SPA,
  RESTAURANT,
  SPORTS_ESPORTS,
  REDEEM,
  SNOWING,
}
