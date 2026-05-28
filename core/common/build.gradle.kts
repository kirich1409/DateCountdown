plugins {
  id("datecountdown.android.library")
  id("datecountdown.detekt")
}

android {
  namespace = "com.datecountdown.app.core.common"

  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
}

dependencies {
  implementation(project(":domain"))

  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
}
