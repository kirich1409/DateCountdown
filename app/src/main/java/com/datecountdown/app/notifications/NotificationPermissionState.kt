package com.datecountdown.app.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * The POST_NOTIFICATIONS permission string, introduced in Android 13 (API 33).
 *
 * Defined as a constant here so both [com.datecountdown.app.navigation.RootContent]
 * (launcher registration) and the permission-check helper share a single source of truth.
 */
internal const val POST_NOTIFICATIONS_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

/**
 * Returns `true` when a POST_NOTIFICATIONS runtime permission dialog is required.
 *
 * POST_NOTIFICATIONS was introduced in API 33 (Android 13 / Tiramisu). On API ≤ 32 the
 * system grants the permission implicitly — no dialog is needed or possible.
 */
internal fun isPostNotificationsRequired(): Boolean =
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Checks whether the POST_NOTIFICATIONS permission is currently granted.
 *
 * Returns `true` unconditionally on API ≤ 32 (permission is implicitly granted by the system).
 * On API 33+ performs the standard [ContextCompat.checkSelfPermission] check.
 */
internal fun checkPostNotificationsGranted(context: Context): Boolean =
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
    true
  } else {
    ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS_PERMISSION) ==
      PackageManager.PERMISSION_GRANTED
  }
