package com.datecountdown.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.datecountdown.app.R

/**
 * Central registry for notification channels used by the app.
 *
 * Call [ensureChannel] once from [com.datecountdown.app.MainActivity.onCreate] before
 * any notification is posted. [NotificationManagerCompat.createNotificationChannel]
 * is idempotent on Android 8+ — duplicate calls are safe (AC-NT-7).
 */
internal object NotificationChannels {

  /** Channel id for event reminder notifications. Referenced in [AlarmReceiver]. */
  const val EVENTS_CHANNEL_ID = "events"

  /**
   * Creates the events notification channel on Android 8+ (API 26+).
   *
   * No-op below API 26 — channels do not exist there.
   * Safe to call multiple times: [NotificationManagerCompat.createNotificationChannel]
   * silently ignores duplicate registrations (AC-NT-7).
   */
  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = context.getString(R.string.notification_channel_name)
      val description = context.getString(R.string.notification_channel_description)
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(EVENTS_CHANNEL_ID, name, importance).apply {
        this.description = description
      }
      NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
  }
}
