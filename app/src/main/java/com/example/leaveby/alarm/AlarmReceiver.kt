package com.example.leaveby.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.leaveby.R

/**
 * Fired by AlarmManager when an in-app alarm triggers.
 * Shows a full-screen notification + starts AlarmService (sound/vibrate).
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val label   = intent.getStringExtra(EXTRA_LABEL) ?: "LeaveBy"
        val message = intent.getStringExtra(EXTRA_MSG) ?: "Time to head out"
        val alarmId = intent.getIntExtra(EXTRA_ID, -1)

        val nm = context.getSystemService(NotificationManager::class.java)
        ensureChannel(context, nm)

        // Full-screen tap goes to RingingActivity
        val fullPi = PendingIntent.getActivity(
            context, 2001,
            Intent(context, RingingActivity::class.java).apply {
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_MSG, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(label)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)                 // looks/behaves like a ringing alarm
            .setAutoCancel(false)
            .setContentIntent(fullPi)
            .setFullScreenIntent(fullPi, true)
            .build()

        nm.notify(NOTIF_ID, notif)

        // Start the foreground sound/vibration service
        val svc = Intent(context, AlarmService::class.java)
            .putExtra(EXTRA_LABEL, label)
            .putExtra(EXTRA_MSG, message)
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }
        } catch (_: Throwable) {
            // As a fallback, surface the ringing UI
            context.startActivity(
                Intent(context, RingingActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_LABEL, label)
                    .putExtra(EXTRA_MSG, message)
            )
        }

        // Remove the fired alarm from our persisted list
        if (alarmId != -1) {
            try { AlarmScheduler.cancelAlarm(context, alarmId) } catch (_: Throwable) {}
        }
    }

    private fun ensureChannel(ctx: Context, nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ALARMS) != null) return

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val ch = NotificationChannel(
            CHANNEL_ALARMS,
            "LeaveBy Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm notifications"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            enableVibration(true)
            setSound(sound, attrs)
        }
        nm.createNotificationChannel(ch)
    }

    companion object {
        // Shared constants (used by AlarmService / RingingActivity / schedulers)
        const val CHANNEL_ALARMS = "alarms"
        const val NOTIF_ID = 39271

        const val EXTRA_LABEL = "label"
        const val EXTRA_MSG   = "msg"
        const val EXTRA_ID    = "alarm_id"
    }
}
