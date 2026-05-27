package com.datecountdown.app.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// DateCountdown typography uses Roboto Flex for all text roles (AC-TH-8).
// The large countdown counter numbers use displayLarge at ExtraBold weight
// so the variable font's wght axis is exercised at a visually impactful size.
//
// Only the slots present in the original JSX design tokens are explicitly set;
// remaining M3 slots inherit MaterialTheme defaults.
// ---------------------------------------------------------------------------

internal val DateCountdownTypography = Typography(
  // Counter numbers (AC-TH-8) — large, heavy, Roboto Flex at wght=800
  displayLarge = TextStyle(
    fontFamily = RobotoFlex,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 57.sp,
    lineHeight = 64.sp,
    letterSpacing = (-0.25).sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = RobotoFlex,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp,
  ),
)
