plugins {
  id("datecountdown.android.feature")
  id("datecountdown.detekt")
}

android {
  namespace = "com.datecountdown.app.feature.counter"

  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.ui.tooling.preview)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.activity.compose)
  implementation(project(":domain"))
  implementation(project(":core:common"))
  implementation(project(":core:design"))

  // kotlinx-datetime is an implementation dep in :domain and therefore not visible here.
  // The Store uses Instant and Clock directly, so we declare it explicitly.
  implementation(libs.kotlinx.datetime)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)
  testImplementation(platform(libs.androidx.compose.bom))
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.compose.ui.test.manifest)
  testImplementation(libs.robolectric)
}
