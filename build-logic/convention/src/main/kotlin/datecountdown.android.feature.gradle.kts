/**
 * Convention for :feature:* modules. Composite of android-library + compose.
 * Wires shared Decompose (navigation/component lifecycle) and MVIKotlin (MVI store) deps so that
 * every feature module gets them without repeating coordinates. Added in issue #17.
 *
 * Decompose is `api` because ComponentContext / Component types cross module boundaries — :app
 * instantiates feature components and must see these types on its compile classpath.
 * MVIKotlin is `implementation` because Store/State/Intent types are internal to each feature.
 */
plugins {
  id("datecountdown.android.library")
  id("datecountdown.android.compose")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencies {
  api(libs.decompose)
  api(libs.decompose.extensions.compose)
  implementation(libs.mvikotlin)
  implementation(libs.mvikotlin.main)
  implementation(libs.mvikotlin.extensions.coroutines)
}
