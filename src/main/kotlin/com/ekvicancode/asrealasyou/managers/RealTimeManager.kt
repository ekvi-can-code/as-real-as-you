package com.ekvicancode.asrealasyou.managers

import java.time.LocalTime
import java.time.ZoneId

object RealTimeManager {
    var daySpeed: Double = 24.0

    fun getCurrentGameTimeExact(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val now = LocalTime.now(zoneId)
        val realMsOfDay = (now.hour * 3600L + now.minute * 60L + now.second) * 1000L +
                now.nano / 1_000_000L

        return if (daySpeed == 24.0) {
            val shiftedMs = (realMsOfDay - 6 * 3600 * 1000L + 86400 * 1000L) % (86400 * 1000L)
            ((shiftedMs.toDouble() / (86400.0 * 1000.0)) * 24000L).toLong()
        } else {
            val realMsPerGameDay = (86400.0 * 1000.0 / daySpeed)
            val positionInGameDay = realMsOfDay % realMsPerGameDay
            val shiftedPosition = (positionInGameDay - realMsPerGameDay * 0.25 + realMsPerGameDay) % realMsPerGameDay
            ((shiftedPosition / realMsPerGameDay) * 24000L).toLong()
        }
    }

    fun gameTimeToRealClock(gameTicks: Long): Triple<Int, Int, Int> {
        val totalRealSeconds = ((gameTicks.toDouble() / 24000.0) * 86400.0).toLong()
        val shiftedSeconds = (totalRealSeconds + 6 * 3600L) % 86400L
        val hours = (shiftedSeconds / 3600L).toInt()
        val minutes = ((shiftedSeconds % 3600L) / 60L).toInt()
        val seconds = (shiftedSeconds % 60L).toInt()
        return Triple(hours, minutes, seconds)
    }
}