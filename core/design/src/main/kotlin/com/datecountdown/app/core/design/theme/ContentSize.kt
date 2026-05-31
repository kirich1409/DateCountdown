package com.datecountdown.app.core.design.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Content-shaped layout caps for large-screen adaptivity (epic #185).
 * Named by the content they constrain, NOT by device class — values are chosen so they are
 * a no-op on phone widths (the cap only engages once the window is wider than a phone).
 */
object ContentSize {
  /** Min cell width for the adaptive event grid. 135dp keeps 2 columns even at 320dp (minSdk 29). */
  val GridCardMin: Dp = 135.dp
  /** Max width of the grid container on ultra-wide windows so the grid does not stretch unbounded. */
  val GridContainerMax: Dp = 1200.dp
  /** Max width of the counter content column; hero background stays edge-to-edge behind it. */
  val CounterColumnMax: Dp = 520.dp
  /** Max width of a readable text block (~60–80 chars) so lines do not run full-width on tablets. */
  val ReadableTextMax: Dp = 600.dp
  /** Max width of secondary buttons / CTAs so they do not stretch full-width on large screens. */
  val ButtonMax: Dp = 360.dp
}
