import com.android.build.api.dsl.LibraryExtension
import com.datecountdown.buildlogic.configureKotlinAndroid

/**
 * Convention for Android library modules: :data, :core:common, :core:design.
 * Same Kotlin/SDK baseline as the app convention, minus application-only config.
 * AGP 9 built-in Kotlin (the default) provides the Kotlin plugin — not applied explicitly.
 */
plugins {
  id("com.android.library")
}

extensions.configure<LibraryExtension> {
  configureKotlinAndroid(this)
}
