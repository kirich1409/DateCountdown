@file:Suppress("MagicNumber")

package com.datecountdown.app.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * WCAG 2.1 contrast-ratio utilities for the DateCountdown design system.
 *
 * ## Compositing model
 * Blending is performed in **sRGB** (gamma-encoded space): `c = bg*(1-α) + fg*α`.
 * Linearisation for luminance happens **after** compositing — only inside [relativeLuminance].
 * This matches how the counter screen layers are rendered on Android.
 *
 * ## Layer order for glass cells (counter screen)
 * ```
 * hero (opaque) → white@glassAlpha → black@scrimAlpha → text
 * ```
 * The effective background for text contrast is therefore:
 * `composite(composite(hero, White, glassAlpha), Black, scrimAlpha)`.
 *
 * ## Scope
 * These functions are intentionally free of Android-framework types in their
 * computation so they can run on a plain JVM (unit tests without Robolectric).
 * The only framework type accepted as input is [Color] — its RGB component
 * accessors work on any JVM.
 */

// ---------------------------------------------------------------------------
// Core WCAG functions
// ---------------------------------------------------------------------------

/**
 * Computes the WCAG 2.1 relative luminance of [color].
 *
 * Algorithm:
 * 1. Extract sRGB components (0..1 range from [Color]).
 * 2. Linearise each channel: `if c ≤ 0.03928 → c/12.92 else ((c+0.055)/1.055)^2.4`.
 * 3. Y = 0.2126·R + 0.7152·G + 0.0722·B.
 *
 * Uses [Color]'s float components directly (0..1); no 8-bit intermediate.
 */
internal fun relativeLuminance(color: Color): Double {
  fun linearize(c: Double): Double =
    if (c <= 0.03928) c / 12.92
    else Math.pow((c + 0.055) / 1.055, 2.4)

  val r = linearize(color.red.toDouble())
  val g = linearize(color.green.toDouble())
  val b = linearize(color.blue.toDouble())
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/**
 * Computes the WCAG 2.1 contrast ratio between [fg] and [bg].
 *
 * Formula: `(Lmax + 0.05) / (Lmin + 0.05)`.
 *
 * Result ≥ 4.5 satisfies WCAG AA for normal text;
 * ≥ 3.0 satisfies WCAG AA for large text / UI components.
 */
internal fun contrastRatio(fg: Color, bg: Color): Double {
  val lFg = relativeLuminance(fg)
  val lBg = relativeLuminance(bg)
  val lMax = maxOf(lFg, lBg)
  val lMin = minOf(lFg, lBg)
  return (lMax + 0.05) / (lMin + 0.05)
}

/**
 * Alpha-composites [fg] over [bg] in **sRGB** space (gamma-encoded).
 *
 * `channel = bg*(1-alpha) + fg*alpha`
 *
 * Linearisation is NOT applied here; it is deferred to [relativeLuminance].
 * This mirrors how Android composites translucent layers on the framebuffer.
 *
 * @param bg    Opaque or previously-composited background color.
 * @param fg    Foreground color to layer on top.
 * @param alpha Opacity of [fg] in the range 0..1.
 */
internal fun composite(bg: Color, fg: Color, alpha: Float): Color {
  val a = alpha.coerceIn(0f, 1f)
  val r = bg.red * (1f - a) + fg.red * a
  val g = bg.green * (1f - a) + fg.green * a
  val b = bg.blue * (1f - a) + fg.blue * a
  return Color(r, g, b)
}

// ---------------------------------------------------------------------------
// Counter-screen specific helpers
// ---------------------------------------------------------------------------

/**
 * Returns the minimum black-scrim alpha that makes white text reach [target] contrast
 * ratio over a glass-cell background composed as:
 *
 * ```
 * composite(composite(hero, White, glassAlpha), Black, scrimAlpha)
 * ```
 *
 * The search steps in increments of 0.01 and returns the first value that satisfies
 * the target. If even `scrimAlpha = 1.0` does not satisfy [target], returns `1.0`.
 *
 * @param hero       Opaque hero color of the event palette (light theme).
 * @param glassAlpha Alpha of the white glass fill (e.g. 0.16f for API 31+, 0.26f for API 29–30).
 * @param target     Minimum required contrast ratio (default 4.5 for WCAG AA).
 */
fun scrimAlphaFor(hero: Color, glassAlpha: Float, target: Double = 4.5): Float {
  val glass = composite(hero, Color.White, glassAlpha)
  for (step in 0..100) {
    val scrim = step / 100f
    val bg = composite(glass, Color.Black, scrim)
    if (contrastRatio(Color.White, bg) >= target) return scrim
  }
  return 1f
}
