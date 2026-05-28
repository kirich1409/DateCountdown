package com.datecountdown.app.domain

/**
 * One of the 9 fixed event color identities.
 *
 * **Ordinal contract** — the ordinal of each entry is a stable index into
 * `:core:design`'s `EventPaletteId` enum. Feature modules must NOT hard-code
 * the ordinal values; instead use `EventColor.ordinal` as a direct index:
 *
 * ```kotlin
 * val palette = eventPaletteByIndex(event.color.ordinal, dark = isSystemInDarkTheme())
 * ```
 *
 * Entry order is fixed:
 *   0=ORANGE, 1=PINK, 2=BLUE, 3=PURPLE, 4=INDIGO, 5=TEAL, 6=GREEN, 7=RED, 8=AMBER.
 *
 * **Do not reorder entries** — doing so breaks the palette mapping in all feature modules.
 */
enum class EventColor {
  ORANGE,
  PINK,
  BLUE,
  PURPLE,
  INDIGO,
  TEAL,
  GREEN,
  RED,
  AMBER,
}
