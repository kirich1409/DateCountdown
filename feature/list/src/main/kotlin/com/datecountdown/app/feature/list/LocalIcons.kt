@file:Suppress("MagicNumber")

package com.datecountdown.app.feature.list

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Local ImageVector for the hourglass_empty Material symbol (24×24 viewport).
 *
 * Defined here rather than via EventIcon/EventSymbol because the hourglass_empty codepoint is
 * absent from the bundled Material Symbols Rounded font subset. The path data matches the standard
 * Material hourglass_empty 24dp filled silhouette. Additional local icons for #144/#145 will be
 * added to this file.
 *
 * fill = SolidColor(Color.Black) is required so the path has pixels; the caller's tint
 * (via Icon composable) applies a ColorFilter on top and overrides the actual colour.
 */
internal val HourglassIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "HourglassEmpty",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).addPath(
    pathData = addPathNodes("M6 2v6h.01L6 8.01 10 12l-4 4 .01.01H6V22h12v-5.99h-.01L18 16l-4-4 4-3.99-.01-.01H18V2H6z"),
    fill = SolidColor(Color.Black),
  ).build()
}
