package com.datecountdown.app.core.design.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

// ---------------------------------------------------------------------------
// Fallback color schemes (API 29-30, or when dynamic color is unavailable).
// Token source: swarm-report/design-source/project/m3-app.jsx.
//
// M3 ColorScheme slots not present in the JSX design tokens
// (onSecondary, onTertiary, onError, surfaceVariant, surfaceTint,
//  inversePrimary, inverseSurface, inverseOnSurface) are intentionally left
// at their M3 defaults — no invented hex values.
// ---------------------------------------------------------------------------

private val FallbackLightColorScheme = lightColorScheme(
  primary = LightPrimary,
  onPrimary = LightOnPrimary,
  primaryContainer = LightPrimaryContainer,
  onPrimaryContainer = LightOnPrimaryContainer,

  secondary = LightSecondary,
  secondaryContainer = LightSecondaryContainer,
  onSecondaryContainer = LightOnSecondaryContainer,

  tertiary = LightTertiary,
  tertiaryContainer = LightTertiaryContainer,
  onTertiaryContainer = LightOnTertiaryContainer,

  error = LightError,
  errorContainer = LightErrorContainer,
  onErrorContainer = LightOnErrorContainer,

  surface = LightSurface,
  surfaceDim = LightSurfaceDim,
  surfaceBright = LightSurfaceBright,
  surfaceContainerLowest = LightSurfaceContainerLowest,
  surfaceContainerLow = LightSurfaceContainerLow,
  surfaceContainer = LightSurfaceContainer,
  surfaceContainerHigh = LightSurfaceContainerHigh,
  surfaceContainerHighest = LightSurfaceContainerHighest,
  onSurface = LightOnSurface,
  onSurfaceVariant = LightOnSurfaceVariant,

  outline = LightOutline,
  outlineVariant = LightOutlineVariant,

  scrim = LightScrim,
  // background aliases surface in M3's recommended setup
  background = LightSurface,
)

private val FallbackDarkColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = DarkOnPrimary,
  primaryContainer = DarkPrimaryContainer,
  onPrimaryContainer = DarkOnPrimaryContainer,

  secondary = DarkSecondary,
  secondaryContainer = DarkSecondaryContainer,
  onSecondaryContainer = DarkOnSecondaryContainer,

  tertiary = DarkTertiary,
  tertiaryContainer = DarkTertiaryContainer,
  onTertiaryContainer = DarkOnTertiaryContainer,

  error = DarkError,
  errorContainer = DarkErrorContainer,
  onErrorContainer = DarkOnErrorContainer,

  surface = DarkSurface,
  surfaceDim = DarkSurfaceDim,
  surfaceBright = DarkSurfaceBright,
  surfaceContainerLowest = DarkSurfaceContainerLowest,
  surfaceContainerLow = DarkSurfaceContainerLow,
  surfaceContainer = DarkSurfaceContainer,
  surfaceContainerHigh = DarkSurfaceContainerHigh,
  surfaceContainerHighest = DarkSurfaceContainerHighest,
  onSurface = DarkOnSurface,
  onSurfaceVariant = DarkOnSurfaceVariant,

  outline = DarkOutline,
  outlineVariant = DarkOutlineVariant,

  scrim = DarkScrim,
  // background aliases surface in M3's recommended setup
  background = DarkSurface,
)

/**
 * Root theme composable for DateCountdown.
 *
 * @param themeMode Controls which color scheme is applied. The caller supplies this value;
 *   production code will read it from `SettingsRepository` (issue #28). The settings dialog
 *   that lets the user change it is issue #44.
 * @param content The composable content rendered inside the theme.
 *
 * ## Color scheme selection
 * - **API 31+**: Material You dynamic color via [dynamicLightColorScheme] / [dynamicDarkColorScheme].
 * - **API 29-30**: fallback [FallbackLightColorScheme] / [FallbackDarkColorScheme] from the
 *   design tokens in `swarm-report/design-source/project/m3-app.jsx`.
 *
 * ## Edge-to-edge (AC-TH-6)
 * This theme intentionally does NOT configure system-bar colors or call
 * `WindowCompat.setDecorFitsSystemWindows`. That is the Activity's responsibility
 * (`enableEdgeToEdge()` in [MainActivity]). Keeping it out of the theme preserves
 * composability with fullscreen surfaces and bottom-sheet overlays.
 */
@Composable
fun DateCountdownTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    // API 31+ — Material You dynamic color derived from the device wallpaper.
    val context = LocalContext.current
    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  } else {
    // API 29-30 — static fallback palette from the JSX design tokens.
    if (darkTheme) FallbackDarkColorScheme else FallbackLightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = DateCountdownTypography,
  ) {
    CompositionLocalProvider(LocalResolvedDarkTheme provides darkTheme) {
      content()
    }
  }
}
