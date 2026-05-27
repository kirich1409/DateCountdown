import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

/**
 * Detekt convention: static analysis + the io.nlopez compose-rules ruleset.
 * Applied to production modules (not to build-logic itself).
 */
plugins {
  id("io.gitlab.arturbosch.detekt")
}

private val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

extensions.configure<DetektExtension> {
  // Shared config + baseline live at the repo root so every module reports consistently.
  config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
  buildUponDefaultConfig = true
  parallel = true
}

tasks.withType<Detekt>().configureEach {
  // SARIF output per module; CI collects and uploads all *.sarif files.
  reports.sarif.required.set(true)
}

dependencies {
  "detektPlugins"(libs.detekt.rules.compose)
}
