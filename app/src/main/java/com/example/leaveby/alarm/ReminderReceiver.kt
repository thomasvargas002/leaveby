package com.example.leaveby.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.leaveby.MainActivity
import com.example.leaveby.R
import com.example.leaveby.data.Prefs
import java.time.LocalTime

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        // new channel so devices don’t reuse old (non-vibrate) settings
        private const val CHANNEL_ID = "leaveby_reminders_v2"
        private const val NOTIF_ID = 7101
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val nm = context.getSystemService(NotificationManager::class.java)

        // Channel: HIGH + vibration + public
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "LeaveBy Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminder notifications for LeaveBy"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }

        val openApp = PendingIntent.getActivity(
            context,
            9101,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // launcher icon = safe
            .setContentTitle("LeaveBy")
            .setContentText("Time to check LeaveBy and drop the TimeCard")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // heads-up pre-26
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        nm.notify(NOTIF_ID, notif)

        // extra vibration in case the user allowed vib in Settings
        if (Prefs.vibrate(context)) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                    context.getSystemService(VibratorManager::class.java).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                val pattern = longArrayOf(0, 500, 250, 500, 250, 500)
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION") vibrator.vibrate(pattern, -1)
                }
            } catch (_: Throwable) {}
        }

        // keep the chain going with current prefs
        val enabled = Prefs.remEnabled(context)
        val days = Prefs.remDays(context)
        val time: LocalTime = Prefs.remTime(context)
        if (enabled && days.isNotEmpty()) {
            ReminderScheduler.scheduleNext(context, days, time)
        }
    }
}
