package com.ekvicancode.asrealasyou.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

data class LifeSyncPayload(
    val birthEpochMs: Long,
    val totalDeaths: Int
) : CustomPayload {

    companion object {
        val ID: CustomPayload.Id<LifeSyncPayload> = CustomPayload.Id(
            Identifier.of("asrealasyou", "life_sync")
        )

        val CODEC: PacketCodec<PacketByteBuf, LifeSyncPayload> =
            PacketCodec.of(
                { value, buf ->
                    buf.writeLong(value.birthEpochMs)
                    buf.writeInt(value.totalDeaths)
                },
                { buf ->
                    val birthEpochMs = buf.readLong()
                    val totalDeaths = buf.readInt()
                    LifeSyncPayload(birthEpochMs, totalDeaths)
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

    fun sendLifeData(
        player: ServerPlayerEntity,
        birthEpochMs: Long,
        totalDeaths: Int
    ) {
        ServerPlayNetworking.send(
            player,
            LifeSyncPayload(birthEpochMs, totalDeaths)
        )
    }
}