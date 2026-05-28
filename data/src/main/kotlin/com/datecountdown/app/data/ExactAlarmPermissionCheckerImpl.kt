package com.datecountdown.app.data

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import com.datecountdown.app.domain.ExactAlarmPermissionChecker

/**
 * [AlarmManager]-backed implementation of [ExactAlarmPermissionChecker] (AC-NT-13).
 *
 * On API < 31 exact alarms are unrestricted, so [canScheduleExactAlarms] returns `true`
 * unconditionally. On API 31+ the result reflects [AlarmManager.canScheduleExactAlarms].
 *
 * Class is `public` (not `internal`) for Metro DI wiring in `:app` — same constraint as
 * [NotificationSchedulerImpl]. It is not part of the intended public API of `:data`.
 */
class ExactAlarmPermissionCheckerImpl(
  private val context: Context,
) : ExactAlarmPermissionChecker {

  override fun canScheduleExactAlarms(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      // Exact alarms are unrestricted below API 31.
      return true
    }
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    return alarmManager?.canScheduleExactAlarms() ?: false
  }
}
