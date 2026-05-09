package com.example.leaveby.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderScheduler {

    /** Persisted -> schedule or cancel */
    fun saveAndSchedule(ctx: Context, enabled: Boolean, days: Set<Int>, time: LocalTime) {
        if (!enabled || days.isEmpty()) {
            cancel(ctx); return
        }
        scheduleNext(ctx, days, time)
    }

    /** Re-created after boot using stored prefs */
    fun rescheduleFromPrefs(ctx: Context) {
        val p = com.example.leaveby.data.Prefs
        if (p.remEnabled(ctx) && p.remDays(ctx).isNotEmpty()) {
            scheduleNext(ctx, p.remDays(ctx), p.remTime(ctx))
        } else cancel(ctx)
    }

    fun scheduleNext(ctx: Context, days: Set<Int>, time: LocalTime) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(ctx)

        val now = ZonedDateTime.now()
        val next = computeNext(now, days, time)

        val whenMs = next.toInstant().toEpochMilli()
        // exact + idle so it actually fires
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            // fallback, still try to be precise-ish
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
        }
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pendingIntent(ctx))
    }

    private fun pendingIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            9001,
            Intent(ctx, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** days = 1..7 (Mon..Sun) */
    private fun computeNext(now: ZonedDateTime, days: Set<Int>, time: LocalTime): ZonedDateTime {
        val today = now.dayOfWeek.value // 1..7
        var best: ZonedDateTime? = null

        for (d in 1..7) {
            if (d !in days) continue
            val delta = ((d - today) + 7) % 7
            var candidate = now.withHour(time.hour).withMinute(time.minute)
                .withSecond(0).withNano(0)
                .plusDays(delta.toLong())
            if (delta == 0 && candidate.isBefore(now)) candidate = candidate.plusDays(7)
            if (best == null || candidate.isBefore(best)) best = candidate
        }
        // fallback: tomorrow same time
        return best ?: now.plusDays(1).withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
    }
}
