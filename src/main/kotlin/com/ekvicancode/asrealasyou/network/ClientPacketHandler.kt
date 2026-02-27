package com.ekvicancode.asrealasyou.network

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

@Environment(EnvType.CLIENT)
object ClientLifeData {
    var totalDeaths: Int = 0
    var receivedAgeTicks: Long = 0L
    var receivedAtMs: Long = System.currentTimeMillis()
    var receivedDaySpeed: Double = 24.0

    val ageTicks: Long
        get() {
            val elapsedMs = System.currentTimeMillis() - receivedAtMs
            val ticksSinceReceived = (elapsedMs.toDouble() * receivedDaySpeed / 86_400.0).toLong()
            return receivedAgeTicks + ticksSinceReceived
        }

    val ageDays: Long get() = ageTicks / 24_000L
    val ageYears: Long get() = ageDays / 365L

    fun onSyncReceived(totalDeaths: Int, ageTicks: Long, daySpeed: Double) {
        this.totalDeaths = totalDeaths
        this.receivedAgeTicks = ageTicks
        this.receivedAtMs = System.currentTimeMillis()
        this.receivedDaySpeed = daySpeed
    }
}

@Environment(EnvType.CLIENT)
object ClientPacketHandler {
    fun register() {
        ClientPlayNetworking.registerGlobalReceiver(LifeSyncPayload.ID) { payload, _ ->
            ClientLifeData.onSyncReceived(
                payload.totalDeaths,
                payload.ageTicks,
                payload.daySpeed
            )
        }
    }
}