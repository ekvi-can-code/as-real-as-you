package com.ekvicancode.network

import com.ekvicancode.comps.PlayerLife
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.util.Identifier

object SyncNetwork {
    private val LIFE_SYNC = Identifier("asrealasyou:life")

    data class LifeDataPacket(val days: Long, val years: Long)

    fun register() {
        ServerPlayNetworking.registerGlobalReceiver(LIFE_SYNC) { server, player, _, buf, _ ->
            val packet = PacketCodec.streamCoded(LifeDataPacket::class.java).decode(buf)
        }
    }

    fun sendLifeData(player: PlayerEntity) {
        val lifeComp = PlayerLife.ID.get(player)
        val stats = lifeComp.getStats()

        val buf = PacketByteBufs.create()
        buf.writeLong(stats.daysLived)
        buf.writeLong(stats.yearsLived)

        ServerPlayNetworking.send(player, LIFE_SYNC, buf)
    }
}
