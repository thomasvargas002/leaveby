package com.example.leaveby.data

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

private const val TAG = "LeaveBy"

/* =======================
 * Timecard (Actual) types
 * ======================= */
data class DayRow(
    val date: LocalDate?,
    val startStr: String?,
    val endStr: String?,
    val hours: Double
)

data class Parsed(
    val wtd: Double,
    val perDay: Map<String, DayRow>,
    val weekStart: LocalDate? = null
)

/* =======================
 * Schedule (Planned) types
 * ======================= */
data class PlannedDay(
    val day: String,              // "Sat".."Fri"
    val startStr: String?,        // "10:00 AM"
    val endStr: String?,          // "7:00 PM"
    val hours: Double             // net hours, from “8.5 hours” or range fallback
)

data class ParsedSchedule(
    val weekStart: LocalDate? = null,   // <<< NEW: anchor for UI
    val perDay: Map<String, PlannedDay>
)

/* ==========================================================
 * RAW TEXT (kept for debug/other call sites)
 * ========================================================== */
suspend fun extractText(ctx: Context, uri: Uri): String {
    val img = InputImage.fromFilePath(ctx, uri)
    val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return rec.process(img).await().text
}

/* ==========================================================
 * MAIN TIMECARD PARSER  (Publix timecard weekly view)
 * - Geometry-based mapping Sat..Fri
 * - Prefers per-day “x.x hrs” lines; falls back to time range
 * - Accepts times without minutes (“10 a.m.”) and combines split times
 * ========================================================== */
// --- TIME CARD (Actual) ---
suspend fun parseTimecardFromImage(ctx: Context, uri: Uri): Parsed {
    val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val bmp = loadScaledBitmap(ctx, uri, maxDim = 1600)
    return try {
        val img = InputImage.fromBitmap(bmp, 0)
        val result = rec.process(img).await()

        // line holder w/ geometry
        data class L(val text: String, val top: Int, val left: Int, val height: Int) {
            val y get() = top + height / 2
        }
        fun canon(s: String) = s
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()

        val lines: List<L> = result.textBlocks
            .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
            .flatMap { b -> b.lines.sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 })) }
            .mapNotNull { ln ->
                ln.boundingBox?.let { box ->
                    L(canon(ln.text), box.top, box.left, max(1, box.height()))
                }
            }

        val days = listOf("Sat","Sun","Mon","Tue","Wed","Thu","Fri")

        // regex
        val dName     = """(Sat(?:urday)?|Sun(?:day)?|Mon(?:day)?|Tue(?:sday)?|Wed(?:nesday)?|Thu(?:rsday)?|Fri(?:day)?)"""
        val dayOnlyRe = Regex("""\b$dName\b""", RegexOption.IGNORE_CASE)
        val mdRe      = Regex("""([0-9]{1,2})\s*/\s*([0-9]{1,2})""")

        // accept times with or without minutes
        val timeRe     = Regex("""(?i)\b(\d{1,2})(?::(\d{2}))?\s*([ap]\.?\s*m\.?)\b""")
        val timePairRe = Regex("""(?i)\b(\d{1,2})(?::(\d{2}))?\s*([ap]\.?\s*m\.?)\s*[-–—]\s*(\d{1,2})(?::(\d{2}))?\s*([ap]\.?\s*m\.?)\b""")

        val hrsRe     = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*h\s*?r\s*?s?""", RegexOption.IGNORE_CASE)
        val weekRe    = Regex("""Week[: ]+([0-9]{1,2}/[0-9]{1,2}/[0-9]{4})""", RegexOption.IGNORE_CASE)
        val totalRe   = Regex("""Total\s+Worked[: ]\s*([0-9]+(?:[.,][0-9]+)?)\s*hrs""", RegexOption.IGNORE_CASE)

        val headerWeek: LocalDate? = lines.firstNotNullOfOrNull { l ->
            weekRe.find(l.text)?.groupValues?.getOrNull(1)?.let { parseUsDate(it) }
        }
        val bannerWtd: Double? = lines.firstNotNullOfOrNull { l ->
            totalRe.find(l.text)?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
        }
        val earliestOnPage: LocalDate? = lines.mapNotNull { l ->
            mdRe.find(l.text)?.let { m ->
                val mm = m.groupValues[1]; val dd = m.groupValues[2]
                val y = headerWeek?.year ?: LocalDate.now().year
                parseUsDate("$mm/$dd/$y")
            }
        }.minOrNull()
        val baseWeek = startOfWeekSaturday(headerWeek ?: earliestOnPage ?: LocalDate.now())

        val avgH = lines.map { it.height }.average().toInt().coerceAtLeast(24)
        val timeThresh  = (avgH * 1.6).toInt()
        val hoursThresh = (avgH * 2.6).toInt()

        data class Row(var y: Int? = null, var date: LocalDate? = null)
        val rows = days.associateWith { Row() }.toMutableMap()

        // day names
        for (l in lines) {
            val m = dayOnlyRe.find(l.text) ?: continue
            val abbr = m.groupValues[1].take(3).replaceFirstChar { it.uppercase() }
            if (rows[abbr]?.y == null) rows[abbr]?.y = l.y
        }
        // attach dates
        for (l in lines) {
            val m = mdRe.find(l.text) ?: continue
            val closest = rows.filter { it.value.y != null }
                .minByOrNull { abs((it.value.y ?: 0) - l.y) } ?: continue
            if (abs((closest.value.y ?: 0) - l.y) <= hoursThresh) {
                val y = headerWeek?.year ?: LocalDate.now().year
                val mm = m.groupValues[1]; val dd = m.groupValues[2]
                rows[closest.key]?.date = parseUsDate("$mm/$dd/$y") ?: rows[closest.key]?.date
                rows[closest.key]?.y = ((rows[closest.key]?.y ?: l.y) + l.y) / 2
            }
        }
        if (rows.any { it.value.y == null } && headerWeek != null) {
            for (l in lines) {
                val m = mdRe.find(l.text) ?: continue
                val d = parseUsDate("${m.groupValues[1]}/${m.groupValues[2]}/${headerWeek.year}") ?: continue
                val idx = ((d.toEpochDay() - headerWeek.toEpochDay()).toInt()).coerceIn(0,6)
                val abbr = days[idx]
                if (rows[abbr]?.y == null) { rows[abbr]?.y = l.y; rows[abbr]?.date = d }
            }
        }

        data class TL(val y: Int, val start: String?, val end: String?)
        val timeLines = mutableListOf<TL>()
        for (l in lines) {
            val pair = timePairRe.find(l.text)
            if (pair != null) {
                val s = toAmPmFromParts(pair.groupValues[1], pair.groupValues[2], pair.groupValues[3])
                val e = toAmPmFromParts(pair.groupValues[4], pair.groupValues[5], pair.groupValues[6])
                timeLines += TL(l.y, s, e)
                continue
            }
            val singles = timeRe.findAll(l.text).map {
                toAmPmFromParts(it.groupValues[1], it.groupValues[2], it.groupValues[3])
            }.toList()
            if (singles.size == 1) timeLines += TL(l.y, singles[0], null)
            else if (singles.size >= 2) timeLines += TL(l.y, singles[0], singles[1])
        }
        data class HL(val y: Int, val hours: Double)
        val hourLines = lines.mapNotNull { l ->
            hrsRe.find(l.text)?.groupValues?.getOrNull(1)?.let { v ->
                HL(l.y, v.replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null)
            }
        }

        fun <T> nearest(list: List<T>, centerY: Int, getY: (T) -> Int, thresh: Int): T? {
            if (list.isEmpty()) return null
            val best = list.minByOrNull { abs(getY(it) - centerY) } ?: return null
            return if (abs(getY(best) - centerY) <= thresh) best else null
        }

        val out = linkedMapOf<String, DayRow>()
        days.forEachIndexed { idx, abbr ->
            val r = rows[abbr]!!
            val centerY = r.y
            val date = r.date ?: baseWeek.plusDays(idx.toLong())   // <<< NEW: always set date
            var start: String? = null
            var end: String? = null
            var hours: Double? = null

            if (centerY != null) {
                nearest(hourLines, centerY, { it.y }, hoursThresh)?.let { hours = it.hours }

                val nearTimes = timeLines
                    .filter { abs(it.y - centerY) <= timeThresh }
                    .flatMap { listOfNotNull(it.start, it.end) }
                    .mapNotNull { parseLocalTimeFlex(it) }
                    .sorted()

                if (nearTimes.size >= 2) {
                    start = formatAmPm(nearTimes.first())
                    end   = formatAmPm(nearTimes.last())
                } else if (nearTimes.size == 1) {
                    start = formatAmPm(nearTimes.first())
                    end   = null
                }
            }

            val perDayHours = when {
                hours != null -> hours!!
                start != null && end != null -> rangeHours(start!!, end!!)
                else -> 0.0
            }

            out[abbr] = DayRow(
                date = date,
                startStr = start,
                endStr = end,
                hours = round2(perDayHours)
            )
        }

        val sumRows = out.values.sumOf { it.hours }
        val wtd = round2(bannerWtd ?: sumRows)

        Log.d(TAG, "parsed rows => " + days.joinToString { d ->
            val r = out[d]!!
            "$d=${r.hours} (${r.startStr}–${r.endStr})"
        })

        Parsed(wtd = wtd, perDay = out, weekStart = baseWeek)
    } finally {
        if (!bmp.isRecycled) bmp.recycle()
    }
}

/* ==========================================================
 * PUBLIX SCHEDULE PARSER (Planned) — POSITION-MERGED MATCHING
 * ========================================================== */
// --- SCHEDULE (Planned) ---
suspend fun parsePublixScheduleFromImage(ctx: Context, uri: Uri): ParsedSchedule {
    val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // RAW (scaled)
    val raw = runCatching {
        val bmpRaw = loadScaledBitmap(ctx, uri, maxDim = 1600)
        try {
            rec.process(InputImage.fromBitmap(bmpRaw, 0)).await().text
        } finally {
            if (!bmpRaw.isRecycled) bmpRaw.recycle()
        }
    }.getOrNull() ?: ""

    // PREPROCESSED (grayscale/upscale)
    val pre = runCatching {
        val bmpPre = loadAndPreprocess(ctx, uri)
        try {
            rec.process(InputImage.fromBitmap(bmpPre, 0)).await().text
        } finally {
            if (!bmpPre.isRecycled) bmpPre.recycle()
        }
    }.getOrNull() ?: ""

    val pick = if (pre.length > raw.length) pre else raw

    val clean = pick
        .replace('\u00A0', ' ')
        .replace('\u202F', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()

    Log.d(TAG, "Schedule OCR (normalized):\n$clean")

    val days = listOf("Sat","Sun","Mon","Tue","Wed","Thu","Fri")
    val out = linkedMapOf<String, PlannedDay>().apply { days.forEach { put(it, PlannedDay(it, null, null, 0.0)) } }

    val dayMatches = Regex("""(?i)\b(sat|sun|mon|tue|wed|thu|fri)\b""")
        .findAll(clean)
        .map { it.groupValues[1].replaceFirstChar(Char::uppercase) to it.range.first }
        .toList()
        .sortedBy { it.second }

    if (dayMatches.isEmpty()) {
        Log.w(TAG, "Schedule: no day tokens present.")
        return ParsedSchedule(weekStart = startOfWeekSaturday(LocalDate.now()), perDay = out)
    }
// ---- Week anchor (Saturday) ----
// Base guess from any mm/dd on the page (or today)
    val baseGuess: LocalDate =
        Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\b""").find(clean)?.let {
            val (mm, dd, yy) = it.destructured
            val year = when {
                yy.isBlank()   -> LocalDate.now().year
                yy.length == 2 -> 2000 + yy.toInt()
                else           -> yy.toInt()
            }
            runCatching { LocalDate.of(year, mm.toInt(), dd.toInt()) }.getOrNull()
        } ?: LocalDate.now()

    val baseWeek: LocalDate = run {
        val dayNumRe = Regex("""\b(\d{1,2})\b""")
        var anchor: LocalDate? = null

        for ((abbr, pos) in dayMatches) {               // no withIndex() here
            val i = listOf("Sat","Sun","Mon","Tue","Wed","Thu","Fri").indexOf(abbr)
            if (i < 0) continue

            // Look a few chars after the day token for a day-of-month number
            val sub = clean.substring(pos, minOf(clean.length, pos + 12))
            val m = dayNumRe.find(sub) ?: continue
            val n = m.groupValues[1].toIntOrNull() ?: continue

            // Month/year guess from first mm/dd on page, else today
            val mdyOnPage = Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\b""").find(clean)
            val now = LocalDate.now()
            val monthGuess = mdyOnPage?.groupValues?.getOrNull(1)?.toIntOrNull() ?: now.monthValue
            val yearGuess  = mdyOnPage?.groupValues?.getOrNull(3)?.let { yy ->
                when {
                    yy.isBlank()   -> now.year
                    yy.length == 2 -> 2000 + (yy.toIntOrNull() ?: (now.year % 100))
                    else           -> yy.toIntOrNull() ?: now.year
                }
            } ?: now.year

            val expectedDow = listOf(
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )[i]

            // Try previous/current/next month; pick the one whose DOW matches the token
            val pick = (-1L..1L).mapNotNull { mo ->
                val ref = LocalDate.of(yearGuess, monthGuess, 1).plusMonths(mo)
                runCatching { LocalDate.of(ref.year, ref.month, n) }.getOrNull()
            }.firstOrNull { it.dayOfWeek == expectedDow }

            if (pick != null) { anchor = pick; break }
        }

        startOfWeekSaturday(anchor ?: baseGuess)
    }

    data class Tok(
        val kind: String,              // "time" | "off" | "hours"
        val start24: String? = null,
        val end24: String? = null,
        val hours: Double? = null,
        val pos: Int
    )

    val timeRe = Regex(
        """(?i)\b(\d{1,2})(?::(\d{2}))?\s*(a\.?\s?m\.?|p\.?\s?m\.?|am|pm)\s*[-–—]\s*(\d{1,2})(?::(\d{2}))?\s*(a\.?\s?m\.?|p\.?\s?m\.?|am|pm)\b"""
    )
    val notRe  = Regex("""(?i)\bnot\s*scheduled\b""")
    val hrsRe  = Regex("""(?i)\b(\d+(?:[.,]\d+)?)\s*hours?\b""")

    val tokensAll = mutableListOf<Tok>()
    timeRe.findAll(clean).forEach { m ->
        val s = buildTime24(m.groupValues[1], m.groupValues[2], m.groupValues[3])
        val e = buildTime24(m.groupValues[4], m.groupValues[5], m.groupValues[6])
        val h = runCatching { diffHours24(s, e) }.getOrNull()
        tokensAll += Tok("time", s, e, h, m.range.first)
    }
    notRe.findAll(clean).forEach { m -> tokensAll += Tok("off", pos = m.range.first) }
    hrsRe.findAll(clean).forEach { m ->
        m.groupValues.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
            ?.let { tokensAll += Tok("hours", hours = it, pos = m.range.first) }
    }
    tokensAll.sortBy { it.pos }

    val times = tokensAll.filter { it.kind == "time" }
    val offs  = tokensAll.filter { it.kind == "off" }
    val hours = tokensAll.filter { it.kind == "hours" }

    Log.d(TAG, "Sched tokens: times=${times.size} offs=${offs.size} hours=${hours.size}")

    var iT = 0; var iO = 0; var iH = 0
    fun nextPos(list: List<Tok>, idx: Int) = list.getOrNull(idx)?.pos ?: Int.MAX_VALUE
    fun takeHoursNear(timeTok: Tok): Double? {
        val hTok = hours.getOrNull(iH) ?: return null
        return if (hTok.pos >= timeTok.pos && (hTok.pos - timeTok.pos) <= 80) {
            iH += 1
            hTok.hours
        } else null
    }

    for ((day, _) in dayMatches) {
        val nextO = nextPos(offs, iO)
        val nextT = nextPos(times, iT)
        val nextH = nextPos(hours, iH)

        when {
            nextO < nextT && nextO < nextH -> {
                out[day] = PlannedDay(day, null, null, 0.0)
                iO += 1
            }
            nextT <= nextH -> {
                val t = times.getOrNull(iT)
                if (t != null) {
                    iT += 1
                    val hoursFromNear = takeHoursNear(t)
                    val hrs = round2(hoursFromNear ?: (t.hours ?: 0.0))
                    out[day] = PlannedDay(day, toAmPm(t.start24!!), toAmPm(t.end24!!), hrs)
                } else {
                    val h = hours.getOrNull(iH)
                    if (h != null) {
                        iH += 1
                        out[day] = PlannedDay(day, null, null, round2(h.hours ?: 0.0))
                    }
                }
            }
            else -> {
                val h = hours[iH]
                iH += 1
                out[day] = PlannedDay(day, null, null, round2(h.hours ?: 0.0))
            }
        }
    }

    Log.d(TAG, "Schedule parsed => " + days.joinToString { d ->
        val r = out[d]!!; "$d=${r.hours} (${r.startStr}-${r.endStr})"
    })

    return ParsedSchedule(weekStart = baseWeek, perDay = out)
}

/* ==========================================================
 * OCR fallback used elsewhere (kept for reference)
 * ========================================================== */
private suspend fun recognizeBestText(ctx: Context, uri: Uri): String {
    val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Path 1: RAW
    val rawRes = runCatching { rec.process(InputImage.fromFilePath(ctx, uri)).await() }.getOrNull()
    val rawText = rawRes?.text ?: ""

    // Path 2: PREPROCESSED
    val bmp = runCatching { loadAndPreprocess(ctx, uri) }.getOrNull()
    val preText = if (bmp != null) {
        val t = runCatching { rec.process(InputImage.fromBitmap(bmp, 0)).await() }.getOrNull()
        t?.text ?: ""
    } else ""

    val pick = if (preText.length > rawText.length) preText else rawText
    if (pick.isBlank()) Log.w(TAG, "Schedule OCR empty. raw='${rawText.take(120)}...' pre='${preText.take(120)}...'")
    return pick
}

/* ==========================================================
 * Image preprocessing (rotate per EXIF + upscale + mild grayscale)
 * ========================================================== */
private fun loadAndPreprocess(ctx: Context, uri: Uri): Bitmap {
    val in1 = ctx.contentResolver.openInputStream(uri)!!
    val base = decodeRespectOrientation(in1)
    in1.close()

    val scale = if (base.width < 900) 2.2f else 1.6f
    val scaled = Bitmap.createScaledBitmap(
        base, (base.width * scale).toInt(), (base.height * scale).toInt(), true
    )

    // mild grayscale for readability
    val cm = ColorMatrix().apply { setSaturation(0.2f) }
    val out = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
    val p = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
    Canvas(out).drawBitmap(scaled, 0f, 0f, p)
    return out
}

private fun decodeRespectOrientation(input: InputStream): Bitmap {
    // Need two reads: one for EXIF, one for bitmap
    val bytes = input.readBytes()

    val exif = runCatching { ExifInterface(bytes.inputStream()) }.getOrNull()
    val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ?: ExifInterface.ORIENTATION_NORMAL

    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
    }
    return if (!matrix.isIdentity) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true) else bmp
}

/* ==========================================================
 * Helpers
 * ========================================================== */

private fun String.canonTime(): String {
    var s = this.replace('\u00A0', ' ')
        .replace('\u202F', ' ')
        .replace(".", "")
        .replace(Regex("""\s+"""), " ")
        .trim()
    s = Regex("""\b([AaPp])\s*\.?\s*[Mm]\.?(?)\b""").replace(s) {
        if (it.groupValues[1].equals("a", true)) "AM" else "PM"
    }
    val m = Regex("""([0-9]{1,2}):([0-9]{2})\s*([AP]M)""", RegexOption.IGNORE_CASE).find(s)
        ?: return s.uppercase(Locale.US)
    val hh = m.groupValues[1].toInt().coerceIn(0, 23)
    val mm = m.groupValues[2]
    val am = m.groupValues[3].startsWith("A", true)
    val hh12 = ((hh - 1 + 24) % 12) + 1
    return "${hh12}:${mm} ${if (am) "AM" else "PM"}"
}

// normalize from regex parts to "h:mm AM/PM" (adds :00 if missing)
private fun toAmPmFromParts(hh: String, mmOpt: String?, ampmRaw: String): String {
    val m = (mmOpt ?: "00").padStart(2, '0')
    val ampm = ampmRaw.replace(".", "").replace(" ", "").lowercase(Locale.US) // am or pm
    val h = hh.toInt().coerceIn(1, 12)
    return "%d:%s %s".format(h, m, if (ampm.startsWith("a")) "AM" else "PM")
}

// parse flexible "h a" or "h:mm a"
private fun parseLocalTimeFlex(s: String): LocalTime? {
    val pList = listOf("h:mm a", "h a")
    for (p in pList) {
        val f = DateTimeFormatter.ofPattern(p, Locale.US)
        runCatching { return LocalTime.parse(s.uppercase(Locale.US), f) }.getOrNull()
    }
    return null
}

// format LocalTime back to "h:mm AM/PM"
private fun formatAmPm(t: LocalTime): String {
    val h = (t.hour % 12).let { if (it == 0) 12 else it }
    val ampm = if (t.hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(h, t.minute, ampm)
}

private fun rangeHours(startRaw: String, endRaw: String): Double {
    val s = parseLocalTime(startRaw.canonTime()) ?: return 0.0
    val e0 = parseLocalTime(endRaw.canonTime()) ?: return 0.0
    var e = e0
    if (e.isBefore(s)) e = e.plusHours(24)
    val minutes = Duration.between(s, e).toMinutes().toDouble()
    return round2(minutes / 60.0)
}

private fun parseLocalTime(t: String): LocalTime? = try {
    LocalTime.parse(t.uppercase(Locale.US), DateTimeFormatter.ofPattern("h:mm a", Locale.US))
} catch (_: Throwable) { null }

private fun parseUsDate(mdy: String): LocalDate? = try {
    DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US)
        .withResolverStyle(ResolverStyle.SMART)
        .let { LocalDate.parse(mdy, it) }
} catch (_: Throwable) { null }

private fun round2(v: Double) = round(v * 100.0) / 100.0

// <<< NEW: anchor any date to the Saturday of that week
private fun startOfWeekSaturday(d: LocalDate): LocalDate {
    var x = d
    while (x.dayOfWeek != DayOfWeek.SATURDAY) x = x.minusDays(1)
    return x
}

/* ---------- Schedule-specific helpers ---------- */

private fun buildTime24(hh: String, mm: String?, ampmRaw: String): String {
    val ampm = ampmRaw.replace(".", "").replace(" ", "").lowercase(Locale.US) // am, pm
    val h = hh.toInt().coerceIn(0, 12)
    val m = (mm ?: "00").padStart(2, '0')
    val isAm = ampm.startsWith("a")
    val h24 = when {
        isAm && h == 12 -> 0
        !isAm && h != 12 -> h + 12
        else -> h
    }
    val h2 = h24.toString().padStart(2, '0')
    return "$h2:$m"
}

private fun diffHours24(start24: String, end24: String): Double {
    val s = LocalTime.parse(start24)
    var e = LocalTime.parse(end24)
    if (e.isBefore(s)) e = e.plusHours(24)
    val minutes = Duration.between(s, e).toMinutes().toDouble()
    return round2(minutes / 60.0)
}

private fun toAmPm(hhmm: String): String {
    val t = LocalTime.parse(hhmm)
    val h = t.hour % 12
    val h12 = if (h == 0) 12 else h
    val ampm = if (t.hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(Locale.US, h12, t.minute, ampm)
}
