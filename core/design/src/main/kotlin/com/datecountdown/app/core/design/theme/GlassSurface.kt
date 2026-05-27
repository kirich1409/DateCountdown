package com.datecountdown.app.core.design.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

// ---------------------------------------------------------------------------
// Alpha tokens for the translucent glass fill.
//
// Design source: "background: rgba(255,255,255,0.16), backdropFilter: blur(8px)"
// CSS backdrop-filter blurs the content *behind* the element; Compose's
// Modifier.blur blurs the element's own drawing (including its children).
// Applying Modifier.blur to a glass cell that contains countdown numbers
// would blur the numbers — the opposite of the intent.
//
// Over the solid hero-color background used in the MVP counter screen the
// visual difference between a true backdrop blur and a translucent fill is
// imperceptible, so we approximate with translucent fill only.
//
// On API 29-30 we raise the alpha slightly to compensate for the absence of
// blur (the translucent fill has to carry the full "glass" illusion alone).
//
// KDoc note: if ever used over a varied/multi-color background and true
// backdrop blur is required, consider the Haze library (third-party,
// requires dependency approval) or AGSL RuntimeShader on API 33+.
// ---------------------------------------------------------------------------

/** Glass fill alpha on API 31+. Matches the JSX `rgba(255,255,255,0.16)`. */
private const val GLASS_ALPHA_DEFAULT = 0.16f

/**
 * Slightly elevated alpha for API 29-30, where no blur is available.
 * Higher opacity compensates so the cell still reads as "glass" without blur.
 */
private const val GLASS_ALPHA_LEGACY = 0.26f

/** Default glass fill color (white at the above alpha). */
private val GlassFillWhite = Color.White

/**
 * A decorative container that approximates CSS `backdrop-filter: blur(8px)` via a
 * translucent fill.
 *
 * ## Accessibility
 * `GlassSurface` is a pure visual primitive. Callers are responsible for semantics —
 * set `contentDescription = null` on decorative usages per AC-CL-17.
 *
 * ## SDK behavior
 * - **API 31+**: translucent fill at alpha 0.16 (matches the JSX `rgba(255,255,255,0.16)`).
 * - **API 29-30**: translucent fill at alpha 0.26 (elevated to compensate for the absence
 *   of blur, so the cell still reads as "glass").
 *
 * ## Parameters
 * @param modifier Applied to the container [Box].
 * @param shape Clipping shape for the glass surface.
 * @param fillColor Base fill color; blended with the SDK-gated alpha.
 * @param scrimColor Optional darkening scrim layered on top of the fill.
 *   Pass a semi-transparent [Color.Black] value (e.g. `Color.Black.copy(alpha = 0.18f)`)
 *   to ensure contrast compliance when the hero background is light (AC-CL-18).
 * @param content Content drawn inside the glass container.
 */
@Composable
fun GlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = RectangleShape,
  fillColor: Color = GlassFillWhite,
  scrimColor: Color = Color.Transparent,
  content: @Composable BoxScope.() -> Unit,
) {
  val glassAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    GLASS_ALPHA_DEFAULT
  } else {
    GLASS_ALPHA_LEGACY
  }
  Box(
    modifier = modifier
      .background(color = fillColor.copy(alpha = glassAlpha), shape = shape)
      .then(
        if (scrimColor != Color.Transparent) {
          Modifier.background(color = scrimColor, shape = shape)
        } else {
          Modifier
        },
      ),
    content = content,
  )
}
