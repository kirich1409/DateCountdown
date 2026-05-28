package com.datecountdown.app.notifications

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.datecountdown.app.di.AppGraph
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Reschedules all upcoming event alarms after device reboot.
 *
 * ## Security (AC-NT-9)
 * Registered with `android:exported="true"` and `android:permission="android.permission.RECEIVE_BOOT_COMPLETED"` —
 * only the system can deliver [Intent.ACTION_BOOT_COMPLETED]. Intent action is validated
 * inside [onReceive] as a defense-in-depth measure against unexpected deliveries.
 *
 * ## Async execution
 * Iterating events and calling [com.datecountdown.app.domain.NotificationScheduler.schedule]
 * are both suspend operations. [goAsync] extends the broadcast window while the coroutine
 * runs. [android.content.BroadcastReceiver.PendingResult.finish] is called in `finally` to
 * release the wake lock regardless of outcome.
 *
 * ## Idempotency
 * [com.datecountdown.app.domain.NotificationScheduler.schedule] uses FLAG_UPDATE_CURRENT
 * semantics (AC-NT-6) — firing this receiver multiple times (BOOT_COMPLETED +
 * QUICKBOOT_POWERON on some devices) is safe.
 *
 * ## DI (Metro)
 * Constructs a fresh [AppGraph] from the application context, matching the pattern in
 * [AlarmReceiver]. The graph is not held beyond the coroutine lifetime.
 */
class BootReceiver : BroadcastReceiver() {

  // Catching Exception broadly is intentional: top-level safety net in a BroadcastReceiver.
  // Any unhandled exception here would crash the process silently (OS kills the receiver).
  @Suppress("TooGenericExceptionCaught")
  override fun onReceive(context: Context, intent: Intent) {
    // AC-NT-9: validate intent.action — receiver is exported and could be triggered
    // with actions other than BOOT_COMPLETED (e.g. by a malicious app with no permission,
    // or on devices that also fire QUICKBOOT_POWERON separately).
    val action = intent.action
    if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_QUICK_BOOT_POWERON) {
      Log.w(TAG, "Ignoring unexpected action: $action")
      return
    }

    val pendingResult = goAsync()
    val appContext = context.applicationContext

    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
      try {
        // Idempotent — zero-cost when channel already exists. Guards the case where
        // this receiver fires before MainActivity has ever run and the channel is not yet created.
        NotificationChannels.ensureChannel(appContext)

        val graph = createGraphFactory<AppGraph.Factory>().create(appContext as Application)

        val events = withTimeout(BOOT_RESCHEDULE_TIMEOUT) {
          graph.eventsRepository.observeEvents().first()
        }

        val now = Clock.System.now()
        var scheduled = 0
        var skipped = 0

        events.forEach { event ->
          // AC-NT-10: skip events whose target is in the past — no point scheduling them.
          // NotificationScheduler.schedule() would also no-op on past events per its contract,
          // but we skip here explicitly to get accurate counts in the log.
          if (event.targetDateTime <= now) {
            skipped++
            return@forEach
          }
          graph.notificationScheduler.schedule(event)
          scheduled++
        }

        // AC-NT-15: log counts only, never event titles or any PII.
        Log.i(TAG, "Boot reschedule complete — scheduled=$scheduled skipped=$skipped")
      } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "Boot reschedule timed out", e)
      } catch (e: Exception) {
        Log.e(TAG, "Boot reschedule failed", e)
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    private const val TAG = "BootReceiver"

    // HTC / Huawei / OnePlus devices fire this action in addition to (or instead of)
    // BOOT_COMPLETED for quick-boot / warm-boot scenarios.
    private const val ACTION_QUICK_BOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"

    // Boot reschedule may iterate many events — allow more time than AlarmReceiver's 8s.
    private val BOOT_RESCHEDULE_TIMEOUT = 30.seconds
  }
}
