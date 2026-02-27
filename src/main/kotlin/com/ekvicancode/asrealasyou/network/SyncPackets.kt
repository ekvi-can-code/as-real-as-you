package com.ekvicancode.asrealasyou.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

data class LifeSyncPayload(
    val totalDeaths: Int,
    val currentAgeMs: Long,
    val daySpeed: Double
) : CustomPayload {

    companion object {
        val ID: CustomPayload.Id<LifeSyncPayload> = CustomPayload.Id(
            Identifier.of("asrealasyou", "life_sync")
        )

        val CODEC: PacketCodec<PacketByteBuf, LifeSyncPayload> =
            PacketCodec.of(
                { value, buf ->
                    buf.writeInt(value.totalDeaths)
                    buf.writeLong(value.currentAgeMs)
                    buf.writeDouble(value.daySpeed)
                },
                { buf ->
                    val totalDeaths = buf.readInt()
                    val currentAgeMs = buf.readLong()
                    val daySpeed = buf.readDouble()
                    LifeSyncPayload(totalDeaths, currentAgeMs, daySpeed)
                }
            )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}

object SyncPackets {

    fun registerServer() {
        PayloadTypeRegistry.playS2C().register(
            LifeSyncPayload.ID,
            LifeSyncPayload.CODEC
        )
    }

    fun sendLifeData(player: ServerPlayerEntity) {
        val data = com.ekvicancode.asrealasyou.managers.LifeSystemManager.getData(player)
        ServerPlayNetworking.send(
            player,
            LifeSyncPayload(
                totalDeaths = data.totalDeaths,
                currentAgeMs = data.currentAgeMs(),
                daySpeed = com.ekvicancode.asrealasyou.managers.RealTimeManager.daySpeed
            )
        )
    }
}