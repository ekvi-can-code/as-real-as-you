package com.ekvicancode.asrealasyou.managers

import java.time.LocalTime
import java.time.ZoneId
object RealTimeManager {
    fun getCurrentGameTime(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val now = LocalTime.now(zoneId)
        val realMinutesOfDay = now.hour * 60 + now.minute
        val shiftedMinutes = (realMinutesOfDay - 360 + 1440) % 1440
        return ((shiftedMinutes / 1440.0) * 24000L).toLong()
    }

    fun getCurrentGameTimeExact(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val now = LocalTime.now(zoneId)
        val realSecondsOfDay = now.hour * 3600 + now.minute * 60 + now.second
        val shiftedSeconds = (realSecondsOfDay - 21600 + 86400) % 86400
        return ((shiftedSeconds / 86400.0) * 24000L).toLong()
    }
}