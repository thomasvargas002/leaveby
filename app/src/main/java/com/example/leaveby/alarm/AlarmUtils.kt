package com.example.leaveby.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Opens the user's Clock app to set an alarm at [whenAt].
 * We intentionally avoid privileged flags like EXTRA_SKIP_UI and never
 * target a specific package/component to prevent SecurityException.
 */
fun setLeaveAlarmExternal(ctx: Context, whenAt: ZonedDateTime, label: String) {
    val local = whenAt.withSecond(0).withNano(0).withZoneSameInstant(ZoneId.systemDefault())
    val hour = local.hour
    val minute = local.minute

    val set = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_MESSAGE, label)
        putExtra(AlarmClock.EXTRA_HOUR, hour)
        putExtra(AlarmClock.EXTRA_MINUTES, minute)
        // DO NOT set AlarmClock.EXTRA_SKIP_UI — it requires a privileged permission.
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        ctx.startActivity(set)
        return
    } catch (_: Throwable) { /* fall through */ }

    // Some devices don't expose SET_ALARM; at least open the alarms list.
    try {
        ctx.startActivity(
            Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return
    } catch (_: Throwable) { /* fall through */ }

    // Final fallback: tell the user what time to set.
    val fmt = DateTimeFormatter.ofPattern("h:mm a")
    Toast.makeText(
        ctx,
        "Open your Clock app and set an alarm for ${fmt.format(local)}",
        Toast.LENGTH_LONG
    ).show()
}
