@file:Suppress("MagicNumber")

package com.datecountdown.app.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * A fixed tonal palette for a single event identity.
 *
 * These values are NOT derived from Material You / dynamic color — they are hard-coded per
 * AC-TH-5 and do not change with the system theme. Light/dark variants exist to maintain
 * legibility in either appearance, but the palette identity (the hue family) is always the
 * same regardless of device settings.
 *
 * @property container   Background chip / card color.
 * @property onContainer Foreground text / icon on [container].
 * @property hero        Prominent accent — countdown number, filled button background.
 * @property onHero      Foreground text / icon on [hero].
 */
data class EventPalette(
  val container: Color,
  val onContainer: Color,
  val hero: Color,
  val onHero: Color,
)

/**
 * Identifies one of the 9 fixed event color palettes.
 *
 * Entry order matches `EventColor.ordinal` in `:domain` so that
 * `EventColor.ordinal` can be used as a direct index without a mapping table:
 *   0=ORANGE, 1=PINK, 2=BLUE, 3=PURPLE, 4=INDIGO, 5=TEAL, 6=GREEN, 7=RED, 8=AMBER.
 *
 * Feature modules perform the mapping:
 *   `eventPaletteByIndex(eventColor.ordinal, dark = isDarkTheme)`
 */
enum class EventPaletteId {
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

// ---------------------------------------------------------------------------
// Light palettes — hex values sourced from m3-app.jsx:64-85
// ---------------------------------------------------------------------------

private val OrangePaletteLight = EventPalette(
  container = Color(0xFFFFDCC1),
  onContainer = Color(0xFF2C1600),
  hero = Color(0xFF9C4900),
  onHero = Color(0xFFFFFFFF),
)

private val PinkPaletteLight = EventPalette(
  container = Color(0xFFFFD9E2),
  onContainer = Color(0xFF3F001E),
  hero = Color(0xFFB11C5C),
  onHero = Color(0xFFFFFFFF),
)

private val BluePaletteLight = EventPalette(
  container = Color(0xFFD3E3FF),
  onContainer = Color(0xFF001B3F),
  hero = Color(0xFF005AC1),
  onHero = Color(0xFFFFFFFF),
)

private val PurplePaletteLight = EventPalette(
  container = Color(0xFFECDCFF),
  onContainer = Color(0xFF22005D),
  hero = Color(0xFF6750A4),
  onHero = Color(0xFFFFFFFF),
)

private val IndigoPaletteLight = EventPalette(
  container = Color(0xFFDEE0FF),
  onContainer = Color(0xFF000965),
  hero = Color(0xFF4A4FB5),
  onHero = Color(0xFFFFFFFF),
)

private val TealPaletteLight = EventPalette(
  container = Color(0xFF7DF2E0),
  onContainer = Color(0xFF00201C),
  hero = Color(0xFF00695E),
  onHero = Color(0xFFFFFFFF),
)

private val GreenPaletteLight = EventPalette(
  container = Color(0xFFA8F0B5),
  onContainer = Color(0xFF002107),
  hero = Color(0xFF1F7A37),
  onHero = Color(0xFFFFFFFF),
)

private val RedPaletteLight = EventPalette(
  container = Color(0xFFFFDAD6),
  onContainer = Color(0xFF410002),
  hero = Color(0xFFB3261E),
  onHero = Color(0xFFFFFFFF),
)

private val AmberPaletteLight = EventPalette(
  container = Color(0xFFF7E47A),
  onContainer = Color(0xFF231B00),
  hero = Color(0xFF735C00),
  onHero = Color(0xFFFFFFFF),
)

// ---------------------------------------------------------------------------
// Dark palettes — hex values sourced from m3-app.jsx:64-85
// ---------------------------------------------------------------------------

private val OrangePaletteDark = EventPalette(
  container = Color(0xFF562C00),
  onContainer = Color(0xFFFFDCC1),
  hero = Color(0xFFFFB68A),
  onHero = Color(0xFF4E2400),
)

private val PinkPaletteDark = EventPalette(
  container = Color(0xFF7A0040),
  onContainer = Color(0xFFFFD9E2),
  hero = Color(0xFFFFB1C7),
  onHero = Color(0xFF5F0030),
)

private val BluePaletteDark = EventPalette(
  container = Color(0xFF003972),
  onContainer = Color(0xFFD3E3FF),
  hero = Color(0xFFA6C8FF),
  onHero = Color(0xFF002F65),
)

private val PurplePaletteDark = EventPalette(
  container = Color(0xFF4F378B),
  onContainer = Color(0xFFECDCFF),
  hero = Color(0xFFD0BCFF),
  onHero = Color(0xFF371E73),
)

private val IndigoPaletteDark = EventPalette(
  container = Color(0xFF222783),
  onContainer = Color(0xFFDEE0FF),
  hero = Color(0xFFBAC0FF),
  onHero = Color(0xFF0C1399),
)

private val TealPaletteDark = EventPalette(
  container = Color(0xFF005048),
  onContainer = Color(0xFF9EF2E4),
  hero = Color(0xFF82D5C8),
  onHero = Color(0xFF003731),
)

private val GreenPaletteDark = EventPalette(
  container = Color(0xFF005317),
  onContainer = Color(0xFFA8F0B5),
  hero = Color(0xFF8BDA9C),
  onHero = Color(0xFF003910),
)

private val RedPaletteDark = EventPalette(
  container = Color(0xFF93000A),
  onContainer = Color(0xFFFFDAD6),
  hero = Color(0xFFFFB4AB),
  onHero = Color(0xFF690005),
)

private val AmberPaletteDark = EventPalette(
  container = Color(0xFF574500),
  onContainer = Color(0xFFF7E47A),
  hero = Color(0xFFDBC85F),
  onHero = Color(0xFF3D2F00),
)

// ---------------------------------------------------------------------------
// Accessor API
// ---------------------------------------------------------------------------

/**
 * Returns the [EventPalette] for this palette id in the given appearance.
 *
 * @param dark `true` for dark-theme values, `false` for light-theme values.
 */
fun EventPaletteId.palette(dark: Boolean): EventPalette =
  if (dark) darkPalette() else lightPalette()

private fun EventPaletteId.lightPalette(): EventPalette = when (this) {
  EventPaletteId.ORANGE -> OrangePaletteLight
  EventPaletteId.PINK   -> PinkPaletteLight
  EventPaletteId.BLUE   -> BluePaletteLight
  EventPaletteId.PURPLE -> PurplePaletteLight
  EventPaletteId.INDIGO -> IndigoPaletteLight
  EventPaletteId.TEAL   -> TealPaletteLight
  EventPaletteId.GREEN  -> GreenPaletteLight
  EventPaletteId.RED    -> RedPaletteLight
  EventPaletteId.AMBER  -> AmberPaletteLight
}

private fun EventPaletteId.darkPalette(): EventPalette = when (this) {
  EventPaletteId.ORANGE -> OrangePaletteDark
  EventPaletteId.PINK   -> PinkPaletteDark
  EventPaletteId.BLUE   -> BluePaletteDark
  EventPaletteId.PURPLE -> PurplePaletteDark
  EventPaletteId.INDIGO -> IndigoPaletteDark
  EventPaletteId.TEAL   -> TealPaletteDark
  EventPaletteId.GREEN  -> GreenPaletteDark
  EventPaletteId.RED    -> RedPaletteDark
  EventPaletteId.AMBER  -> AmberPaletteDark
}

/**
 * Returns the [EventPalette] for the given zero-based palette index and appearance.
 *
 * The index contract mirrors `EventColor.ordinal` in `:domain`:
 *   0=ORANGE, 1=PINK, 2=BLUE, 3=PURPLE, 4=INDIGO, 5=TEAL, 6=GREEN, 7=RED, 8=AMBER.
 *
 * Feature-module usage:
 * ```kotlin
 * val palette = eventPaletteByIndex(event.color.ordinal, dark = isSystemInDarkTheme())
 * ```
 *
 * @throws IndexOutOfBoundsException if [index] is outside 0..8.
 */
fun eventPaletteByIndex(index: Int, dark: Boolean): EventPalette =
  EventPaletteId.entries[index].palette(dark)
