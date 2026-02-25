package com.ekvicancode.asrealasyou.network

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

@Environment(EnvType.CLIENT)
object ClientLifeData {
    var birthEpochMs: Long = System.currentTimeMillis()
    var totalDeaths: Int = 0

    val ageMs: Long get() = System.currentTimeMillis() - birthEpochMs
    val ageDays: Long get() = ageMs / (1000L * 60 * 60 * 24)
    val ageYears: Long get() = ageDays / 365L
}

@Environment(EnvType.CLIENT)
object ClientPacketHandler {

    fun register() {
        ClientPlayNetworking.registerGlobalReceiver(LifeSyncPayload.ID) { payload, _ ->
            ClientLifeData.birthEpochMs = payload.birthEpochMs
            ClientLifeData.totalDeaths = payload.totalDeaths
        }
    }
}