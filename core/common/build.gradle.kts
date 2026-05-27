plugins {
  id("datecountdown.android.library")
  id("datecountdown.detekt")
}

android {
  namespace = "com.datecountdown.app.core.common"
}

dependencies {
  implementation(project(":domain"))
}
