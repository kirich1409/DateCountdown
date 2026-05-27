@file:Suppress("MagicNumber")

package com.datecountdown.app.core.design.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Fallback color tokens — sourced from swarm-report/design-source/project/m3-app.jsx
//
// These are used on API 29-30 where Material You dynamic color is unavailable,
// and whenever the system does not provide a dynamic palette.
//
// Slot names follow the M3 ColorScheme property names.
// Missing JSX slots (onSecondary, onTertiary, onError, surfaceVariant, surfaceTint,
// inversePrimary, inverseSurface, inverseOnSurface) use M3 lightColorScheme()/
// darkColorScheme() defaults — no invented hex values here.
// ---------------------------------------------------------------------------

// Light scheme tokens
internal val LightPrimary = Color(0xFF006A60)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFF9EF2E4)
internal val LightOnPrimaryContainer = Color(0xFF00201C)

internal val LightSecondary = Color(0xFF4A635F)
internal val LightSecondaryContainer = Color(0xFFCCE8E2)
internal val LightOnSecondaryContainer = Color(0xFF051F1C)

internal val LightTertiary = Color(0xFF456179)
internal val LightTertiaryContainer = Color(0xFFCBE6FF)
internal val LightOnTertiaryContainer = Color(0xFF001D34)

internal val LightError = Color(0xFFBA1A1A)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)

internal val LightSurface = Color(0xFFF6FAF6)
internal val LightSurfaceDim = Color(0xFFD8DCD7)
internal val LightSurfaceBright = Color(0xFFF6FAF6)
internal val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
internal val LightSurfaceContainerLow = Color(0xFFF1F4F0)
internal val LightSurfaceContainer = Color(0xFFEBEEEA)
internal val LightSurfaceContainerHigh = Color(0xFFE5E8E4)
internal val LightSurfaceContainerHighest = Color(0xFFDFE3DF)
internal val LightOnSurface = Color(0xFF181D1A)
internal val LightOnSurfaceVariant = Color(0xFF3F4843)

internal val LightOutline = Color(0xFF6F7973)
internal val LightOutlineVariant = Color(0xFFBFC9C2)

internal val LightScrim = Color(0xFF000000)

// Dark scheme tokens
internal val DarkPrimary = Color(0xFF82D5C8)
internal val DarkOnPrimary = Color(0xFF003731)
internal val DarkPrimaryContainer = Color(0xFF005048)
internal val DarkOnPrimaryContainer = Color(0xFF9EF2E4)

internal val DarkSecondary = Color(0xFFB0CCC6)
internal val DarkSecondaryContainer = Color(0xFF324B47)
internal val DarkOnSecondaryContainer = Color(0xFFCCE8E2)

internal val DarkTertiary = Color(0xFFAFCAE6)
internal val DarkTertiaryContainer = Color(0xFF2C4961)
internal val DarkOnTertiaryContainer = Color(0xFFCBE6FF)

internal val DarkError = Color(0xFFFFB4AB)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)

internal val DarkSurface = Color(0xFF0F1411)
internal val DarkSurfaceDim = Color(0xFF0F1411)
internal val DarkSurfaceBright = Color(0xFF353A37)
internal val DarkSurfaceContainerLowest = Color(0xFF0A0F0C)
internal val DarkSurfaceContainerLow = Color(0xFF181D1A)
internal val DarkSurfaceContainer = Color(0xFF1C211E)
internal val DarkSurfaceContainerHigh = Color(0xFF262B28)
internal val DarkSurfaceContainerHighest = Color(0xFF313633)
internal val DarkOnSurface = Color(0xFFDEE3DF)
internal val DarkOnSurfaceVariant = Color(0xFFBFC9C2)

internal val DarkOutline = Color(0xFF89938D)
internal val DarkOutlineVariant = Color(0xFF3F4843)

internal val DarkScrim = Color(0xFF000000)
