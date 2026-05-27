import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.report.ReportMergeTask

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

// Register the merge task once on the root project; subsequent modules reuse the same instance.
val reportMerge = rootProject.tasks.maybeCreate("detektReportMerge", ReportMergeTask::class.java).apply {
  output.set(rootProject.layout.buildDirectory.file("reports/detekt/merged.sarif"))
}

tasks.withType<Detekt>().configureEach {
  // Enable SARIF output on every module's detekt task; required for the merge step.
  reports.sarif.required.set(true)
  // Wire each module's SARIF file into the merge task lazily (inside configureEach,
  // not outside, to avoid eager task realization).
  finalizedBy(reportMerge)
  reportMerge.input.from(sarifReportFile)
}

dependencies {
  "detektPlugins"(libs.detekt.rules.compose)
}
