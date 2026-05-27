package com.datecountdown.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/**
 * Shared Android + Kotlin configuration applied by both the application and library
 * convention plugins: SDK levels, Java 17 source/target.
 *
 * Under AGP 9 with the built-in Kotlin plugin, jvmTarget automatically inherits
 * compileOptions.targetCompatibility — no explicit KotlinAndroidExtension setup needed.
 */
// AGP 9 dropped the type parameters from CommonExtension (AGP 8 had CommonExtension<*,*,*,*,*,*>);
// the bare interface now carries compileSdk/defaultConfig/compileOptions directly.
internal fun Project.configureKotlinAndroid(
  commonExtension: CommonExtension,
) {
  commonExtension.compileSdk = 36
  commonExtension.defaultConfig.minSdk = 29
  commonExtension.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
  commonExtension.compileOptions.targetCompatibility = JavaVersion.VERSION_17
}
