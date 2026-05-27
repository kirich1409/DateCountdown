@file:Suppress("MagicNumber") // Font weight axis values (100-900) are part of the standard type scale.

package com.datecountdown.app.core.design.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.datecountdown.app.core.design.R

// ---------------------------------------------------------------------------
// Roboto Flex variable font (Apache-2.0).
// Source: github.com/googlefonts/roboto-flex — full variable TTF bundled.
//
// The variable font uses the 'wght' axis for weight (100–900) in addition to
// the standard FontWeight matching. Providing explicit FontVariation.Settings
// ensures the runtime honours the correct position on the axis even when
// strong-skipping optimises font loading.
//
// @ExperimentalTextApi is required for the `variationSettings` parameter on
// Font(resId, weight, style, loadingStrategy, variationSettings). The API is
// stable in practice across Compose 1.4+ but the annotation has not yet been
// promoted. Suppress at use-site rather than propagating to every consumer.
// ---------------------------------------------------------------------------

@Suppress("UnusedPrivateMember") // font entries are consumed by FontFamily constructor
@OptIn(ExperimentalTextApi::class)
internal val RobotoFlex: FontFamily = FontFamily(
  Font(
    resId = R.font.roboto_flex,
    weight = FontWeight.Light,
    variationSettings = FontVariation.Settings(FontVariation.weight(300)),
  ),
  Font(
    resId = R.font.roboto_flex,
    weight = FontWeight.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(400)),
  ),
  Font(
    resId = R.font.roboto_flex,
    weight = FontWeight.Medium,
    variationSettings = FontVariation.Settings(FontVariation.weight(500)),
  ),
  Font(
    resId = R.font.roboto_flex,
    weight = FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(600)),
  ),
  Font(
    resId = R.font.roboto_flex,
    weight = FontWeight.Bold,
    variationSettings = FontVariation.Settings(FontVariation.weight(700)),
  ),
  Font(
    resId = R.font.roboto_flex,
    weight = FontWeight.ExtraBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(800)),
  ),
  Font(
    resId = R.font.roboto_flex,
    weight = FontWeight.Black,
    variationSettings = FontVariation.Settings(FontVariation.weight(900)),
  ),
)

// ---------------------------------------------------------------------------
// Material Symbols Rounded variable font (Apache-2.0).
// Source: github.com/google/material-design-icons — subset to the 16 EventIcon
// glyph codepoints. Icons are rendered by codepoint (not text ligature) via
// the EventSymbol composable in EventSymbol.kt.
//
// Note: the variable font axes (FILL, GRAD, opsz, wght) produce a subset that
// is still ~11 MB even for 16 glyphs, because the gvar variation data per
// glyph is large. The font is bundled in the APK; a further optimisation
// (tofu-strip or subsetting by codepoint range) can be applied in a follow-up.
// ---------------------------------------------------------------------------

internal val MaterialSymbolsRounded: FontFamily = FontFamily(
  Font(resId = R.font.material_symbols_rounded),
)
