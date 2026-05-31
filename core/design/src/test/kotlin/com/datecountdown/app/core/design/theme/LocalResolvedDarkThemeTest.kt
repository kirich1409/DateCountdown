package com.datecountdown.app.core.design.theme

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that [DateCountdownTheme] provides [LocalResolvedDarkTheme] according to [ThemeMode],
 * not the raw system dark setting (fix #169).
 *
 * Robolectric's default system configuration is light (uiMode = 0), so:
 * - [ThemeMode.DARK] must provide `true` even though the system is light.
 * - [ThemeMode.LIGHT] must provide `false`.
 * - [ThemeMode.SYSTEM] must provide `false` (follows the Robolectric light default).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
internal class LocalResolvedDarkThemeTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `ThemeMode DARK provides true on a light system`() {
    var resolved = false
    composeRule.setContent {
      DateCountdownTheme(themeMode = ThemeMode.DARK) {
        resolved = LocalResolvedDarkTheme.current
      }
    }
    assertTrue(
      "ThemeMode.DARK must expose LocalResolvedDarkTheme=true regardless of system setting",
      resolved,
    )
  }

  @Test
  fun `ThemeMode LIGHT provides false on a light system`() {
    var resolved = true
    composeRule.setContent {
      DateCountdownTheme(themeMode = ThemeMode.LIGHT) {
        resolved = LocalResolvedDarkTheme.current
      }
    }
    assertFalse(
      "ThemeMode.LIGHT must expose LocalResolvedDarkTheme=false regardless of system setting",
      resolved,
    )
  }

  @Test
  fun `ThemeMode SYSTEM provides false when system is light`() {
    // Robolectric default uiMode has no night flag set — system is light.
    var resolved = true
    composeRule.setContent {
      DateCountdownTheme(themeMode = ThemeMode.SYSTEM) {
        resolved = LocalResolvedDarkTheme.current
      }
    }
    assertFalse(
      "ThemeMode.SYSTEM on a light system must expose LocalResolvedDarkTheme=false",
      resolved,
    )
  }
}
