plugins {
  id("datecountdown.android.feature")
  id("datecountdown.detekt")
}

android {
  namespace = "com.datecountdown.app.feature.edit"
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  implementation(libs.androidx.compose.ui)
  implementation(project(":domain"))
  implementation(project(":core:common"))
  implementation(project(":core:design"))
}
