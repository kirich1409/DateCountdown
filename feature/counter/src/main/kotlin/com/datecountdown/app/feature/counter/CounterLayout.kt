@file:Suppress("MagicNumber")

package com.datecountdown.app.feature.counter

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datecountdown.app.core.design.theme.ContentSize

// Floor/ceiling for primary-value autoSize. maxLines=1 + softWrap=false force single-line layout;
// autoSize then shrinks the font until the text fits rather than wrapping to a second line.
// 24sp floor: extends coverage to higher fontScale (1.5–2.0) and wider device widths,
// preventing clipping of values like "245 дней". At fontScale 1.0, autoSize still chooses
// the maximum font (up to the adaptive cap), so readability is unaffected.
internal val primaryValueMinFontSize = 24.sp

// Base cap used at phone widths (≤600dp). The effective max is raised adaptively on larger
// screens via adaptivePrimaryMaxFontSize — see #190.
internal val primaryValueMaxFontSize = 96.sp

// Upper ceiling for the adaptive hero-number cap on very wide screens (≥1280dp).
internal val primaryValueMaxFontSizeLarge = 160.sp

// Upper breakpoint (dp) at which the adaptive font size reaches its ceiling (primaryValueMaxFontSizeLarge).
private const val ADAPTIVE_FONT_UPPER_BREAKPOINT_DP = 1280f

/**
 * #190: derives the adaptive primary-value max font size from the available content width.
 *
 * Formula: linearly ramp from 96sp at ≤600dp to 160sp at ≥1280dp.
 *   fraction = ((width - 600) / (1280 - 600)).coerceIn(0, 1)
 *   cap = 96 + 64 * fraction (in sp)
 *
 * Key values:
 *   360dp → 96sp (phone portrait, no change)
 *   600dp → 96sp (threshold, ramp starts here — equals ContentSize.ReadableTextMax)
 *   800dp → ≈115sp
 *   1280dp → 160sp
 *
 * The width argument is the full BoxWithConstraints.maxWidth, measured BEFORE the
 * CounterColumnMax (520dp) cap is applied, so the ramp fires correctly on wide windows.
 * The 24sp floor (primaryValueMinFontSize) is unaffected.
 *
 * Guard: returns the base cap (96sp) for [Dp.Unspecified] or NaN inputs.
 */
internal fun adaptivePrimaryMaxFontSize(maxWidth: Dp): TextUnit {
  if (maxWidth == Dp.Unspecified || maxWidth.value.isNaN()) return primaryValueMaxFontSize
  val rampStart = ContentSize.ReadableTextMax.value  // 600f
  val fraction = ((maxWidth.value - rampStart) / (ADAPTIVE_FONT_UPPER_BREAKPOINT_DP - rampStart))
    .coerceIn(0f, 1f)
  val baseSp = primaryValueMaxFontSize.value
  val rangeSp = primaryValueMaxFontSizeLarge.value - baseSp
  return (baseSp + rangeSp * fraction).sp
}
