package com.datecountdown.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.datecountdown.app.theme.DateCountdownTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      DateCountdownTheme {
        // Decompose RootComponent will be wired in #18
      }
    }
  }
}
