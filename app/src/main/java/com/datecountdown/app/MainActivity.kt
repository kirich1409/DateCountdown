package com.datecountdown.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.datecountdown.app.core.design.theme.DateCountdownTheme
import com.datecountdown.app.core.design.theme.ThemeMode
import com.datecountdown.app.di.AppGraph
import com.datecountdown.app.navigation.RootComponent
import dev.zacsweers.metro.createGraph

/**
 * Composition root for the application.
 *
 * Pattern (per spike 1.0):
 *  1. Metro [AppGraph] is created once here via [createGraph]; its lifetime spans the Activity.
 *  2. [RootComponent] is created with [defaultComponentContext] — this binds Decompose's
 *     lifecycle/state-keeper/back-handler to the Activity (requires [ComponentActivity]).
 *  3. [setContent] renders a minimal placeholder; epic 4 replaces this with the real nav host.
 *
 * TODO(epic 4): replace the placeholder [Text] with the real Decompose nav host composable that
 *  dispatches on [RootComponent.stack] and [RootComponent.editSlot].
 */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Metro root graph — holds all app-scoped dependencies.
    // Real bindings (EventsRepositoryImpl, NotificationScheduler) added in epics 2/3 (#25-27, #29).
    // Currently empty; will be passed into RootComponent constructor when bindings exist.
    @Suppress("UnusedPrivateProperty")
    val graph: AppGraph = createGraph()

    // Decompose root component — binds navigation lifecycle to this Activity.
    val root = RootComponent(componentContext = defaultComponentContext())

    enableEdgeToEdge()
    setContent {
      // TODO(#28/#44): replace ThemeMode.SYSTEM with the DataStore-backed value from
      //  SettingsRepository once issue #28 (settings persistence) lands.
      DateCountdownTheme(themeMode = ThemeMode.SYSTEM) {
        // Placeholder: display the active stack config name so the skeleton is visually verifiable.
        // TODO(epic 4, #35/#39/#41): replace with NavHost that renders feature screens.
        val stack by root.stack.subscribeAsState()
        Text(text = "Active: ${stack.active.configuration}")
      }
    }
  }
}
