plugins {
  id("datecountdown.android.application")
  id("datecountdown.android.compose")
  id("datecountdown.detekt")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

android {
  namespace = "com.datecountdown.app"

  defaultConfig {
    applicationId = "com.datecountdown.app"
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Metro DI runtime — KSP-generated graph implementation
  implementation(libs.metro.runtime)

  // Decompose: navigation + component context. api() from feature modules via android-feature
  // convention is transitive to :app, but explicit here for RootComponent which lives in :app.
  implementation(libs.decompose)
  implementation(libs.decompose.extensions.compose)

  // Domain layer — EventsRepository interface used in AppGraph accessor and provider return type
  implementation(project(":domain"))
  // Data layer — EventsRepositoryImpl, AppDatabase, EventDao
  implementation(project(":data"))
  // Room runtime needed here to call Room.databaseBuilder in AppGraph
  implementation(libs.androidx.room.runtime)
  // DataStore needed here to call PreferenceDataStoreFactory.create in AppGraph
  implementation(libs.androidx.datastore.preferences)

  // MVIKotlin — StoreFactory is provided in AppGraph; DefaultStoreFactory in mvikotlin-main
  implementation(libs.mvikotlin)
  implementation(libs.mvikotlin.main)

  // Design system — theme, colors, typography
  implementation(project(":core:design"))

  // Feature modules — :app instantiates their component interfaces in RootComponent
  implementation(project(":feature:list"))
  implementation(project(":feature:counter"))
  implementation(project(":feature:edit"))

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  testImplementation(libs.junit)

  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
