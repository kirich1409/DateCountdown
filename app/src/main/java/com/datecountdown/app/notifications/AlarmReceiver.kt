package com.datecountdown.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives exact alarm broadcasts fired by [AlarmManager] for scheduled events.
 *
 * This is a placeholder class whose [onReceive] is intentionally empty.
 * The full implementation — reading [com.datecountdown.app.data.NotificationSchedulerImpl.EXTRA_EVENT_ID],
 * looking up the event via [com.datecountdown.app.domain.EventsRepository], and posting the
 * notification via [android.app.NotificationManager] — is delivered in issue #45.
 *
 * The class is `public` so that [com.datecountdown.app.data.NotificationSchedulerImpl] can
 * reference it via [android.content.ComponentName] string. The manifest registers it with
 * `android:exported="false"` (no intent-filter means no external callers — AC-NT-9).
 */
class AlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    // no-op: full implementation delivered in #45
  }
}
