plugins {
  `kotlin-dsl`
}

group = "com.datecountdown.buildlogic"

java {
  // Match the project toolchain (JDK 17) so precompiled script plugins target the same bytecode.
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  // AGP via compileOnly so build-logic exposes the AGP DSL to convention plugins without
  // leaking the plugin onto the classpath of consumer builds (issue #14 requirement).
  compileOnly(libs.android.gradlePlugin)
  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.compose.gradlePlugin)
  compileOnly(libs.ksp.gradlePlugin)
  // detekt must be implementation (not compileOnly) because datecountdown.detekt convention plugin
  // applies id("io.gitlab.arturbosch.detekt") at runtime — consumers don't declare detekt directly.
  implementation(libs.detekt.gradlePlugin)

  // Expose the version catalog accessor (LibrariesForLibs) inside precompiled script plugins.
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
