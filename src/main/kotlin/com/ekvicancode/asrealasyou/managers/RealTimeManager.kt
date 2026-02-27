package com.ekvicancode.asrealasyou.managers

import java.time.ZoneId

object RealTimeManager {
    var daySpeed: Double = 1.0
    private var baseRealMs: Long = System.currentTimeMillis()
    private var baseGameTicks: Long = 0L

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

    fun initBase(zoneId: ZoneId = ZoneId.systemDefault()) {
        val now = java.time.LocalTime.now(zoneId)
        val realMsOfDay = (now.hour * 3_600L + now.minute * 60L + now.second) * 1_000L +
                now.nano / 1_000_000L

        val shiftedMs = (realMsOfDay - 6 * 3_600_000L + 86_400_000L) % 86_400_000L
        baseGameTicks = ((shiftedMs.toDouble() / 86_400_000.0) * 24_000L).toLong()
        baseRealMs = System.currentTimeMillis()
    }

    fun getCurrentGameTimeTicks(): Long {
        val elapsedRealMs = System.currentTimeMillis() - baseRealMs
        val elapsedGameTicks = (elapsedRealMs.toDouble() * daySpeed / realMsPerTick / daySpeed).toLong()
        val ticks = (elapsedRealMs.toDouble() * 24_000.0 * daySpeed / 86_400_000.0).toLong()
        return (baseGameTicks + ticks) % 24_000L
    }

}