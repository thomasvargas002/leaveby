@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class      // ← add this
)

package com.example.leaveby.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.DisposableEffect
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.key
import kotlinx.coroutines.isActive
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Warning
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.clip
import android.content.Intent
import android.graphics.Paint
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.example.leaveby.alarm.AlarmScheduler
import com.example.leaveby.alarm.ReminderScheduler
import com.example.leaveby.alarm.setLeaveAlarmExternal
import com.example.leaveby.data.Prefs
import com.example.leaveby.data.Parsed
import com.example.leaveby.data.ParsedSchedule
import com.example.leaveby.data.parsePublixScheduleFromImage
import com.example.leaveby.data.parseTimecardFromImage
import com.example.leaveby.domain.CalcResult
import com.example.leaveby.domain.computeLeaveBy
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

// Pastel colors for the chips
// Pastel colors (slightly stronger)
private val PastelGreen  = Color(0xFFA5D6A7) // IN  (green 300)
private val PastelYellow = Color(0xFFFFF176) // BREAK (yellow 300)
private val PastelRed    = Color(0xFFEF9A9A) // OUT (red 200)
private const val TAG = "LeaveByANR"
// Keep OCR heavy work off the main thread and avoid two scans running at once
private val OCR_DISPATCHER: CoroutineDispatcher =
    Dispatchers.Default.limitedParallelism(1)
// Try to read a LocalDate weekStart from any parsed object (via reflection-ish scan)

@Composable
private fun PastelChip(
    title: String,
    subLabel: String?,              // e.g. "3:04 PM", "30m", "—"
    bg: Color,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subLabel ?: "—", style = MaterialTheme.typography.labelSmall)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = bg,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/* ---------- models ---------- */
private data class Floaty(
    val id: Long,
    val emoji: String,
    val baseX: Float,
    val swayPx: Float,
    val phase: Float,
    val freq: Float,
    val dir: Float,
    val startNanos: Long,
    val durationNanos: Long,
    val sizePx: Float
)
private data class DayManual(
    val start: LocalTime? = null,
    val end: LocalTime? = null,
    val breakMin: Int = 0
) {
    fun netHours(): Double {
        val s = start ?: return 0.0
        val e = end ?: return 0.0
        val sMin = s.hour * 60 + s.minute
        val eMin = e.hour * 60 + e.minute
        val spanMin = if (eMin >= sMin) eMin - sMin else (eMin + 24 * 60) - sMin
        return max(0.0, spanMin / 60.0 - breakMin / 60.0)
    }
}

private enum class ViewMode { PLANNED, ACTUAL, COMPARE }

/* chips state types */
private enum class EditKind { IN, BREAK, OUT }
private data class EditTarget(val day: String, val plannedTrack: Boolean, val kind: EditKind)

private data class ClearTarget(val day: String, val plannedTrack: Boolean)

/* ---------- tiny util ---------- */
// Try to read a LocalDate weekStart from any parsed object (via reflection, safe if absent)
// Replace the whole old weekStartFrom with this:

private fun weekStartFrom(obj: Any?): LocalDate? {
    fun parseUsDate(s: String): LocalDate? {
        val m = Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\b""").find(s) ?: return null
        val (mmStr, ddStr, yyStr) = m.destructured
        val mm = mmStr.toIntOrNull() ?: return null
        val dd = ddStr.toIntOrNull() ?: return null
        val year = when {
            yyStr.isBlank()   -> LocalDate.now().year
            yyStr.length == 2 -> 2000 + (yyStr.toIntOrNull() ?: return null)
            else              -> yyStr.toIntOrNull() ?: return null
        }
        return runCatching { LocalDate.of(year, mm, dd) }.getOrNull()
    }

    val seen = java.util.IdentityHashMap<Any, Boolean>()  // prevent cycles

    fun scan(o: Any?, depth: Int = 0): LocalDate? {
        if (o == null || depth > 4) return null
        if (o !is String && o !is LocalDate) {
            if (seen.containsKey(o)) return null
            seen[o] = true
        }
        return when (o) {
            is LocalDate -> o
            is String    -> parseUsDate(o)
            is Map<*, *> -> o.values.firstNotNullOfOrNull { v -> scan(v, depth + 1) }
            is Iterable<*> -> o.firstNotNullOfOrNull { v -> scan(v, depth + 1) }
            is Array<*>  -> o.firstNotNullOfOrNull { v -> scan(v, depth + 1) }
            else         -> parseUsDate(o.toString())  // safe fallback, no reflection
        }
    }

    return scan(obj)?.let(::startOfWeekSaturday)
}
private inline fun <T, R> List<T>.associateWithIndexed(crossinline transform: (index: Int) -> R): Map<T, R> {
    val map = LinkedHashMap<T, R>(size)
    for (i in indices) map[this[i]] = transform(i)
    return map
}
// ---------- OUT math helper (single source of truth) ----------
private data class OutCalc(
    val boundary: ZonedDateTime?,      // same as displayAt (kept for fallback paths)
    val plannedEnd: ZonedDateTime?,
    val capExact: ZonedDateTime?,      // exact leave-by (what UI shows & chips use)
    val notifyAt: ZonedDateTime?,      // leave-by − buffer
    val usedBreakMin: Int
)

/**
 * Computes the OUT suggestion and alarm time for a given day.
 *
 * - Takes start time from the chosen track (planned vs actual).
 * - Reserves future planned hours so today doesn't try to finish the whole cap.
 * - Applies the rule "don't suggest earlier than planned" by pushing to planned end if it's later.
 * - Never feeds buffer into the cap math; buffer is only used to compute the alarm time.
 */
private fun suggestPunchOut(
    dayIdx: Int,
    days: List<String>,
    usePlannedTrackForStart: Boolean,
    planned: Map<String, DayManual>,
    actualMan: Map<String, DayManual>,
    parsedPlanned: ParsedSchedule?,
    parsedActual: Parsed?,
    plannedHours: (Int) -> Double,
    actualHours: (Int) -> Double,
    effectiveCap: Double,
    defaultBreakMin: Int,
    bufferLeadMin: Int,
    fmtAmPm: DateTimeFormatter,
    guardNotEarlierThanPlanned: Boolean = true
): OutCalc {
    val day = days[dayIdx]

    // Planned end (used as a guard only if requested)
    val plannedEndLt: LocalTime? =
        planned[day]?.end ?: parsedPlanned?.perDay?.get(day)?.endStr?.let(::tryParseTime)
    val plannedEndZdt: ZonedDateTime? = plannedEndLt?.let {
        ZonedDateTime.now().withSecond(0).withNano(0).withHour(it.hour).withMinute(it.minute)
    }

    // Start time comes from the chosen track
    val startLt: LocalTime? = if (usePlannedTrackForStart) {
        planned[day]?.start ?: parsedPlanned?.perDay?.get(day)?.startStr?.let(::tryParseTime)
    } else {
        actualMan[day]?.start ?: parsedActual?.perDay?.get(day)?.startStr?.let(::tryParseTime)
    }

    val usedBreak = (if (usePlannedTrackForStart) planned[day]?.breakMin else actualMan[day]?.breakMin)
        ?: defaultBreakMin

    if (startLt == null) {
        return OutCalc(
            boundary = null,
            plannedEnd = plannedEndZdt,
            capExact = null,
            notifyAt = null,
            usedBreakMin = usedBreak
        )
    }

    val startStr = fmtAmPm.format(
        ZonedDateTime.now().withHour(startLt.hour).withMinute(startLt.minute)
    )

    // Reserve *future planned* hours so today doesn't try to finish the whole cap
    val futureReserve = ((dayIdx + 1)..6).sumOf { plannedHours(it) }.coerceAtLeast(0.0)
    val capForToday = (effectiveCap - futureReserve).coerceAtLeast(0.0)

    val c = computeLeaveBy(
        wtdBeforeToday = (0 until dayIdx).sumOf { i ->
            if (usePlannedTrackForStart) plannedHours(i) else actualHours(i)
        },
        clockInStr = startStr,
        weeklyCap = capForToday,
        unpaidBreakMin = usedBreak,
        bufferMin = 0                 // lead is ONLY used for the alarm time
    )

    // EXACT leave-by (no -1m). This is what we DISPLAY and feed to dialogs.
    val capExact = c.leaveAt

    // Optionally don't tell the user to leave earlier than the plan
    val displayAt: ZonedDateTime? = capExact?.let { ce ->
        if (guardNotEarlierThanPlanned && plannedEndZdt?.isAfter(ce) == true) plannedEndZdt else ce
    } ?: plannedEndZdt

    // ✅ No 1-minute rule anywhere:
    // boundary is the same as displayAt (kept for backward compatibility),
    // and notifyAt is just displayAt minus the user buffer.
    val boundaryForAlarm = displayAt
    val notifyAt = displayAt?.minusMinutes(bufferLeadMin.toLong())

    return OutCalc(
        boundary = boundaryForAlarm,
        plannedEnd = plannedEndZdt,
        capExact = capExact,           // ✅ raw cap time (e.g., 6:35 PM), never replaced by plan
        notifyAt = notifyAt,
        usedBreakMin = usedBreak
    )
}

/* ---------- parsing helpers ---------- */
private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun parseHHmm(s: String): LocalTime? =
    runCatching { LocalTime.parse(s, HHMM) }.getOrNull()

// Accepts "10 a.m." / "10 am" / "10am" / "10:00 AM" / 24h variants
private fun tryParseTime(raw: String): LocalTime? {
    val cleaned = raw
        .trim()
        .replace(".", "")
        .replace("a m", "am", ignoreCase = true)
        .replace("p m", "pm", ignoreCase = true)
        .replace("AM", "AM")
        .replace("PM", "PM")

    val patterns = listOf("h:mm a", "hh:mm a", "h a", "hh a", "H:mm", "HH:mm")
    for (p in patterns) {
        val f = DateTimeFormatter.ofPattern(p, Locale.US)
        val candidate = cleaned
            .replace("AM", " AM")
            .replace("PM", " PM")
            .replace("  ", " ")
            .uppercase(Locale.US)
        val t = runCatching { LocalTime.parse(candidate, f) }.getOrNull()
        if (t != null) return t
    }
    return null
}
/* ---------- MainScreen ---------- */
@Composable
fun MainScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = remember { Locale.getDefault() }
    // prefs handle for persisting week anchors
    val sp = remember(ctx) { ctx.getSharedPreferences("leaveby_prefs", Context.MODE_PRIVATE) }
    // settings
    var weeklyCap by rememberSaveable { mutableFloatStateOf(Prefs.cap(ctx)) }
    var breakMin by rememberSaveable { mutableIntStateOf(Prefs.brk(ctx)) }
    var bufferMin by rememberSaveable { mutableIntStateOf(Prefs.buf(ctx)) }
    var allowOT by rememberSaveable { mutableStateOf(Prefs.allowOT(ctx)) }
    var otHours by rememberSaveable { mutableFloatStateOf(Prefs.otHours(ctx)) }
    var useInAppAlarm by rememberSaveable { mutableStateOf(Prefs.useInAppAlarm(ctx)) }
    var vibrate by rememberSaveable { mutableStateOf(Prefs.vibrate(ctx)) }
    var volumePercent by rememberSaveable { mutableIntStateOf(Prefs.volume(ctx)) }
    var showTutorial by rememberSaveable { mutableStateOf(false) }

    // reminders
    var remEnabled by rememberSaveable { mutableStateOf(true) }
    var remDays    by rememberSaveable { mutableStateOf(Prefs.remDays(ctx)) }
    var remTime    by rememberSaveable { mutableStateOf(LocalTime.of(11, 0)) }
    // --- One-time default: enable reminders at 11:00 AM (doesn't re-apply after first run)
    LaunchedEffect(Unit) {
        val sp = ctx.getSharedPreferences("leaveby_prefs", Context.MODE_PRIVATE)
        if (!sp.getBoolean("rem_defaults_applied_v1", false)) {
            remEnabled = true
            remTime = LocalTime.of(11, 0)

            // persist + schedule
            Prefs.setRemEnabled(ctx, true)
            Prefs.setRemTime(ctx, remTime)
            ReminderScheduler.saveAndSchedule(ctx, remEnabled, remDays, remTime)

            sp.edit().putBoolean("rem_defaults_applied_v1", true).apply()
        }
    }
    LaunchedEffect(Unit) {
        if (!Prefs.tutorialSeen(ctx)) {
            showTutorial = true
        }
    }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    var showAlarms by rememberSaveable { mutableStateOf(false) } // in-app alarms manager

    // OCR results
    var parsedActual by remember { mutableStateOf<Parsed?>(null) }
    var parsedPlanned by remember { mutableStateOf<ParsedSchedule?>(null) }
    var ocrErr by rememberSaveable { mutableStateOf<String?>(null) }

// shows a tiny spinner & disables buttons while OCR runs (optional UI in step 3)
    var isScanningActual by rememberSaveable { mutableStateOf(false) }
    var isScanningPlanned by rememberSaveable { mutableStateOf(false) }

    // hours display format: false = decimal, true = h:mm
    var showHm by rememberSaveable { mutableStateOf(false) }
    var showChart by rememberSaveable { mutableStateOf(false) }

    // app "week" is Sat→Fri
    val days = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
    var dayIdx by rememberSaveable { mutableIntStateOf(Prefs.getInt(ctx, "last_day_idx", 6)) }
    LaunchedEffect(dayIdx) { Prefs.putInt(ctx, "last_day_idx", dayIdx) }

    // Week anchors (Sat→Fri) — PERSISTED across app restarts
    var plannedWeekStartStr by rememberSaveable {
        mutableStateOf(
            sp.getString("planned_week_start", null)
                ?: startOfWeekSaturday(LocalDate.now()).toString()
        )
    }
    var actualWeekStartStr by rememberSaveable {
        mutableStateOf(
            sp.getString("actual_week_start", null)
                ?: startOfWeekSaturday(LocalDate.now()).toString()
        )
    }

    val plannedWeekStart = remember(plannedWeekStartStr) { LocalDate.parse(plannedWeekStartStr) }
    val actualWeekStart  = remember(actualWeekStartStr)  { LocalDate.parse(actualWeekStartStr) }

    // Save anchors whenever they change (including after OCR sets them)
    LaunchedEffect(plannedWeekStartStr) {
        sp.edit().putString("planned_week_start", plannedWeekStartStr).apply()
    }
    LaunchedEffect(actualWeekStartStr) {
        sp.edit().putString("actual_week_start", actualWeekStartStr).apply()
    }

    val plannedWeekKey = plannedWeekStartStr
    val actualWeekKey  = actualWeekStartStr

    val weeksMisaligned by remember(plannedWeekStart, actualWeekStart) {
        derivedStateOf { plannedWeekStart != actualWeekStart }
    }

// Date labels
    val dayDateFmt = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }
    fun dayDateFrom(base: LocalDate, i: Int): LocalDate = base.plusDays(i.toLong())

    // For list headers (uses Planned anchor so Compare view is stable)
    fun dayLabel(i: Int): String = "${days[i]} ${dayDateFmt.format(dayDateFrom(plannedWeekStart, i))}"

    // per-day overrides (persisted per *track* week)
    var planned  by remember(plannedWeekKey) { mutableStateOf(loadManualWeek(ctx, plannedWeekKey, days, true)) }
    var actualMan by remember(actualWeekKey)  { mutableStateOf(loadManualWeek(ctx, actualWeekKey,  days, false)) }

    // Manual quick alarm picker
    var pickedHour by rememberSaveable { mutableStateOf<Int?>(null) }
    var pickedMinute by rememberSaveable { mutableStateOf<Int?>(null) }

    val is24h = DateFormat.is24HourFormat(ctx)
    val fmtDisplay = remember(is24h) { DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "hh:mm a") }
    val fmtAmPm  = remember(is24h) { DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a") }

// toggle: Planned / Actual / Compare  —— default to ACTUAL
    var view by rememberSaveable { mutableStateOf(ViewMode.ACTUAL) }
    LaunchedEffect(view) {
        if (view == ViewMode.ACTUAL) {
            dayIdx = when (LocalDate.now().dayOfWeek) {
                DayOfWeek.SATURDAY -> 0
                DayOfWeek.SUNDAY   -> 1
                DayOfWeek.MONDAY   -> 2
                DayOfWeek.TUESDAY  -> 3
                DayOfWeek.WEDNESDAY-> 4
                DayOfWeek.THURSDAY -> 5
                DayOfWeek.FRIDAY   -> 6
            }
        }
    }
// image pickers (timecard / schedule)

    // image pickers (timecard / schedule)
    val timecardPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                isScanningActual = true
                try {
                    Log.d(TAG, "timecard OCR start, uri=$uri (thread=${Thread.currentThread().name})")
                    val parsed = withContext(OCR_DISPATCHER) {
                        parseTimecardFromImage(ctx, uri) // heavy work off main
                    }
                    Log.d(TAG, "timecard OCR done (thread=${Thread.currentThread().name})")
                    parsedActual = parsed
                    parsed.weekStart?.let { actualWeekStartStr = it.toString() }
                    val targetKeyActual = actualWeekStartStr
                    val next = actualMan.toMutableMap()
                    val order = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")

                    for (d in order) {
                        val row = parsed.perDay[d] ?: continue
                        val s = row.startStr?.let { tryParseTime(it) }
                        val eRaw = row.endStr?.let { tryParseTime(it) }
                        when {
                            s != null && eRaw != null -> {
                                val brk = if (row.hours > 0.0) inferBreakMin(s, eRaw, row.hours) else 0
                                val entry = DayManual(start = s, end = eRaw, breakMin = brk)
                                saveManualForDay(ctx, targetKeyActual, d, entry, planned = false)
                                next[d] = entry
                            }
                            row.hours > 0.0 -> {
                                val fakeStart = LocalTime.of(9, 0)
                                val fakeEnd = fakeStart.plusMinutes((row.hours * 60).toLong())
                                val entry = DayManual(start = fakeStart, end = fakeEnd, breakMin = 0)
                                saveManualForDay(ctx, targetKeyActual, d, entry, planned = false)
                                next[d] = entry
                            }
                            else -> {
                                val partial = if (s != null || eRaw != null) DayManual(s, eRaw, 0) else null
                                saveManualForDay(ctx, targetKeyActual, d, partial, planned = false)
                                if (partial != null) next[d] = partial else next.remove(d)
                            }
                        }
                    }
                    actualMan = next
                    view = ViewMode.ACTUAL   // ← NEW: jump to Actual after timecard scan
                    Toast.makeText(ctx, "Showing Actual", Toast.LENGTH_SHORT).show()
                    ocrErr = null
                    val lastWorked = parsed.let { p -> lastWorkedIndex(p, days) }
                    if (lastWorked >= 0) dayIdx = lastWorked
                } catch (t: Throwable) {
                    Log.e(TAG, "timecard OCR error", t)
                    ocrErr = t.message ?: "Timecard OCR failed"
                } finally {
                    isScanningActual = false
                }
            }
        }
    }
    val schedulePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                isScanningPlanned = true
                try {
                    Log.d(TAG, "schedule OCR start, uri=$uri (thread=${Thread.currentThread().name})")
                    val sched = withContext(OCR_DISPATCHER) {
                        parsePublixScheduleFromImage(ctx, uri) // heavy work off main
                    }
                    Log.d(TAG, "schedule OCR done (thread=${Thread.currentThread().name})")
                    parsedPlanned = sched
                    sched.weekStart?.let { plannedWeekStartStr = it.toString() }
                    val targetKeyPlanned = plannedWeekStartStr
                    val next = planned.toMutableMap()
                    val order = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")

                    for (d in order) {
                        val pd = sched.perDay[d] ?: continue
                        val s = pd.startStr?.let { tryParseTime(it) }
                        val eRaw = pd.endStr?.let { tryParseTime(it) }
                        when {
                            s != null && eRaw != null -> {
                                val brk = if (pd.hours > 0.0) inferBreakMin(s, eRaw, pd.hours) else 0
                                val entry = DayManual(start = s, end = eRaw, breakMin = brk)
                                saveManualForDay(ctx, targetKeyPlanned, d, entry, true)
                                next[d] = entry
                            }
                            pd.hours > 0.0 -> {
                                val fs = LocalTime.of(9, 0)
                                val fe = fs.plusMinutes((pd.hours * 60).toLong())
                                val entry = DayManual(fs, fe, 0)
                                saveManualForDay(ctx, targetKeyPlanned, d, entry, true)
                                next[d] = entry
                            }
                            else -> {
                                val partial = if (s != null || eRaw != null) DayManual(s, eRaw, 0) else null
                                saveManualForDay(ctx, targetKeyPlanned, d, partial, true)
                                if (partial != null) next[d] = partial else next.remove(d)
                            }
                        }
                    }
                    planned = next
                    view = ViewMode.PLANNED  // ← NEW: jump to Planned after schedule scan
                    Toast.makeText(ctx, "Showing Planned", Toast.LENGTH_SHORT).show()
                    ocrErr = null
                } catch (t: Throwable) {
                    Log.e(TAG, "schedule OCR error", t)
                    ocrErr = t.message ?: "Schedule OCR failed"
                } finally {
                    isScanningPlanned = false
                }
            }
        }
    }

    // ringtone picker
    val soundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res: ActivityResult ->
        val data = res.data
        val picked: Uri? = if (Build.VERSION.SDK_INT >= 33) {
            data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        Prefs.setSoundUri(ctx, picked)
        Toast.makeText(ctx, if (picked != null) "Sound set" else "Silent", Toast.LENGTH_SHORT).show()
    }
    fun openSoundPicker() {
        val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Prefs.soundUri(ctx))
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose alarm sound")
        }
        soundPicker.launch(i)
    }

    // manual time display string
    val clockInForDisplay: String? = if (pickedHour != null && pickedMinute != null) {
        fmtDisplay.format(ZonedDateTime.now().withHour(pickedHour!!).withMinute(pickedMinute!!).withSecond(0))
    } else null

    // toggle: Planned / Actual / Compare  —— default to ACTUAL


    // helpers to get hours per day for each track
    fun plannedHours(i: Int): Double {
        planned[days[i]]?.let { return it.netHours() }
        val p = parsedPlanned ?: return 0.0
        return p.perDay[days[i]]?.hours ?: 0.0
    }
    fun actualHours(i: Int): Double {
        actualMan[days[i]]?.let { return it.netHours() }
        val p = parsedActual ?: return 0.0
        return p.perDay[days[i]]?.hours ?: 0.0
    }

    fun wtdBefore(idx: Int, usePlanned: Boolean): Double {
        var sum = 0.0
        for (i in 0 until idx.coerceIn(0, 6)) sum += if (usePlanned) plannedHours(i) else actualHours(i)
        return sum
    }

    fun todayIndexSatToFri(): Int = when (LocalDate.now().dayOfWeek) {
        DayOfWeek.SATURDAY -> 0
        DayOfWeek.SUNDAY   -> 1
        DayOfWeek.MONDAY   -> 2
        DayOfWeek.TUESDAY  -> 3
        DayOfWeek.WEDNESDAY-> 4
        DayOfWeek.THURSDAY -> 5
        DayOfWeek.FRIDAY   -> 6
    }
    val todayIdx = remember { todayIndexSatToFri() }

    val effectiveCap by remember(weeklyCap, otHours) {
        derivedStateOf { (weeklyCap + otHours).coerceAtLeast(0f) }
    }

    val calc: CalcResult? by remember(
        parsedActual, parsedPlanned, planned, actualMan,
        effectiveCap, breakMin, bufferMin, dayIdx, view, todayIdx
    ) {
        derivedStateOf {
            if (view != ViewMode.ACTUAL || dayIdx != todayIdx) return@derivedStateOf null

            val dayLabel = days[dayIdx]

            // ACTUAL clock-in required (no planned fallback)
            val actualStartLt: LocalTime? =
                actualMan[dayLabel]?.start
                    ?: parsedActual?.perDay?.get(dayLabel)?.startStr?.let(::tryParseTime)
            val chosenStart = actualStartLt ?: return@derivedStateOf null

            val startStr = fmtAmPm.format(
                ZonedDateTime.now()
                    .withHour(chosenStart.hour)
                    .withMinute(chosenStart.minute)
            )

            // Use only ACTUAL break (or default), never planned
            val usedBreak = actualMan[dayLabel]?.breakMin ?: breakMin

            // Reserve future planned hours so today doesn’t try to finish the cap
            val futureReserve = ((dayIdx + 1)..6).sumOf { plannedHours(it) }.coerceAtLeast(0.0)
            val capForToday = (effectiveCap.toDouble() - futureReserve).coerceAtLeast(0.0)

            computeLeaveBy(
                wtdBeforeToday = wtdBefore(dayIdx, usePlanned = false),
                clockInStr = startStr,
                weeklyCap = capForToday,
                unpaidBreakMin = usedBreak,
                bufferMin = 0                      // ✅ lead handled only in the alarm time
            )
        }
    }

    // Inline chip editor state
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }
    var clearTarget by remember { mutableStateOf<ClearTarget?>(null) }   // for Planned/Actual views
    var clearChooserDay by remember { mutableStateOf<String?>(null) }    // for Compare view

    // Copy dialog toggle
    var showCopyDialog by rememberSaveable { mutableStateOf(false) }

    // manual alarm wheel
    if (showManualWheel) {
        val initialLt = LocalTime.of(
            (pickedHour ?: ZonedDateTime.now().hour),
            (pickedMinute ?: ZonedDateTime.now().minute)
        )
        WheelTimeDialog(
            initial = initialLt,
            is24h = is24h,
            onConfirm = { lt ->
                pickedHour = lt.hour
                pickedMinute = lt.minute
                var whenAt = ZonedDateTime.now().withSecond(0).withNano(0)
                    .withHour(lt.hour).withMinute(lt.minute)
                if (whenAt.isBefore(ZonedDateTime.now())) whenAt = whenAt.plusDays(1)
                if (useInAppAlarm) {
                    AlarmScheduler.scheduleExactAlarm(ctx, whenAt, "LeaveBy: time to head out")
                    Toast.makeText(ctx, "Alarm scheduled for ${fmtDisplay.format(whenAt)}", Toast.LENGTH_LONG).show()
                } else {
                    setLeaveAlarmExternal(ctx, whenAt, "LeaveBy: time to head out")
                }
                showManualWheel = false
            },
            onDismiss = { showManualWheel = false }
        )
    }

    val publixUrl = "https://publix.org"
    val publixGreen = Color(0xFF00843D)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LeaveBy") },
                actions = {
                    AssistChip(
                        onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, publixUrl.toUri())) },
                        label = { Text("publix.org") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = publixGreen,
                            labelColor = Color.White
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showAlarms = true }) {
                        Icon(Icons.Filled.Alarm, contentDescription = "In-app alarms")
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showChart = true }) {
                        Icon(Icons.Filled.GridOn, contentDescription = "Hours ↔ h:mm chart")
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top actions — Schedule left, Timecard right
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { schedulePicker.launch("image/*") },
                    enabled = !isScanningPlanned && !isScanningActual
                ) {
                    Text(if (isScanningPlanned) "Scanning…" else "Scan schedule (Planned)")
                }
                Button(
                    onClick = { timecardPicker.launch("image/*") },
                    enabled = !isScanningPlanned && !isScanningActual
                ) {
                    Text(if (isScanningActual) "Scanning…" else "Scan timecard (Actual)")
                }
                if (isScanningPlanned || isScanningActual) {
                    Spacer(Modifier.width(6.dp))
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showManualWheel = true }, enabled = !isScanningPlanned && !isScanningActual) {
                    Text("Pick time (manual alarm)")
                    if (isScanningPlanned || isScanningActual) {
                        Text("Working…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (ocrErr != null) Text("⚠️ $ocrErr", color = MaterialTheme.colorScheme.error)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegBtn("Planned", view == ViewMode.PLANNED) { view = ViewMode.PLANNED }
                SegBtn("Actual",  view == ViewMode.ACTUAL)  { view = ViewMode.ACTUAL }
                SegBtn("Compare", view == ViewMode.COMPARE) { view = ViewMode.COMPARE }
            }
            // Subtle warning if Planned and Actual anchor weeks differ
            if (weeksMisaligned) {
                val pStr = dayDateFmt.format(plannedWeekStart)   // e.g., "Sep 30"
                val aStr = dayDateFmt.format(actualWeekStart)    // e.g., "Aug 30"

                AssistChip(
                    onClick = {},                 // non-interactive hint
                    enabled = false,              // renders subtly
                    leadingIcon = {
                        Icon(Icons.Filled.Warning, contentDescription = null)
                    },
                    label = {
                        Text("Heads-up: Planned week $pStr ≠ Actual week $aStr. Suggestions may be off.")
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            // Copy Planned -> Actual (only show when not on Actual)
            if (view != ViewMode.ACTUAL) {
                OutlinedButton(onClick = { showCopyDialog = true }) { Text("Copy Planned → Actual") }
            }

            // Summaries
            val plannedWTD = (0..dayIdx).sumOf { plannedHours(it) }
            val actualWTD = (0..dayIdx).sumOf { actualHours(it) }
            val plannedWeekSum = (0..6).sumOf { plannedHours(it) }
            val actualWeekSum = (0..6).sumOf { actualHours(it) }
            val remainingPlanned = (effectiveCap.toDouble() - plannedWTD).coerceAtLeast(0.0)
            val remainingActual  = (effectiveCap.toDouble() - actualWTD).coerceAtLeast(0.0)
            val projPlannedDelta = effectiveCap.toDouble() - plannedWeekSum
            val projActualDelta  = effectiveCap.toDouble() - actualWeekSum

            fun hoursStr(v: Double) = fmtHours(locale, v, showHm)

            // Row with WTD + toggle (format)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    val label = when (view) {
                        ViewMode.ACTUAL  -> "Actual WTD: ${hoursStr(actualWTD)}"
                        ViewMode.PLANNED -> "Planned WTD: ${hoursStr(plannedWTD)}"
                        ViewMode.COMPARE -> "WTD — P: ${hoursStr(plannedWTD)}  A: ${hoursStr(actualWTD)}"
                    }
                    Text(label, style = MaterialTheme.typography.titleMedium)

                    // Show cap and OT add-on (if any)
                    val capLine = buildString {
                        append("Cap: ")
                        append(fmtHours(locale, weeklyCap.toDouble(), showHm))
                        if (otHours > 0f) {
                            append("  + OT ")
                            append(fmtHours(locale, otHours.toDouble(), showHm))
                        }
                    }
                    Text(
                        capLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showHm = !showHm }) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Toggle hours format")
                }
            }

            when (view) {
                ViewMode.PLANNED -> {
                    Text("Remaining to cap (now): ${hoursStr(remainingPlanned)}")
                    Text(
                        if (projPlannedDelta >= 0)
                            "If you keep this plan: ${hoursStr(projPlannedDelta)} left at week end"
                        else "If you keep this plan: ${hoursStr(-projPlannedDelta)} over cap"
                    )
                }
                ViewMode.ACTUAL -> {
                    clockInForDisplay?.let { Text("Clock-in: $it") }
                    Text("Remaining to cap (now): ${hoursStr(remainingActual)}")
                    Text(
                        if (projActualDelta >= 0)
                            "Projected week (actual pace): ${hoursStr(projActualDelta)} left"
                        else "Projected week (actual pace): ${hoursStr(-projActualDelta)} over"
                    )

                    val todayIdx = todayIndexSatToFri()
                    if (dayIdx != todayIdx) {
                        Text(
                            "Tip: Leave-by alarm is only shown for today.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    calc?.let {
                        val out = suggestPunchOut(
                            dayIdx = dayIdx,
                            days = days,
                            usePlannedTrackForStart = false,           // ACTUAL
                            planned = planned,
                            actualMan = actualMan,
                            parsedPlanned = parsedPlanned,
                            parsedActual = parsedActual,
                            plannedHours = { i -> plannedHours(i) },
                            actualHours  = { i -> actualHours(i) },
                            effectiveCap = effectiveCap.toDouble(),
                            defaultBreakMin = breakMin,
                            bufferLeadMin = bufferMin,
                            fmtAmPm = fmtAmPm,
                            guardNotEarlierThanPlanned = false         // ← show/schedule from ACTUAL cap, not plan
                        )

                        // Show the real cap-aware time; fall back if needed
                        val leaveBy = out.capExact ?: out.plannedEnd ?: out.boundary ?: return@let
                        val notifyAt = out.notifyAt ?: leaveBy.minusMinutes(bufferMin.toLong())

                        val brkNote = if (out.usedBreakMin > 0) "incl. ${out.usedBreakMin}m break" else "no break"
                        Text(
                            "Leave by ($brkNote): ${fmtDisplay.format(leaveBy)}  •  alarm −${bufferMin}m → ${fmtDisplay.format(notifyAt)}"
                        )

                        Button(onClick = {
                            if (useInAppAlarm) {
                                AlarmScheduler.scheduleExactAlarm(ctx, notifyAt, "LeaveBy: time to head out")
                            } else {
                                setLeaveAlarmExternal(ctx, notifyAt, "LeaveBy: time to head out")
                            }
                        }) {
                            Text(if (useInAppAlarm) "Schedule (no extra taps)" else "Set alarm in Clock")
                        }
                    }
                }
                ViewMode.COMPARE -> {
                    Text(
                        "Totals — Planned: ${hoursStr(plannedWeekSum)} • Actual: ${hoursStr(actualWeekSum)} • Δ ${hoursStr(actualWeekSum - plannedWeekSum)}"
                    )
                    Spacer(Modifier.height(8.dp))
                    PlannedActualBarChart(
                        days = days,
                        planned = days.associateWithIndexed { i -> plannedHours(i) },
                        actual = days.associateWithIndexed { i -> actualHours(i) },
                        height = 200.dp,
                        onBarTap = { dayIndex, isPlanned ->
                            val day = days[dayIndex]
                            editTarget = EditTarget(day, isPlanned, EditKind.IN)
                        },
                        showAsClock = showHm,
                        locale = locale
                    )
                }
            }

            HorizontalDivider()

// ------- Per-day UI -------
// Compare = original list (with P/A + Δ + edit chips)
// Planned & Actual = unified pastel pills
            if (view == ViewMode.COMPARE) {

                days.forEachIndexed { i, d ->
                    val pRow = parsedPlanned?.perDay?.get(d)
                    val aRow = parsedActual?.perDay?.get(d)
                    val pH = plannedHours(i)
                    val aH = actualHours(i)

                    val plannedTimes = planned[d]?.let { partialLabel(it.start, it.end, fmtAmPm) }
                        ?: labelFromStrings(pRow?.startStr, pRow?.endStr, fmtAmPm)

                    val actualTimes = actualMan[d]?.let { partialLabel(it.start, it.end, fmtAmPm) }
                        ?: labelFromStrings(aRow?.startStr, aRow?.endStr, fmtAmPm)

                    ListItem(
                        headlineContent = { Text(dayLabel(i)) },
                        supportingContent = {
                            fun both(times: String?, hrs: Double): String =
                                when {
                                    times != null && hrs > 0.0 -> "$times (${fmtHours(locale, hrs, showHm)})"
                                    times != null              -> times
                                    hrs > 0.0                  -> "≈ ${fmtHours(locale, hrs, showHm)}"
                                    else                       -> "—"
                                }

                            Text("Planned ${both(plannedTimes, pH)} • Actual ${both(actualTimes, aH)} • Δ ${fmtHours(locale, aH - pH, showHm)}")
                        },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("P", style = MaterialTheme.typography.labelSmall)
                                EditChips(
                                    onIn    = { editTarget = EditTarget(d, true,  EditKind.IN) },
                                    onBreak = { editTarget = EditTarget(d, true,  EditKind.BREAK) },
                                    onOut   = { editTarget = EditTarget(d, true,  EditKind.OUT) },
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("A", style = MaterialTheme.typography.labelSmall)
                                EditChips(
                                    onIn    = { editTarget = EditTarget(d, false, EditKind.IN) },
                                    onBreak = { editTarget = EditTarget(d, false, EditKind.BREAK) },
                                    onOut   = { editTarget = EditTarget(d, false, EditKind.OUT) },
                                )
                            }
                        },
                        modifier = Modifier.combinedClickable(
                            onClick = { dayIdx = i },
                            onLongClick = { clearChooserDay = d }      // long press to pick Planned vs Actual to clear
                        )
                    )
                    HorizontalDivider()
                }

            } else {
                // ---- Unified pastel pills for PLANNED & ACTUAL ----
                val chipGreen  = PastelGreen
                val chipYellow = PastelYellow
                val chipRed    = PastelRed

                @Composable
                fun DayHeader(i: Int, d: String, wtd: Double) {
                    val base = when (view) {
                        ViewMode.PLANNED -> plannedWeekStart
                        ViewMode.ACTUAL  -> actualWeekStart
                        else -> plannedWeekStart
                    }
                    val dateStr = dayDateFmt.format(dayDateFrom(base, i))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text("$d $dateStr: ${hoursStr(wtd)} (wtd)", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                }

                days.forEachIndexed { i, d ->
                    val pRow = parsedPlanned?.perDay?.get(d)
                    val aRow = parsedActual?.perDay?.get(d)

                    val isPlannedView = (view == ViewMode.PLANNED)
                    fun hrsAt(idx: Int) = if (isPlannedView) plannedHours(idx) else actualHours(idx)
                    val wtdForDay = (0..i).sumOf { hrsAt(it) }

                    // ONLY read from the active track so tabs don't fill each other
                    val startLt: LocalTime? = if (isPlannedView) {
                        planned[d]?.start ?: pRow?.startStr?.let(::tryParseTime)
                    } else {
                        actualMan[d]?.start ?: aRow?.startStr?.let(::tryParseTime)
                    }

                    val endLt: LocalTime? = if (isPlannedView) {
                        planned[d]?.end ?: pRow?.endStr?.let(::tryParseTime)
                    } else {
                        actualMan[d]?.end ?: aRow?.endStr?.let(::tryParseTime)
                    }

                    val dayHours = hrsAt(i)
                    val isOffDay = (dayHours <= 0.0 && startLt == null && endLt == null)

                    val breakMinForDay =
                        (if (isPlannedView) planned[d]?.breakMin else actualMan[d]?.breakMin)
                            ?: breakMin

                    val breakLabel = if (isOffDay || breakMinForDay <= 0) "—" else "${breakMinForDay}m"
                    fun fmt(t: LocalTime?) =
                        t?.let { fmtAmPm.format(ZonedDateTime.now().withHour(it.hour).withMinute(it.minute)) } ?: "—"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { dayIdx = i },
                                onLongClick = { clearTarget = ClearTarget(d, isPlannedView) } // long press to clear this track
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            DayHeader(i, d, wtdForDay)

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ModernChip(Modifier.weight(1f), "IN", fmt(startLt), PastelGreen) {
                                    editTarget = EditTarget(d, isPlannedView, EditKind.IN)
                                }
                                ModernChip(Modifier.weight(1f), "BREAK", breakLabel, PastelYellow) {
                                    editTarget = EditTarget(d, isPlannedView, EditKind.BREAK)
                                }
                                ModernChip(Modifier.weight(1f), "OUT", fmt(endLt), PastelRed) {
                                    editTarget = EditTarget(d, isPlannedView, EditKind.OUT)
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    HorizontalDivider()
                }
            }
            // Reset buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = {
                    planned = emptyMap()
                    parsedPlanned = null
                    clearManualWeek(ctx, plannedWeekKey, days, true)
                    Toast.makeText(ctx, "Cleared Planned (scans + edits).", Toast.LENGTH_SHORT).show()
                }) { Text("Reset Planned") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = {
                    actualMan = emptyMap()
                    parsedActual = null
                    clearManualWeek(ctx, actualWeekKey,  days, false)
                    Toast.makeText(ctx, "Cleared Actual (scans + edits).", Toast.LENGTH_SHORT).show()
                }) { Text("Reset Actual") }
            }
        }
    }
// ---- Long-press clear dialogs (place right before "Conversion chart dialog") ----
    clearTarget?.let { t ->
        val which = if (t.plannedTrack) "Planned" else "Actual"
        AlertDialog(
            onDismissRequest = { clearTarget = null },
            title = { Text("Clear $which for ${t.day}?") },
            text = { Text("This removes IN, BREAK, and OUT for this day on the $which track.") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        val wk = if (t.plannedTrack) plannedWeekKey else actualWeekKey
                        val tombstone = DayManual(null, null, 0)
                        saveManualForDay(ctx, wk, t.day, tombstone, planned = t.plannedTrack)
                        if (t.plannedTrack) {
                            planned = planned + (t.day to tombstone)
                        } else {
                            actualMan = actualMan + (t.day to tombstone)
                        }
                        clearTarget = null
                        Toast.makeText(ctx, "Cleared $which for ${t.day}.", Toast.LENGTH_SHORT).show()
                    }) { Text("Clear") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { clearTarget = null }) { Text("Cancel") }
                }
            },
            dismissButton = {}
        )
    }

    clearChooserDay?.let { d ->
        AlertDialog(
            onDismissRequest = { clearChooserDay = null },
            title = { Text("Clear $d") },
            text = { Text("Which track do you want to clear for this day?") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        val tombstone = DayManual(null, null, 0)
                        saveManualForDay(ctx, plannedWeekKey, d, tombstone, planned = true)
                        planned = planned + (d to tombstone)
                        clearChooserDay = null
                        Toast.makeText(ctx, "Cleared Planned for $d.", Toast.LENGTH_SHORT).show()
                    }) { Text("Planned") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val tombstone = DayManual(null, null, 0)
                        saveManualForDay(ctx, actualWeekKey, d, tombstone, planned = false)
                        actualMan = actualMan + (d to tombstone)
                        clearChooserDay = null
                        Toast.makeText(ctx, "Cleared Actual for $d.", Toast.LENGTH_SHORT).show()
                    }) { Text("Actual") }
                }
            },
            dismissButton = { TextButton(onClick = { clearChooserDay = null }) { Text("Cancel") } }
        )
    }

    // Conversion chart dialog
    if (showChart) {
        HoursChartDialog(onDismiss = { showChart = false })
    }

    // In-app alarms dialog
    if (showAlarms) {
        InAppAlarmsDialog(onClose = { showAlarms = false })
    }

    // Copy dialog UI
    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = { Text("Copy Planned → Actual") },
            text = { Text("Do you want to fill only empty days, or overwrite all Actual days for this week?") },
            confirmButton = {
                TextButton(onClick = {
                    val newMap = mutableMapOf<String, DayManual>()
                    for (d in listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")) {
                        val srcManual = planned[d]
                        val src = srcManual ?: parsedPlanned?.perDay?.get(d)?.let {
                            val s = it.startStr?.let(::tryParseTime)
                            val e = it.endStr?.let(::tryParseTime)
                            when {
                                s != null && e != null -> {
                                    val brk = if (it.hours > 0.0) inferBreakMin(s, e, it.hours) else 0
                                    DayManual(s, e, brk)
                                }
                                it.hours > 0.0 -> {
                                    val fs = LocalTime.of(9, 0)
                                    val fe = fs.plusMinutes((it.hours * 60).toLong())
                                    DayManual(fs, fe, 0)
                                }
                                else -> null
                            }
                        }
                        if (src != null) {
                            saveManualForDay(ctx, actualWeekKey, d, src, false)
                            newMap[d] = src
                        } else {
                            saveManualForDay(ctx, actualWeekKey, d, null, false)
                        }
                    }
                    actualMan = newMap
                    showCopyDialog = false
                    Toast.makeText(ctx, "Copied (overwrite all).", Toast.LENGTH_SHORT).show()
                }) { Text("Overwrite all") }
            },
            dismissButton = {
                TextButton(onClick = {
                    var changed = false
                    var next = actualMan.toMutableMap()
                    for (d in listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")) {
                        if (next.containsKey(d)) continue
                        val srcManual = planned[d]
                        val src = srcManual ?: parsedPlanned?.perDay?.get(d)?.let {
                            val s = it.startStr?.let(::tryParseTime)
                            val e = it.endStr?.let(::tryParseTime)
                            when {
                                s != null && e != null -> {
                                    val brk = if (it.hours > 0.0) inferBreakMin(s, e, it.hours) else 0
                                    DayManual(s, e, brk)
                                }
                                it.hours > 0.0 -> {
                                    val fs = LocalTime.of(9, 0)
                                    val fe = fs.plusMinutes((it.hours * 60).toLong())
                                    DayManual(fs, fe, 0)
                                }
                                else -> null
                            }
                        }
                        if (src != null) {
                            saveManualForDay(ctx, actualWeekKey, d, src, false)
                            next[d] = src
                            changed = true
                        }
                    }
                    if (changed) {
                        actualMan = next
                        Toast.makeText(ctx, "Copied planned into empty actual days.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, "Nothing to copy 🤙", Toast.LENGTH_SHORT).show()
                    }
                    showCopyDialog = false
                }) { Text("Fill empty only") }
            }
        )
    }

    // Settings dialog
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Done") } },
            title = { Text("Settings") },
            text = {
                SettingsSheet(
                    weeklyCap = weeklyCap, onWeeklyCap = { weeklyCap = it },
                    breakMin = breakMin, onBreakMin = { breakMin = it },
                    bufferMin = bufferMin, onBufferMin = { bufferMin = it },
                    allowOT = allowOT, onAllowOT = { allowOT = it },
                    otHours = otHours, onOtHours = { otHours = it },
                    useInAppAlarm = useInAppAlarm, onUseInAppAlarm = {
                        useInAppAlarm = it; Prefs.setUseInAppAlarm(ctx, it)
                    },
                    onPickSound = { openSoundPicker() },
                    vibrate = vibrate, onVibrate = { vibrate = it; Prefs.setVibrate(ctx, it) },
                    volumePercent = volumePercent, onVolume = { volumePercent = it; Prefs.setVolume(ctx, it) },
                    remEnabled = remEnabled, onRemEnabled = {
                        remEnabled = it; Prefs.setRemEnabled(ctx, it)
                        ReminderScheduler.saveAndSchedule(ctx, remEnabled, remDays, remTime)
                    },
                    remDays = remDays, onRemDays = {
                        remDays = it; Prefs.setRemDays(ctx, it)
                        ReminderScheduler.saveAndSchedule(ctx, remEnabled, remDays, remTime)
                    },
                    remTime = remTime, onRemTime = {
                        remTime = it; Prefs.setRemTime(ctx, it)
                        ReminderScheduler.saveAndSchedule(ctx, remEnabled, remDays, remTime)
                    },
                    onShowMessage = { showMore = true },
                    onShowTutorial = { showTutorial = true }   // ← add this line
                )
            }
        )
    }

    if (showMore) HeartsMessageDialog(
        onDismiss = { showMore = false },
        enableHeartbeat = vibrate
    )
// Tutorial dialog
    if (showTutorial) {
        TutorialDialog(
            onClose = {
                showTutorial = false
                Prefs.setTutorialSeen(ctx, true)
            }
        )
    }

    // persist calc settings
    LaunchedEffect(weeklyCap, breakMin, bufferMin, allowOT, otHours, useInAppAlarm) {
        Prefs.save(ctx, weeklyCap, breakMin, bufferMin, allowOT, otHours, useInAppAlarm)
    }

    // Traditional editors (keep available)
    var editPlannedDay by rememberSaveable { mutableStateOf<String?>(null) }
    var editActualDay by rememberSaveable { mutableStateOf<String?>(null) }

    editPlannedDay?.let { d ->
        val cur = planned[d]
        DayEditDialog(
            dayLabel = "${d} ${dayDateFmt.format(dayDateFrom(plannedWeekStart, days.indexOf(d)))} (Planned)",
            initialStart = cur?.start,
            initialEnd = cur?.end,
            initialBreakMin = cur?.breakMin ?: 0,
            quarterStepMinutes = true,
            onClear = {
                saveManualForDay(ctx, plannedWeekKey, d, null, true)
                planned = planned - d
                editPlannedDay = null
            },
            onSave = { s, e, brk ->
                val prev = planned[d]
                val s2 = s ?: prev?.start
                val e2 = e ?: prev?.end
                val eObj = DayManual(s2, e2, brk)
                saveManualForDay(ctx, plannedWeekKey, d, eObj, true)
                planned = if (s2 == null && e2 == null) planned - d else planned + (d to eObj)
                editPlannedDay = null
            },
            onDismiss = { editPlannedDay = null }
        )
    }
    editActualDay?.let { d ->
        val cur = actualMan[d]
        DayEditDialog(
            dayLabel = "${d} ${dayDateFmt.format(dayDateFrom(actualWeekStart, days.indexOf(d)))} (Actual)",
            initialStart = cur?.start,
            initialEnd = cur?.end,
            initialBreakMin = cur?.breakMin ?: 0,
            quarterStepMinutes = false,
            onClear = {
                saveManualForDay(ctx, actualWeekKey, d, null, false)
                actualMan = actualMan - d
                editActualDay = null
            },
            onSave = { s, e, brk ->
                val prev = actualMan[d]
                val s2 = s ?: prev?.start
                val e2 = e ?: prev?.end
                val eObj = DayManual(s2, e2, brk)
                saveManualForDay(ctx, actualWeekKey, d, eObj, false)
                actualMan = if (s2 == null && e2 == null) actualMan - d else actualMan + (d to eObj)
                editActualDay = null
            },
            onDismiss = { editActualDay = null }
        )
    }

    /* ------------ Chip dialogs launcher (IN / BREAK / OUT) ------------ */
    editTarget?.let { t ->
        val day = t.day
        val isPlanned = t.plannedTrack
        val dayIndex = days.indexOf(day)
        val base = if (isPlanned) plannedWeekStart else actualWeekStart
        val dayWithDate = "$day ${dayDateFmt.format(base.plusDays(dayIndex.toLong()))}"


        val entry: DayManual? = if (isPlanned) planned[day] else actualMan[day]
        fun saveEntry(newEntry: DayManual?) {
            val wkKey = if (isPlanned) plannedWeekKey else actualWeekKey
            saveManualForDay(ctx, wkKey, day, newEntry, planned = isPlanned)
            if (isPlanned) {
                planned = if (newEntry == null) planned - day else planned + (day to newEntry)
            } else {
                actualMan = if (newEntry == null) actualMan - day else actualMan + (day to newEntry)
            }
        }

        val actualStartLt: LocalTime? =
            actualMan[day]?.start ?: parsedActual?.perDay?.get(day)?.startStr?.let(::tryParseTime)
        val plannedEndLt: LocalTime? =
            planned[day]?.end ?: parsedPlanned?.perDay?.get(day)?.endStr?.let(::tryParseTime)
        val plannedNetHrs: Double =
            (planned[day]?.netHours() ?: parsedPlanned?.perDay?.get(day)?.hours ?: 0.0)
        val usedBreakForSuggest = (if (isPlanned) planned[day]?.breakMin else actualMan[day]?.breakMin)
            ?: breakMin

        when (t.kind) {
            EditKind.IN -> {
                // What we already have from OCR and manual edits
                val plannedStartLt: LocalTime? =
                    planned[day]?.start ?: parsedPlanned?.perDay?.get(day)?.startStr?.let(::tryParseTime)
                val actualStartLt2: LocalTime? =
                    actualMan[day]?.start ?: parsedActual?.perDay?.get(day)?.startStr?.let(::tryParseTime)

                // Suggest the "other" track's start
                val suggestion: LocalTime? = if (isPlanned) actualStartLt2 else plannedStartLt

                InDialog(
                    title = "$dayWithDate — ${if (isPlanned) "Planned" else "Actual"}: IN",
                    initial = entry?.start ?: suggestion ?: LocalTime.of(9, 0),
                    is24h = is24h,
                    suggestion = suggestion,
                    onSave = { newStart ->
                        val e = entry ?: DayManual()
                        saveEntry(e.copy(start = newStart))
                        editTarget = null
                    },
                    onClear = {
                        val e = entry ?: DayManual()
                        saveEntry(e.copy(start = null))
                        editTarget = null
                    },
                    onDismiss = { editTarget = null }
                )
            }
            EditKind.BREAK -> {
                BreakWithStartDialog(
                    title = "$dayWithDate — ${if (isPlanned) "Planned" else "Actual"}: Break",
                    initialBreak = (entry?.breakMin ?: breakMin).coerceIn(0, 120),
                    initialStart = LocalTime.now(),
                    is24h = is24h,
                    onConfirm = { newMin ->
                        val e = entry ?: DayManual()
                        saveEntry(e.copy(breakMin = newMin))
                        editTarget = null
                    },
                    onStartAtWithAlarm = { startAt, mins, alarmLead ->
                        val e = entry ?: DayManual()
                        saveEntry(e.copy(breakMin = mins))
                        val now = ZonedDateTime.now()
                        val alarmAt = now.withSecond(0).withNano(0)
                            .withHour(startAt.hour).withMinute(startAt.minute)
                            .plusMinutes(mins.toLong())
                            .minusMinutes(alarmLead.toLong())
                        if (alarmAt.isBefore(now)) {
                            Toast.makeText(ctx, "That alarm time is in the past.", Toast.LENGTH_SHORT).show()
                        } else {
                            if (useInAppAlarm) AlarmScheduler.scheduleExactAlarm(ctx, alarmAt, "Break ends")
                            else setLeaveAlarmExternal(ctx, alarmAt, "Break ends")
                            Toast.makeText(ctx, "Break alarm set for ${fmtDisplay.format(alarmAt)}", Toast.LENGTH_SHORT).show()
                        }
                        editTarget = null
                    },
                    onClear = {                          // NEW
                        val e = entry ?: DayManual()
                        saveEntry(e.copy(breakMin = 0))
                        editTarget = null
                    },
                    onDismiss = { editTarget = null }
                )
            }

            EditKind.OUT -> {
                val dayIndex = days.indexOf(day)

                // same break fallback you were using before
                val usedBreakForSuggest =
                    (if (isPlanned) planned[day]?.breakMin else actualMan[day]?.breakMin) ?: breakMin

                // one truthy calc for both suggestions
                val out = suggestPunchOut(
                    dayIdx = dayIndex,
                    days = days,
                    usePlannedTrackForStart = isPlanned,
                    planned = planned,
                    actualMan = actualMan,
                    parsedPlanned = parsedPlanned,
                    parsedActual = parsedActual,
                    plannedHours = { i -> plannedHours(i) },
                    actualHours  = { i -> actualHours(i) },
                    effectiveCap = effectiveCap.toDouble(),
                    defaultBreakMin = usedBreakForSuggest,
                    bufferLeadMin = bufferMin,
                    fmtAmPm = fmtAmPm,
                    guardNotEarlierThanPlanned = isPlanned   // ✅ false for Actual, true for Planned
                )
                OutDialog(
                    title = "$dayWithDate — ${if (isPlanned) "Planned" else "Actual"}: OUT",
                    initial = entry?.end
                        ?: out.capExact?.toLocalTime()          // default to “1:59” style cap
                        ?: out.plannedEnd?.toLocalTime()
                        ?: LocalTime.of(17, 0),
                    is24h = is24h,
                    suggestionA = out.capExact,                 // “Use 01:59 PM”
                    suggestionB = out.plannedEnd,               // “Planned 03:00 PM”
                    onSave = { newEnd ->
                        val e = entry ?: DayManual()
                        saveEntry(e.copy(end = newEnd))
                        editTarget = null
                    },
                    onSaveAndAlarm = { newEnd, alarmLeadMin ->
                        val e = entry ?: DayManual()
                        saveEntry(e.copy(end = newEnd))

                        var whenAt = ZonedDateTime.now().withSecond(0).withNano(0)
                            .withHour(newEnd.hour).withMinute(newEnd.minute)
                            .minusMinutes(alarmLeadMin.toLong())
                        if (whenAt.isBefore(ZonedDateTime.now())) whenAt = whenAt.plusDays(1)

                        if (useInAppAlarm) AlarmScheduler.scheduleExactAlarm(ctx, whenAt, "LeaveBy: time to head out")
                        else setLeaveAlarmExternal(ctx, whenAt, "LeaveBy: time to head out")

                        Toast.makeText(ctx, "Alarm set for ${fmtDisplay.format(whenAt)}", Toast.LENGTH_SHORT).show()
                        editTarget = null
                    },
                    onClear = {
                        val e = entry ?: DayManual()
                        saveEntry(e.copy(end = null))
                        editTarget = null
                    },
                    onDismiss = { editTarget = null }
                )
            }
        }
    }
}
/* ---------- Modern “pill” chip (pastel-friendly, softened) ---------- */
private fun lighten(c: Color, t: Float) = lerp(c, Color.White, t.coerceIn(0f, 1f))
private fun darken(c: Color, t: Float)  = lerp(c, Color.Black, t.coerceIn(0f, 1f))

// softer gradient amounts
private const val CHIP_LIGHT = 0.08f   // was 0.18f
private const val CHIP_DARK  = 0.05f   // was 0.12f

private fun chipGradient(base: Color): Brush = Brush.linearGradient(
    listOf(
        lighten(base, CHIP_LIGHT),   // top/left highlight
        base,                        // body
        darken(base, CHIP_DARK)      // bottom/right shade
    )
)

@Composable
private fun ModernChip(
    modifier: Modifier = Modifier,
    label: String,
    sub: String,
    color: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val brush = remember(color) { chipGradient(color) }

    androidx.compose.material3.Surface(
        modifier = modifier.height(56.dp),
        onClick = onClick,
        color = Color.Transparent,                 // let our gradient show
        shape = shape,
        tonalElevation = 1.dp,                     // was 2.dp
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f)) // was 0.25f
    ) {
        Box(
            Modifier
                .background(brush = brush, shape = shape)
                .padding(vertical = 6.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(sub,   color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/* ---------- In-app alarms UI ---------- */

@Composable
private fun InAppAlarmsDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(AlarmScheduler.listAlarms(ctx)) }
    var showNew by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<AlarmScheduler.InAppAlarm?>(null) }

    fun refresh() { items = AlarmScheduler.listAlarms(ctx) }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
        title = { Text("In-app alarms") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (items.isEmpty()) {
                    Text("No alarms scheduled.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    items.forEach { e ->
                        AlarmRow(
                            id = e.id,
                            triggerAt = e.triggerAt,
                            label = e.label,
                            onEdit = { editItem = e },
                            onDelete = { id ->
                                AlarmScheduler.cancelAlarm(ctx, id)
                                refresh()
                            }
                        )
                        Divider()
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = { showNew = true }
                ) { Text("Add alarm") }

                if (Build.VERSION.SDK_INT >= 31 && !AlarmScheduler.canScheduleExact(ctx)) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            ctx.startActivity(i)
                        }) { Text("Enable exact alarms in system settings") }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    )

    if (showNew) {
        NewAlarmDialog(
            onCreate = { whenAtMillis, label ->
                val at = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(whenAtMillis),
                    ZoneId.systemDefault()
                )
                AlarmScheduler.scheduleExactAlarm(ctx, at, label)
                Toast.makeText(ctx, "Alarm added.", Toast.LENGTH_SHORT).show()
                showNew = false
                refresh()
            },
            onDismiss = { showNew = false }
        )
    }

    editItem?.let { e ->
        EditAlarmDialog(
            initialTriggerAt = e.triggerAt,
            initialLabel = e.label,
            onSave = { newWhen, newLabel ->
                AlarmScheduler.cancelAlarm(ctx, e.id)
                val at = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(newWhen),
                    ZoneId.systemDefault()
                )
                AlarmScheduler.scheduleExactAlarm(ctx, at, newLabel)
                editItem = null
                refresh()
            },
            onDismiss = { editItem = null }
        )
    }
}

@Composable
private fun AlarmRow(
    id: Int,
    triggerAt: Long,
    label: String,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    val local = ZonedDateTime.ofInstant(Instant.ofEpochMilli(triggerAt), ZoneId.systemDefault())
    val fmt = DateTimeFormatter.ofPattern("EEE, MMM d • hh:mm a")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(fmt.format(local), fontWeight = FontWeight.SemiBold)
            if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onEdit(id) }) { Text("Edit") }
            TextButton(onClick = { onDelete(id) }) { Text("Delete") }
        }
    }
}

@Composable
private fun NewAlarmDialog(
    onCreate: (whenAtMillis: Long, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    var open by remember { mutableStateOf(true) }
    if (!open) return

    var label by remember { mutableStateOf("LeaveBy alarm") }
    val now = ZonedDateTime.now()
    var picked by remember { mutableStateOf(now.withSecond(0).withNano(0).plusMinutes(5)) }

    AlertDialog(
        onDismissRequest = { open = false; onDismiss() },
        title = { Text("New alarm") },
        confirmButton = {
            TextButton(onClick = {
                var at = picked
                if (at.isBefore(ZonedDateTime.now())) at = at.plusDays(1)
                onCreate(at.toInstant().toEpochMilli(), label)
                open = false
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { open = false; onDismiss() }) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true
                )
                TimeWheelZdt(
                    initial = picked,
                    onChange = { picked = it }
                )
            }
        }
    )
}

@Composable
private fun EditAlarmDialog(
    initialTriggerAt: Long,
    initialLabel: String,
    onSave: (whenAtMillis: Long, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    var open by remember { mutableStateOf(true) }
    if (!open) return

    var label by remember { mutableStateOf(initialLabel) }
    var picked by remember {
        mutableStateOf(ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialTriggerAt), ZoneId.systemDefault()))
    }

    AlertDialog(
        onDismissRequest = { open = false; onDismiss() },
        title = { Text("Edit alarm") },
        confirmButton = {
            TextButton(onClick = {
                var at = picked
                if (at.isBefore(ZonedDateTime.now())) at = at.plusDays(1)
                onSave(at.toInstant().toEpochMilli(), label)
                open = false
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { open = false; onDismiss() }) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true
                )
                TimeWheelZdt(
                    initial = picked,
                    onChange = { picked = it }
                )
            }
        }
    )
}

/* Simple hour/minute wheel that returns a ZonedDateTime on the same day */
@Composable
private fun TimeWheelZdt(
    initial: ZonedDateTime,
    onChange: (ZonedDateTime) -> Unit
) {
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var ampm by remember { mutableStateOf(if (initial.hour < 12) 0 else 1) }

    fun emit() {
        val base = ZonedDateTime.now().withSecond(0).withNano(0)
        onChange(base.withHour(hour).withMinute(minute))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidNumberPicker(
            count = if (is24) 24 else 12,
            min = if (is24) 0 else 1,
            value = if (is24) hour else ((hour % 12).let { if (it == 0) 12 else it }),
            labels = if (is24) null else (1..12).map { it.toString().padStart(2, '0') }.toTypedArray(),
            onChange = { v ->
                hour = if (is24) v else {
                    val h12 = v % 12
                    if (ampm == 0) (if (h12 == 12) 0 else h12) else (if (h12 == 12) 12 else h12 + 12)
                }
                emit()
            }
        )
        Spacer(Modifier.width(12.dp))
        AndroidNumberPicker(
            count = 60, min = 0, value = minute, labels = null,
            onChange = { v -> minute = v; emit() }
        )
        if (!is24) {
            Spacer(Modifier.width(12.dp))
            AndroidNumberPicker(
                count = 2, min = 0, value = ampm, labels = arrayOf("AM", "PM"),
                onChange = { v ->
                    ampm = v
                    val h12 = hour % 12
                    hour = if (v == 0) h12 else h12 + 12
                    emit()
                }
            )
        }
    }
}

/* Tiny helper so we don't pull in NumberPicker everywhere */
@Composable
private fun AndroidNumberPicker(
    count: Int,
    min: Int,
    value: Int,
    labels: Array<String>?,
    onChange: (Int) -> Unit
) {
    AndroidView(factory = { ctx ->
        NumberPicker(ctx).apply {
            minValue = min
            maxValue = min + count - 1
            this.value = value
            wrapSelectorWheel = true
            if (labels != null) displayedValues = labels
            setOnValueChangedListener { _, _, newVal -> onChange(newVal) }
        }
    })
}

/* ---------- UI helpers ---------- */

@Composable
private fun SegBtn(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)

    // tweak these two lists to change the gradient look
    val selectedBrush = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    val unselectedBrush = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface
        )
    )

    val brush = if (selected) selectedBrush else unselectedBrush
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface

    androidx.compose.material3.Surface(
        onClick = onClick,            // keeps ripple & semantics
        shape = shape,
        color = Color.Transparent,    // we draw our own background
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Box(
            Modifier
                .background(brush, shape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

private fun fmtHours(locale: Locale, v: Double, asClock: Boolean): String {
    return if (!asClock) {
        if (abs(v) < 0.005) "0.00 h" else String.format(locale, "%.2f h", v)
    } else {
        val totalMin = (v * 60.0).roundToInt().coerceAtLeast(0)
        val h = totalMin / 60
        val m = totalMin % 60
        "%d:%02d h".format(h, m)
    }
}

private fun fmtBarLabel(locale: Locale, v: Float, asClock: Boolean): String {
    return if (!asClock) String.format(locale, "%.1f", v) else {
        val totalMin = (v * 60f).roundToInt().coerceAtLeast(0)
        val h = totalMin / 60
        val m = totalMin % 60
        "%d:%02d".format(h, m)
    }
}

// partial label helpers
private fun partialLabel(s: LocalTime?, e: LocalTime?, fmt: DateTimeFormatter): String? {
    val now = ZonedDateTime.now()
    val start = s?.let { fmt.format(now.withHour(it.hour).withMinute(it.minute)) }
    val end   = e?.let { fmt.format(now.withHour(it.hour).withMinute(it.minute)) }
    return when {
        start != null && end != null -> "$start–$end"
        start != null -> "IN $start"
        end != null -> "OUT $end"
        else -> null
    }
}

private fun labelFromStrings(startStr: String?, endStr: String?, fmt: DateTimeFormatter): String? {
    val sLt = startStr?.let { tryParseTime(it) }
    val eLt = endStr?.let { tryParseTime(it) }
    return partialLabel(sLt, eLt, fmt)
}

/* ---------- Settings sheet ---------- */
@Composable
private fun SettingsSheet(
    weeklyCap: Float, onWeeklyCap: (Float) -> Unit,
    breakMin: Int, onBreakMin: (Int) -> Unit,
    bufferMin: Int, onBufferMin: (Int) -> Unit,
    allowOT: Boolean, onAllowOT: (Boolean) -> Unit,
    otHours: Float, onOtHours: (Float) -> Unit,
    useInAppAlarm: Boolean, onUseInAppAlarm: (Boolean) -> Unit,
    onPickSound: () -> Unit,
    vibrate: Boolean, onVibrate: (Boolean) -> Unit,
    volumePercent: Int, onVolume: (Int) -> Unit,
    remEnabled: Boolean, onRemEnabled: (Boolean) -> Unit,
    remDays: Set<Int>, onRemDays: (Set<Int>) -> Unit,
    remTime: LocalTime, onRemTime: (LocalTime) -> Unit,
    onShowMessage: () -> Unit,
    onShowTutorial: () -> Unit,                 // ← add this
) {
    val haptics = LocalHapticFeedback.current
    val ctx = LocalContext.current
    val locale = remember { Locale.getDefault() }

    var showCap by remember { mutableStateOf(false) }
    var showOt by remember { mutableStateOf(false) }
    var showBreak by remember { mutableStateOf(false) }
    var showBuf by remember { mutableStateOf(false) }
    var showRemTime by remember { mutableStateOf(false) }

    Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard(title = "Work targets", initiallyExpanded = false) {
            ValueRow("Cap (hrs)", String.format(locale, "%.1f", weeklyCap)) { showCap = true }
            ValueRow("OT hours (+cap)", String.format(locale, "%.1f", otHours)) { showOt = true }
            ValueRow("Default break (min)", String.format(locale, "%d", breakMin)) { showBreak = true }
            ValueRow("Alarm early (min)", String.format(locale, "%d", bufferMin)) { showBuf = true }
        }

        SectionCard(title = "Alarm & sound", initiallyExpanded = false) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Use built-in alarm (no confirmation)")
                Switch(checked = useInAppAlarm, onCheckedChange = onUseInAppAlarm)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibrate"); Switch(checked = vibrate, onCheckedChange = onVibrate)
            }
            Spacer(Modifier.height(6.dp))
            Text("Volume")
            Slider(
                value = volumePercent.toFloat(),
                onValueChange = { onVolume(it.toInt()) },
                valueRange = 0f..100f,
                steps = 99
            )
            Text(String.format(locale, "%d%%", volumePercent))
            Spacer(Modifier.height(6.dp))
            Button(onClick = onPickSound) { Text("Choose alarm sound…") }
        }

        SectionCard(title = "Reminders", initiallyExpanded = false) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Enable"); Switch(checked = remEnabled, onCheckedChange = onRemEnabled)
            }

            Spacer(Modifier.height(6.dp))
            Text("Days")
            Spacer(Modifier.height(4.dp))
            val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..7) {
                    val selected = i in remDays
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val next = remDays.toMutableSet().apply { if (contains(i)) remove(i) else add(i) }
                            onRemDays(next)
                        },
                        label = { Text(labels[i - 1]) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            ValueRow(
                label = "Time",
                value = run {
                    val is24 = DateFormat.is24HourFormat(ctx)
                    if (is24) "%02d:%02d".format(locale, remTime.hour, remTime.minute)
                    else {
                        val h12 = (remTime.hour % 12).let { if (it == 0) 12 else it }
                        val amPm = if (remTime.hour < 12) "AM" else "PM"
                        "%02d:%02d %s".format(locale, h12, remTime.minute, amPm)
                    }
                },
                onClick = { showRemTime = true }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = onShowTutorial,
                label = { Text("Tutorial") }
            )
            AssistChip(
                onClick = {
                    if (vibrate) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShowMessage()
                },
                label = { Text("More… 🦦") }
            )
        }
    }

    if (showCap) {
        val values = (0..800).map { it / 10f }
        WheelNumberDialog("Cap (hrs)", values, { v -> String.format(locale, "%.1f", v) }, weeklyCap,
            onConfirm = { onWeeklyCap(it); showCap = false }, onDismiss = { showCap = false })
    }
    if (showOt) {
        val values = (0..400).map { it / 10f }
        WheelNumberDialog("OT hours", values, { v -> String.format(locale, "%.1f", v) }, otHours,
            onConfirm = { onOtHours(it); showOt = false }, onDismiss = { showOt = false })
    }
    if (showBreak) {
        WheelIntDialog("Default break (min)", (0..180).toList(), breakMin,
            onConfirm = { onBreakMin(it); showBreak = false }, onDismiss = { showBreak = false })
    }
    if (showBuf) {
        WheelIntDialog("Alarm early (min)", (0..120).toList(), bufferMin,
            onConfirm = { onBufferMin(it); showBuf = false }, onDismiss = { showBuf = false })
    }
    if (showRemTime) {
        WheelTimeDialog(initial = remTime, is24h = DateFormat.is24HourFormat(ctx),
            onConfirm = { onRemTime(it); showRemTime = false }, onDismiss = { showRemTime = false })
    }
}

/* ------------ Value row / Section card (unchanged) ------------ */
@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { expanded = !expanded }) {
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
            }
        }
        if (expanded) {
            HorizontalDivider()
            Column(Modifier.padding(12.dp), content = content)
        }
    }
}

/* ------------ Wheel dialogs (int/float/time) ------------ */
@Composable
private fun WheelIntDialog(
    title: String,
    values: List<Int>,
    initial: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var localIdx by remember { mutableStateOf(values.indexOf(initial).coerceAtLeast(0)) }
    val current = values.getOrElse(localIdx) { values.first() }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(current) }) { Text("Done") } },
        title = { Text(title) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                AndroidView(factory = { ctx ->
                    NumberPicker(ctx).apply {
                        minValue = 0; maxValue = values.lastIndex; value = localIdx
                        setOnValueChangedListener { _, _, newVal -> localIdx = newVal }
                        wrapSelectorWheel = true
                    }
                })
            }
        }
    )
}

@Composable
private fun WheelNumberDialog(
    title: String,
    values: List<Float>,
    display: (Float) -> String,
    initial: Float,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var idx by remember { mutableStateOf(values.indexOfFirst { it == initial }.takeIf { it >= 0 } ?: 0) }
    val labels = remember(values) { values.map(display).toTypedArray() }
    val current = values.getOrElse(idx) { values.first() }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(current) }) { Text("Done") } },
        title = { Text(title) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                AndroidView(factory = { ctx ->
                    NumberPicker(ctx).apply {
                        minValue = 0; maxValue = labels.lastIndex; displayedValues = labels; value = idx
                        setOnValueChangedListener { _, _, newVal -> idx = newVal }
                        wrapSelectorWheel = true
                    }
                })
            }
        }
    )
}

/* ---- Time editor dialog (IN / OUT) ---- */
@Composable
private fun DayEditDialog(
    dayLabel: String,
    initialStart: LocalTime?,
    initialEnd: LocalTime?,
    initialBreakMin: Int,
    quarterStepMinutes: Boolean,
    onClear: () -> Unit,
    onSave: (LocalTime?, LocalTime?, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val is24h = DateFormat.is24HourFormat(LocalContext.current)
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    var brk by remember { mutableStateOf(initialBreakMin.coerceIn(0, 300)) }

    val now = ZonedDateTime.now()
    val fmt = remember(is24h) { DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a") }

    fun labelFor(t: LocalTime?): String =
        t?.let { fmt.format(now.withHour(it.hour).withMinute(it.minute)) } ?: "—"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onSave(start, end, brk) }) { Text("Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(dayLabel) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp, max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Clock-in")
                TimeWheelRow(
                    initial = start ?: LocalTime.of(9, 0),
                    is24h = is24h,
                    quarterSteps = quarterStepMinutes,
                    onChange = { start = it }
                )
                Text("Clock-out")
                TimeWheelRow(
                    initial = end ?: LocalTime.of(17, 0),
                    is24h = is24h,
                    quarterSteps = quarterStepMinutes,
                    onChange = { end = it }
                )

                Spacer(Modifier.height(6.dp))

                Text("Break minutes (0–300)")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    AndroidView(factory = { ctx ->
                        NumberPicker(ctx).apply {
                            minValue = 0; maxValue = 300; value = brk
                            setOnValueChangedListener { _, _, v -> brk = v }
                            wrapSelectorWheel = true
                        }
                    })
                }

                val net = DayManual(start, end, brk).netHours()
                Text("Net counted: ${"%.2f".format(net)} h", fontWeight = FontWeight.SemiBold)
                Text("IN: ${labelFor(start)}   OUT: ${labelFor(end)}")
            }
        }
    )
}

@Composable
private fun TimeWheelRow(
    initial: LocalTime,
    is24h: Boolean,
    quarterSteps: Boolean,
    onChange: (LocalTime) -> Unit
) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var ampm by remember { mutableStateOf(if (initial.hour < 12) 0 else 1) }

    // ← keeps the wheels in sync when parent changes `initial` (e.g., tapping a suggestion)
    LaunchedEffect(initial, is24h) {
        hour = initial.hour
        minute = initial.minute
        ampm = if (initial.hour < 12) 0 else 1
    }

    fun emit() { onChange(LocalTime.of(hour, minute)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            factory = { ctx ->
                NumberPicker(ctx).apply {
                    if (is24h) {
                        minValue = 0; maxValue = 23; value = hour
                        setOnValueChangedListener { _, _, v -> hour = v; emit() }
                    } else {
                        val labels = (1..12).map { it.toString().padStart(2, '0') }.toTypedArray()
                        minValue = 1; maxValue = 12
                        value = (hour % 12).let { if (it == 0) 12 else it }
                        displayedValues = labels
                        setOnValueChangedListener { _, _, v ->
                            val base = v % 12
                            hour = if (ampm == 0) { if (v == 12) 0 else base } else { if (v == 12) 12 else base + 12 }
                            emit()
                        }
                    }
                    wrapSelectorWheel = true
                }
            },
            update = { picker ->
                if (is24h) {
                    if (picker.value != hour) picker.value = hour
                } else {
                    val h12 = (hour % 12).let { if (it == 0) 12 else it }
                    if (picker.value != h12) picker.value = h12
                }
            }
        )
        Spacer(Modifier.width(12.dp))
        AndroidView(
            factory = { ctx ->
                NumberPicker(ctx).apply {
                    if (quarterSteps) {
                        val labels = arrayOf("00", "15", "30", "45")
                        minValue = 0; maxValue = 3
                        displayedValues = labels
                        value = intArrayOf(0, 15, 30, 45).indexOf((minute / 15) * 15).coerceAtLeast(0)
                        setOnValueChangedListener { _, _, v -> minute = intArrayOf(0, 15, 30, 45)[v]; emit() }
                    } else {
                        minValue = 0; maxValue = 59; value = minute
                        setOnValueChangedListener { _, _, v -> minute = v; emit() }
                    }
                    wrapSelectorWheel = true
                }
            },
            update = { picker ->
                if (quarterSteps) {
                    val idx = intArrayOf(0, 15, 30, 45).indexOf((minute / 15) * 15).coerceAtLeast(0)
                    if (picker.value != idx) picker.value = idx
                } else {
                    if (picker.value != minute) picker.value = minute
                }
            }
        )
        if (!is24h) {
            Spacer(Modifier.width(12.dp))
            AndroidView(
                factory = { ctx ->
                    NumberPicker(ctx).apply {
                        minValue = 0; maxValue = 1
                        displayedValues = arrayOf("AM", "PM")
                        value = ampm
                        setOnValueChangedListener { _, _, v ->
                            ampm = v
                            val h = hour % 12
                            hour = if (v == 0) h else h + 12
                            emit()
                        }
                        wrapSelectorWheel = true
                    }
                },
                update = { picker ->
                    if (picker.value != ampm) picker.value = ampm
                }
            )
        }
    }
}

/* -------- WheelTimeDialog -------- */
@Composable
private fun WheelTimeDialog(
    initial: LocalTime,
    is24h: Boolean,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var ampm by remember { mutableStateOf(if (initial.hour < 12) 0 else 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(hour, minute)) }) { Text("Done") } },
        title = { Text("Pick time") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp, max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AndroidView(factory = { ctx ->
                        NumberPicker(ctx).apply {
                            if (is24h) {
                                minValue = 0; maxValue = 23; value = hour
                                setOnValueChangedListener { _, _, v -> hour = v }
                            } else {
                                val labels = (1..12).map { it.toString().padStart(2, '0') }.toTypedArray()
                                minValue = 1; maxValue = 12
                                value = ((hour % 12).let { if (it == 0) 12 else it })
                                displayedValues = labels
                                setOnValueChangedListener { _, _, v ->
                                    val base = v % 12
                                    hour = if (ampm == 0) { if (v == 12) 0 else base } else { if (v == 12) 12 else base + 12 }
                                }
                            }
                            wrapSelectorWheel = true
                        }
                    })
                    Spacer(Modifier.width(12.dp))
                    AndroidView(factory = { ctx ->
                        NumberPicker(ctx).apply {
                            minValue = 0; maxValue = 59; value = minute
                            setOnValueChangedListener { _, _, v -> minute = v }
                            wrapSelectorWheel = true
                        }
                    })
                    if (!is24h) {
                        Spacer(Modifier.width(12.dp))
                        AndroidView(factory = { ctx ->
                            NumberPicker(ctx).apply {
                                minValue = 0; maxValue = 1
                                displayedValues = arrayOf("AM", "PM")
                                value = ampm
                                setOnValueChangedListener { _, _, v ->
                                    ampm = v
                                    val h = hour % 12
                                    hour = if (v == 0) h else h + 12
                                }
                                wrapSelectorWheel = true
                            }
                        })
                    }
                }
            }
        }
    )
}

/* ---------- Bar chart (unchanged) ---------- */
// (kept as in your file)
@Composable
private fun PlannedActualBarChart(
    days: List<String>,
    planned: Map<String, Double>,
    actual: Map<String, Double>,
    height: Dp = 200.dp,
    barGap: Dp = 4.dp,
    groupGap: Dp = 12.dp,
    onBarTap: (dayIndex: Int, isPlanned: Boolean) -> Unit,
    showAsClock: Boolean,
    locale: Locale
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurface

    val maxVal = max(
        planned.values.maxOrNull() ?: 0.0,
        actual.values.maxOrNull() ?: 0.0
    ).let { if (it <= 0.0) 1.0 else it }

    var layout by remember { mutableStateOf<ChartLayout?>(null) }

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        layout?.let { l ->
                            val hit = l.hitTest(offset.x, offset.y)
                            if (hit != null) onBarTap(hit.groupIndex, hit.isPlanned)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val groups = days.size
            val groupGapPx = groupGap.toPx()
            val barGapPx = barGap.toPx()

            val totalGap = groupGapPx * (groups + 1)
            val available = (w - totalGap).coerceAtLeast(0f)
            val barWidth = (available / groups / 2f) - (barGapPx / 2f)
            var x = groupGapPx

            val bars = mutableListOf<BarRect>()

            // baseline
            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.3f),
                start = Offset(0f, h - 2f),
                end = Offset(w, h - 2f),
                strokeWidth = 2f
            )

            val paint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 28f
                color = android.graphics.Color.argb(
                    (255 * 0.9f).toInt(),
                    (labelColor.red * 255).toInt(),
                    (labelColor.green * 255).toInt(),
                    (labelColor.blue * 255).toInt()
                )
            }

            days.forEachIndexed { idx, d ->
                val p = (planned[d] ?: 0.0).toFloat()
                val a = (actual[d] ?: 0.0).toFloat()
                val ph = (p / maxVal.toFloat()) * (h * 0.88f)
                val ah = (a / maxVal.toFloat()) * (h * 0.88f)

                val pTopLeft = Offset(x, h - ph)
                val pSize = Size(barWidth, ph)
                drawRect(color = primary, topLeft = pTopLeft, size = pSize)
                bars += BarRect(idx, true, pTopLeft, pSize)
                if (p > 0f) {
                    drawContext.canvas.nativeCanvas.drawText(
                        fmtBarLabel(locale, p, showAsClock),
                        x + barWidth / 2f,
                        (h - ph) - 6f,
                        paint
                    )
                }

                val aX = x + barWidth + barGapPx
                val aTopLeft = Offset(aX, h - ah)
                val aSize = Size(barWidth, ah)
                drawRect(color = secondary, topLeft = aTopLeft, size = aSize)
                bars += BarRect(idx, false, aTopLeft, aSize)
                if (a > 0f) {
                    drawContext.canvas.nativeCanvas.drawText(
                        fmtBarLabel(locale, a, showAsClock),
                        aX + barWidth / 2f,
                        (h - ah) - 6f,
                        paint
                    )
                }

                x += (barWidth * 2f) + barGapPx + groupGapPx
            }

            layout = ChartLayout(
                groupGapPx = groupGapPx,
                barGapPx = barGapPx,
                barWidth = barWidth,
                bars = bars
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendSwatch(color = MaterialTheme.colorScheme.primary, label = "Planned")
            LegendSwatch(color = MaterialTheme.colorScheme.secondary, label = "Actual")
        }
    }
}

private data class BarRect(
    val groupIndex: Int,
    val isPlanned: Boolean,
    val topLeft: Offset,
    val size: Size
) {
    fun contains(x: Float, y: Float): Boolean =
        x >= topLeft.x && x <= (topLeft.x + size.width) &&
                y >= topLeft.y && y <= (topLeft.y + size.height)
}

private data class ChartLayout(
    val groupGapPx: Float,
    val barGapPx: Float,
    val barWidth: Float,
    val bars: List<BarRect>
) {
    fun hitTest(x: Float, y: Float): BarRect? = bars.firstOrNull { it.contains(x, y) }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(14.dp)
                .graphicsLayer { alpha = 1f }
                .background(color = color, shape = RoundedCornerShape(3.dp))
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

/* ---------- Conversion chart dialog ---------- */
@Composable
private fun HoursChartDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onDismiss) { Text("Close") } },
        title = { Text("Hours ⇄ h:mm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Rules", fontWeight = FontWeight.SemiBold)
                Text("• minutes = hours × 60")
                Text("• hours (decimal) = minutes ÷ 60")

                Spacer(Modifier.height(6.dp))
                Text("Quick chart (0.01 … 0.99 hours)", fontWeight = FontWeight.SemiBold)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 320.dp, max = 520.dp)
                ) {
                    val entries = remember { (1..99).map { it / 100.0 } }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(entries) { h ->
                            val totalMin = (h * 60.0).roundToInt()
                            val mm = totalMin % 60
                            val hh = totalMin / 60
                            val cell = String.format(Locale.US, "%.2f → %d:%02d", h, hh, mm)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    cell,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
/* ---------- Chips for IN/BREAK/OUT (used in non-Actual views) ---------- */
@Composable
private fun EditChips(
    onIn: () -> Unit,
    onBreak: () -> Unit,
    onOut: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ColoredChip("IN",    Color(0xFF2E7D32), onIn)    // green
        ColoredChip("BREAK", Color(0xFFF9A825), onBreak) // yellow
        ColoredChip("OUT",   Color(0xFFC62828), onOut)   // red
    }
}

@Composable
private fun ColoredChip(text: String, color: Color, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text, color = Color.White) },
        colors = AssistChipDefaults.assistChipColors(containerColor = color)
    )
}

/* ---- Small single-time picker dialog (used for IN/OUT) ---- */
@Composable
private fun InOutSingleTimeDialog(
    title: String,
    initial: LocalTime,
    is24h: Boolean,
    onConfirm: (LocalTime) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var time by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onConfirm(time) }) { Text("Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            TimeWheelRow(
                initial = initial,
                is24h = is24h,
                quarterSteps = false,
                onChange = { time = it }
            )
        }
    )
}

/* ---- Break dialog (reordered, default lead=1, no Save button, no 45m pick) ---- */
@Composable
private fun BreakWithStartDialog(
    title: String,
    initialBreak: Int,
    initialStart: LocalTime,
    is24h: Boolean,
    onConfirm: (breakMin: Int) -> Unit, // Save only (no alarm)
    onStartAtWithAlarm: (startAt: LocalTime, breakMin: Int, alarmLeadMin: Int) -> Unit,
    onClear: () -> Unit,                             // ← NEW
    onDismiss: () -> Unit
) {
    var open by remember { mutableStateOf(true) }
    if (!open) return

    var brk by remember { mutableStateOf(initialBreak.coerceIn(0, 120)) }
    var startAt by remember { mutableStateOf(initialStart) }
    var lead by remember { mutableStateOf(1) } // default 1 minute

    AlertDialog(
        onDismissRequest = { open = false; onDismiss() },
        title = { Text(title) },
        confirmButton = {
            Row {
                TextButton(onClick = { onClear(); open = false }) { Text("Clear") } // ← NEW
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onConfirm(brk); open = false }) { Text("Save") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    onStartAtWithAlarm(startAt, brk, lead)
                    open = false
                }) { Text("Save & alarm") }
            }
        },
        dismissButton = { TextButton(onClick = { open = false; onDismiss() }) { Text("Cancel") } },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp, max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Break started at")
                TimeWheelRow(
                    initial = startAt,
                    is24h = is24h,
                    quarterSteps = false,
                    onChange = { startAt = it }
                )

                Text("Quick picks")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { brk = 30 }, label = { Text("30m") })
                    AssistChip(onClick = { brk = 60 }, label = { Text("60m") })
                }

                Text("Break minutes (0–120)")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    AndroidView(factory = { ctx ->
                        NumberPicker(ctx).apply {
                            minValue = 0; maxValue = 120; value = brk
                            setOnValueChangedListener { _, _, v -> brk = v }
                            wrapSelectorWheel = true
                        }
                    })
                }

                Text("Start alarm minutes before break ends")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    AndroidView(factory = { ctx ->
                        NumberPicker(ctx).apply {
                            minValue = 0; maxValue = 60; value = lead
                            setOnValueChangedListener { _, _, v -> lead = v }
                            wrapSelectorWheel = true
                        }
                    })
                }
            }
        }
    )
}

/* ---- Out dialog: default lead=1; suggestions consider break ---- */
@Composable
private fun InDialog(
    title: String,
    initial: LocalTime,
    is24h: Boolean,
    suggestion: LocalTime?,                       // from the other track (Planned ↔ Actual)
    onSave: (LocalTime) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var open by remember { mutableStateOf(true) }
    if (!open) return

    var time by remember { mutableStateOf(initial) }
    val chipFmt = DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "hh:mm a")

    AlertDialog(
        onDismissRequest = { open = false; onDismiss() },
        title = { Text(title) },
        confirmButton = {
            Row {
                TextButton(onClick = { onClear(); open = false }) { Text("Clear") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onSave(time); open = false }) { Text("Save") }
            }
        },
        dismissButton = { TextButton(onClick = { open = false; onDismiss() }) { Text("Cancel") } },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp, max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                suggestion?.let { s ->
                    Text("Suggestion")
                    AssistChip(
                        onClick = { time = s },
                        label = {
                            Text(
                                "Use " + chipFmt.format(
                                    ZonedDateTime.now()
                                        .withHour(s.hour)
                                        .withMinute(s.minute)
                                )
                            )
                        }
                    )
                }

                TimeWheelRow(
                    initial = time,
                    is24h = is24h,
                    quarterSteps = false,
                    onChange = { time = it }
                )
            }
        }
    )
}

@Composable
private fun OutDialog(
    title: String,
    initial: LocalTime,
    is24h: Boolean,
    suggestionA: ZonedDateTime?, // cap-aware suggestion (leave-by − 1m)
    suggestionB: ZonedDateTime?, // planned end
    onSave: (outTime: LocalTime) -> Unit,
    onSaveAndAlarm: (outTime: LocalTime, alarmLeadMin: Int) -> Unit,
    onClear: () -> Unit,                                // ← NEW
    onDismiss: () -> Unit
) {
    var open by remember { mutableStateOf(true) }
    if (!open) return

    var outLt by remember { mutableStateOf(initial) }
    var lead by remember { mutableStateOf(1) }
    val chipFmt = DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "hh:mm a")

    AlertDialog(
        onDismissRequest = { open = false; onDismiss() },
        title = { Text(title) },
        confirmButton = {
            Row {
                TextButton(onClick = { onClear(); open = false }) { Text("Clear") }  // ← NEW
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onSave(outLt); open = false }) { Text("Save") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onSaveAndAlarm(outLt, lead); open = false }) { Text("Save & alarm") }
            }
        },
        dismissButton = { TextButton(onClick = { open = false; onDismiss() }) { Text("Cancel") } },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp, max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (suggestionA != null || suggestionB != null) {
                    Text("Suggestions")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestionA?.let { exact ->
                            AssistChip(
                                onClick = { outLt = exact.toLocalTime() },       // ← no −1 here
                                label = { Text("Use ${exact.format(chipFmt)}") }
                            )
                        }
                        suggestionB?.let { plan ->
                            AssistChip(
                                onClick = { outLt = plan.toLocalTime() },        // ← no math here either
                                label = { Text("Planned ${plan.format(chipFmt)}") }
                            )
                        }
                    }
                }

                TimeWheelRow(
                    initial = outLt,
                    is24h = is24h,
                    quarterSteps = false,
                    onChange = { outLt = it }
                )

                Text("Alarm lead (minutes before)")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    AndroidView(factory = { ctx ->
                        NumberPicker(ctx).apply {
                            minValue = 0; maxValue = 120; value = lead
                            setOnValueChangedListener { _, _, v -> lead = v }
                            wrapSelectorWheel = true
                        }
                    })
                }
            }
        }
    )
}

/* ---------- Persistence helpers, last worked, break inference (unchanged) ---------- */
private fun startOfWeekSaturday(today: LocalDate): LocalDate {
    var d = today
    while (d.dayOfWeek != DayOfWeek.SATURDAY) d = d.minusDays(1)
    return d
}

private fun loadManualWeek(
    ctx: android.content.Context,
    weekKey: String,
    days: List<String>,
    planned: Boolean
): Map<String, DayManual> {
    val sp = ctx.getSharedPreferences("leaveby_prefs", android.content.Context.MODE_PRIVATE)
    val prefix = if (planned) "pmanual" else "amanual"
    val out = mutableMapOf<String, DayManual>()
    for (d in days) {
        val raw = sp.getString("${prefix}_${weekKey}_$d", null) ?: continue
        val parts = raw.split(';')
        val s = parts.getOrNull(0)?.takeIf { it.isNotBlank() && it != "-" }?.let { parseHHmm(it) }
        val e = parts.getOrNull(1)?.takeIf { it.isNotBlank() && it != "-" }?.let { parseHHmm(it) }
        val brk = parts.getOrNull(2)?.toIntOrNull() ?: 0
        out[d] = DayManual(s, e, brk)
    }
    return out
}

private fun saveManualForDay(
    ctx: android.content.Context,
    weekKey: String,
    day: String,
    entry: DayManual?,
    planned: Boolean
) {
    val sp = ctx.getSharedPreferences("leaveby_prefs", android.content.Context.MODE_PRIVATE)
    val prefix = if (planned) "pmanual" else "amanual"
    with(sp.edit()) {
        if (entry == null) {
            remove("${prefix}_${weekKey}_$day")
        } else {
            val s = entry.start?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "-"
            val e = entry.end?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "-"
            putString("${prefix}_${weekKey}_$day", "$s;$e;${entry.breakMin}")
        }
        apply()
    }
}

private fun clearManualWeek(
    ctx: android.content.Context,
    weekKey: String,
    days: List<String>,
    planned: Boolean
) {
    val sp = ctx.getSharedPreferences("leaveby_prefs", android.content.Context.MODE_PRIVATE)
    val prefix = if (planned) "pmanual" else "amanual"
    with(sp.edit()) {
        for (d in days) remove("${prefix}_${weekKey}_$d")
        apply()
    }
}

private fun lastWorkedIndex(p: Parsed, days: List<String>): Int {
    for (i in days.lastIndex downTo 0) {
        val r = p.perDay[days[i]]
        if (r != null && (r.hours > 0.0 || r.startStr != null || r.endStr != null)) return i
    }
    return -1
}

private fun inferBreakMin(start: LocalTime, endRaw: LocalTime, hoursNet: Double): Int {
    val sMin = start.hour * 60 + start.minute
    var eMin = endRaw.hour * 60 + endRaw.minute
    if (eMin < sMin) eMin += 24 * 60
    val spanMin = eMin - sMin
    val netMin = (hoursNet * 60.0)
    val brk = round(spanMin - netMin).toInt().coerceAtLeast(0).coerceAtMost(300)
    return brk
}

/* ---------- Message dialog & hearts overlay (unchanged) ---------- */
@Composable
private fun HeartsMessageDialog(
    onDismiss: () -> Unit,
    enableHeartbeat: Boolean = true
) {
    val seed = remember { Random.nextInt() }
    val ctx = LocalContext.current

// Get a vibrator instance once
    val vibrator = remember(ctx) {
        if (Build.VERSION.SDK_INT >= 31) {
            val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

// Subtle heartbeat pattern: short beat, tiny gap, second beat, long rest (repeats)
    val effect = remember {
        val timings = longArrayOf(
            0L,   // start immediately
            140L,  // beat 1
            350L,  // small gap
            210L,  // beat 2 (slightly longer)
            700L  // rest before next cycle
        )
        val amplitudes = intArrayOf(
            0,    // required placeholder
            100,   // beat 1 intensity (0..255)
            0,    // gap
            70,   // beat 2 intensity (softer than beat 1)
            0     // rest
        )
        // Repeat from the beginning (index 0)
        VibrationEffect.createWaveform(timings, amplitudes, 0)
    }

// Start when enabled, stop on disable or when the dialog leaves composition
    LaunchedEffect(enableHeartbeat) {
        if (enableHeartbeat && vibrator.hasVibrator()) {
            vibrator.vibrate(effect)   // <-- not vibrator.vibrate(vibe = effect)
        } else {
            vibrator.cancel()
        }
    }

    DisposableEffect(true) {
        onDispose { vibrator.cancel() }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize()) {
            HeartsRandomOverlay(
                seed = seed,
                modifier = Modifier.fillMaxSize().zIndex(2f),
                edgePaddingDp = 12.dp,
                minSeparationDp = 22.dp,
                spawnDelayMsRange = 90..320,
                maxOnScreen = 180,
                durationMsRange = 11000..16000,
                heartsFlowersShare = 0.50f     // ← more hearts/flowers
            )
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(28.dp)
                    .zIndex(3f)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("To the little girl who can do anything:", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("keep going—no matter what.", fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
                    Text("I believe in you.", fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text("💜", fontSize = 28.sp, textAlign = TextAlign.Center)
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
            }
        }
    }
}

@Composable
private fun HeartsRandomOverlay(
    seed: Int,
    modifier: Modifier = Modifier,
    edgePaddingDp: Dp = 12.dp,
    minSeparationDp: Dp = 22.dp,
    spawnDelayMsRange: IntRange = 200..500,   // slower spawn
    maxOnScreen: Int = 48,                    // fewer items on screen
    durationMsRange: IntRange = 9000..14000,  // keep them around but not forever
    heartsFlowersShare: Float = 0.50f
) {
    val density = LocalDensity.current
    val rng = remember(seed) { kotlin.random.Random(seed) }
    val items = remember(seed) { mutableStateListOf<Floaty>() }

    var widthPx by remember { mutableStateOf(0f) }
    var heightPx by remember { mutableStateOf(0f) }

    // Drive animation at ~30fps (lighter than withFrameNanos@60fps)
    var tick by remember { mutableStateOf(0L) }

    LaunchedEffect(seed) {
        while (isActive) {
            withFrameNanos { frameTimeNanos: Long ->
                tick = frameTimeNanos
            }
        }
    }
    // Periodic cleanup so the list doesn’t grow unbounded
    LaunchedEffect(Unit) {
        while (isActive) {
            val now = System.nanoTime()
            items.removeAll { now - it.startNanos >= it.durationNanos }
            kotlinx.coroutines.delay(300L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { c ->
                widthPx = c.size.width.toFloat()
                heightPx = c.size.height.toFloat()
            }
    ) {
        if (widthPx <= 0f || heightPx <= 0f) return@Box

        val edgePx = with(density) { edgePaddingDp.toPx() }
        val minSepPx = with(density) { minSeparationDp.toPx() }

        val hearts  = listOf("💜","💖","💗","💘","💝","❤️")
        val flowers = listOf("🌸","🌷","🌼","🌺","🌻","💐")
        val all     = hearts + flowers + listOf("✨","⭐","🌟","💫","🎉","🥳","🌈","🐱","🐾","\uD83D\uDC85","\uD83D\uDC60","\uD83D\uDCF8","\uD83C\uDFA5","\uD83D\uDD8A\uFE0F","\uD83C\uDF66","\uD83C\uDF55","\uD83E\uDD42","\uD83D\uDC15","\uD83D\uDC83\uD83C\uDFFC","\uD83C\uDFB6","\uD83D\uDEA2","\uD83D\uDEE9","\uD83C\uDDEB\uD83C\uDDF7","\uD83D\uDDFD")

        fun pickEmoji(): String =
            if (rng.nextFloat() < heartsFlowersShare)
                (hearts + flowers)[rng.nextInt(hearts.size + flowers.size)]
            else
                all[rng.nextInt(all.size)]

        fun pickX(): Float {
            var x = rng.nextFloat() * (widthPx - edgePx * 2) + edgePx
            repeat(8) {
                val close = items.any { kotlin.math.abs(it.baseX - x) < minSepPx }
                if (!close) return x
                x = rng.nextFloat() * (widthPx - edgePx * 2) + edgePx
            }
            return x
        }

        fun newFloaty(now: Long): Floaty {
            val sizeSp = rng.nextInt(20, 42).toFloat()
            val sizePx = with(density) { sizeSp.sp.toPx() }
            val durMs = rng.nextInt(durationMsRange.first, durationMsRange.last + 1)
            return Floaty(
                id = System.nanoTime(),
                emoji = pickEmoji(),
                baseX = pickX(),
                swayPx = rng.nextInt(12, 60).toFloat(),
                phase = (rng.nextFloat() * (2 * PI)).toFloat(),
                freq = rng.nextFloat() * 2.0f + 2.2f,
                dir = if (rng.nextBoolean()) 1f else -1f,
                startNanos = now,
                durationNanos = durMs * 1_000_000L,
                sizePx = sizePx
            )
        }

        // Spawner (respects maxOnScreen)
        LaunchedEffect(seed, widthPx, heightPx) {
            while (isActive) {
                if (items.size < maxOnScreen) {
                    items += newFloaty(System.nanoTime())
                }
                val gap = rng.nextInt(spawnDelayMsRange.first, spawnDelayMsRange.last + 1)
                kotlinx.coroutines.delay(gap.toLong())
            }
        }
        val bmpCache = remember(seed) { HashMap<String, android.graphics.Bitmap>() }

        fun emojiBitmap(emoji: String, sizePx: Float): android.graphics.Bitmap {
            val key = "$emoji@${sizePx.toInt()}"
            bmpCache[key]?.let { return it }

            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = sizePx
                textAlign = android.graphics.Paint.Align.LEFT
            }
            val bounds = android.graphics.Rect()
            paint.getTextBounds(emoji, 0, emoji.length, bounds)

            val w = (bounds.width() + 8).coerceAtLeast(1)
            val h = (bounds.height() + 12).coerceAtLeast(1)
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(bmp)

            // draw centered horizontally; baseline adjusted by bounds
            val x = (w - bounds.width()) / 2f
            val y = h - bounds.bottom.toFloat()
            c.drawText(emoji, x, y, paint)

            bmpCache[key] = bmp
            return bmp
        }

        // One canvas draws everything (no per-emoji composables/layers)
        val paint = remember {
            android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val now = tick
            items.forEach { f ->
                val elapsed = (now - f.startNanos).coerceAtLeast(0L)
                val p = (elapsed.toFloat() / f.durationNanos).coerceIn(0f, 1f)
                val y = heightPx * (1.05f - p * 1.15f)
                val x = f.baseX + (sin((p * f.freq + f.phase).toDouble()) * f.swayPx * f.dir).toFloat()
                val a = if (p < 0.9f) 0.95f else 0.95f * (1f - (p - 0.9f) / 0.1f)

                paint.alpha = (a * 255f).toInt().coerceIn(0, 255)
                paint.textSize = f.sizePx
                // old
                val bmp = emojiBitmap(f.emoji, f.sizePx)
                drawContext.canvas.nativeCanvas.drawBitmap(
                    bmp,
                    x - bmp.width / 2f,   // center horizontally
                    y - bmp.height,       // anchor at visual baseline
                    null
                )
            }
        }
    }
}
@Composable
private fun TutorialDialog(onClose: () -> Unit) {
    var open by remember { mutableStateOf(true) }
    if (!open) return

    val pages = listOf(
        Triple(
            "Scan your week",
            "Use “Scan schedule (Planned)” and “Scan timecard (Actual)”. The top-right publix.org button opens the site so you can grab your schedule faster. Tip: scanning both Planned and Actual makes suggestions and totals more accurate.",
            "🗓️"
        ),
        Triple(
            "Planned vs Actual",
            "Switch tabs at the top. Planned = schedule, Actual = what you worked. Compare shows both with the Δ (difference).",
            "↔️"
        ),
        Triple(
            "Edit fast",
            "Tap the IN / BREAK / OUT chips, adjust the time, then Save to apply. Suggestions help, but they aren’t always 100% accurate—tweak if something looks off.",
            "✍️"
        ),
        Triple(
            "Leave-by alarm",
            "On ACTUAL for today we suggest a leave-by time that respects your cap and plan. Use Save & alarm to schedule it.",
            "⏰"
        ),
        Triple(
            "Format & conversions",
            "Tap the Format button (two arrows) to switch decimal hours ↔ h:mm. Tap the Grid button to open a quick conversion chart for 0.01–0.99 hours.",
            "📊"
        ),
        Triple(
            "In-app alarms",
            "Use the alarm button to view, add, edit, or delete built-in alarms.",
            "🔔"
        ),
        Triple(
            "Reset",
            "Bottom of the list: Reset Planned and Reset Actual. They clear this week’s scans & edits for that track only.",
            "♻️"
        ),
        Triple(
            "Settings",
            "• Work targets: Weekly cap, OT hours (adds to cap), default break, and alarm lead.\n" +
                    "• Alarm & sound: Built-in alarm (no extra taps), vibrate, volume, choose sound.\n" +
                    "• Reminders: Enable, choose days, set a time.\n" +
                    "• More…: just press it… lol",
            "⚙️"
        )

    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { open = false; onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .heightIn(min = 0.dp, max = 560.dp)
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quick tutorial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { open = false; onClose() }) { Text("Close") }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) { page ->
                    val (title, body, emoji) = pages[page]
                    Column(
                        Modifier.fillMaxSize().padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(emoji, fontSize = 42.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(pages.size) { i ->
                        val selected = i == pagerState.currentPage
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp, vertical = 6.dp)
                                .size(if (selected) 10.dp else 8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        enabled = pagerState.currentPage > 0,
                        onClick = {
                            scope.launch {
                                val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                                pagerState.animateScrollToPage(target)
                            }
                        }
                    ) { Text("Back") }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { open = false; onClose() }) { Text("Skip") }
                        Button(onClick = {
                            scope.launch {
                                val last = pages.lastIndex
                                if (pagerState.currentPage >= last) {
                                    open = false; onClose()
                                } else {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }) { Text(if (pagerState.currentPage == pages.lastIndex) "Done" else "Next") }
                    }
                }
            }
        }
    }
}

/* ---------- simple global for manual alarm picker ---------- */
private var showManualWheel by mutableStateOf(false)
