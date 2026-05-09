package com.example.leaveby.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import java.time.LocalTime

object Prefs {
    private const val FILE = "leaveby_prefs"

    // main calc settings
    private const val KEY_CAP = "weekly_cap"
    private const val KEY_BRK = "unpaid_break"
    private const val KEY_BUF = "buffer_min"
    private const val KEY_ALLOW_OT = "allow_ot"
    private const val KEY_OT_HOURS = "ot_hours"
    private const val KEY_USE_IN_APP_ALARM = "use_in_app_alarm"

    // alarm sound prefs
    private const val KEY_SOUND_URI = "alarm_sound_uri"
    private const val KEY_VIBRATE = "alarm_vibrate"
    private const val KEY_VOLUME = "alarm_volume" // 0..100

    // reminders
    private const val KEY_REM_ENABLED = "rem_enabled"
    private const val KEY_REM_DAYS = "rem_days"     // CSV 1..7 (Mon..Sun)
    private const val KEY_REM_TIME = "rem_time"     // "HH:mm"

    // ---------------- main getters ----------------
    fun cap(ctx: Context): Float =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getFloat(KEY_CAP, 40f)

    fun brk(ctx: Context): Int =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_BRK, 30)

    fun tutorialSeen(ctx: Context): Boolean =
        getBoolean(ctx, "tutorial_seen_v1", false)

    fun setTutorialSeen(ctx: Context, value: Boolean) {
        putBoolean(ctx, "tutorial_seen_v1", value)
    }

    fun buf(ctx: Context): Int =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_BUF, 2)

    fun allowOT(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ALLOW_OT, false)

    fun otHours(ctx: Context): Float =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getFloat(KEY_OT_HOURS, 0f)

    fun useInAppAlarm(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_USE_IN_APP_ALARM, true)

    fun setUseInAppAlarm(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_USE_IN_APP_ALARM, on)
        }
    }

    fun save(
        ctx: Context,
        cap: Float,
        brk: Int,
        buf: Int,
        allowOT: Boolean,
        otHours: Float,
        useInAppAlarm: Boolean = useInAppAlarm(ctx)
    ) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putFloat(KEY_CAP, cap)
            putInt(KEY_BRK, brk)
            putInt(KEY_BUF, buf)
            putBoolean(KEY_ALLOW_OT, allowOT)
            putFloat(KEY_OT_HOURS, otHours)
            putBoolean(KEY_USE_IN_APP_ALARM, useInAppAlarm)
        }
    }

    // ---------------- sound getters/setters ----------------
    fun soundUri(ctx: Context): Uri? =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_SOUND_URI, null)?.toUri()

    fun setSoundUri(ctx: Context, uri: Uri?) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            if (uri == null) remove(KEY_SOUND_URI) else putString(KEY_SOUND_URI, uri.toString())
        }
    }

    fun vibrate(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_VIBRATE, true)

    fun setVibrate(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putBoolean(KEY_VIBRATE, on) }
    }

    fun volume(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_VOLUME, 100)

    fun setVolume(ctx: Context, percent: Int) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putInt(KEY_VOLUME, percent.coerceIn(0, 100))
        }
    }

    // ---------------- reminders (survive reboot) ----------------
    fun remEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_REM_ENABLED, false)

    fun setRemEnabled(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putBoolean(KEY_REM_ENABLED, enabled) }
    }

    /** Days are 1..7 (Mon..Sun). Default = Mon..Fri */
    fun remDays(ctx: Context): Set<Int> {
        val csv = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_REM_DAYS, "1,2,3,4,5")
        if (csv.isNullOrBlank()) return emptySet()
        return csv.split(',').mapNotNull { it.toIntOrNull() }.filter { it in 1..7 }.toSet()
    }

    fun setRemDays(ctx: Context, days: Set<Int>) {
        val safe = days.filter { it in 1..7 }.sorted().joinToString(",")
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putString(KEY_REM_DAYS, safe) }
    }

    /** Default = 09:00 local */
    fun remTime(ctx: Context): LocalTime {
        val s = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_REM_TIME, "09:00")
        return try { LocalTime.parse(s) } catch (_: Throwable) { LocalTime.of(9, 0) }
    }

    fun setRemTime(ctx: Context, time: LocalTime) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putString(KEY_REM_TIME, time.toString()) }
    }

    // tiny helpers (UI state)
    fun getInt(ctx: Context, key: String, def: Int) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(key, def)

    fun putInt(ctx: Context, key: String, value: Int) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putInt(key, value) }
    }
    fun getBoolean(ctx: Context, key: String, def: Boolean) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(key, def)

    fun putBoolean(ctx: Context, key: String, value: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putBoolean(key, value) }
    }
}
