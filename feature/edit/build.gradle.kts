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

  // kotlinx-datetime is an implementation dep in :domain and therefore not visible here.
  // AddEditStore uses Instant and Clock directly, so we declare it explicitly.
  implementation(libs.kotlinx.datetime)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)
}
