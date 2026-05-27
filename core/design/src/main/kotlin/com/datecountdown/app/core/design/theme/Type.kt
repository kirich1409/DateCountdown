package com.datecountdown.app.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO(#21): Replace FontFamily.Default with the Roboto Flex variable font once
//  issue #21 "custom fonts / font scale" lands. At that point this file gains a
//  FontFamily declaration and all TextStyle entries below reference it.
internal val DateCountdownTypography = Typography(
  bodyLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp,
  ),
)
