package com.ekvicancode.asrealasyou.network

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

@Environment(EnvType.CLIENT)
object ClientLifeData {
    var totalDeaths: Int = 0
    var receivedAgeMs: Long = 0L
    var receivedAtMs: Long = System.currentTimeMillis()

    val ageMs: Long
        get() {
            val elapsed = System.currentTimeMillis() - receivedAtMs
            return receivedAgeMs + elapsed
        }

    val ageDays: Long get() = ageMs / (1000L * 60 * 60 * 24)
    val ageYears: Long get() = ageDays / 365L

    fun onSyncReceived(totalDeaths: Int, currentAgeMs: Long) {
        this.totalDeaths = totalDeaths
        this.receivedAgeMs = currentAgeMs
        this.receivedAtMs = System.currentTimeMillis()
    }
}

@Environment(EnvType.CLIENT)
object ClientPacketHandler {
    fun register() {
        ClientPlayNetworking.registerGlobalReceiver(LifeSyncPayload.ID) { payload, _ ->
            ClientLifeData.onSyncReceived(
                payload.totalDeaths,
                payload.currentAgeMs
            )
        }
    }
}