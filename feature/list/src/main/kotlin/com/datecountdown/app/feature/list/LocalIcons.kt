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

/**
 * Local ImageVector for the Material "schedule" icon (clock face, 24×24 viewport).
 * Used as the leading icon on the "Soon" filter chip (#144).
 */
internal val ScheduleIcon: ImageVector by lazy {
  // Path: Material "schedule" filled 24dp.
  val path = "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99" +
    " 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zM12.5 7H11v6l5.25 3.15" +
    ".75-1.23-4.5-2.67z"
  ImageVector.Builder(
    name = "Schedule",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).addPath(
    pathData = addPathNodes(path),
    fill = SolidColor(Color.Black),
  ).build()
}

/**
 * Local ImageVector for the Material "calendar_today" icon (24×24 viewport).
 * Used as the leading icon on the "This month" filter chip (#144).
 */
internal val CalendarMonthIcon: ImageVector by lazy {
  // Path: Material "calendar_today" filled 24dp.
  val path = "M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14" +
    "c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zm0-12H5V6h14v2z"
  ImageVector.Builder(
    name = "CalendarMonth",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).addPath(
    pathData = addPathNodes(path),
    fill = SolidColor(Color.Black),
  ).build()
}

/**
 * Local ImageVector for the Material "history" icon (24×24 viewport).
 * Used as the leading icon on the "Past" filter chip (#144).
 */
internal val HistoryIcon: ImageVector by lazy {
  // Path: Material "history" filled 24dp.
  val path = "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7" +
    " 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97" +
    " 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"
  ImageVector.Builder(
    name = "History",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).addPath(
    pathData = addPathNodes(path),
    fill = SolidColor(Color.Black),
  ).build()
}
