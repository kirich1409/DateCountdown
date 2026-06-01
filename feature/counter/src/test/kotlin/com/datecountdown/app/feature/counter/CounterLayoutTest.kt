package com.datecountdown.app.feature.counter

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterLayoutTest {

  private fun assertFontSp(input: Dp, expected: Float, delta: Float = 0.5f) {
    val result = adaptivePrimaryMaxFontSize(input).value
    assertEquals("adaptivePrimaryMaxFontSize($input)", expected, result, delta)
  }

  @Test fun phone_portrait_returns_base_cap() = assertFontSp(input = 360.dp, expected = 96f)

  @Test fun ramp_start_returns_base_cap() = assertFontSp(input = 600.dp, expected = 96f)

  @Test fun mid_ramp_returns_interpolated() = assertFontSp(input = 800.dp, expected = 114.8f)

  @Test fun upper_breakpoint_returns_large_cap() = assertFontSp(input = 1280.dp, expected = 160f)

  @Test fun beyond_upper_breakpoint_is_clamped() = assertFontSp(input = 2000.dp, expected = 160f)

  @Test
  fun unspecified_returns_base_cap() {
    val result = adaptivePrimaryMaxFontSize(Dp.Unspecified).value
    assertEquals("adaptivePrimaryMaxFontSize(Dp.Unspecified)", 96f, result, 0.01f)
  }
}
