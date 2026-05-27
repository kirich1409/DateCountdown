// @Preview-annotated private functions are intentionally unused by production code —
// they are rendered by the Android Studio Preview tool only. The suppression below
// silences the detekt UnusedPrivateMember false-positive for the entire file.
@file:Suppress("UnusedPrivateMember")

package com.datecountdown.app.core.design.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// BlobShape variants
// ---------------------------------------------------------------------------

@Preview(name = "BlobShape variants — light", showBackground = true)
@Composable
private fun BlobVariantsLightPreview() {
  DateCountdownTheme(themeMode = ThemeMode.LIGHT) {
    BlobVariantsSwatch()
  }
}

@Preview(
  name = "BlobShape variants — dark",
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun BlobVariantsDarkPreview() {
  DateCountdownTheme(themeMode = ThemeMode.DARK) {
    BlobVariantsSwatch()
  }
}

@Composable
private fun BlobVariantsSwatch() {
  Surface(color = MaterialTheme.colorScheme.surface) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = "BlobShape variants",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BlobSwatch(
          shape = BlobShape.Variant1,
          label = "V1\n36/64",
          size = 72,
        )
        BlobSwatch(
          shape = BlobShape.Variant2,
          label = "V2\n38/62",
          size = 72,
        )
        BlobSwatch(
          shape = BlobShape.Variant3,
          label = "V3\n50/30",
          size = 72,
        )
        BlobSwatch(
          shape = BlobShape.Variant4,
          label = "V4\n40/60",
          size = 72,
        )
      }
      // Large variant (132 dp) — matches the empty-state hourglass size.
      BlobSwatch(
        shape = BlobShape.Variant1,
        label = "V1 132dp\nempty-state",
        size = 132,
      )
    }
  }
}

@Composable
private fun BlobSwatch(
  shape: BlobShape,
  label: String,
  size: Int,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier = Modifier
        .size(size.dp)
        .clip(shape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}

// ---------------------------------------------------------------------------
// GlassSurface over a hero-color background
// ---------------------------------------------------------------------------

@Preview(name = "GlassSurface — light hero", showBackground = false)
@Composable
private fun GlassSurfaceLightPreview() {
  DateCountdownTheme(themeMode = ThemeMode.LIGHT) {
    GlassPreviewScene(heroColor = Color(0xFF9C4900)) // Orange hero
  }
}

@Preview(
  name = "GlassSurface — dark hero",
  showBackground = false,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GlassSurfaceDarkPreview() {
  DateCountdownTheme(themeMode = ThemeMode.DARK) {
    GlassPreviewScene(heroColor = Color(0xFF562C00)) // Orange dark hero
  }
}

@Preview(
  name = "GlassSurface — scrim variant (contrast, AC-CL-18)",
  showBackground = false,
)
@Composable
private fun GlassSurfaceScrimPreview() {
  DateCountdownTheme(themeMode = ThemeMode.LIGHT) {
    GlassPreviewScene(
      heroColor = Color(0xFFDBC85F), // Amber hero — light, needs scrim
      scrimColor = Color.Black.copy(alpha = 0.18f),
    )
  }
}

@Composable
private fun GlassPreviewScene(
  heroColor: Color,
  scrimColor: Color = Color.Transparent,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(140.dp)
      .background(heroColor)
      .padding(16.dp),
    contentAlignment = Alignment.BottomCenter,
  ) {
    // Simulates the counter bottom row: three glass cells side-by-side.
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      repeat(3) { index ->
        GlassSurface(
          modifier = Modifier
            .weight(1f)
            .height(76.dp),
          shape = MaterialTheme.shapes.extraLarge,
          scrimColor = scrimColor,
        ) {
          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = listOf("15", "24", "07")[index],
              style = MaterialTheme.typography.headlineSmall,
              color = Color.White,
            )
            Text(
              text = listOf("ЧАСОВ", "МИНУТ", "СЕКУНД")[index],
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
            )
          }
        }
      }
    }
  }
}
