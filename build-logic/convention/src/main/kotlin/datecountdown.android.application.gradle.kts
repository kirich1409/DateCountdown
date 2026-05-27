import com.android.build.api.dsl.ApplicationExtension
import com.datecountdown.buildlogic.configureKotlinAndroid

/**
 * Convention for the :app module — the only com.android.application target.
 * Applies AGP only; AGP 9's built-in Kotlin (the default) wires the Kotlin plugin at the
 * AGP-bundled version, so org.jetbrains.kotlin.android must NOT be applied explicitly.
 */
plugins {
  id("com.android.application")
}

extensions.configure<ApplicationExtension> {
  configureKotlinAndroid(this)
  defaultConfig.targetSdk = 36
  androidResources {
    generateLocaleConfig = true
  }
}
