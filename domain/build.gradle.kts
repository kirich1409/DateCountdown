plugins {
  id("datecountdown.kotlin.library")
  id("datecountdown.detekt")
}

dependencies {
  implementation(libs.kotlinx.datetime)
}
