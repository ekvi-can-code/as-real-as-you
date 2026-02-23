package com.ekvicancode.asrealasyou.network

import com.ekvicancode.asrealasyou.AsRealAsYou
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

object SyncNetwork {
    private val TIME_SYNC_ID = Identifier("asrealasyou", "time_sync")

    val TIME_SYNC_CODEC = ServerPlayNetworking.createPacketIdCodec(TIME_SYNC_ID)

    fun register() {
        ServerPlayNetworking.registerGlobalReceiver(TIME_SYNC_ID) { server, player, handler, buf, responseSender ->
            val time = buf.readLong()
            AsRealAsYou.realDayTime = time
        }
    }
}
