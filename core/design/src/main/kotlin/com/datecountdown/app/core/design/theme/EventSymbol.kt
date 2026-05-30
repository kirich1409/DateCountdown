package com.datecountdown.app.core.design.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders a Material Symbols Rounded glyph for the given [EventIcon].
 *
 * Icons are encoded as Unicode codepoints in the bundled [MaterialSymbolsRounded] variable font
 * (res/font/material_symbols_rounded.ttf). Each [EventIcon] carries its codepoint via
 * [EventIcon.codepoint]; this composable maps that to a single-character string and renders it
 * with the icon font.
 *
 * ## Accessibility
 * The icon is purely visual; it is described via [contentDescription] in the semantics tree so
 * screen readers can announce the icon's meaning. Pass `null` to suppress the description when the
 * icon is already described by a sibling element (e.g. an adjacent label).
 *
 * ## Feature-module usage
 * ```kotlin
 * EventSymbol(
 *   icon = event.icon,
 *   contentDescription = stringResource(R.string.icon_description, stringResource(designIcon.labelRes)),
 * )
 * ```
 *
 * @param icon The [EventIcon] to render.
 * @param modifier Modifier applied to the underlying [Text] composable.
 * @param size Font size (= visual icon size). Defaults to 24.sp, matching the M3 icon grid.
 * @param tint Color of the glyph. Defaults to [MaterialTheme.colorScheme.onSurface].
 * @param contentDescription Accessibility description for screen readers. Pass `null` if the icon
 *   is decorative and a sibling element already describes it.
 */
@Composable
fun EventSymbol(
  icon: EventIcon,
  modifier: Modifier = Modifier,
  size: TextUnit = 24.sp,
  tint: Color = Color.Unspecified,
  contentDescription: String? = null,
) {
  val semanticsModifier = if (contentDescription != null) {
    modifier.semantics { this.contentDescription = contentDescription }
  } else {
    modifier
  }
  Text(
    text = String(Character.toChars(icon.codepoint)),
    modifier = semanticsModifier,
    fontFamily = MaterialSymbolsRounded,
    fontSize = size,
    color = tint,
    // Disable line height adjustments that can clip icon glyphs at small sizes.
    lineHeight = size,
    maxLines = 1,
  )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Suppress("UnusedPrivateMember")
@Preview(name = "EventSymbol — all 16 icons", showBackground = true)
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EventSymbolAllIconsPreview() {
  DateCountdownTheme {
    Surface(color = MaterialTheme.colorScheme.surface) {
      FlowRow(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        EventIcon.entries.forEach { icon ->
          EventSymbol(
            icon = icon,
            size = 32.sp,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null,
          )
        }
      }
    }
  }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "EventSymbol — sizes", showBackground = true)
@Composable
private fun EventSymbolSizesPreview() {
  DateCountdownTheme {
    Surface(color = MaterialTheme.colorScheme.surface) {
      androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(16.sp, 24.sp, 32.sp, 48.sp).forEach { size ->
          EventSymbol(
            icon = EventIcon.CELEBRATION,
            size = size,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null,
          )
        }
      }
    }
  }
}
