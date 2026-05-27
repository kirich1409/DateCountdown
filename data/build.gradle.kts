plugins {
  id("datecountdown.android.library")
  alias(libs.plugins.ksp)
  id("datecountdown.detekt")
}

android {
  namespace = "com.datecountdown.app.data"
}

dependencies {
  implementation(project(":domain"))
}
