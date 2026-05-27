package com.datecountdown.app.core.design.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// @Preview-annotated private functions are intentionally unused by production code —
// they are rendered by the Android Studio Preview tool only. The suppression below
// silences the detekt UnusedPrivateMember false-positive for the entire file.
@Suppress("UnusedPrivateMember")
@Preview(name = "Theme — SYSTEM (light host)", showBackground = true)
@Composable
private fun ThemeSystemLightPreview() {
  DateCountdownTheme(themeMode = ThemeMode.SYSTEM) {
    ThemeSwatchSurface()
  }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Theme — LIGHT explicit", showBackground = true)
@Composable
private fun ThemeLightExplicitPreview() {
  DateCountdownTheme(themeMode = ThemeMode.LIGHT) {
    ThemeSwatchSurface()
  }
}

@Suppress("UnusedPrivateMember")
@Preview(
  name = "Theme — DARK explicit",
  showBackground = true,
  uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ThemeDarkExplicitPreview() {
  DateCountdownTheme(themeMode = ThemeMode.DARK) {
    ThemeSwatchSurface()
  }
}

/** Renders the primary color role swatches so the theme can be visually verified in the IDE. */
@Composable
private fun ThemeSwatchSurface() {
  Surface(color = MaterialTheme.colorScheme.surface) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "DateCountdown Theme",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorSwatch(
          label = "Primary",
          background = MaterialTheme.colorScheme.primary,
          foreground = MaterialTheme.colorScheme.onPrimary,
        )
        ColorSwatch(
          label = "Secondary",
          background = MaterialTheme.colorScheme.secondary,
          foreground = MaterialTheme.colorScheme.onSecondary,
        )
        ColorSwatch(
          label = "Tertiary",
          background = MaterialTheme.colorScheme.tertiary,
          foreground = MaterialTheme.colorScheme.onTertiary,
        )
        ColorSwatch(
          label = "Error",
          background = MaterialTheme.colorScheme.error,
          foreground = MaterialTheme.colorScheme.onError,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorSwatch(
          label = "PrimCont",
          background = MaterialTheme.colorScheme.primaryContainer,
          foreground = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        ColorSwatch(
          label = "SecCont",
          background = MaterialTheme.colorScheme.secondaryContainer,
          foreground = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        ColorSwatch(
          label = "Surface",
          background = MaterialTheme.colorScheme.surface,
          foreground = MaterialTheme.colorScheme.onSurface,
        )
        ColorSwatch(
          label = "SurfCont",
          background = MaterialTheme.colorScheme.surfaceContainer,
          foreground = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun ColorSwatch(
  label: String,
  background: androidx.compose.ui.graphics.Color,
  foreground: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .size(width = 72.dp, height = 56.dp)
      .background(color = background, shape = MaterialTheme.shapes.small),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = foreground,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
    )
  }
}
