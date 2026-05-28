package com.datecountdown.app.notifications

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.datecountdown.app.MainActivity
import com.datecountdown.app.R
import com.datecountdown.app.data.NotificationSchedulerImpl
import com.datecountdown.app.di.AppGraph
import com.datecountdown.app.domain.EventId
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * Receives exact alarm broadcasts fired by [android.app.AlarmManager] for scheduled events
 * and posts a local notification for the event.
 *
 * ## Security (AC-NT-9)
 * Registered with `android:exported="false"` — only this app can trigger the receiver.
 * No `<intent-filter>` is declared; AlarmManager targets it via explicit [android.content.ComponentName].
 *
 * ## Async execution
 * [android.app.NotificationManager] itself is synchronous, but the repository lookup
 * ([com.datecountdown.app.domain.EventsRepository.getById]) is a `suspend` function.
 * [goAsync] extends the broadcast window (default 10 s on modern Android) while the
 * coroutine runs. [android.content.BroadcastReceiver.PendingResult.finish] is called in
 * `finally` to prevent the receiver from holding the wake lock forever.
 *
 * ## DI (Metro)
 * Each alarm fire constructs a fresh [AppGraph] from the application context. This matches
 * the pattern used in [com.datecountdown.app.MainActivity]:
 * `createGraphFactory<AppGraph.Factory>().create(application)`.
 * A side effect is a second SQLite connection when the Activity is also alive — acceptable for MVP
 * given the low alarm frequency.
 *
 * ## Privacy (AC-NT-14, AC-NT-15)
 * The notification is posted with [NotificationCompat.VISIBILITY_PRIVATE] and a generic
 * public version that contains no event title. The event title never appears in logs.
 */
class AlarmReceiver : BroadcastReceiver() {

  companion object {
    private const val TAG = "AlarmReceiver"

    /**
     * Intent extra key used by MainActivity to navigate directly to the event counter.
     * Exposed here so #50 (deep-link wiring) has a single canonical source in `:app`.
     * The value must match [NotificationSchedulerImpl.EXTRA_EVENT_ID] — both carry
     * the same event id from scheduling through to delivery.
     */
    const val EXTRA_EVENT_ID = NotificationSchedulerImpl.EXTRA_EVENT_ID
  }

  // Catching Exception broadly is intentional: this is a top-level safety net in a
  // BroadcastReceiver. Any unhandled exception here would crash the process silently
  // (the OS kills the receiver), so catching broadly + logging is the correct recovery.
  // POST_NOTIFICATIONS is declared in the manifest (#47); when the user has denied the
  // permission, NotificationManagerCompat silently drops the notify() call — intentional for MVP.
  @Suppress("TooGenericExceptionCaught")
  @SuppressLint("MissingPermission", "NotificationPermission")
  override fun onReceive(context: Context, intent: Intent) {
    val eventIdValue = intent.getStringExtra(NotificationSchedulerImpl.EXTRA_EVENT_ID)
    if (eventIdValue.isNullOrBlank()) {
      Log.w(TAG, "Received alarm with missing or blank event_id extra — ignoring")
      return
    }

    val pendingResult = goAsync()
    val appContext = context.applicationContext

    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
      try {
        // Idempotent — zero-cost when channel already exists. Guards the rare case where
        // BootReceiver fires before MainActivity has ever run and the channel is not yet created.
        NotificationChannels.ensureChannel(appContext)

        val graph = createGraphFactory<AppGraph.Factory>().create(appContext as Application)
        val event = withTimeout(8.seconds) { graph.eventsRepository.getById(EventId(eventIdValue)) }

        if (event == null) {
          // Event was deleted after alarm was scheduled — silent no-op (AC-NT-4).
          Log.w(TAG, "Event not found for alarm, eventId=$eventIdValue — no notification posted")
          return@launch
        }

        val todayText = appContext.getString(R.string.notification_today_text)
        val publicText = appContext.getString(R.string.notification_public_text)

        // Generic notification shown on lock screen when visibility is PRIVATE (AC-NT-14).
        val publicVersion = NotificationCompat.Builder(appContext, NotificationChannels.EVENTS_CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_notification)
          .setContentTitle(appContext.getString(R.string.app_name))
          .setContentText(publicText)
          .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
          .build()

        // Content intent for tap-to-open (AC-NT-8): deep-link wiring deferred to #50.
        // The EXTRA_EVENT_ID extra is set so MainActivity can navigate to the counter.
        val tapIntent = Intent(appContext, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          putExtra(NotificationSchedulerImpl.EXTRA_EVENT_ID, eventIdValue)
        }
        val contentIntent = PendingIntent.getActivity(
          appContext,
          eventIdValue.hashCode(),
          tapIntent,
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(appContext, NotificationChannels.EVENTS_CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_notification)
          .setContentTitle(event.title)
          .setContentText(todayText)
          .setContentIntent(contentIntent)
          .setAutoCancel(true)
          // AC-NT-14: private on lock screen; publicVersion shown instead.
          .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
          .setPublicVersion(publicVersion)
          .build()

        // Unique per-event notification id so each event has its own entry in the tray.
        // hashCode on a UUID string has negligible collision probability for <<1000 events.
        val notificationId = eventIdValue.hashCode()
        NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        // AC-NT-15: log only eventId, never event.title.
        Log.i(TAG, "Notification posted, eventId=$eventIdValue")
      } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "Repository lookup timed out for eventId=$eventIdValue — no notification posted", e)
      } catch (e: Exception) {
        // AC-NT-15: no event.title in the log message.
        Log.e(TAG, "Failed to post notification for eventId=$eventIdValue", e)
      } finally {
        pendingResult.finish()
      }
    }
  }
}
