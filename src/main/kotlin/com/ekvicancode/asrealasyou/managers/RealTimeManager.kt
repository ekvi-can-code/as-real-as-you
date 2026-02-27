package com.ekvicancode.asrealasyou.managers

import java.time.Instant
import java.time.ZoneId

object RealTimeManager {
    var daySpeed: Double = 24.0

    val realMsPerGameDay: Double
        get() = 86_400_000.0 / daySpeed

    val realMsPerTick: Double
        get() = realMsPerGameDay / 24_000.0

    fun realMsToGameTicks(elapsedMs: Long): Long {
        return (elapsedMs * daySpeed / 3_600.0).toLong()
    }

    fun gameTicksToRealMs(ticks: Long): Long {
        return (ticks * 3_600.0 / daySpeed).toLong()
    }

    fun getCurrentGameTimeExact(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val now = java.time.LocalTime.now(zoneId)
        val realMsOfDay = (now.hour * 3_600L + now.minute * 60L + now.second) * 1_000L +
                now.nano / 1_000_000L

        val msPerDay = realMsPerGameDay
        val shiftedMs = (realMsOfDay - 6 * 3_600_000L + 86_400_000L) % 86_400_000L
        val positionInGameDay = shiftedMs % msPerDay
        return ((positionInGameDay / msPerDay) * 24_000L).toLong()
    }

    fun gameTimeToRealClock(gameTicks: Long): Triple<Int, Int, Int> {
        val realMsFromDawn = gameTicksToRealMs(gameTicks)
        val realMsOfDay = (realMsFromDawn + 6 * 3_600_000L) % 86_400_000L
        val totalSeconds = realMsOfDay / 1_000L
        val hours = (totalSeconds / 3_600L).toInt()
        val minutes = ((totalSeconds % 3_600L) / 60L).toInt()
        val seconds = (totalSeconds % 60L).toInt()
        return Triple(hours, minutes, seconds)
    }
}