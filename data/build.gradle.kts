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
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.datetime)
}
