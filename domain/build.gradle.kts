plugins {
  id("datecountdown.kotlin.library")
  id("datecountdown.detekt")
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.datetime)

  testImplementation(libs.junit)
}
