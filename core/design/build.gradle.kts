plugins {
  id("datecountdown.android.library")
  id("datecountdown.android.compose")
  id("datecountdown.detekt")
}

android {
  namespace = "com.datecountdown.app.core.design"
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)

  debugImplementation(libs.androidx.compose.ui.tooling)
}
