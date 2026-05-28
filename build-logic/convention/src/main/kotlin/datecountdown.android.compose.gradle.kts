import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Compose convention applied on top of an Android application/library convention wherever
 * Compose UI lives (:app, :core:design, :feature:*). Enables the Compose build feature and
 * the Compose compiler plugin, plus opt-in compiler reports/metrics for recomposition audits.
 */
plugins {
  id("org.jetbrains.kotlin.plugin.compose")
}

// CommonExtension is not a registered extension type — the concrete type is Application/Library.
// Branch on whichever Android plugin is present and enable the compose build feature there.
pluginManager.withPlugin("com.android.application") {
  extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
}
pluginManager.withPlugin("com.android.library") {
  extensions.configure<LibraryExtension> { buildFeatures.compose = true }
}

extensions.configure<ComposeCompilerGradlePluginExtension> {
  val composeReports = layout.buildDirectory.dir("compose-compiler")
  reportsDestination.set(composeReports)
  metricsDestination.set(composeReports)
  stabilityConfigurationFiles.add(
    isolated.rootProject.projectDirectory.file(
      "build-logic/convention/src/main/kotlin/compose-stability-config.conf"
    )
  )
}
