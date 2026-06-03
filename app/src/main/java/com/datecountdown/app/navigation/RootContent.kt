package com.datecountdown.app.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.datecountdown.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.datecountdown.app.core.design.theme.LocalResolvedDarkTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.datecountdown.app.core.design.theme.ContentSize
import com.datecountdown.app.core.design.theme.LocalNotificationPermissionState
import com.datecountdown.app.core.design.theme.NotificationPermissionState
import com.datecountdown.app.domain.SettingsRepository
import com.datecountdown.app.feature.counter.CounterScreen
import com.datecountdown.app.feature.edit.AddEditScreen
import com.datecountdown.app.feature.list.EventListScreen
import com.datecountdown.app.notifications.POST_NOTIFICATIONS_PERMISSION
import com.datecountdown.app.notifications.checkPostNotificationsGranted
import com.datecountdown.app.notifications.isPostNotificationsRequired
import kotlinx.coroutines.launch

/**
 * Root composable: wires [RootComponent.stack] and [RootComponent.editSlot] into Compose UI.
 *
 * Primary back-stack ([stack]) is rendered via [Children] with a horizontal slide animation.
 * The add/edit slot ([editSlot]) is rendered as a [ModalBottomSheet] overlay on top of the stack.
 *
 * ## POST_NOTIFICATIONS permission (AC-NT-11 / AC-NT-12)
 * The launcher is hosted here (Activity context is required). Permission state is threaded
 * into the composition via [LocalNotificationPermissionState] so descendants (EventListScreen)
 * can read and trigger it without prop drilling or component coupling.
 *
 * Auto-trigger fires once per lifetime (tracked via [SettingsRepository.notificationsPermissionRequested])
 * on the first time the add/edit overlay becomes visible — only on API 33+, only when the
 * permission is not granted, and only when it has never been requested before (AC-NT-11).
 *
 * The persist call ([SettingsRepository.setNotificationsPermissionRequested]) runs inside the
 * launcher result callback so it records the request only after the system dialog is dismissed.
 * Grant/deny outcome is irrelevant for persistence — both outcomes stop the auto-trigger.
 *
 * Note (predictive back): [RootComponent] registers a [com.arkivanov.essenty.backhandler.BackCallback]
 * at [com.arkivanov.essenty.backhandler.BackCallback.PRIORITY_DEFAULT + 1], which takes priority over
 * Decompose's predictive-back animation callback (registered at default priority). The slide animation
 * is therefore used as a non-predictive fallback; predictive-back gesture is still active at the OS
 * level via [android:enableOnBackInvokedCallback="true"] in the manifest.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RootContent(
  root: RootComponent,
  settings: SettingsRepository,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // --- App-wide bar-icon appearance ----------------------------------------------------------
  // WindowCompat.enableEdgeToEdge (MainActivity) makes bars transparent but does not manage icon
  // appearance. This effect provides the default for all screens (list, edit, initial counter
  // frame). The counter screen overrides it per its hero/surface background while shown, and
  // restores this default in onDispose when the user navigates back.
  // LocalResolvedDarkTheme is the single source of truth — it respects the in-app ThemeMode
  // LIGHT/DARK override, unlike isSystemInDarkTheme() which only reads the system setting.
  val view = LocalView.current
  val isDark = LocalResolvedDarkTheme.current
  if (!view.isInEditMode) {
    LaunchedEffect(isDark) {
      val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
      val controller = WindowCompat.getInsetsController(window, view)
      controller.isAppearanceLightStatusBars = !isDark
      controller.isAppearanceLightNavigationBars = !isDark
    }
  }

  // --- Permission state setup ----------------------------------------------------------------

  val requestedOnce by settings.notificationsPermissionRequested
    .collectAsStateWithLifecycle(initialValue = false)

  // ON_RESUME observer: bump a counter each time the Activity resumes so that isGranted
  // is recomputed after the user returns from system Settings (AC-NT-12 banner-dismissal).
  val lifecycleOwner = LocalLifecycleOwner.current
  var resumeTick by remember { mutableStateOf(0) }
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) resumeTick++
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // resumeTick as key ensures recomputation on every resume — including after Settings return.
  val isGranted = remember(resumeTick) { checkPostNotificationsGranted(context) }
  val shouldShowBanner = isPostNotificationsRequired() && requestedOnce && !isGranted

  // Keep a fresh reference to the persist call so the launcher callback is always up to date.
  val currentSetRequested by rememberUpdatedState(
    newValue = suspend { settings.setNotificationsPermissionRequested() }
  )

  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { _ ->
    // Persist regardless of grant/deny — the auto-trigger must not fire again (AC-NT-11).
    // isGranted is re-evaluated from context on the next recomposition; no explicit update needed.
    scope.launch { currentSetRequested() }
  }

  var showRationale by remember { mutableStateOf(false) }

  // triggerRequest keys on launcher + isGranted + requestedOnce so the lambda always captures
  // the latest permission state without going stale between recompositions.
  val triggerRequest: () -> Unit = remember(launcher, isGranted, requestedOnce) {
    {
      val activity = context.findActivity()
      val isPermanentlyDenied = activity != null &&
        requestedOnce && !isGranted &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, POST_NOTIFICATIONS_PERMISSION)
      val shouldShowRationale = activity != null &&
        ActivityCompat.shouldShowRequestPermissionRationale(activity, POST_NOTIFICATIONS_PERMISSION)

      when {
        isPermanentlyDenied -> openAppSettings(context)
        shouldShowRationale -> showRationale = true
        else -> launcher.launch(POST_NOTIFICATIONS_PERMISSION)
      }
    }
  }

  // Auto-trigger: fires once per session when editSlot first becomes non-null.
  // Guards: API 33+, permission not granted, never requested before.
  val editSlot by root.editSlot.subscribeAsState()
  val editSlotNonNull = editSlot.child != null
  val shouldAutoTrigger = editSlotNonNull && isPostNotificationsRequired() && !requestedOnce && !isGranted
  val currentTrigger by rememberUpdatedState(newValue = triggerRequest)
  LaunchedEffect(editSlotNonNull) {
    if (shouldAutoTrigger) {
      currentTrigger()
    }
  }

  // Stable state object: recreated only when shouldShowBanner changes (triggerRequest is stable
  // across recompositions because launcher identity does not change after registration).
  val permissionState = remember(shouldShowBanner) {
    NotificationPermissionState(
      shouldShowBanner = shouldShowBanner,
      triggerRequest = triggerRequest,
    )
  }

  // --- UI ------------------------------------------------------------------------------------

  if (showRationale) {
    PermissionRationaleDialog(
      onConfirm = {
        showRationale = false
        launcher.launch(POST_NOTIFICATIONS_PERMISSION)
      },
      onDismiss = { showRationale = false },
    )
  }

  CompositionLocalProvider(LocalNotificationPermissionState provides permissionState) {
    Children(
      stack = root.stack,
      modifier = modifier,
      animation = stackAnimation(slide()),
    ) { child ->
      when (val instance = child.instance) {
        is RootComponent.Child.ListChild -> EventListScreen(component = instance.component)
        is RootComponent.Child.CounterChild -> CounterScreen(component = instance.component)
      }
    }

    val editChild = editSlot.child?.instance
    if (editChild != null) {
      ModalBottomSheet(
        onDismissRequest = {
          // AC-AE-10: route through the component so unsaved-changes confirmation dialog can
          // intercept the dismiss before the slot is closed.
          editChild.component.onDismissRequest()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = ContentSize.ReadableTextMax,
        // Decorative pill — no accessibility node (TalkBack must not announce it).
        // M3 default DragHandle announces itself as "drag handle", which is noise for a
        // decorative element.
        dragHandle = {
          Box(
            modifier = Modifier
              .padding(vertical = 12.dp)
              .size(width = 32.dp, height = 4.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(MaterialTheme.colorScheme.outlineVariant),
          )
        },
      ) {
        AddEditScreen(component = editChild.component)
      }
    }
  }
}

/**
 * Dialog explaining why POST_NOTIFICATIONS is needed (AC-NT-11 rationale step).
 *
 * Shown when [ActivityCompat.shouldShowRequestPermissionRationale] returns `true`.
 * Confirms the user-intent before re-launching the system permission dialog.
 */
@Composable
private fun PermissionRationaleDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.permission_rationale_title)) },
    text = { Text(stringResource(R.string.permission_rationale_message)) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(R.string.permission_rationale_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.permission_rationale_cancel))
      }
    },
  )
}

/** Walks the [Context] wrapper chain to find the hosting [Activity], or `null` if not found. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

/** Opens the system App Details settings screen for this app (AC-NT-12 permanent-deny path). */
private fun openAppSettings(context: Context) {
  val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
    data = Uri.fromParts("package", context.packageName, null)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
  }
  context.startActivity(intent)
}
