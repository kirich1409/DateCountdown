package com.datecountdown.app.core.design.theme

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

// Each "H%" and "V%" below is a fraction in [0, 1].
// Mapping from CSS border-radius "H1% H2% H3% H4% / V1% V2% V3% V4%":
//   corner order: top-left (TL), top-right (TR), bottom-right (BR), bottom-left (BL)
//   per-corner ellipse rx = H * width,  ry = V * height.
// The CSS percentage is stored as a float fraction (e.g. 36% → 0.36f).

private const val V1_TL_H = 0.36f
private const val V1_TL_V = 0.50f
private const val V1_TR_H = 0.64f
private const val V1_TR_V = 0.50f
private const val V1_BR_H = 0.64f
private const val V1_BR_V = 0.50f
private const val V1_BL_H = 0.36f
private const val V1_BL_V = 0.50f

private const val V2_TL_H = 0.38f
private const val V2_TL_V = 0.50f
private const val V2_TR_H = 0.62f
private const val V2_TR_V = 0.50f
private const val V2_BR_H = 0.71f
private const val V2_BR_V = 0.50f
private const val V2_BL_H = 0.29f
private const val V2_BL_V = 0.50f

private const val V3_TL_H = 0.50f
private const val V3_TL_V = 0.50f
private const val V3_TR_H = 0.50f
private const val V3_TR_V = 0.50f
private const val V3_BR_H = 0.30f
private const val V3_BR_V = 0.50f
private const val V3_BL_H = 0.70f
private const val V3_BL_V = 0.50f

private const val V4_TL_H = 0.40f
private const val V4_TL_V = 0.50f
private const val V4_TR_H = 0.60f
private const val V4_TR_V = 0.50f
private const val V4_BR_H = 0.60f
private const val V4_BR_V = 0.50f
private const val V4_BL_H = 0.40f
private const val V4_BL_V = 0.50f

/**
 * A [Shape] that produces an organic "blob" outline by mapping CSS-style
 * per-corner elliptical border-radius fractions to a [Path].
 *
 * Each corner is drawn as a 90° elliptical arc with:
 *   `rx = hFraction × width`,  `ry = vFraction × height`
 *
 * The eight parameters follow CSS `border-radius` order:
 * `H1% H2% H3% H4% / V1% V2% V3% V4%` (top-left, top-right, bottom-right, bottom-left).
 * All fractions are in `[0, 1]`.
 *
 * Use one of the four design-system presets via the companion object, or
 * construct directly when a custom shape is needed.
 *
 * ```kotlin
 * Box(
 *   modifier = Modifier
 *     .size(72.dp)
 *     .clip(BlobShape.Variant4)
 *     .background(palette.container)
 * )
 * ```
 */
data class BlobShape(
  val tlH: Float,
  val tlV: Float,
  val trH: Float,
  val trV: Float,
  val brH: Float,
  val brV: Float,
  val blH: Float,
  val blV: Float,
) : Shape {

  // Cache the last computed outline to avoid re-allocating a Path when Size is unchanged.
  @Volatile private var cachedSize: Size? = null
  @Volatile private var cachedPath: Path? = null

  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
  ): Outline {
    val cached = cachedPath
    if (cached != null && cachedSize == size) {
      return Outline.Generic(cached)
    }
    val path = buildPath(size)
    cachedSize = size
    cachedPath = path
    return Outline.Generic(path)
  }

  private fun buildPath(size: Size): Path {
    val w = size.width
    val h = size.height

    // Per-corner ellipse half-axes.
    val tlRx = tlH * w
    val tlRy = tlV * h
    val trRx = trH * w
    val trRy = trV * h
    val brRx = brH * w
    val brRy = brV * h
    val blRx = blH * w
    val blRy = blV * h

    return Path().apply {
      // Start at the point where the top-left arc ends on the top edge.
      moveTo(tlRx, 0f)
      // Top edge → top-right arc start.
      lineTo(w - trRx, 0f)
      // Top-right arc: bounding rect is the 2*trRx × 2*trRy ellipse at the corner.
      arcTo(
        rect = Rect(left = w - 2 * trRx, top = 0f, right = w, bottom = 2 * trRy),
        startAngleDegrees = 270f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
      )
      // Right edge.
      lineTo(w, h - brRy)
      // Bottom-right arc.
      arcTo(
        rect = Rect(left = w - 2 * brRx, top = h - 2 * brRy, right = w, bottom = h),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
      )
      // Bottom edge.
      lineTo(blRx, h)
      // Bottom-left arc.
      arcTo(
        rect = Rect(left = 0f, top = h - 2 * blRy, right = 2 * blRx, bottom = h),
        startAngleDegrees = 90f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
      )
      // Left edge.
      lineTo(0f, tlRy)
      // Top-left arc.
      arcTo(
        rect = Rect(left = 0f, top = 0f, right = 2 * tlRx, bottom = 2 * tlRy),
        startAngleDegrees = 180f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
      )
      close()
    }
  }

  companion object {

    /**
     * CSS: `36% 64% 64% 36% / 50% 50% 50% 50%`
     * Used for the large (132 dp) empty-state hourglass icon on the list screen.
     */
    val Variant1: BlobShape = BlobShape(
      tlH = V1_TL_H, tlV = V1_TL_V,
      trH = V1_TR_H, trV = V1_TR_V,
      brH = V1_BR_H, brV = V1_BR_V,
      blH = V1_BL_H, blV = V1_BL_V,
    )

    /**
     * CSS: `38% 62% 71% 29% / 50% 50% 50% 50%`
     * Used as a decorative background blob on the fullscreen counter (top-right).
     */
    val Variant2: BlobShape = BlobShape(
      tlH = V2_TL_H, tlV = V2_TL_V,
      trH = V2_TR_H, trV = V2_TR_V,
      brH = V2_BR_H, brV = V2_BR_V,
      blH = V2_BL_H, blV = V2_BL_V,
    )

    /**
     * CSS: `50% 50% 30% 70% / 50% 50% 50% 50%`
     * Used as a decorative background blob on the fullscreen counter (bottom-left).
     */
    val Variant3: BlobShape = BlobShape(
      tlH = V3_TL_H, tlV = V3_TL_V,
      trH = V3_TR_H, trV = V3_TR_V,
      brH = V3_BR_H, brV = V3_BR_V,
      blH = V3_BL_H, blV = V3_BL_V,
    )

    /**
     * CSS: `40% 60% 60% 40% / 50% 50% 50% 50%`
     * Used for the 72 dp event icon on list-screen cards and the fullscreen counter header.
     */
    val Variant4: BlobShape = BlobShape(
      tlH = V4_TL_H, tlV = V4_TL_V,
      trH = V4_TR_H, trV = V4_TR_V,
      brH = V4_BR_H, brV = V4_BR_V,
      blH = V4_BL_H, blV = V4_BL_V,
    )
  }
}
