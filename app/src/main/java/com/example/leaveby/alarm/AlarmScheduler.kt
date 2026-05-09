package com.example.leaveby.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * In-app alarm scheduler that supports multiple alarms + persistence.
 * Stored in SharedPreferences so UI can list/edit/delete them later.
 */
object AlarmScheduler {

    // ----- Public API -----

    /** True if we’re allowed to set exact alarms without user confirmation (S+). */
    fun canScheduleExact(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            true
        }
    }

    /** Schedule an exact in-app alarm. Returns the generated alarm id. */
    fun scheduleExactAlarm(context: Context, at: ZonedDateTime, label: String): Int {
        val am = context.getSystemService(AlarmManager::class.java)
        val local = at.withZoneSameInstant(ZoneId.systemDefault())
        val triggerAt = local.toInstant().toEpochMilli()

        val id = nextId(context)

        val firePi = firePendingIntent(context, id, label)
        // Use system Clock as the "show" target so users land on the alarm list when they tap the icon
        val showPi = clockShowPendingIntent(context, id) ?: ringingShowPendingIntent(context, id)

        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPi), firePi)

        val entry = InAppAlarm(id = id, triggerAt = triggerAt, label = label)
        saveEntry(context, entry)
        return id
    }

    /** Cancel a previously scheduled in-app alarm. */
    fun cancelAlarm(context: Context, id: Int) {
        val am = context.getSystemService(AlarmManager::class.java)

        // Cancel broadcast
        val firePi = firePendingIntentNoCreate(context, id)
        if (firePi != null) {
            am.cancel(firePi)
            firePi.cancel()
        }

        // Cancel the "show" intent too (not strictly required)
        clockShowPendingIntentNoCreate(context, id)?.cancel()
        ringingShowPendingIntentNoCreate(context, id)?.cancel()

        removeEntry(context, id)
    }

    /** List all scheduled in-app alarms (sorted soonest first). */
    fun listAlarms(context: Context): List<InAppAlarm> {
        return loadAll(context).sortedBy { it.triggerAt }
    }

    /** Re-schedule any future alarms persisted in prefs. Call from BootReceiver. */
    fun rescheduleAllAfterBoot(context: Context) {
        val now = System.currentTimeMillis()
        val am = context.getSystemService(AlarmManager::class.java)

        loadAll(context)
            .filter { it.triggerAt > now }
            .forEach { e ->
                val showPi = clockShowPendingIntent(context, e.id) ?: ringingShowPendingIntent(context, e.id)
                val firePi = firePendingIntent(context, e.id, e.label)
                am.setAlarmClock(AlarmManager.AlarmClockInfo(e.triggerAt, showPi), firePi)
            }
    }

    // ----- Model + persistence -----

    data class InAppAlarm(val id: Int, val triggerAt: Long, val label: String)

    private const val SP = "leaveby_prefs"
    private const val KEY_LIST = "inapp_alarms"
    private const val KEY_NEXT_ID = "inapp_alarms_next_id"

    private fun saveEntry(ctx: Context, e: InAppAlarm) {
        val list = loadAll(ctx).toMutableList()
        list.removeAll { it.id == e.id }
        list += e
        persist(ctx, list)
    }

    private fun removeEntry(ctx: Context, id: Int) {
        val list = loadAll(ctx).filterNot { it.id == id }
        persist(ctx, list)
    }

    private fun loadAll(ctx: Context): List<InAppAlarm> {
        val sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_LIST, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split('\n')
            .mapNotNull { line ->
                val parts = line.split(',')
                if (parts.size < 3) return@mapNotNull null
                val id = parts[0].toIntOrNull() ?: return@mapNotNull null
                val triggerAt = parts[1].toLongOrNull() ?: return@mapNotNull null
                val label = Uri.decode(parts[2])
                InAppAlarm(id, triggerAt, label)
            }
    }

    private fun persist(ctx: Context, list: List<InAppAlarm>) {
        val sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val body = buildString {
            list.forEachIndexed { i, e ->
                if (i > 0) append('\n')
                append(e.id).append(',')
                    .append(e.triggerAt).append(',')
                    .append(Uri.encode(e.label))
            }
        }
        sp.edit().putString(KEY_LIST, body).apply()
    }

    private fun nextId(ctx: Context): Int {
        val sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val next = sp.getInt(KEY_NEXT_ID, 20000) // start high to avoid clashes
        sp.edit().putInt(KEY_NEXT_ID, next + 1).apply()
        return next
    }

    // ----- PendingIntent helpers -----

    private fun firePendingIntent(context: Context, id: Int, label: String): PendingIntent {
        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("label", label)
            putExtra("alarm_id", id)
        }
        return PendingIntent.getBroadcast(
            context, id, fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun firePendingIntentNoCreate(context: Context, id: Int): PendingIntent? {
        val i = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, id, i,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Preferred "show" intent: open system Clock's alarm list.
     * Returns null if no handler exists (very rare), in which case use [ringingShowPendingIntent].
     */
    private fun clockShowPendingIntent(context: Context, id: Int): PendingIntent? {
        val showClock = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // If there is no app to handle ACTION_SHOW_ALARMS, return null so we can fall back.
        val pm = context.packageManager
        val canHandle = showClock.resolveActivity(pm) != null
        if (!canHandle) return null

        return PendingIntent.getActivity(
            context, 1000 + id, showClock,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun clockShowPendingIntentNoCreate(context: Context, id: Int): PendingIntent? {
        val i = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        return PendingIntent.getActivity(
            context, 1000 + id, i,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Fallback "show" intent: open our RingingActivity. */
    private fun ringingShowPendingIntent(context: Context, id: Int): PendingIntent {
        val showIntent = Intent(context, RingingActivity::class.java).apply {
            putExtra("alarm_id", id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context, 2000 + id, showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ringingShowPendingIntentNoCreate(context: Context, id: Int): PendingIntent? {
        val i = Intent(context, RingingActivity::class.java)
        return PendingIntent.getActivity(
            context, 2000 + id, i,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
