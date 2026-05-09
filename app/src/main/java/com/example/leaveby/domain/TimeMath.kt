package com.example.leaveby.domain

import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

data class CalcResult(
    val leaveAt: ZonedDateTime,      // <-- cap boundary (paid+break), no buffer
    val remainingPaidHrs: Double,
    val otOnly: Boolean
)

private fun parseAmPm(t: String, zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    val fmt = DateTimeFormatter.ofPattern("h:mm a")
    val local = LocalTime.parse(t.uppercase().replace("\\s+".toRegex(), " "), fmt)
    return ZonedDateTime.of(LocalDate.now(zone), local, zone)
}

/**
 * Computes the time at which you *reach* the weekly cap.
 * - Rounds PAID minutes **up** to the next whole minute (ceil),
 * - Then adds the unpaid break minutes,
 * - Does **not** subtract any buffer here (UI will show alarm time separately).
 */
fun computeLeaveBy(
    wtdBeforeToday: Double,
    clockInStr: String,
    weeklyCap: Double,
    unpaidBreakMin: Int,
    bufferMin: Int // kept for signature stability; not used in the math
): CalcResult {
    val remainingPaid = (weeklyCap - wtdBeforeToday).coerceAtLeast(0.0)
    val ot = remainingPaid <= 0.0

    val inTime = parseAmPm(clockInStr)
    val capBoundary = if (ot) {
        inTime
    } else {
        val paidMinUp = ceil(remainingPaid * 60.0).toLong()
        inTime.plusMinutes(paidMinUp).plusMinutes(unpaidBreakMin.toLong())
    }

    return CalcResult(leaveAt = capBoundary, remainingPaidHrs = remainingPaid, otOnly = ot)
}
