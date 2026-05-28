// TODO(arch debt): NotificationPermissionState is app-level state, not a design token.
// Long-term home: :core:common (requires adding compose-runtime dep) or a new :core:ui module.
// Kept in :core:design temporarily to avoid the broader refactor. Don't add more
// non-design types here as precedent.
package com.datecountdown.app.core.design.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Snapshot of the notification-permission UI state.
 *
 * Provided via [LocalNotificationPermissionState] by the root composable in `:app` and
 * consumed by `:feature:list` (banner + menu item) without prop drilling.
 *
 * @param shouldShowBanner `true` when the persistent banner must be visible:
 *   API 33+, permission not granted, and requested at least once.
 * @param triggerRequest Lambda that fires the Activity-level permission launcher.
 */
data class NotificationPermissionState(
  val shouldShowBanner: Boolean,
  val triggerRequest: () -> Unit,
)

/**
 * [androidx.compose.runtime.CompositionLocal] that carries [NotificationPermissionState] through
 * the composition tree.
 *
 * The default value has `shouldShowBanner = false` and a no-op lambda — safe for previews
 * and composables that are not descendants of the root composable.
 */
val LocalNotificationPermissionState = compositionLocalOf {
  NotificationPermissionState(shouldShowBanner = false, triggerRequest = {})
}
