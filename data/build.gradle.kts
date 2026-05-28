plugins {
  id("datecountdown.android.library")
  alias(libs.plugins.ksp)
  id("datecountdown.detekt")
}

android {
  namespace = "com.datecountdown.app.data"

  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  implementation(project(":domain"))
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.datetime)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
}
