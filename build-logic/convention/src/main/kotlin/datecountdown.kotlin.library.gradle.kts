import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Convention for :domain — pure Kotlin/JVM, no Android plugin. Compile-time guarantee that
 * the domain stays free of Android types (module-structure.md R1).
 */
plugins {
  id("org.jetbrains.kotlin.jvm")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  jvmToolchain(17)
  compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}
