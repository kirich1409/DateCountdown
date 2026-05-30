@file:Suppress("MagicNumber")

package com.datecountdown.app.core.design.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Verifies WCAG AA (≥ 4.5:1) contrast for the counter-screen components.
 *
 * ## Coverage
 * - Glass-cell label (ЧАСОВ/МИНУТ/СЕКУНД) — white text at label alpha 1.0 over
 *   `hero + glass@glassAlpha + scrim@scrimAlphaFor(hero, glassAlpha)` — light theme only.
 * - Section header on glass+scrim — same model as glass-cell label (light theme).
 * - Section header on bare hero — "ДО СОБЫТИЯ" / "ПРОШЛО" renders on the raw hero bg,
 *   not over glass. Dedicated guard to keep the correct (more conservative) bg model.
 * - Date-chip label light — [EventPalette.onContainer] over `composite(hero, container, 0.4f)`.
 * - Date-chip label dark — [EventPalette.onContainer] over `composite(hero, container, 0.8f)`;
 *   dark heroes are light and containers are dark, requiring a higher alpha to pass 4.5:1.
 *
 * ## Tier coverage
 * Both glass tiers are exercised for label/header:
 * - [GLASS_ALPHA_DEFAULT] = 0.16f (API 31+)
 * - [GLASS_ALPHA_LEGACY]  = 0.26f (API 29–30)
 *
 * ## Remark on AMBER legacy
 * With target = 4.5 the AMBER palette requires scrimAlpha = 0.12 on the legacy tier
 * and achieves exactly 4.500:1 by calculation. On-device pixel sampling may yield
 * slightly lower values due to sub-pixel rendering and float32 rounding. If L5
 * verification reveals a failure on AMBER/LEGACY, bump the generation target to 4.6
 * when computing scrimAlpha in the runtime call and re-run this test.
 *
 * ## Dark-theme note
 * The black scrim model is only valid in light theme. Dark-palette heroes are light
 * (e.g. ORANGE dark hero = 0xFFFFB68A), so a black scrim would reduce contrast there.
 * The runtime caller must NOT apply scrimAlphaFor to dark-theme palettes.
 */
class WcagContrastTest {

  // ---------------------------------------------------------------------------
  // Constants — single source of truth from GlassSurface.kt
  // ---------------------------------------------------------------------------

  /** Alpha at which the container color is blended over the hero for the date chip — light theme. */
  private val chipContainerAlpha = 0.4f

  /**
   * Alpha at which the container color is blended over the hero for the date chip — dark theme.
   *
   * Dark heroes are light; dark containers are dark. At 0.4 the composite mid-tone yields only
   * 2.3–2.7:1 for onContainer (light text). 0.8 is the minimum alpha that clears 4.5:1 across
   * all 9 dark palettes (worst case: GREEN 4.751:1, PURPLE 4.899:1).
   */
  private val chipContainerAlphaDark = 0.8f

  private val target = 4.5

  // ---------------------------------------------------------------------------
  // All 9 light palettes (light theme only — scrim model is for light hero colors)
  // ---------------------------------------------------------------------------

  private val lightPalettes: List<Pair<String, EventPalette>> = listOf(
    "ORANGE" to EventPaletteId.ORANGE.palette(dark = false),
    "PINK"   to EventPaletteId.PINK.palette(dark = false),
    "BLUE"   to EventPaletteId.BLUE.palette(dark = false),
    "PURPLE" to EventPaletteId.PURPLE.palette(dark = false),
    "INDIGO" to EventPaletteId.INDIGO.palette(dark = false),
    "TEAL"   to EventPaletteId.TEAL.palette(dark = false),
    "GREEN"  to EventPaletteId.GREEN.palette(dark = false),
    "RED"    to EventPaletteId.RED.palette(dark = false),
    "AMBER"  to EventPaletteId.AMBER.palette(dark = false),
  )

  private val darkPalettes: List<Pair<String, EventPalette>> = listOf(
    "ORANGE" to EventPaletteId.ORANGE.palette(dark = true),
    "PINK"   to EventPaletteId.PINK.palette(dark = true),
    "BLUE"   to EventPaletteId.BLUE.palette(dark = true),
    "PURPLE" to EventPaletteId.PURPLE.palette(dark = true),
    "INDIGO" to EventPaletteId.INDIGO.palette(dark = true),
    "TEAL"   to EventPaletteId.TEAL.palette(dark = true),
    "GREEN"  to EventPaletteId.GREEN.palette(dark = true),
    "RED"    to EventPaletteId.RED.palette(dark = true),
    "AMBER"  to EventPaletteId.AMBER.palette(dark = true),
  )

  private fun fmt2(v: Float): String = String.format(Locale.ROOT, "%.2f", v)
  private fun fmt3(v: Double): String = String.format(Locale.ROOT, "%.3f", v)
  private fun fmt4(v: Double): String = String.format(Locale.ROOT, "%.4f", v)

  // ---------------------------------------------------------------------------
  // Glass-cell label — white text @1.0 over (hero + glass + scrim)
  // ---------------------------------------------------------------------------

  @Test
  fun `glass cell label - white text - all 9 light palettes - GLASS_ALPHA_DEFAULT`() {
    println("\n=== RECIPE: glass cell / header — GLASS_ALPHA_DEFAULT (0.16) ===")
    println("Palette  | scrimAlpha | ratio")
    println("---------|------------|------")
    for ((name, palette) in lightPalettes) {
      val scrim = scrimAlphaFor(hero = palette.hero, glassAlpha = GLASS_ALPHA_DEFAULT, target = target)
      val bg = composite(composite(palette.hero, Color.White, GLASS_ALPHA_DEFAULT), Color.Black, scrim)
      val ratio = contrastRatio(Color.White, bg)
      println("${name.padEnd(8)} | ${fmt2(scrim)}       | ${fmt3(ratio)}")
      assertTrue(
        "$name GLASS_DEFAULT: white text ratio $ratio < $target (scrim=$scrim)",
        ratio >= target,
      )
    }
  }

  @Test
  fun `glass cell label - white text - all 9 light palettes - GLASS_ALPHA_LEGACY`() {
    println("\n=== RECIPE: glass cell / header — GLASS_ALPHA_LEGACY (0.26) ===")
    println("Palette  | scrimAlpha | ratio")
    println("---------|------------|------")
    for ((name, palette) in lightPalettes) {
      val scrim = scrimAlphaFor(hero = palette.hero, glassAlpha = GLASS_ALPHA_LEGACY, target = target)
      val bg = composite(composite(palette.hero, Color.White, GLASS_ALPHA_LEGACY), Color.Black, scrim)
      val ratio = contrastRatio(Color.White, bg)
      println("${name.padEnd(8)} | ${fmt2(scrim)}       | ${fmt3(ratio)}")
      assertTrue(
        "$name GLASS_LEGACY: white text ratio $ratio < $target (scrim=$scrim)",
        ratio >= target,
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Section header — same model as glass-cell label (white @1.0 over same bg)
  // ---------------------------------------------------------------------------

  @Test
  fun `section header - white text - all 9 light palettes - GLASS_ALPHA_DEFAULT`() {
    // Section header uses the same background and text model as the glass cell label.
    // This test is a dedicated regression guard for the GREEN palette (was 4.38 before fix).
    for ((name, palette) in lightPalettes) {
      val scrim = scrimAlphaFor(hero = palette.hero, glassAlpha = GLASS_ALPHA_DEFAULT, target = target)
      val bg = composite(composite(palette.hero, Color.White, GLASS_ALPHA_DEFAULT), Color.Black, scrim)
      val ratio = contrastRatio(Color.White, bg)
      assertTrue(
        "$name section header GLASS_DEFAULT: white text ratio $ratio < $target (scrim=$scrim)",
        ratio >= target,
      )
    }
  }

  @Test
  fun `section header - white text - all 9 light palettes - GLASS_ALPHA_LEGACY`() {
    for ((name, palette) in lightPalettes) {
      val scrim = scrimAlphaFor(hero = palette.hero, glassAlpha = GLASS_ALPHA_LEGACY, target = target)
      val bg = composite(composite(palette.hero, Color.White, GLASS_ALPHA_LEGACY), Color.Black, scrim)
      val ratio = contrastRatio(Color.White, bg)
      assertTrue(
        "$name section header GLASS_LEGACY: white text ratio $ratio < $target (scrim=$scrim)",
        ratio >= target,
      )
    }
  }

  @Test
  fun `section header - white text - all 9 light palettes - bare hero (non-glass background)`() {
    // The "ДО СОБЫТИЯ" / "ПРОШЛО" label renders directly on the bare hero, not over glass+scrim.
    // This test guards the correct (more conservative) background model for that element.
    println("\n=== section header on bare hero ===")
    println("Palette  | ratio(white/hero)")
    println("---------|------------------")
    for ((name, palette) in lightPalettes) {
      val ratio = contrastRatio(Color.White, palette.hero)
      println("${name.padEnd(8)} | ${fmt3(ratio)}")
      assertTrue(
        "$name section header on bare hero: white text ratio $ratio < $target",
        ratio >= target,
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Date chip label — onContainer over composite(hero, container, 0.4)
  // ---------------------------------------------------------------------------

  @Test
  fun `date chip label - onContainer text - all 9 light palettes - container alpha 0_4`() {
    println("\n=== RECIPE: date chip — onContainer text, container@0.4 ===")
    println("Palette  | chipBg_lum | ratio(onContainer)")
    println("---------|------------|-------------------")
    for ((name, palette) in lightPalettes) {
      val chipBg = composite(palette.hero, palette.container, chipContainerAlpha)
      val ratio = contrastRatio(palette.onContainer, chipBg)
      val lumStr = fmt4(relativeLuminance(chipBg))
      println("${name.padEnd(8)} | $lumStr     | ${fmt3(ratio)}")
      assertTrue(
        "$name date-chip light: onContainer ratio $ratio < $target (container alpha=$chipContainerAlpha)",
        ratio >= target,
      )
    }
  }

  @Test
  fun `date chip label - onContainer text - all 9 dark palettes - container alpha 0_8`() {
    // Dark theme: hero is light, container is dark.
    // onContainer is light text over a darkening composite bg (higher alpha = darker → better contrast).
    // chipContainerAlphaDark=0.8 is the minimum alpha that clears 4.5:1 across all 9 dark palettes.
    // Worst-case palettes at 0.8: GREEN 4.751:1, PURPLE 4.899:1, TEAL 4.924:1.
    println("\n=== RECIPE: date chip dark — onContainer text, container@0.8 ===")
    println("Palette  | chipBg_lum | ratio(onContainer)")
    println("---------|------------|-------------------")
    for ((name, palette) in darkPalettes) {
      val chipBg = composite(palette.hero, palette.container, chipContainerAlphaDark)
      val ratio = contrastRatio(palette.onContainer, chipBg)
      val lumStr = fmt4(relativeLuminance(chipBg))
      println("${name.padEnd(8)} | $lumStr     | ${fmt3(ratio)}")
      assertTrue(
        "$name date-chip dark: onContainer ratio $ratio < $target (container alpha=$chipContainerAlphaDark)",
        ratio >= target,
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Core utility tests
  // ---------------------------------------------------------------------------

  @Test
  fun `relativeLuminance - white is 1_0`() {
    val lum = relativeLuminance(Color.White)
    assertTrue("Expected white luminance ≈ 1.0, got $lum", Math.abs(lum - 1.0) < 0.001)
  }

  @Test
  fun `relativeLuminance - black is 0_0`() {
    val lum = relativeLuminance(Color.Black)
    assertTrue("Expected black luminance = 0.0, got $lum", lum == 0.0)
  }

  @Test
  fun `contrastRatio - white on black is 21_0`() {
    val ratio = contrastRatio(Color.White, Color.Black)
    assertTrue("Expected 21.0, got $ratio", Math.abs(ratio - 21.0) < 0.01)
  }

  @Test
  fun `contrastRatio - white on white is 1_0`() {
    val ratio = contrastRatio(Color.White, Color.White)
    assertTrue("Expected 1.0, got $ratio", Math.abs(ratio - 1.0) < 0.001)
  }

  @Test
  fun `composite - 50pct white over black yields mid-grey`() {
    val result = composite(Color.Black, Color.White, 0.5f)
    // sRGB midpoint: each channel ≈ 0*(0.5) + 1*(0.5) = 0.5.
    // Compose Color stores channels in float32; a tolerance of 0.002 covers
    // float-representation deltas (e.g. 128/255 ≈ 0.5019).
    assertTrue("Expected red≈0.5, got ${result.red}", Math.abs(result.red - 0.5f) < 0.002f)
    assertTrue("Expected green≈0.5, got ${result.green}", Math.abs(result.green - 0.5f) < 0.002f)
    assertTrue("Expected blue≈0.5, got ${result.blue}", Math.abs(result.blue - 0.5f) < 0.002f)
  }

  @Test
  fun `scrimAlphaFor - returns 0 when hero is already dark enough`() {
    // Pure black hero: white on black = 21:1, no scrim needed
    val alpha = scrimAlphaFor(hero = Color.Black, glassAlpha = 0.16f, target = 4.5)
    assertTrue("Expected 0f for black hero, got $alpha", alpha == 0f)
  }
}
